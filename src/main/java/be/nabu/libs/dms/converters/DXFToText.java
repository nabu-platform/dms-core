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
import be.nabu.libs.dms.utils.FileUtils;
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
