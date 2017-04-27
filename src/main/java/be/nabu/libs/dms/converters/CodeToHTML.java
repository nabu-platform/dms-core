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

public class CodeToHTML implements Converter {

	@Override
	public void convert(DocumentManager documentManager, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = FileUtils.toString(file, "UTF-8");
		// encode
		content = content.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
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
		pattern = Pattern.compile("(?s)(/\\*.*?\\*/)");
		matcher = pattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			comments.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[comment=" + uuid + "]");
		}
		// replace single line comments
		pattern = Pattern.compile("(?m)(//.*|#.*|^[\\s]*--[\\s]+.*)$");
		matcher = pattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			comments.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[comment=" + uuid + "]");
		}
		
		// keywords
		content = content.replaceAll("\\b(" + getKeyWordRegex() + ")\\b", "<span class='code-keyword'>$1</span>");

		// methods
		content = content.replaceAll("\\b([\\w.]+)\\(", "<span class='code-method'>$1</span>(");
		
		// replace comments
		for (String uuid : comments.keySet()) {
			String replacement = comments.get(uuid).replaceAll("(?m)^(.*)$", "<span class='code-comment'>$1</span>");
			content = content.replaceAll(Pattern.quote("[comment=" + uuid + "]"), Matcher.quoteReplacement(replacement));
		}
		
		// replace strings
		for (String uuid : strings.keySet())
			content = content.replaceAll(Pattern.quote("[string=" + uuid + "]"), "<span class='code-string'>" + Matcher.quoteReplacement(strings.get(uuid)) + "</span>");

		// line feeds
		content = "<span class='line'>" + content.replaceAll("\r", "").replaceAll("\n", "</span><br/><span class='line'>") + "</span>";

		// wrap labels
		content = content.replaceAll("(<span class='line'>)([\\w]+):", "$1<span class='code-label'>$2</span>:"); 

		// add surrounding paragraph
		content = "<p>" + content + "</p>";
		
		// tabs
		content = content.replaceAll("\t", TAB_SPACES);
		
		ByteArrayInputStream result = new ByteArrayInputStream(content.getBytes("UTF-8"));
		IOUtils.copyBytes(IOUtils.wrap(result), IOUtils.wrap(output));
	}

	protected String [] getKeyWords() {
		return keyWords;
	}
	
	private static String getKeyWordRegex() {
		StringBuilder builder = new StringBuilder();
		for (String keyWord : keyWords)
			builder.append(builder.length() == 0 ? "" : "|").append(keyWord);
		return builder.toString();
	}
	
	private static String [] keyWords = new String [] {
		"abstract",
		"continue",
		"for",
		"new",
		"switch",
		"assert",
		"default",
		"goto",
		"package",
		"synchronized",
		"boolean",
		"do",
		"if",
		"private",
		"this",
		"break",
		"double",
		"implements",
		"protected",
		"throw",
		"byte",
		"else",
		"import",
		"public",
		"throws",
		"case",
		"enum",
		"instanceof",
		"return",
		"transient",
		"catch",
		"extends",
		"int",
		"short",
		"try",
		"char",
		"final",
		"interface",
		"static",
		"void",
		"class",
		"finally",
		"long",
		"strictfp",
		"volatile",
		"const",
		"float",
		"native",
		"super",
		"while",
		"select",
		"from",
		"where",
		"insert",
		"delete",
		"update"
	};

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] {
				"application/bat",
				"text/x-script.php",
				"text/x-java-source",
				"text/x-c",
				"application/x-javascript",
				"application/json",
				"text/x-script.python",
				"text/x-script.glue",
				"text/x-script.scheme",
				"text/x-script.sh",
				"text/x-script.tcl",
				"text/x-script.tcsh",
				"text/x-script.zsh",
				"application/x-sql"
		});
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
