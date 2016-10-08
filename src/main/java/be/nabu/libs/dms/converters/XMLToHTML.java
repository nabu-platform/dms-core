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

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class XMLToHTML implements Converter {

	@Override
	public void convert(DocumentManager documentManager, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = FileUtils.toString(file, "UTF-8");
		// encode all tags and the ampersand
		content = content.replaceAll("&", "&amp;");
		
		// replace the fixed double quoted strings so they don't get formatted
		Map<String, String> strings = new HashMap<String, String>();
		Pattern pattern = Pattern.compile("(\".*?(?<!\\\\)\")");
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			strings.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[string=" + uuid + "]");
		}
		
		// replace fixed single quoted strings
		pattern = Pattern.compile("('.*?(?<!\\\\)')");
		matcher = pattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			strings.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[string=" + uuid + "]");
		}
		
		// replace multiline comments
		Map<String, String> comments = new HashMap<String, String>();
		pattern = Pattern.compile("(?s)(<!--.*?-->)");
		matcher = pattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			comments.put(uuid, matcher.group().replaceAll("<", "&lt").replaceAll(">", "&gt;"));
			content = content.replaceAll(Pattern.quote(matcher.group()), "[comment=" + uuid + "]");
		}
	
		// find all the "tags"
		pattern = Pattern.compile("(?s)<[^>]+>");
		matcher = pattern.matcher(content);
		while(matcher.find()) {
			String result = matcher.group().replaceAll(">", "&gt;"); 
			result = result.replaceAll("^<([^\\s>]+)", "<span class='xml-element'>&lt;$1</span>");
			result = result.replaceAll("&gt;$", "<span class='xml-element'>&gt;</span>");
			// replace all attributes
			result = result.replaceAll("(?<=[\\s])(?<!<|<span )([\\w:]+)[\\s]*=[\\s]*", "<span class='xml-attribute'>$1</span> = ");
			content = content.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(result));
		}
		
		// replace comments
		for (String uuid : comments.keySet()) {
			String replacement = comments.get(uuid).replaceAll("(?m)^(.*)$", "<span class='code-comment'>$1</span>");
			content = content.replaceAll(Pattern.quote("[comment=" + uuid + "]"), Matcher.quoteReplacement(replacement));
		}
		
		// replace strings
		for (String uuid : strings.keySet())
			content = content.replaceAll(Pattern.quote("[string=" + uuid + "]"), "<span class='code-string'>" + Matcher.quoteReplacement(strings.get(uuid)) + "</span>");

		// line feeds
		content = "<span class=\"line\">" + content.replaceAll("\r", "").replaceAll("\n", "</span><br/><span class=\"line\">") + "</span>";

		// add surrounding paragraph
		content = "<p>" + content + "</p>";
		
		// tabs
		content = content.replaceAll("\t", TAB_SPACES);
		
		ByteArrayInputStream result = new ByteArrayInputStream(content.getBytes("UTF-8"));
		IOUtils.copyBytes(IOUtils.wrap(result), IOUtils.wrap(output));
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String[] { "application/xml", "text/xml" });
	}

	@Override
	public String getOutputContentType() {
		return "text/html";
	}

	@Override
	public boolean isLossless() {
		return false;
	}

}
