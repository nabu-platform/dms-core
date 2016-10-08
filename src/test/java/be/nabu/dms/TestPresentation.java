package be.nabu.dms;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.converters.DXFToSlides;
import be.nabu.libs.dms.converters.DXFToStandaloneHTML;
import be.nabu.libs.dms.converters.MarkdownToDXF;
import be.nabu.libs.dms.converters.WikiToDXF;
import be.nabu.libs.dms.utils.SimpleDocumentManager;
import be.nabu.libs.events.EventDispatcherFactory;
import be.nabu.libs.vfs.api.File;
import be.nabu.libs.vfs.api.FileSystem;
import be.nabu.libs.vfs.resources.impl.ResourceFileSystem;
import be.nabu.utils.io.ContentTypeMap;

public class TestPresentation {
	
	private FileSystem system;
	private DocumentManager manager;
	
	public static void main(String...args) throws IOException, URISyntaxException, FormatException {
		TestPresentation test = new TestPresentation();
//		test.convertToHTML("markup/frameworks/datastore.md");
		test.convertToHTML("syntax.md");
		test.convertToPresentation("syntax.md");
//		test.convertToPresentation("wiki/glue.wiki");
//		test.convertToHTML("wiki/glue.wiki");
//		test.convertToHTML("pharos-notes.wiki");
	}
	
	public TestPresentation() throws IOException, URISyntaxException, FormatException {
		ContentTypeMap.register();
		ContentTypeMap.getInstance().registerContentType(WikiToDXF.WIKI_CONTENT_TYPE, "wiki");
		ContentTypeMap.getInstance().registerContentType(MarkdownToDXF.CONTENT_TYPE, "markdown");
		ContentTypeMap.getInstance().registerContentType("text/x-java-source", "java");
		system = new ResourceFileSystem(EventDispatcherFactory.getInstance().getEventDispatcher(), new URI("file:/home/alex/code/trunk/documentation"), null);
		manager = new SimpleDocumentManager();
	}
	
	public void convertToPresentation(String path) throws IOException, FormatException {
		File file = system.resolve(path);
		Converter converter = manager.getConverter(path.endsWith(".wiki") ? WikiToDXF.WIKI_CONTENT_TYPE : MarkdownToDXF.CONTENT_TYPE, DXFToSlides.HTML_SLIDE_CONTENT_TYPE);
		File target = system.resolve(path.replaceAll("\\.[^.]+$", ".html"));
		OutputStream output = target.getOutputStream();
		try {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put("fragment", "p,h2,h3,h4,h5,h6,h7,img,li,blockquote,table");
			properties.put("mouseWheel", "false");
			properties.put("style", DXFToStandaloneHTML.getAdditionalStyles("slides/custom.css", "slides/tables.css"));
			converter.convert(manager, file, output, properties);
		}
		finally {
			output.close();
		}
	}
	
	public void convertToHTML(String path) throws IOException, FormatException {
		File file = system.resolve(path);
		Converter converter = manager.getConverter(path.endsWith(".wiki") ? WikiToDXF.WIKI_CONTENT_TYPE : MarkdownToDXF.CONTENT_TYPE, "text/html+standalone");
		File target = system.resolve(path.replaceAll("\\.[^.]+$", ".standalone.html"));
		OutputStream output = target.getOutputStream();
		try {
			converter.convert(manager, file, output, null);
		}
		finally {
			output.close();
		}
	}
	
	public void convertToODT(String path) throws IOException, FormatException {
		File file = system.resolve(path);
		Converter converter = manager.getConverter(WikiToDXF.WIKI_CONTENT_TYPE, "application/vnd.oasis.opendocument.text");
		File target = system.resolve(path.replaceAll(".wiki", ".odt"));
		OutputStream output = target.getOutputStream();
		try {
			converter.convert(manager, file, output, null);
		}
		finally {
			output.close();
		}
	}
}
