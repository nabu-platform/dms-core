package be.nabu.dms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.converters.WikiToDXF;
import be.nabu.libs.dms.utils.SimpleDocumentManager;
import be.nabu.libs.events.EventDispatcherFactory;
import be.nabu.libs.vfs.api.File;
import be.nabu.libs.vfs.api.FileSystem;
import be.nabu.libs.vfs.resources.impl.ResourceFileSystem;
import be.nabu.utils.io.ContentTypeMap;
import junit.framework.TestCase;

public class DMSTest extends TestCase {
	
	private FileSystem system;
	private DocumentManager manager;
	
	public DMSTest() throws IOException, URISyntaxException {
		ContentTypeMap.register();
		ContentTypeMap.getInstance().registerContentType(WikiToDXF.WIKI_CONTENT_TYPE, "wiki");
		system = new ResourceFileSystem(EventDispatcherFactory.getInstance().getEventDispatcher(), new URI("classpath:/"), null);
		manager = new SimpleDocumentManager();
	}
	
	public void testWiki2Html() throws IOException, FormatException {
		File file = system.resolve("test.wiki");
		Converter converter = manager.getConverter(WikiToDXF.WIKI_CONTENT_TYPE, "text/html");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		converter.convert(manager, file, output, null);
		assertEquals(
				"<h1>This is a test</h1><h2>What kind of a test?</h2><p>The <b>good</b> kind.</p>",
				new String(output.toByteArray())
		);
	}
	
	public void testWiki2Html2() throws IOException, FormatException {
		File file = system.resolve("test2.wiki");
		Converter converter = manager.getConverter(WikiToDXF.WIKI_CONTENT_TYPE, "text/html");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		converter.convert(manager, file, output, null);
		System.out.println(new String(output.toByteArray()));
		assertEquals(
				"<meta name=\"tags\" content=\"something\"/><h1>This is a test</h1><h2>What kind of a test?</h2><p>The <b>good</b> kind.</p>",
				new String(output.toByteArray())
		);
	}

}
