package be.nabu.libs.dms.converters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.libs.dms.FileUtils;
import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;

public class XMLToDXF implements Converter {

	@Override
	public void convert(DocumentManager documentManager, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String string = FileUtils.toString(file, "UTF-8").replaceAll("&", "&amp;")
			.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
			.replaceAll("\t", TAB_SPACES).replaceAll(" ", "&nbsp;")
			.replaceAll("\n", "<br/>");
		output.write(string.getBytes("UTF-8"));
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { "application/xml", "text/xml", "text/html" });
	}

	@Override
	public String getOutputContentType() {
		return DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE;
	}

	@Override
	public boolean isLossless() {
		return true;
	}
}
