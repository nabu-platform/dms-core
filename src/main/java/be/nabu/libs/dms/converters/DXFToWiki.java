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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.xml.XMLUtils;

public class DXFToWiki implements Converter {

	private Transformer transformer;
	
	public static final String SERVER = "server";
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String html = FileUtils.toString(file, "UTF-8").replaceAll("[\r\n]", "");
		html = "<body>" + html  + "</body>";
		// resolve local images if there is a server property
		if (properties != null && properties.containsKey(SERVER)) {
			Pattern pattern = Pattern.compile("(<img[^<]*src[\\s]*=[\\s]*(?:'|\"))([^'\"]+)");
			Matcher matcher = pattern.matcher(html);
			String server = properties.get(SERVER);
			if (!server.endsWith("/"))
				server += "/";
			while(matcher.find()) {
				String preamble = matcher.group().replaceAll(pattern.pattern(), "$1");
				String src = matcher.group().replaceAll(pattern.pattern(), "$2");
				if (src.startsWith(server + "download/") || src.startsWith("download/")) {
					String path = src.replaceAll(".*\\?path=(.*)", "$1");
					// try to relativize against current path
					path = URIUtils.relativize(file.getPath(), path);
					html = html.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(preamble + SCHEME_STREAM + ":" + path));
				}
			}
		}
		
		// process quoted parts
		Pattern pattern = Pattern.compile("(?s)<blockquote([^>]*)>(.*?)</blockquote>");
		Matcher matcher = pattern.matcher(html);
		Map<String, String> quotes = new HashMap<String, String>();
		while (matcher.find()) {
			String params = matcher.group().replaceAll(pattern.pattern(), "$1");
			String content = matcher.group().replaceAll(pattern.pattern(), "$2");
			// default format
			String format = "txt";
			if (params.matches(".*\\bformat[\\s]*=.*"))
				format = params.replaceAll(".*\\bformat[\\s]*=[\\s]*(?:'|\")([^'\"]*).*", "$1");
			String uuid = UUID.randomUUID().toString();
			html = html.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement("[quote=" + uuid + "]"));
			quotes.put(uuid + ";" + format, content);
		}
		html = html.replaceAll("&#160;", " ").replaceAll("&nbsp;", " ").replaceAll("&#x9;", "	");
		ByteArrayInputStream input = new ByteArrayInputStream(html.getBytes("UTF-8"));
		InputStream xsl = Thread.currentThread().getContextClassLoader().getResourceAsStream(getXSLT());
		if (xsl == null)
			throw new IOException("Could not find: " + getXSLT());
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			getTransformer().transform(new StreamSource(input), new StreamResult(buffer));
		}
		catch (TransformerException e) {
			throw new FormatException(e);
		}
		String result = new String(buffer.toByteArray(), "UTF-8");
		result = result.replaceAll("\r", "");
		// clean up linefeeds
		result = result.replaceAll("[\n]{3,}", "\n\n");
		// remove leading empty lines
		result = result.replaceFirst("(?s)^[\n]+", "");
		// unescape
		result = result.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
		
		for (String key : quotes.keySet()) {
			String targetType = key.replaceAll("^.*;", "");
			String uuid = key.replaceAll(";.*$", "");
			String quote = quotes.get(key);
			// unescape
			quote = quote.replaceAll("<br[^>]*/>", "\n").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&"); 
//			MemoryFileFragment fragment = new MemoryFileFragment(file, quote.getBytes("UTF-8"), "quote", DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE);
//			Converter converter = repository.getConverter(fragment.getContentType(), URLConnection.guessContentTypeFromName(targetType));
//			ByteArrayOutputStream transformed = new ByteArrayOutputStream();
//			converter.convert(repository, fragment, transformed, null);
//			String quote = new String(transformed.toByteArray(), "UTF-8");
			result = result.replaceAll(Pattern.quote("[quote=" + uuid + "]"), createQuote(targetType, quote));
		}
		
		result = postProcessQuotes(result);
				
		// clean up linefeeds
		result = result.replaceAll("[\n]{3,}", "\n\n");
				
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(result.getBytes("UTF-8"))), IOUtils.wrap(output));
	}

	protected String getXSLT() {
		return "dxf2wiki.xsl";
	}

	protected String postProcessQuotes(String result) {
		// quotes after one another do not have the appropriate linefeeds
		result = result.replace("[/quote][quote", "[/quote]\n\n[quote");
		return result;
	}

	protected String createQuote(String targetType, String quote) {
		return "\n[quote" + (targetType.equals("txt") ? "" : "|" + targetType) + "]\n" + Matcher.quoteReplacement(quote) + "\n[/quote]\n";
	}

	private Transformer getTransformer() throws TransformerConfigurationException, IOException {
		if (transformer == null) {
			InputStream xsl = Thread.currentThread().getContextClassLoader().getResourceAsStream(getXSLT());
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
	
	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return WikiToDXF.WIKI_CONTENT_TYPE;
	}

	@Override
	public boolean isLossless() {
		return true;
	}

}
