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

public class TextToDXF implements Converter {

	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = FileUtils.toString(file, "UTF-8").replaceAll("\r", "");
		// escape
		content = content.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

		content = content.replaceAll(" ", "&nbsp;");
		
		// fix spaces and linefeeds
		content = content.replaceAll("(?m)^(.*)$", "<span class=\"line\">$1</span><br/>");
		
		// the lines have been created, remove linefeeds
		content = content.replaceAll("\n", "");
		
		// add surrounding paragraph
		content = "<p>" + content + "</p>";
		
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(content.getBytes("UTF-8"))), IOUtils.wrap(output));
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] {
			// default
			"text/plain",
			// custom
			"text/x-plain.ini",
			"text/x-plain.properties",
			// other formats that have their own converters but not to dxf
			// when formatting in a specific output format, the more specific converters will be chosen
			// these are mostly for when explicitly converting to dxf (e.g. for search)
			// EDIT: this intermediate converter is used when converting to say ODT, e.g. "java > dxf > odt"
			// this means any blockquotes are converted to their own odt (their own zip file) and then put into the parent odt file
			// EDIT: on the other hand, xml tags are stripped for things like search cause they are assumed to be html tags... dxf-level transformation would be nice...
			"application/xml",
			"text/xml",
			"text/x-java-source",
			"text/x-c",
			"application/x-javascript",
			"text/x-script.python",
			"text/x-script.scheme",
			"text/x-script.sh",
			"text/x-script.tcl",
			"text/x-script.tcsh",
			"text/x-script.zsh",
			"text/x-diff"
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
