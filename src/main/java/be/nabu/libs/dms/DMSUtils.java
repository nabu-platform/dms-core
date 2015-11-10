package be.nabu.libs.dms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;

public class DMSUtils {
	
	public static final String PROPERTY_WEB_ROOT = "be.nabu.dms.converter.webRoot";
	
	public static String getWebRoot() {
		String root = System.getProperty(PROPERTY_WEB_ROOT, "/");
		if (root.endsWith("/"))
			root = root.substring(0, root.length() - 1);
		return root;
	}

	public static byte [] convertQuote(DocumentManager repository, File context, byte [] content, String fromContentType, String toContentType) throws IOException, FormatException {
		File fragment = new MemoryFileFragment(context, content, "quote", fromContentType);
		Converter converter = repository.getConverter(fragment.getContentType(), toContentType);
		if (converter == null)
			throw new FormatException("Can not find converter from '" + fragment.getContentType() + "' to '" + toContentType + "'");
		ByteArrayOutputStream transformedOutput = new ByteArrayOutputStream();
		converter.convert(repository, fragment, transformedOutput, null);
		return transformedOutput.toByteArray();
	}
	
	public static byte [] convertFile(DocumentManager repository, File include, String toContentType) throws FormatException, IOException {
		Converter converter = repository.getConverter(include.getContentType(), toContentType);
		if (converter == null)
			throw new FormatException("Can not find converter from '" + include.getContentType() + "' to '" + toContentType + "'");
		ByteArrayOutputStream transformedOutput = new ByteArrayOutputStream();
		converter.convert(repository, include, transformedOutput, null);
		return transformedOutput.toByteArray();
	}
}
