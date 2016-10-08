package be.nabu.libs.dms.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class FileUtils {
	
	public static String toString(File file, String encoding) throws IOException {
		return new String(toBytes(file), encoding);
	}
	
	public static byte [] toBytes(File file) throws IOException {
		InputStream input = file.getInputStream();
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			IOUtils.copyBytes(IOUtils.wrap(input), IOUtils.wrap(output));
			return output.toByteArray();
		}
		finally {
			input.close();
		}
	}
	
}
