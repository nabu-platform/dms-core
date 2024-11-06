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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.xml.XMLUtils;

public class ODTToDXF implements Converter {

	private static Transformer transformer;
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		Document content = getDocument(file, "content.xml");
		
		InputStream xsl = Thread.currentThread().getContextClassLoader().getResourceAsStream("odt2dxf.xsl");
		if (xsl == null)
			throw new RuntimeException("Could not find odt2dxf.xsl");
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			getTransformer().transform(new DOMSource(content), new StreamResult(buffer));
			String converted = new String(buffer.toByteArray(), "UTF-8").trim().replaceAll("\t", TAB_SPACES);
			converted = converted.replaceAll("(?s)(<img[^>]*src[='\"]+)", "$1" + SCHEME_STREAM + ":" + file.getPath() + "/");
			Map<String, String> metadata = getMetadata(file);
			for (String key : metadata.keySet())
				converted = "<meta name='" + key + "' content='" + metadata.get(key) + "'/>" + converted;
			IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(converted.getBytes("UTF-8"))), IOUtils.wrap(output));
		}
		catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		finally {
			xsl.close();
		}
	}
	
	public Map<String, String> getMetadata(File file) throws IOException {
		Map<String, String> metadata = new HashMap<String, String>();
		Document meta = getDocument(file, "meta.xml");
		Element metaElement = (Element) meta.getDocumentElement().getElementsByTagNameNS(DXFToODT.OFFICE, "meta").item(0);
		NodeList nodeList = metaElement.getElementsByTagNameNS(DXFToODT.META, "keyword");
		if (nodeList.getLength() > 0)
			metadata.put("tags", nodeList.item(0).getTextContent());
		return metadata;
	}
	
	private Document getDocument(File root, String child) throws IOException {
		File content = (File) root.resolve(child);
		if (content == null)
			throw new IOException("Can not find child " + child + " in parent " + root.getPath());
		InputStream input = content.getInputStream();
		try {
			return XMLUtils.toDocument(input, true);
		}
		catch (SAXException e) {
			throw new RuntimeException(e);
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		finally {
			input.close();
		}		
	}

	private static Transformer getTransformer() throws TransformerConfigurationException, IOException {
		if (transformer == null) {
			InputStream xsl = Thread.currentThread().getContextClassLoader().getResourceAsStream("odt2dxf.xsl");
			if (xsl == null)
				throw new RuntimeException("Could not find odt2dxf.xsl");
			try {
				transformer = XMLUtils.newTransformer(new StreamSource(xsl), null);
			}
			finally {
				xsl.close();
			}
		}
		return transformer;
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { "application/vnd.oasis.opendocument.text" });
	}

	@Override
	public String getOutputContentType() {
		return DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE;
	}

	@Override
	public boolean isLossless() {
		return false;
	}

}
