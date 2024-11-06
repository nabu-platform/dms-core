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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class ImageToDXF implements Converter {

	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String title = properties != null && properties.containsKey("title") ? properties.get("title") : file.getPath();
		String alt = properties != null && properties.containsKey("alt") ? properties.get("alt") : title;
		String css = properties != null && properties.containsKey("float") ? "style='float:" + properties.get("float") + "' " : "";
		// encode the path if it contains a scheme
		String path = file.getPath().matches("^[\\w]+:.*") ? URIUtils.encodeURIComponent(file.getPath()) : file.getPath();
		String image = "<img src=\"" + SCHEME_STREAM + ":" + path + "\"" + css + " reference=\"" + file.getPath() + "\" title=\"" + title + "\" alt=\"" + alt + "\"";
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
