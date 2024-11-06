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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
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
