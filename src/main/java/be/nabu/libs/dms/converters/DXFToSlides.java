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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.impl.Base64Encoder;
import be.nabu.utils.io.IOUtils;

public class DXFToSlides extends DXFConverter {

	public static final String HTML_SLIDE_CONTENT_TYPE = "application/html+slides";
	
	// previous version
//	private static List<String> transitions = Arrays.asList(new String [] { "default", "cube", "page", "concave", "zoom", "linear", "fade", "none" });
	private static List<String> transitions = Arrays.asList(new String [] { "none", "fade", "slide", "convex", "concave", "zoom" });
	
	@Override
	public void convert(DocumentManager documentManager, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		boolean embedded = properties != null && properties.containsKey("embedded") && properties.get("embedded").equals("true");
		
		// the tags we should set class="fragment" on, these can be stepped through
		String [] fragmentedTags = properties != null && properties.containsKey("fragment") ? properties.get("fragment").split("[\\s]*,[\\s]*") : null;

		// some of the options
		String transition = properties != null && properties.containsKey("transition") && transitions.contains(properties.get("transition")) ? properties.get("transition") : "slide";
		// mouseWheel is disabled by default because slides will regularly be larger then the screen so we need the scroll to actually scroll down
		boolean mouseWheel = properties != null && properties.containsKey("mouseWheel") && properties.get("mouseWheel").equals("true");
		// for more options, see https://github.com/hakimel/reveal.js#configuration
		
		// allow for additional styling
		String additionalStyle = properties != null && properties.containsKey("style") ? properties.get("style") : null;
		
		boolean generateTitlePages = properties != null && "true".equals(properties.get("titlePages"));
		
		String content = FileUtils.toString(file, "UTF-8").trim();
		
		content = transformQuotes(documentManager, file, content, properties, false, "text/html");

		content = replaceIncludes(documentManager, file, content, DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE, properties);
		
		Pattern pattern;
		Matcher matcher;
		content = embedImages(file, content);
		
		// replace spaces etc
		content = content.replaceAll("\t", TAB_SPACES);
		content = content.replaceAll("&#160;", "&nbsp;");
		
		if (fragmentedTags != null) {
			// add class="fragment" to the necessary tags
			for (String fragmentedTag : fragmentedTags)
				content = content.replaceAll("(<" + fragmentedTag + "\\b)", "$1 class='fragment'");
		}
		
		// a null section indicates a title page that has to be generated
		List<String> sections = new ArrayList<String>();
		
		// find annotations
		Map<String, String> annotations = new HashMap<String, String>();
		pattern = Pattern.compile("<meta([^>]+)>");
		matcher = pattern.matcher(content);
		while (matcher.find()) {
			String name = matcher.group(1).replaceAll(".*name[\\s]*=[\\s]*\"(.*?)\".*", "$1");
			String value = matcher.group(1).replaceAll(".*content[\\s]*=[\\s]*\"(.*?)\".*", "$1");
			annotations.put(name, value);
			content = content.replace(matcher.group(), "");
		}
		content = content.trim();
		// there is a title page
		if (annotations.containsKey("title")) {
			String titleContent = "<section class='title'><h1>" + annotations.get("title") + "</h1>";
			if (annotations.containsKey("subtitle")) {
				titleContent += "<h2>" + annotations.get("subtitle") + "</h2>";
			}
			if (annotations.containsKey("author")) {
				titleContent += "<p>" + annotations.get("author") + "</p>";
			}
			titleContent += "</section>";
			sections.add(titleContent);
		}
		
		pattern = Pattern.compile("(?s)<h(1|2)[^>]*>(.*?)</h(1|2)>");
		matcher = pattern.matcher(content);
		
		// this keeps track of all the titles in order, afterwards title pages will be generated
		List<String> headers = new ArrayList<String>();
		int previousIndex = 0;
		while (matcher.find()) {
			// add the title
			headers.add(matcher.group());
			// there is content between the last known position and now
			if (matcher.start() > previousIndex) {
				sections.addAll(getSections(headers, content.substring(previousIndex, matcher.start()), false));
			}
			previousIndex = matcher.end();
			// add an empty section to indicate a header page
			if (generateTitlePages) {
				sections.add(null);
			}
		}
		// add the last bit (if any)
		if (previousIndex < content.length()) {
			sections.addAll(getSections(headers, content.substring(previousIndex), true));
		}
		
		// the "current" header which should be selected
		int headerCount = 0;
		// generate header pages
		for (int i = 0; i < sections.size(); i++) {
			// null indicates a header page to be generated
			if (sections.get(i) == null) {
				String page = "";
				// the current depth of the list
				int depth = 0;
				for (int j = 0; j < headers.size(); j++) {
					int headerNumber = new Integer(headers.get(j).replaceAll("^<h([0-9]+).*", "$1"));
					// close previous lists if necessary
					for (int k = depth; k > headerNumber; k--)
						page += "</ul>";
					// open new lists if necessary
					for (int k = depth; k < headerNumber; k++)
						page += "<ul>";
					page += "<li" + (headerCount == j ? " class='selected'" : "") + ">" + headers.get(j).replaceAll("^<h[^>]*>(.*)</h[^>]*>$", "$1") + "</li>";
					depth = headerNumber;
				}
				// close lists
				for (int k = depth; k > 0; k--)
					page += "</ul>";
				// replace the null with the actual page
				sections.set(i, page);
				headerCount++;
			}
		}

		// if not embedded, add header stuff
		if (!embedded) {
			// write title
			IOUtils.copyBytes(IOUtils.wrap(("<html><head><title>Slides: " + file.getName() + "</title><style>").getBytes("UTF-8"), true), IOUtils.wrap(output));
			// write stylesheets
			IOUtils.copyBytes(IOUtils.wrap(Thread.currentThread().getContextClassLoader().getResourceAsStream("slides/reveal.css")), IOUtils.wrap(output));
			String theme = annotations.containsKey("theme") ? annotations.get("theme") : System.getProperty("slides.theme", "custom");
			InputStream input;
			java.io.File themeFile = new java.io.File("themes/" + theme + ".css");
			if (themeFile.isFile()) {
				input = new FileInputStream(themeFile);
			}
			else {
				input = Thread.currentThread().getContextClassLoader().getResourceAsStream("slides/theme." + theme + ".css");
			}
			if (input == null) {
				throw new RuntimeException("The theme '" + theme + "' was not found, please add it to the themes folder (" + new java.io.File("themes").getAbsolutePath() + ")");
			}
			else {
				input = new BufferedInputStream(input);
			}
			IOUtils.copyBytes(IOUtils.wrap(input), IOUtils.wrap(output));
			if (additionalStyle != null) {
				IOUtils.copyBytes(IOUtils.wrap(additionalStyle.getBytes("UTF-8"), true), IOUtils.wrap(output));
			}
			// start body
			IOUtils.copyBytes(IOUtils.wrap(("</style></head><body>").getBytes("UTF-8"), true), IOUtils.wrap(output));
		}
		
		// write the necessary divs etc
		IOUtils.copyBytes(IOUtils.wrap(("<div id='slideShow' class='reveal'><div class='slides'>").getBytes("UTF-8"), true), IOUtils.wrap(output));
		for (int i = 0; i < sections.size(); i++) {
			IOUtils.copyBytes(IOUtils.wrap(("<section>" + sections.get(i) + "</section>").getBytes("UTF-8"), true), IOUtils.wrap(output));
		}
		// write closing divs and fix script
		IOUtils.copyBytes(IOUtils.wrap(("</div></div>").getBytes("UTF-8"), true), IOUtils.wrap(output));
		
		
		if (!embedded) {
			IOUtils.copyBytes(IOUtils.wrap(("<script>").getBytes("UTF-8"), true), IOUtils.wrap(output));
			IOUtils.copyBytes(IOUtils.wrap(Thread.currentThread().getContextClassLoader().getResourceAsStream("slides/reveal.js")), IOUtils.wrap(output));
			IOUtils.copyBytes(IOUtils.wrap(("</script>").getBytes("UTF-8"), true), IOUtils.wrap(output));
			// initialize the slideshow
			// the event listener "should" reset the scrollbar whenever a new slide is opened, however it does not seem to work in chrome
			IOUtils.copyBytes(IOUtils.wrap(("<script>Reveal.initialize({ controls: true, progress: true, history: true, rollingLinks: false, theme: 'default', transition: '" + transition + "', mouseWheel: " + mouseWheel + "});\nReveal.addEventListener( 'slidechanged', function( event ) { document.body.scrollTop=0; window.scrollTo(0,0); } );" +
					// the following adds mouse controls where left click is "next" and middle mouse click is "previous"
					// this allows for full mouse control (including scroll) of the slides
					"document.getElementById('slideShow').addEventListener('mousedown', function(event) {\n" +
					"	event.stopPropagation();\n" +
					"	event.preventDefault();\n" +
					"	// left mouse button\n" +
					"	if (event.which == 1)\n" +
					"		Reveal.next();\n" +
					"	// middle button\n" +
					"	else if (event.which == 2)\n" +
					"		Reveal.prev();\n" +
					"	// right button == 3\n" +
					"}, false);</script>").getBytes("UTF-8"), true), IOUtils.wrap(output));
			IOUtils.copyBytes(IOUtils.wrap(("</body></html>").getBytes("UTF-8"), true), IOUtils.wrap(output));
		}
	}

	public static String embedImages(File file, String content) throws IOException, MalformedURLException, FormatException {
		// need to embed images
		Pattern pattern = Pattern.compile("(<img[^>]+?src[\\s='\"]+)([^'\"]+)('|\")");
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String src = matcher.group().replaceAll(pattern.pattern(), "$2");
			// already embedded
			if (src.startsWith("data:"))
				continue;
			String extension = src.replaceAll("^.*\\.([^.]+)$", "$1");
			try {
				URI uri = new URI(URIUtils.encodeURI(src));
				System.out.println("Embedding " + src + ": " + uri);
				InputStream imageContent = null;
				// if we have the streaming scheme, it is in the local repo
				// alternatively you could reference an image on a remote site (e.g. src="http://example.com/image.gif")
				if (uri.getScheme().equals(SCHEME_STREAM)) {
					File image = file.getParent().resolve(uri.getPath());
					if (!image.exists()) {
						throw new IOException("Could not find local image: " + image.getPath());
					}
					imageContent = image.getInputStream();
				}
				else {
					imageContent = uri.toURL().openStream();
				}
				
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				IOUtils.copyBytes(IOUtils.wrap(imageContent), IOUtils.wrap(bytes));
				String encoded = "data:image/" + extension.toLowerCase() + ";base64," + new String(IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(bytes.toByteArray(), true), new Base64Encoder())));
				String embeddedImage = matcher.group().replaceAll(pattern.pattern(), "$1" + encoded + "$3");
				content = content.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(embeddedImage));
			}
			catch (URISyntaxException e) {
				throw new FormatException(e);
			}
		}
		return content;
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return HTML_SLIDE_CONTENT_TYPE;
	}

	@Override
	public boolean isLossless() {
		return false;
	}

	private List<String> getSections(List<String> headers, String section, boolean last) {
		// get previous header because we are already at the next page
		// remove the fragment (if present)
		section = "<center>" + headers.get(headers.size() - (last ? 1 : 2)).replaceAll("<(/|)h[0-9]{1}", "<$1h1").replaceAll(" class='fragment'", "") + "</center>" + section;
		// increase all the remaining "h" in the section with 2 so h3 becomes h1, h4 becomes h2 etc
		section = section.replaceAll("<(/|)h3", "<$1h2")
			.replaceAll("<(/|)h4", "<$1h3")
			.replaceAll("<(/|)h5", "<$1h4")
			.replaceAll("<(/|)h6", "<$1h5")
			.replaceAll("<(/|)h7", "<$1h6");
		return Arrays.asList(section.split("<hr[^>]*>"));
	}
}
