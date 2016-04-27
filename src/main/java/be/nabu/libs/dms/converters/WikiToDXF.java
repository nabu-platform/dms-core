package be.nabu.libs.dms.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.dms.FileUtils;
import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class WikiToDXF implements Converter {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String WIKI_CONTENT_TYPE = "application/vnd-nabu-wiki";
	
	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String[] { WIKI_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE;
	}
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		logger.debug("Converting file '" + file + "'");
		String content = FileUtils.toString(file, "UTF-8").replaceAll("\r", "");
		
		// find all the "quoted" parts, they have to be processed afterwards
		Map<String, String> quotes = new HashMap<String, String>();
		Pattern quotePattern = Pattern.compile(getQuotePattern());
		Matcher matcher = quotePattern.matcher(content);
		while (matcher.find()) {
			String uuid = UUID.randomUUID().toString();
			quotes.put(uuid, matcher.group());
			content = content.replaceAll(Pattern.quote(matcher.group()), "[quote=" + uuid + "]");
		}

		// preprocessing
		content = escapeXML(content);
		content = replaceAnnotations(content);
		
		// converting
		content = replaceHeaders(content);
		content = replaceLists(content);
		content = replaceTables(content);
		content = replaceStyling(content);
		content = replaceParagraphs(content);
		content = replaceExternalLinks(content);
		content = replaceAnchorLinks(content);
		content = replaceLocalLinks(file, content);
		content = replaceAnchors(content);
		content = removeAnnotationsMarker(content);
		content = replaceEmptyLines(content);
//		content = replaceIncludes(repository, file, content);
		content = content.replaceAll("(?<!\\\\)\\[:([^\\]]+)\\]", "<link href=\"$1\"/>");
		
		// unescape any possibly escaped "["
		content = content.replaceAll("\\\\\\[", "[");
		
		// process the quoted parts
		// once everything else is done, process the quoted parts
		for (String uuid : quotes.keySet()) {
			String targetType = quotes.get(uuid).replaceAll(quotePattern.pattern(), "$1").replaceAll("^[|]+", "");
			if (targetType.length() == 0)
				targetType = "txt";
			String quote = quotes.get(uuid).replaceAll(quotePattern.pattern(), "$2");
//			String mimeType = URLConnection.guessContentTypeFromName(targetType);
//			if (mimeType == null)
//				throw new FormatException("Could not resolve '" + targetType + "' into a mime type");
			// convert later when the target format is known
//			File fragment = new MemoryFileFragment(file, quote.getBytes("UTF-8"), "quote", mimeType);
//			Converter converter = repository.getConverter(fragment.getContentType(), getOutputContentType());
//			if (converter == null)
//				throw new FormatException("Can not find converter from '" + fragment.getContentType() + "' to '" + getOutputContentType() + "'");
//			ByteArrayOutputStream transformedOutput = new ByteArrayOutputStream();
//			converter.convert(repository, fragment, transformedOutput, properties);
//			quote = new String(transformedOutput.toByteArray(), "UTF-8");
			quote = "<blockquote format='" + targetType + "'>" + quote + "</blockquote>";
			content = content.replaceAll(Pattern.quote("[quote=" + uuid + "]"), Matcher.quoteReplacement(quote));
		}
		
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(content.getBytes("UTF-8"))), IOUtils.wrap(output));
	}

	protected String getQuotePattern() {
		return "(?s)(?<!\\\\)\\[quote(\\|[\\w.]+|)\\][\\s]*(.*?)[\\s]*(?<!\\\\)\\[/quote]";
	}
	
	protected String replaceLists(String content) {
		content = replaceLists(content, "*", "ul");
		content = replaceLists(content, "#", "ol");
		return content;
	}
	
	/**
	 * Format: [:page]
	 */
	protected String replaceIncludes(DocumentManager repository, File file, String content) throws IOException, FormatException {
		Pattern pattern = Pattern.compile("(?<!\\\\)\\[:([^\\]]+)\\]");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String url = matcher.group().replaceAll(pattern.pattern(), "$1").replaceAll("[/]+$", "");
			URI uri;
			try {
				// check if you added parameters
				uri = new URI(URIUtils.encodeURI(url));
			}
			catch (URISyntaxException e) {
				throw new FormatException("The link " + url + " is not of a valid format", e);
			}
			// just get the path part for the link
			String link = uri.getPath();
			// any parameters
			Map<String, String> properties = flatten(URIUtils.getQueryProperties(uri));
			String anchor = properties.containsKey("anchor") ? properties.get("anchor") : encodeAnchor(link);
			
			File linkedFile = file.getParent().resolve(link);
			logger.debug("Resolving link '" + link + "' against '" + file.getParent().getPath() + "': " + linkedFile.exists());
						
			if (linkedFile.exists()) {
				link = linkedFile.getPath();
				Converter converter = repository.getConverter(linkedFile.getContentType(), getOutputContentType());
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
				String css = "";
				if (properties.containsKey("float"))
					css = "style='float:" + properties.get("float") + "' ";
				content = content.replaceFirst(Pattern.quote(matcher.group()), 
					Matcher.quoteReplacement("<a name='" + anchor + "'></a><span title='" + link + "' id='" + anchor + "' " + css + "reference='" + url + "'>" + replacement + "</span>"));
			}
			else
				content = content.replaceFirst(Pattern.quote(matcher.group()),
					Matcher.quoteReplacement("<span reference='" + url + "' class='bad'>failed to import <a class='internal' exists='false' href='" + link + "'>" + link + "</a></span>"));
		}
		return content;
	}
	
	public static Map<String, String> flatten(Map<String, List<String>> queryProperties) {
		Map<String, String> flattened = new HashMap<String, String>();
		for (String key : queryProperties.keySet()) {
			flattened.put(key, queryProperties.get(key).isEmpty() ? "true" : queryProperties.get(key).get(0));
		}
		return flattened;
	}

	public String replaceEmptyLines(String content) {
		return content.replaceAll("(?<!\n)\n(?!\n|$)", "<br/>");
	}
	
	/**
	 * Format: [#anchor]
	 */
	public String replaceAnchors(String content) {
		Pattern pattern = Pattern.compile("(?<!\\\\)\\[#([^\\]]+)\\]");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String anchor = matcher.group().replaceAll(pattern.pattern(), "$1");
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<a name='" + encodeAnchor(anchor) + "' original='" + anchor + "'></a>"));			
		}
		return content;
	}
	
	/**
	 * Format: [name|$page] or [$page]
	 */
	public String replaceLocalLinks(File file, String content) throws FormatException, IOException {
		Pattern pattern = Pattern.compile("(?<!\\\\)\\[([^|\\]]*)\\|\\$([^\\]]+)\\]");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String displayName = matcher.group().replaceAll(pattern.pattern(), "$1");
			// strip tracing "/"
			String url = matcher.group().replaceAll(pattern.pattern(), "$2").replaceAll("[/]+$", "");
			URI uri;
			try {
				// check if you added parameters
				uri = new URI(URIUtils.encodeURI(url));
			}
			catch (URISyntaxException e) {
				throw new FormatException("The link " + url + " is not of a valid format");
			}

			File linkedFile = file.getParent().resolve(uri.getPath());
			
			boolean hasDisplayName = true;
			
			if (displayName.length() == 0) {
				hasDisplayName = false;
				displayName = linkedFile.exists() ? linkedFile.getName() : url;
			}
			
			String link = linkedFile.getPath();
			String fullPath = uri.getFragment() != null ? link + "#" + encodeAnchor(uri.getFragment()) : link;
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<a class='internal' hasDisplayName='" + hasDisplayName + "' original='" + url + "' exists='" + linkedFile.exists() + "' href='" + SCHEME_LINK + ":" + fullPath + "' title='" + displayName + "'>" + displayName + "</a>"));
		}
		
		// create local link [$page]
		pattern = Pattern.compile("(?<!\\\\)\\[\\$([^\\]]+)]");
		matcher = pattern.matcher(content);
		while(matcher.find()) {
			// strip tracing "/"
			String url = matcher.group().replaceAll(pattern.pattern(), "$1").replaceAll("[/]+$", "");
			URI uri;
			try {
				// check if you added parameters
				uri = new URI(URIUtils.encodeURI(url));
			}
			catch (URISyntaxException e) {
				throw new FormatException("The link " + url + " is not of a valid format");
			}

			File linkedFile = file.getParent().resolve(uri.getPath());
		
			String displayName = linkedFile.exists() ? linkedFile.getName() : url ;

			String link = linkedFile.getPath();
			String fullPath = uri.getFragment() != null ? link + "#" + encodeAnchor(uri.getFragment()) : link;
			
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<a class='internal' hasDisplayName='false' original='" + url + "' exists='" + linkedFile.exists() + "' href='"+ SCHEME_LINK + ":" + fullPath + "' title='" + displayName + "'>" + displayName + "</a>"));
		}
		return content;
	}
	
	/**
	 * Format: [name|#anchor]
	 */
	public String replaceAnchorLinks(String content) {
		Pattern pattern = Pattern.compile("(?<!\\\\)\\[([^|\\]]*)\\|#([^\\]]+)\\]");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String displayName = matcher.group().replaceAll(pattern.pattern(), "$1");
			String anchor = matcher.group().replaceAll(pattern.pattern(), "$2");
			boolean hasDisplayName = true;
			if (displayName.length() == 0) {
				hasDisplayName = false;
				displayName = anchor;
			}
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<a class='anchor' hasDisplayName='" + hasDisplayName + "' href='#" + encodeAnchor(anchor) + "' original='" + anchor + "'>" + displayName + "</a>"));
		}
		return content;
	}
	
	/**
	 * Format: [name|url]
	 * This must be executed after the replaceHTTPLinks which is basically a more specific version
	 */
	public String replaceExternalLinks(String content) {
		// resolve other url links [name|url]
		Pattern pattern = Pattern.compile("(?<!\\\\)\\[([^|\\]]*)\\|([\\w]+:/[^\\]]+)\\]");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String displayName = matcher.group().replaceAll(pattern.pattern(), "$1");
			String link = matcher.group().replaceAll(pattern.pattern(), "$2");
			if (displayName.length() == 0)
				displayName = link;
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<a rel='nofollow' class='external' href='" + link + "'>" + displayName + "</a>"));
		}
		return content;
	}
	
	public String replaceParagraphs(String content) {
		Pattern pattern = Pattern.compile("(?s)(?:\n\n|^)(.+?)(?=\n\n|$)");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String text = matcher.group().replaceAll(pattern.pattern(), "$1");
			// this is an include, though it will often stand alone, it should not be encapsulated in a paragraph
			if (text.trim().matches("\\[:([^\\]]+)\\]") || text.trim().matches("\\[quote=[^\\]]+\\]"))
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(text.trim()));
			// headers, tables, images, ... shouldn't be encapsulated in a paragraph (only text and text with font styling)
			else if (text.trim().startsWith("<") && !text.trim().matches("^<(del|sub|sup|code|strong|cite|b>|u>|i>).*"))
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(text.trim()));
			// if you have a strong statement which is in a paragraph of its own, encapsulate it in a div to break it out of the inline context
			else if (text.trim().startsWith("<strong") && text.trim().endsWith("</strong>"))
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<p class='message'>" + text.trim() + "</p>"));
			// same for cite
			else if (text.trim().startsWith("<cite") && text.trim().endsWith("</cite>"))
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<p class='message'>" + text.trim() + "</p>"));
			// a page break must stand alone
			else if (text.trim().equals("--"))
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<hr/>"));
			// otherwise its a paragraph
			else
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<p>" + text + "</p>"));
		}
		return content;
	}

	/**
	 * Note that while small is still supported in html5, big is not
	 * hence it is no longer implemented in the syntax
	 * @param content
	 * @return
	 */
	public String replaceStyling(String content) {
		// this must be resolved before the other styling because it uses the "+" sign
		// subscript
		content = content.replaceAll("(?<!\\\\)-\\^([^\n]*?)(?<!\\\\)\\^", "<sub>$1</sub>");
		// superscript
		content = content.replaceAll("(?<!\\\\)\\+\\^([^\n]*?)(?<!\\\\)\\^", "<sup>$1</sup>");

//		// big
//		content = content.replaceAll("(?<!\\\\)\\+%([^\n]*?)(?<!\\\\)%", "<big>$1</big>");
//		// small
//		content = content.replaceAll("(?<!\\\\)\\-%([^\n]*?)(?<!\\\\)%", "<small>$1</small>");

		// create bolded fonts **..** (for text without a double linefeed within a paragraph)
		content = styleText(content, "*", "b");
		// create underlined fonts __..__ (for text without a double linefeed within a paragraph) > this conflicts with <a target='_blank' for external links!
		content = styleText(content, "_", "u");
		// create italic fonts +..+ (for text with word boundaries and no tabs or linefeeds)
		content = styleText(content, "+", "i");
		// create highlighted bits @..@ (for text with word boundaries and no tabs or linefeeds)
		content = styleText(content, "@", "code");
		// create "important" quote !..! (for text with word boundaries and no tabs or linefeeds)
		content = styleText(content, "!", "strong");
		// for texts
		content = styleText(content, "?", "cite");
		// create striked through text ~..~
		content = styleText(content, "~", "del");
		// create inserted text
		content = styleText(content, "%", "ins");
		
		return content;
	}
	
	public String replaceTables(String content) {
		Pattern pattern = Pattern.compile("(?s)([\n]+|\\A)\\|(.*?)\\|(\n\n(?!\\|)|\n(?=\n\\|)|(?=\n\\|)|\\z)");
		Matcher matcher = pattern.matcher(content);
		boolean tableStarted = false;
		boolean bodyStarted = false;
		while(matcher.find()) {
			String startSeparator = matcher.group().replaceAll(pattern.pattern(), "$1");
			String itemContent = matcher.group().replaceAll(pattern.pattern(), "$2");
			String endSeparator = matcher.group().replaceAll(pattern.pattern(), "$3");
			
			boolean isHeader = false;
			if (itemContent.startsWith("|")) {
				itemContent = itemContent.substring(1);
				isHeader = true;
			}
			
			String html = "<tr>";
			if (isHeader) {
				if (bodyStarted) {
					html = "</tbody>" + html;
					bodyStarted = false;
				}
				html = "<thead>" + html;
			}
			else if (!bodyStarted) {
				html = "<tbody>" + html;
				bodyStarted = true;
			}
			for (String part : itemContent.split("\\|")) {
				part = part.trim();
				String cssClass = null;
				if (part.startsWith("!") && part.endsWith("!")) {
					cssClass = "important";
					part = part.substring(1, part.length() - 1);
				}
				else if (part.startsWith("@") && part.endsWith("@")) {
					cssClass = "highlight";
					part = part.substring(1, part.length() - 1);
				}
				html += "<td" + (cssClass != null ? " class='" + cssClass + "'" : "") + ">" + part + "</td>";
			}
			html += "</tr>";
			if (isHeader) {
				html += "</thead>";
			}
			
			// start the table if necessary
			if (!tableStarted) {
				tableStarted = true;
				html = "<table cellspacing='0' cellpadding='0'>" + html;
				// if you have linefeeds in the beginning, add them again for paragraphs and the likes
				if (startSeparator.length() > 1)
					html = "\n\n" + html;
			}
			
			// end the table if necessary
			if (endSeparator.length() > 0 || matcher.hitEnd()) {
				tableStarted = false;
				if (bodyStarted) {
					html += "</tbody>";
					bodyStarted = false;
				}
				html += "</table>";
				if (endSeparator.length() > 1)
					html += "\n\n";
			}
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(html));
		}
		return content;
	}
	public String replaceLists(String content, String indicator, String tag) {
		// a list item starts on a new line with one or more "*" and is stopped by either another list item, an empty line or the end of the text
		Pattern pattern = Pattern.compile(getListRegex(indicator));
		Matcher matcher = pattern.matcher(content);
		
		// keeps track of the depth
		int listDepth = 0;
		while(matcher.find()) {
			String startSeparator = matcher.group().replaceAll(pattern.pattern(), "$1");
			int itemDepth = matcher.group().replaceAll(pattern.pattern(), "$2").length();
			String itemContent = matcher.group().replaceAll(pattern.pattern(), "$3");
			String endSeparator = matcher.group().replaceAll(pattern.pattern(), "$4");
			String preamble = "";
			
			// if the separator indicates the end of this list, postamble it to listDepth 0
			String postamble = "";

			if (listDepth > itemDepth) {
				for (int i = listDepth; i > itemDepth; i--)
					preamble += "</" + tag + ">";
				listDepth = itemDepth;
			}
			else if (listDepth < itemDepth) {
				for (int i = listDepth; i < itemDepth; i++)
					preamble += "<" + tag + ">";
				listDepth = itemDepth;
			}

			// more than one linefeed means a new list, add the linefeeds again so <p/> can be added to previous text
			if (startSeparator.length() > 1)
				preamble = "\n\n" + preamble;

			if (endSeparator.length() > 0 || matcher.hitEnd()) {
				for (int i = listDepth; i > 0; i--)
					postamble += "</" + tag + ">";
				postamble += endSeparator;
				listDepth = 0;
			}
			
			// if you start your item with a ':', you mean to use a title
			// everything after the ':' and before the next one will be wrapped in a title span
			if (itemContent.startsWith(":")) {
				itemContent = itemContent.substring(1);
				// wrap the first bit before a ":" in a span, this allows highlighting of the "title" of the span
				int indexOfSeparator = itemContent.indexOf(':');
				// if your first character is the separator, not much use in wrapping in span...
				if (indexOfSeparator > 0)
					itemContent = "<em>" + itemContent.substring(0, indexOfSeparator) + "</em>" + itemContent.substring(indexOfSeparator);
			}
			
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(
				preamble + "<li>" + itemContent + "</li>" + postamble));
		}
		return content;
	}

	protected String getListRegex(String indicator) {
		return "(?s)([\n]+|\\A)([" + indicator + "]+)[ ]+(.*?)(\n\n(?![" + indicator + "]+[ ]+)|\n(?=\n[" + indicator + "]+[ ]+)|(?=\n[" + indicator + "]+[ ]+)|\\z)";
	}
	
	public String replaceHeaders(String content) {
		// find all the headers
		Pattern pattern = Pattern.compile("(?m)^h([0-7]{1})\\.[\\s]*(.*)$");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			int level = new Integer(matcher.group().replaceAll(pattern.pattern(), "$1"));
			String headerContent = matcher.group().replaceAll(pattern.pattern(), "$2");
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<h" + level + ">" + headerContent + "</h" + level + ">"));
		}
		return content;
	}
	
	public String escapeXML(String content) {
		return content.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	
	/**
	 * The annotations contain metadata for the wiki document
	 * They must be at the very top of the document
	 * @param content
	 * @return
	 */
	public String removeAnnotations(String content) {
		return content.replaceAll("(?s)^[\\s]*@.*?\n(?!@)", "");
//		return content.replaceAll("(?s)^([\n]*@[^\n]+)+", "");
	}
	
	/**
	 * Note that this leaves a dangling linefeed to enable proper parsing of whatever comes after
	 * This linefeed must be removed before resolving other linefeeds
	 * @param content
	 * @return
	 */
	public String replaceAnnotations(String content) {
		String annotationBlock = content.replaceAll("(?s)^([\\s]*@.*?)[\n]+(?!@).*", "$1");
		// no annotations found
		if (!annotationBlock.trim().startsWith("@"))
			return content;
		// strip the block
		content = content.substring(annotationBlock.length());
		annotationBlock = annotationBlock.replaceAll("(?m)^[\\s]*@([^=\\s]+)[\\s=]*(.*)$", "<meta name=\"$1\" content=\"$2\"/>").replaceAll("\n", ""); 
		return annotationBlock + content;
	}
	
	public String removeAnnotationsMarker(String content) {
		return content.replaceAll("(<meta[^>]+>)[\n]+", "$1");
	}
	
	public static String encodeAnchor(String anchor) {
		return anchor.replaceAll("[^\\w\\s]+", "").replaceAll("[\\s]+", "_");
	}
	
	/**
	 * You can usually style text in two ways: with a singular encapsulation or with a double
	 * The single encapsulation makes for easy typing/reading but has more chance of collisions, hence it is restricted to be inline (no linefeeds or tabs can be in it)
	 * The double encapsulation is if you want to explicitly mark more, even then it may not cross paragraphs or the generated xhtml would be invalid, e.g. "<p><u></p><p></u></p>"
	 */
	public String styleText(String content, String wikiSyntax, String htmlTag) {
		String escape = "(?<!\\\\)";
		
		String quotedSyntax = Pattern.quote(wikiSyntax);
		String doubleQuoted = Pattern.quote(wikiSyntax + wikiSyntax);
		
		// first we replace the "double" variant
		// IMPORTANT: this first regex works but on large documents it will generate stack overflows in the java regex implementation
//		String regex = escape + doubleQuoted + "((\n(?!\n)|[^\n])+?)" + escape + doubleQuoted;
		// this regex also seems to work and does not generate stack overflows on the same documents
		String regex = escape + doubleQuoted + "((?:(?<!\n\n).)+?)" + escape + doubleQuoted;
		content = content.replaceAll("(?s)" + regex, "<" + htmlTag + ">$1</" + htmlTag + ">");
		
		// then the single variant
		// note that "_" is part of "word" characters and hence treated differently by \b
		if (wikiSyntax.equals("_"))
			regex = "\\b" + escape + quotedSyntax + "(?!\\b)" + "([^\t\n]+?)" + "(?!\\b)" + escape + quotedSyntax + "\\b";
		else
			// it can't contain '<' either, this prevents false matches in other tags (e.g. lists)
			regex = escape + quotedSyntax + "\\b" + "([^\t\n<]+?)" + "\\b" + escape + quotedSyntax;
		
		content = content.replaceAll("(?s)" + regex, "<" + htmlTag + ">$1</" + htmlTag + ">");

		// unescape any possibly escaped delimiters
		content = content.replaceAll("\\\\" + quotedSyntax, wikiSyntax);
		
		return content;
	}

	@Override
	public boolean isLossless() {
		return true;
	}

}
