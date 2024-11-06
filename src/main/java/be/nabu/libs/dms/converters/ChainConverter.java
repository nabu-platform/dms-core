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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.dms.MemoryFileFragment;
import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

/**
 * Allows you to chain together multiple converters into a single converter
 */
public class ChainConverter implements Converter {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private List<Converter> chain;
	private List<String> fromContentTypes;
	private String toContentType;
	
	public ChainConverter(List<Converter> chain) {
		this.chain = chain;
		fromContentTypes = chain.get(0).getContentTypes();
		toContentType = chain.get(chain.size() - 1).getOutputContentType();
		logger.debug("Creating chain converter consisting of: {}", chain);
	}
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		byte [] result = null;
		String currentContentType = null;
		for (Converter converter : chain) {
			File fileToConvert = result == null ? file : new MemoryFileFragment(file, result, "converted", currentContentType);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			converter.convert(repository, fileToConvert, buffer, properties);
			result = buffer.toByteArray();
			currentContentType = converter.getOutputContentType();
		}
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(result)), IOUtils.wrap(output));
	}

	@Override
	public List<String> getContentTypes() {
		return fromContentTypes;
	}

	@Override
	public String getOutputContentType() {
		return toContentType;
	}

	@Override
	public boolean isLossless() {
		for (Converter converter : chain) {
			if (!converter.isLossless())
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Converter converter : chain)
			builder.append(converter.toString()).append("\n");
		return builder.toString();
	}
	
	public List<Converter> getChain() {
		return chain;
	}
}
