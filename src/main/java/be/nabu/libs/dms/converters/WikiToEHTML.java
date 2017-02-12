package be.nabu.libs.dms.converters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class WikiToEHTML extends WikiToDXF {

	public static final String EDITABLE_HTML = "application/vnd-nabu-ehtml";
	
	@Override
	public void convert(DocumentManager documentManager, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = FileUtils.toString(file, "UTF-8").replaceAll("\r", "");
		
		// find all the "quoted" parts, they have to remain unprocessed
		Map<String, String> quotes = new HashMap<String, String>();
		Pattern quotePattern = Pattern.compile(getQuotePattern());
		Matcher matcher = quotePattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			quotes.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[quote=" + uuid + "]");
		}

		content = escapeXML(content);
		content = replaceAnnotations(content, properties == null || properties.get("annotationDelimiter") == null ? "@" : properties.get("annotationDelimiter"));
		
		// perform a subset of the wiki transforms
		content = replaceHeaders(content);
		content = replaceLists(content);
		content = replaceTables(content);
		content = replaceStyling(content);
		content = replaceParagraphs(content);
		content = removeAnnotationsMarker(content);
		content = replaceEmptyLines(content);
		
		// wrap all includes in a <p>, this will be unwrapped into proper linefeeds in the other direction
		// otherwise the linefeeds before/after includes may disappear on save which will cause invalid dxf/html to be generated
		content = content.replaceAll("(\\[:[^\\]]+])", "<p>$1</p>");
		
		// process the quoted parts
		for (String uuid : quotes.keySet()) {
//			content = content.replaceAll(Pattern.quote("[quote=" + uuid + "]"), Matcher.quoteReplacement(escapeXML(quotes.get(uuid))));
			// the following code replaces the quote with a blockquote but leaves the content unprocessed
			// this poses two challenges though: the reverse converter will try to convert anyway, a boolean could be added to block this
			// however the trickier part is if you want to delete the blockquote or change the formatting
			// if the blockquote is part of the html instead of a wiki command, it is not visible to the user
			String targetType = quotes.get(uuid).replaceAll(quotePattern.pattern(), "$1").replaceAll("^[|]+", "");
			if (targetType.length() == 0)
				targetType = "txt";
			String quote = quotes.get(uuid).replaceAll(quotePattern.pattern(), "$2");
			quote = "<blockquote format='" + targetType + "'>" + escapeXML(quote) + "</blockquote>";
			content = content.replaceAll(Pattern.quote("[quote=" + uuid + "]"), Matcher.quoteReplacement(quote));
		}
		// replace linefeeds
		content = content.replaceAll("\r", "").replaceAll("\n", "<br/>").replaceAll("\t", TAB_SPACES);
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(content.getBytes("UTF-8"))), IOUtils.wrap(output));
	}
	
	protected String replaceLists(String content) {
		content = replaceLists(content, "*", "ul");
		content = replaceLists(content, "#", "ol");
		return content;
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String[] { WIKI_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return EDITABLE_HTML;
	}

	@Override
	public boolean isLossless() {
		return true;
	}
	
}
