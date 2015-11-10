package be.nabu.libs.dms.converters;

import java.io.ByteArrayInputStream;
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
import be.nabu.utils.io.IOUtils;

public class DXFToText implements Converter {

	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = FileUtils.toString(file, "UTF-8").replaceAll("\r", "");
		
		// make sure no rogue linefeeds pollute the result
		content = content.replaceAll("\n", "");

		// fix spaces and linefeeds
		content = content.replaceAll("(?s)<span[^>]*>(.*?)</span><br/>", "$1\n");
		
		content = content.replaceAll("&nbsp;", " ");
		
		// unescape
		content = content.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");

		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(content.getBytes("UTF-8"))), IOUtils.wrap(output));
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE);
	}

	@Override
	public String getOutputContentType() {
		return "text/plain";
	}

	@Override
	public boolean isLossless() {
		return false;
	}

}
