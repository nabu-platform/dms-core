package be.nabu.libs.dms.converters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.datastore.api.DataProperties;
import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

abstract public class DXFConverter implements Converter {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected String transformQuotes(DocumentManager repository, File file, String content, Map<String, String> properties, boolean bestEffort, String targetContentType) throws FormatException, IOException {
		// transform the blockquotes
		Pattern quotePattern = Pattern.compile("(?s)<blockquote[^>]*format[\\s='\"]+([^'\"]+)[^>]*>(.*?)</blockquote>");
		Matcher matcher = quotePattern.matcher(content);
		while (matcher.find()) {
			String targetType = matcher.group().replaceAll(quotePattern.pattern(), "$1");
			String quote = matcher.group().replaceAll(quotePattern.pattern(), "$2");

			String mimeType = URLConnection.guessContentTypeFromName(targetType);
			if (mimeType == null) {
				logger.warn("Could not resolved blockquote type '" + targetType + "' to a mime type");
			}
			else {
				File fragment = new MemoryFileFragment(file, quote.getBytes("UTF-8"), "quote", mimeType);
				Converter converter = repository.getConverter(fragment.getContentType(), targetContentType);
				if (converter == null) {
					logger.warn("Could not find converter from " + fragment.getContentType() + " to " + targetContentType);
				}
				else {
					ByteArrayOutputStream transformedOutput = new ByteArrayOutputStream();
					converter.convert(repository, fragment, transformedOutput, properties);
					quote = new String(transformedOutput.toByteArray(), "UTF-8");
				}
			}
			// we replace the quote either way
			content = content.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<blockquote format=\"" + targetType + "\">" + quote + "</blockquote>"));
		}
		return content;
	}
	
	protected String replaceIncludes(DocumentManager repository, File file, String content, String toContentType, Map<String, String> originalProperties) throws IOException, FormatException {
		Pattern pattern = Pattern.compile("<link[^>]*href[\\s='\"]+([^'\"]+)[^>]*/>");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			// unescape the properties in the url
			// they may have been escape in "general" escaping to prevent injection
			String url = matcher.group().replaceAll(pattern.pattern(), "$1").replaceAll("[/]+$", "").replace("&amp;", "&");
			URI uri;
			try {
				// check if you added parameters
				uri = new URI(URIUtils.encodeURI(url));
			}
			catch (URISyntaxException e) {
				throw new FormatException("The link " + url + " is not of a valid format", e);
			}

			// any parameters
			Map<String, String> properties = WikiToDXF.flatten(URIUtils.getQueryProperties(uri));
			if (originalProperties != null) {
				properties.putAll(originalProperties);
			}
			
			File linkedFile;
			if (uri.getScheme() != null) {
				byte[] bytes;
				DataProperties dataProperties = repository.getDatastore(file).getProperties(uri);
				InputStream retrieve = repository.getDatastore(file).retrieve(uri);
				try {
					bytes = IOUtils.toBytes(IOUtils.wrap(retrieve));
				}
				finally {
					retrieve.close();
				}
				final String path = uri.toString();
				linkedFile = new MemoryFileFragment(file, bytes, URIUtils.getName(uri), dataProperties.getContentType()) {
					@Override
					public String getPath() {
						return path;
					}
				};
			}
			else {
				// just get the path part for the link
				String link = uri.getPath();
				
				
				logger.debug("Resolving link '" + link + "' from file " + file.getPath());
				linkedFile = file.getParent().resolve(link);
				logger.debug("Resolving link '" + link + "' against '" + file.getParent().getPath() + "': " + linkedFile.exists());
			}
						
			if (linkedFile.exists()) {
				Converter converter = repository.getConverter(linkedFile.getContentType(), toContentType);
				logger.debug("Converting " + linkedFile.getPath() + " / " + linkedFile.getContentType() + " to " + toContentType + " using converter: " + converter);
				if (converter == null)
					throw new FormatException("Could not find converter for include from '" + linkedFile.getContentType() + "' to '" + toContentType + "' for: " + matcher.group());
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				converter.convert(repository, linkedFile, output, properties);
				String replacement = new String(output.toByteArray(), "UTF-8");
				
				// shift the headers
				if (properties.containsKey("h")) {
					Integer shift = new Integer(properties.get("h"));
					Pattern headerPattern = Pattern.compile("<h([1-7])>([^<]+)</h[1-7]>");
					Matcher headerMatcher = headerPattern.matcher(replacement);
					while(headerMatcher.find()) {
						int current = new Integer(headerMatcher.group().replaceAll(headerPattern.pattern(), "$1"));
						String replacementHeader = headerMatcher.group().replaceAll(headerPattern.pattern(), "$2");
						current += shift;
						replacement = replacement.replaceAll(Pattern.quote(headerMatcher.group()), Matcher.quoteReplacement("<h" + current + ">" + replacementHeader + "</h" + current + ">"));
					}
				}
				
				// do a recursive resolve of included files, but don't take quotes into account
				Map<String, String> quotes = new HashMap<String, String>();
				replacement = removeQuotes(replacement, quotes);
				replacement = replaceIncludes(repository, linkedFile, replacement, toContentType, originalProperties);
				replacement = addQuotes(replacement, quotes);
				
				// currently don't wrap anything in a span, it breaks css counters because a new counter is started for the span tag
				// so need to fix that or find another solution
				// not sure if it's important to mark referenced content anyway?
//				if (replacement.startsWith("<img ")) {
					content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(replacement));
					
					// replace nested includes
//				}
//				else {
//					String css = "";
//					if (properties.containsKey("float"))
//						css = "style='float:" + properties.get("float") + "' ";
//					content = content.replaceFirst(Pattern.quote(matcher.group()), 
//						Matcher.quoteReplacement("<span title='" + link + "' " + css + "reference='" + url + "'>" + replacement + "</span>"));
//				}
			}
			else
				content = content.replaceFirst(Pattern.quote(matcher.group()),
					Matcher.quoteReplacement("<span reference='" + url + "' class='bad'>failed to import <a class='internal' exists='false' href='" + url + "'>" + url + "</a></span>"));
		}
		return content;
	}
	
	protected String removeQuotes(String content, Map<String, String> quotes) {
		Pattern quotePattern = Pattern.compile("(?s)<blockquote[^>]*format[\\s='\"]+([^'\"]+)[^>]*>(.*?)</blockquote>");
		Matcher matcher = quotePattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			quotes.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[quote=" + uuid + "]");
		}
		return content;
	}
	
	protected String addQuotes(String content, Map<String, String> quotes) {
		for (String uuid : quotes.keySet()) {
			content = content.replaceAll(Pattern.quote("[quote=" + uuid + "]"), Matcher.quoteReplacement(quotes.get(uuid)));
		}
		return content;
	}
}
