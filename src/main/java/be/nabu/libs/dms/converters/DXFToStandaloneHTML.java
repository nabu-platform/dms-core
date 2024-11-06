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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class DXFToStandaloneHTML implements Converter {

	@Override
	public void convert(DocumentManager documentManager, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		Converter converter = documentManager.getConverter(file.getContentType(), "text/html");
		if (converter == null) {
			throw new FormatException("No converter found from " + file.getContentType() + " to text/html");
		}
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		if (properties == null) {
			properties = new HashMap<String, String>();
		}
		properties.put("embed", "true");
		converter.convert(documentManager, file, content, properties);
		String html = new String(content.toByteArray());
		String toc = getTableOfContents(html);
		html = addHeaderAnchors(html);
		
		output.write(("<html><head><title>" + file.getName() + "</title>").getBytes());
		output.write(("<style>" + getAdditionalStyles(
				"standalone/headers.css", 
				"standalone/highlighting.css", 
				"standalone/quotes.css", 
				"standalone/layout.css",
				"standalone/tableOfContents.css",
				"standalone/print.css",
				"standalone/tables.css"
			) + "</style>").getBytes());
		output.write("</head><body><div id='content'>".getBytes());
		output.write(html.getBytes());
		output.write("</div><div class='tableOfContents'>".getBytes());
		output.write(toc.getBytes());
		output.write("</div></body></html>".getBytes());
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return "text/html+standalone";
	}

	@Override
	public boolean isLossless() {
		return true;
	}
	
	public static String getAdditionalStyles(String...names) throws IOException {
		StringBuilder builder = new StringBuilder();
		for (String name : names) {
			InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
			try {
				if (!builder.toString().isEmpty()) {
					builder.append("\n");
				}
				builder.append(new String(IOUtils.toBytes(IOUtils.wrap(input))));
			}
			finally {
				input.close();
			}
		}
		return builder.toString();
	}
	
	public static String getTableOfContents(String content) throws IOException, FormatException {
		Pattern pattern = Pattern.compile("<h([1-7]{1})[^>]*>(.*?)</h[1-7]>");
		Matcher matcher = pattern.matcher(content);
		int currentLevel = 0;
		StringWriter html = new StringWriter();
		while (matcher.find()) {
			int level = new Integer(matcher.group().replaceAll(pattern.pattern(), "$1"));
			String header = matcher.group().replaceAll(pattern.pattern(), "$2");
			if (currentLevel < level) {
				for (int i = currentLevel; i < level; i++)
					html.append("<ul class='toc'>");
			}
			else if (currentLevel > level) {
				for (int i = currentLevel; i > level; i--)
					html.append("</ul>");
			}
			currentLevel = level;
			html.append("<li><a href='#" + WikiToDXF.encodeAnchor(header) + "'>").append(header).append("</a></li>");
		}
		for (int i = currentLevel; i > 0; i--)
			html.append("</ul>");
		return html.toString();
	}
	
	private String addHeaderAnchors(String content) {
		Pattern pattern = Pattern.compile("<h([1-7]{1})[^>]*>([^<]+)</h[1-7]>");
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String header = matcher.group().replaceAll(pattern.pattern(), "$2");
			content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement("<a name='" + WikiToDXF.encodeAnchor(header) + "'></a>" + matcher.group()));
		}
		return content;
	}

}
