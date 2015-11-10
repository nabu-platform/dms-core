package be.nabu.libs.dms.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

/**
 * Allows you to perform a simple copy
 */
public class PassThroughConverter implements Converter {

	private String fromContentType, toContentType;
	
	public PassThroughConverter(String fromContentType, String toContentType) {
		this.fromContentType = fromContentType;
		this.toContentType = toContentType;
	}
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		InputStream input = file.getInputStream();
		try {
			IOUtils.copyBytes(IOUtils.wrap(input), IOUtils.wrap(output));
		}
		finally {
			input.close();
		}
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { fromContentType });
	}

	@Override
	public String getOutputContentType() {
		return toContentType;
	}

	@Override
	public boolean isLossless() {
		return true;
	}

}
