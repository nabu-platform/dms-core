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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import be.nabu.libs.datastore.api.DataProperties;
import be.nabu.libs.datastore.api.WritableDatastore;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.IOUtils;

public class FileDatastore implements WritableDatastore {

	private File file;

	public FileDatastore(File file) {
		this.file = file;
	}
	
	@Override
	public InputStream retrieve(URI uri) throws IOException {
		if (!uri.getPath().startsWith(".resources/")) {
			return null;
		}
		String path = uri.getPath().substring(".resources/".length());
		File target = file.getParent().resolve(".resources");
		if (target.exists()) {
			final File resolve = target.resolve(path);
			if (resolve.exists()) {
				return resolve.getInputStream();
			}
		}
		return null;
	}

	@Override
	public DataProperties getProperties(URI uri) throws IOException {
		if (!uri.getPath().startsWith(".resources/")) {
			return null;
		}
		String path = uri.getPath().substring(".resources/".length());
		File target = file.getParent().resolve(".resources");
		if (target.exists()) {
			final File resolve = target.resolve(path);
			if (resolve.exists()) {
				return new DataProperties() {
					@Override
					public Long getSize() {
						try {
							return resolve.getSize();
						}
						catch (IOException e) {
							throw new RuntimeException();
						}
					}
					@Override
					public String getName() {
						return resolve.getName();
					}
					@Override
					public String getContentType() {
						try {
							return resolve.getContentType();
						}
						catch (IOException e) {
							throw new RuntimeException();
						}
					}
					@Override
					public Date getLastModified() {
						try {
							return resolve.getLastModified();
						}
						catch (IOException e) {
							throw new RuntimeException();
						}
					}
					
				};
			}
		}
		return null;
	}

	@Override
	public URI store(InputStream input, String name, String contentType) throws IOException {
		File target = file.getParent().resolve(".resources");
		if (!target.exists()) {
			target.mkdir();
		}
		
		String extension = ContentTypeMap.getInstance().getExtensionFor(contentType);
		if (name != null && !name.endsWith("." + extension)) {
			name += "." + extension;
		}
		int counter = 0;
		// make sure it's unique
		while (name == null || name.trim().isEmpty() || target.resolve(name).exists()) {
			name = name == null || name.trim().isEmpty()
				? "unnamed" + counter++ + "." + extension
				: name.replaceFirst("^([^.0-9]+)[0-9]*", "$1" + counter++);
		}
		File resolve = target.resolve(name);
		OutputStream output = resolve.getOutputStream();
		try {
			IOUtils.copyBytes(IOUtils.wrap(input), IOUtils.wrap(output));
		}
		finally {
			output.close();
		}
		try {
			return new URI(URIUtils.encodeURI(".resources/" + name));
		}
		catch (URISyntaxException e) {
			throw new IOException("Could not create uri", e);
		}
	}

}
