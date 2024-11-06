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
