package be.nabu.libs.dms.converters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class ImageToDXF implements Converter {

	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String title = properties != null && properties.containsKey("title") ? properties.get("title") : file.getPath();
		String alt = properties != null && properties.containsKey("alt") ? properties.get("alt") : title;
		String css = properties != null && properties.containsKey("float") ? "style='float:" + properties.get("float") + "' " : "";
		String image = "<img src=\"" + SCHEME_STREAM + ":" + file.getPath() + "\"" + css + " reference=\"" + file.getPath() + "\" title=\"" + title + "\" alt=\"" + alt + "\"";
		if (properties != null) {
			if (properties.containsKey("width"))
				image += " width=\"" + properties.get("width") + "\"";
			if (properties.containsKey("height"))
				image += " height=\"" + properties.get("height") + "\"";
			if (properties.containsKey("class"))
				image += " class=\"" + properties.get("class") + "\"";
		}
		image += "/>";
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(image.getBytes("UTF-8"))), IOUtils.wrap(output));
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String[] { 
			"image/png", 
			"image/gif", 
			"image/jpeg",
			"image/tiff", 
			"image/bmp"
		});
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
