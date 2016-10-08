package be.nabu.libs.dms.converters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.dms.utils.FileUtils;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

/**
 * TODO: Should add the "<article>" tag if indicated by the properties (html 5 feature)
 */
public class DXFToHTML extends DXFConverter {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void convert(DocumentManager repository, File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		String content = FileUtils.toString(file, "UTF-8");
		
		// transform the blockquotes
		content = transformQuotes(repository, file, content, properties, false, getOutputContentType());
		content = replaceIncludes(repository, file, content, getOutputContentType(), properties);
		
		// replace tabs
		content = content.replaceAll("\t", TAB_SPACES);
		content = content.replaceAll("&#160;", "&nbsp;");

		logger.debug("Converting to HTML: replacing internal links");
		
		// replace internal links
		if (properties != null && Boolean.TRUE.toString().equals(properties.get("embed"))) {
			try {
				content = DXFToSlides.embedImages(file, content);
			}
			catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			String downloadPath = properties == null ? null : properties.get("downloadPath");
			String viewPath = properties == null ? null : properties.get("viewPath");
			content = content.replaceAll("(?s)(href|src)([\\s'\"=]+)" + Pattern.quote(SCHEME_LINK) + ":", "$1$2/" + (viewPath == null ? "view" : viewPath));
			content = content.replaceAll("(?s)(href|src)([\\s'\"=]+)" + Pattern.quote(SCHEME_STREAM) + ":", "$1$2/" + (downloadPath == null ? "download" : downloadPath));
		}
		
		IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(content.getBytes("UTF-8"))), IOUtils.wrap(output));
	}

	@Override
	public List<String> getContentTypes() {
		return Arrays.asList(new String [] { DOCUMENT_EXCHANGE_FORMAT_CONTENT_TYPE });
	}

	@Override
	public String getOutputContentType() {
		return "text/html";
	}

	@Override
	public boolean isLossless() {
		return true;
	}

}
