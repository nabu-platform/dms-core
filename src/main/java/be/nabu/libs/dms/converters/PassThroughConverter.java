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
