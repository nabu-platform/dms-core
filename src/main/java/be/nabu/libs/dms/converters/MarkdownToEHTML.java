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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownToEHTML extends WikiToEHTML {

	@Override
	protected String getQuotePattern() {
		return "(?s)(?<!\\\\)(?:```([\\w]*)[\\s]+(.*?)[\\s]+```)";
	}
	
	@Override
	protected String replaceLists(String content) {
		content = replaceLists(content, "*-", "ul");
		content = replaceLists(content, "+", "ol");
		return content;	
	}

	@Override
	public String replaceStyling(String content) {
		content = super.replaceStyling(content);
		content = styleText(content, "`", "code");
		return content;
	}
	
	@Override
	public String replaceExternalLinks(String content) {
		// resolve other url links [name|url]
		Pattern pattern = Pattern.compile("(?<!\\\\)\\[([^|\\]]*)\\][\\s]*\\(([\\w]+:/[^)]+)\\)");
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
	
	@Override
	protected String getListRegex(String indicator) {
		// include the indicator because the spaces are 0 for a first level element and the list code expects 1 for a 0-zth level element
		return "(?s)([\n]+|\\A)([\\s]*[" + indicator + "]{1,3})[ ]+(.*?)(\n\n(?![\\s]*[" + indicator + "]{1,3}[ ]+)|\n(?=\n[\\s]*[" + indicator + "]{1,3}[ ]+)|(?=\n[\\s]*[" + indicator + "]{1,3}[ ]+)|\\z)";
	}
	
	@Override
	public String replaceHeaders(String content) {
		// find all the headers
		Pattern pattern = Pattern.compile("(?m)^([#]{1,6})[\\s]*([^#\\r\\n]+)$");
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			int level = matcher.group().replaceAll(pattern.pattern(), "$1").length();
			String headerContent = matcher.group().replaceAll(pattern.pattern(), "$2");
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<h" + level + ">" + headerContent + "</h" + level + ">"));
		}
		return content;
	}
	
	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String[] { MarkdownToDXF.CONTENT_TYPE });
	}
}
