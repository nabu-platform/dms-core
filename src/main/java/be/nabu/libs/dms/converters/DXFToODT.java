/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.dms.converters;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.xml.XMLUtils;
import be.nabu.utils.xml.XPath;

public class DXFToODT extends DXFConverter {

	public static final String XMLNS = "http://www.w3.org/2000/xmlns/";
	
	public static final String OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
	public static final String META = "urn:oasis:names:tc:opendocument:xmlns:meta:1.0";
	public static final String XLINK = "http://www.w3.org/1999/xlink";
	public static final String PURL = "http://purl.org/dc/elements/1.1/";
	public static final String MANIFEST = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0";
	public static final String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
	public static final String TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
	public static final String DRAWING = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0";
	public static final String STYLE = "urn:oasis:names:tc:opendocument:xmlns:style:1.0";
	public static final String FO = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0";
	public static final String SVG = "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0";
	
	private static Transformer transformer;

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return "application/vnd.oasis.opendocument.text";
	}
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = "<body>" + FileUtils.toString(file, "UTF-8") + "</body>";
		
		System.out.println("BEFORE: " + content);
		content = replaceIncludes(repository, file, content, DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE, properties);
		
		// if the format does not have a converter to odt, just paste it verbatim
		// this may use intermediate converters to convert blockquotes directly to odt format (a zip) which means blockquotes are their own zips, embedded in the text content of the parent
//		content = transformQuotes(repository, file, content, properties, true);
		Pattern quotePattern = Pattern.compile("(?s)<blockquote[^>]*format[\\s='\"]+([^'\"]+)[^>]*>(.*?)</blockquote>");
		Matcher quoteMatcher = quotePattern.matcher(content);
		while (quoteMatcher.find()) {
			String targetType = quoteMatcher.group().replaceAll(quotePattern.pattern(), "$1");
			String quote = quoteMatcher.group().replaceAll(quotePattern.pattern(), "$2").replaceAll("&", "&amp;")
				.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
				.replaceAll("\t", TAB_SPACES).replaceAll(" ", "&nbsp;")
				.replaceAll("\n", "<br/>");
			content = content.replaceAll(Pattern.quote(quoteMatcher.group()), Matcher.quoteReplacement("<blockquote format=\"" + targetType + "\">" + quote + "</blockquote>"));
		}
		
		System.out.println(content);
		// fix the spaces
		content = content.replaceAll("&nbsp;", "&#160;");
		
		// create a zip
		ZipOutputStream zip = new ZipOutputStream(output);
		try {
			// need to replace all the "img" references to something stored internally, additionally need to download the images and add them to the manifest
			Pattern pattern = Pattern.compile("(<img[^>]+?src[\\s='\"]+)([^'\"]+)('|\")");
			Matcher matcher = pattern.matcher(content);
			int mediaCounter = 1;
			while(matcher.find()) {
				try {
					String src = matcher.group().replaceAll(pattern.pattern(), "$2");
					if (src.startsWith("data:"))
						throw new FormatException("The dxf > odt converter does not support embedded images at this time");
					URI uri = new URI(URIUtils.encodeURI(src));
					String extension = uri.getPath().replaceAll("^.*\\.([^.]+)$", "$1").toLowerCase();
					InputStream imageContent = null;
					// no scheme defined, we're gonna assume it's a path in the local repo
					// alternatively you could reference an image on a remote site (e.g. src="http://example.com/image.gif")
					if (uri.getScheme().equals(SCHEME_STREAM)) {
						File image = file.getParent().resolve(uri.getPath());
						if (!image.exists())
							throw new IOException("Could not find local image: " + image.getPath());
						imageContent = image.getInputStream();
					}
					else
						imageContent = uri.toURL().openStream();
					
					// we need to know the dimensions of the image for the odt
					try {
						BufferedImage image = ImageIO.read(imageContent);
						// write the content to the zip
						String fileName = "media/image" + mediaCounter++ + "." + extension;
						ZipEntry entry = new ZipEntry(fileName);
						zip.putNextEntry(entry);
						ImageIO.write(image, extension, zip);
						// replace the occurence in the html with a proper reference which can be used by the xsl
						// uses default 96 dpi
						float width = image.getWidth() * 0.0104f,
							height = image.getHeight() * 0.0104f;
						// max A4 width in inches = 8.27
						if (width > 6) {
							// adjust the height to match dimensions
							height = height / (width / 6);
							width = 6;
						}
						// max A4 height in inches = 11.69
						if (height > 10) {
							// adjust the width to match dimensions
							width = width / (height / 10);
							height = 10;
						}
						String newUri = matcher.group().replaceAll(pattern.pattern(), "$1" + fileName + "$3 width='" + width + "' height='" + height + "'");
						content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(newUri));
					}
					finally {
						imageContent.close();
					}
				}
				catch (URISyntaxException e) {
					throw new FormatException(e);
				}
			}
//			content = content.replaceAll("\n", "");
			
			// extract the metadata that remains
			Map<String, String> metadata = new HashMap<String, String>();
			pattern = Pattern.compile("<meta[^>]+>");
			matcher = pattern.matcher(content);
			
			// no need to remove the metadata from the html because it will be ignored by the xsl
			while (matcher.find()) {
				String name = matcher.group().replaceAll(".*name[\\s=]+(?:'|\")([^'\"]+).*", "$1");
				String value = matcher.group().replaceAll(".*content[\\s=]+(?:'|\")([^'\"]+).*", "$1");
				metadata.put(name, value);
			}
			
			// When you have multiple spans after one another (due to formatting), there is no space in between them
			// or better put: the space is ignored
			// add an explicit space between two consecutive spans
			
			// transform input
			InputStream input = new ByteArrayInputStream(content.getBytes("UTF-8"));
			ByteArrayOutputStream transformed = new ByteArrayOutputStream();
			getTransformer().transform(new StreamSource(input), new StreamResult(transformed));

			ZipEntry entry = new ZipEntry("styles.xml");
			zip.putNextEntry(entry);
			Document styles = createStyles(); 
			dump(styles, zip);

			content = new String(transformed.toByteArray(), "UTF-8");
//			content = content.replaceAll("(?s)(</text:span>[\\s]*)(<text:span)", "$1<text:s/>$2");
			entry = new ZipEntry("content.xml");
			zip.putNextEntry(entry);
			dump(createContent(content, styles), zip);
			
			entry = new ZipEntry("meta.xml");
			zip.putNextEntry(entry);
			dump(createMeta(metadata), zip);
			
			entry = new ZipEntry("settings.xml");
			zip.putNextEntry(entry);
			dump(createSettings(), zip);
			
			entry = new ZipEntry("mimetype");
			zip.putNextEntry(entry);
			IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream("application/vnd.oasis.opendocument.text".getBytes("UTF-8"))), IOUtils.wrap(zip));
			
			entry = new ZipEntry("META-INF/manifest.xml");
			zip.putNextEntry(entry);
			dump(createManifest(), zip);
			
			zip.finish();
		}
		catch (TransformerException e) {
			throw new FormatException(e);
		}
		catch (ParserConfigurationException e) {
			throw new FormatException(e);
		}
		catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		catch (SAXException e) {
			throw new FormatException(e);
		}
	}

	private static Transformer getTransformer() throws TransformerConfigurationException, IOException {
		if (transformer == null) {
			InputStream xsl = Thread.currentThread().getContextClassLoader().getResourceAsStream("dxf2odt.xsl");
			if (xsl == null)
				throw new RuntimeException("Could not find dxf2odt.xsl");
			try {
				transformer = XMLUtils.newTransformer(new StreamSource(xsl), null);
			}
			finally {
				xsl.close();
			}
		}
		return transformer;
	}
	
	private void dump(Document document, OutputStream output) throws TransformerException {
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.transform(new DOMSource(document), new StreamResult(output));
	}
	
	private Document createMeta(Map<String, String> metadata) throws ParserConfigurationException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Document document = XMLUtils.newDocument(true);
		Element root = document.createElementNS(OFFICE, "office:document-meta");
		root.setAttributeNS(XMLNS, "xmlns:office", OFFICE);
		root.setAttributeNS(XMLNS, "xmlns:meta", META);
		root.setAttributeNS(XMLNS, "xmlns:dc", PURL);
		root.setAttributeNS(OFFICE, "office:version", "1.1");
		document.appendChild(root);
		
		Element meta = document.createElementNS(OFFICE, "office:meta");
		root.appendChild(meta);
		
		Element tmp = document.createElementNS(META, "meta:generator");
		tmp.appendChild(document.createTextNode("Nabu/1.0"));
		meta.appendChild(tmp);
		
		tmp = document.createElementNS(META, "meta:initial-creator");
		tmp.appendChild(document.createTextNode("Nabu"));
		meta.appendChild(tmp);
		
		// add tags to the keyword if available
		if (metadata.containsKey("tags")) {
			tmp = document.createElementNS(META, "meta:keyword");
			tmp.appendChild(document.createTextNode(metadata.get("tags")));
			meta.appendChild(tmp);
		}
		
		// add title if available
		if (metadata.containsKey("title")) {
			tmp = document.createElementNS(PURL, "dc:title");
			tmp.appendChild(document.createTextNode(metadata.get("title")));
			meta.appendChild(tmp);
		}
		
		// add subject if available
		if (metadata.containsKey("subject")) {
			tmp = document.createElementNS(PURL, "dc:subject");
			tmp.appendChild(document.createTextNode(metadata.get("subject")));
			meta.appendChild(tmp);
		}
		
		tmp = document.createElementNS(PURL, "dc:creator");
		tmp.appendChild(document.createTextNode("Nabu"));
		meta.appendChild(tmp);
		
		tmp = document.createElementNS(META, "meta:creation-date");
		tmp.appendChild(document.createTextNode(formatter.format(new Date())));
		meta.appendChild(tmp);
		
		tmp = document.createElementNS(PURL, "dc:date");
		tmp.appendChild(document.createTextNode(formatter.format(new Date())));
		meta.appendChild(tmp);
		
		return document;
	}
	
	private Document createStyles() throws ParserConfigurationException, SAXException, IOException {
		InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("oo-styles.xml");
		try {
			return XMLUtils.toDocument(input, true);
		}
		finally {
			input.close();
		}
//		Document document = XmlUtils.newDocument();
//		Element root = document.createElementNS(OFFICE, "office:document-styles");
//		root.setAttributeNS(XMLNS, "xmlns:office", OFFICE);
//		document.appendChild(root);
//		
//		Element tmp = document.createElementNS(OFFICE, "office:font-face-decls");
//		root.appendChild(tmp);
//		
//		tmp = document.createElementNS(OFFICE, "office:styles");
//		root.appendChild(tmp);
//		Element style = document.createElementNS(STYLE, "style:style");
//		style.setAttributeNS(STYLE, "style:family", "graphic");
//		style.setAttributeNS(STYLE, "style:name", "Graphics");
//		tmp.appendChild(style);
//		
//		tmp = document.createElementNS(OFFICE, "office:automatic-styles");
//		root.appendChild(tmp);
//		
//		tmp = document.createElementNS(OFFICE, "office:master-styles");
//		root.appendChild(tmp);
//		
//		return document;
	}
	
	private Document createSettings() throws ParserConfigurationException {
		Document document = XMLUtils.newDocument(true);
		Element root = document.createElementNS(OFFICE, "office:document-settings");
		root.setAttributeNS(XMLNS, "xmlns:office", OFFICE);
		root.setAttributeNS(OFFICE, "office:version", "1.1");
		document.appendChild(root);
		return document;
	}
	
	private Document createManifest(String...attachments) throws ParserConfigurationException {
		Document document = XMLUtils.newDocument(true);
		Element root = document.createElementNS(MANIFEST, "manifest:manifest");
		root.setAttributeNS(XMLNS, "xmlns:manifest", MANIFEST);
		document.appendChild(root);
		// append the default files
		addFileEntry(root, "/", "application/vnd.oasis.opendocument.text");
		addFileEntry(root, "mimetype", "text/plain");
		addFileEntry(root, "content.xml", "text/xml");
		addFileEntry(root, "settings.xml", "text/xml");
		addFileEntry(root, "styles.xml", "text/xml");
		addFileEntry(root, "META-INF/manifest.xml", "text/xml");
		addFileEntry(root, "meta.xml", "text/xml");

		// append the attachments
		for (String attachment : attachments)
			addFileEntry(root, attachment, ContentTypeMap.getInstance().getContentTypeFor(attachment));
		
		return document;
	}
	
	private void addFileEntry(Element parent, String fullPath, String mediaType) {
		Element entry = parent.getOwnerDocument().createElementNS(MANIFEST, "manifest:file-entry");
		entry.setAttributeNS(MANIFEST, "manifest:full-path", fullPath);
		entry.setAttributeNS(MANIFEST, "manifest:media-type", mediaType);
		parent.appendChild(entry);
	}
	
	private Document createContent(String content, Document stylesXml) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		Document contentDoc = XMLUtils.toDocument(new ByteArrayInputStream(content.getBytes("UTF-8")), true);
			
		Document document = XMLUtils.newDocument(true);
		Element root = document.createElementNS(OFFICE, "office:document-content");
		root.setAttributeNS(XMLNS, "xmlns:office", OFFICE);
		root.setAttributeNS(XMLNS, "xmlns:text", TEXT);
		root.setAttributeNS(XMLNS, "xmlns:table", TABLE);
		root.setAttributeNS(XMLNS, "xmlns:drawing", DRAWING);
		root.setAttributeNS(XMLNS, "xmlns:xlink", XLINK);
		root.setAttributeNS(XMLNS, "xmlns:style", STYLE);
		root.setAttributeNS(XMLNS, "xmlns:svg", SVG);
		document.appendChild(root);
		
		Element tmp = document.createElementNS(OFFICE, "office:font-face-decls");
		root.appendChild(tmp);
		
		NodeList result = new XPath("//office:automatic-styles").query(stylesXml).asNodeList();
		if (result.getLength() == 0)
			throw new RuntimeException("Could not find automatic styles in styles.xml");
		Element styles = (Element) document.importNode(result.item(0), true); //document.createElementNS(OFFICE, "office:automatic-styles");
		// import the automatic styles from styles.xml
		root.appendChild(styles);
		Element style = document.createElementNS(STYLE, "style:style");
		style.setAttributeNS(STYLE, "style:family", "graphic");
		style.setAttributeNS(STYLE, "style:name", "wrappedImage");
		style.setAttributeNS(STYLE, "style:parent-style-name", "Graphics");
		styles.appendChild(style);
		tmp = document.createElementNS(STYLE, "style:graphic-properties");
//		tmp.setAttributeNS(TEXT, "text:anchor-type", "paragraph");
		tmp.setAttributeNS(STYLE, "style:wrap", "none");
		tmp.setAttributeNS(STYLE, "style:horizontal-rel", "paragraph");
		tmp.setAttributeNS(STYLE, "style:vertical-rel", "paragraph");
		tmp.setAttributeNS(STYLE, "style:horizontal-pos", "center");
		tmp.setAttributeNS(STYLE, "style:vertical-pos", "bottom");
//		tmp.setAttributeNS(SVG, "svg:x", "0in");
//		tmp.setAttributeNS(SVG, "svg:y", "0in");
		style.appendChild(tmp);
		
		root.appendChild(document.importNode(contentDoc.getDocumentElement(), true));
		return document;
	}

	@Override
	public boolean isLossless() {
		return false;
	}
}
