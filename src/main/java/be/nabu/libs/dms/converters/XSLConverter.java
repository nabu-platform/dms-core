package be.nabu.libs.dms.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.xml.XMLUtils;

abstract public class XSLConverter implements Converter {
	
	private Transformer transformer;
	private boolean usesProperties = false;
	
	/**
	 * 
	 * @param xsl
	 * @param usesProperties If set to true, the transformer is synchronized in order to allow for the correct properties. It is adviseable to turn this off if not needed
	 * 		Note that if you set this to false and still pass in properties, they are ignored.
	 * @throws IOException
	 */
	public XSLConverter(URL xsl, boolean usesProperties) throws IOException {
		initialize(xsl);
	}
	
	private void initialize(URL url) throws IOException {
		InputStream xsl = url.openStream();
		if (xsl == null)
			throw new RuntimeException("Could not find dxf2odt.xsl");
		try {
			transformer = XMLUtils.newTransformer(new StreamSource(xsl), null);
		}
		catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
		finally {
			xsl.close();
		}
	}
	
	protected void transform(File file, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		InputStream input = file.getInputStream();
		try {
			transform(input, output, properties);
		}
		finally {
			input.close();
		}
	}
	
	protected void transform(InputStream input, OutputStream output, Map<String, String> properties) throws FormatException {
		try {
			if (!usesProperties)
				transformer.transform(new StreamSource(input), new StreamResult(output));
			else {
				synchronized(transformer) {
					transformer.clearParameters();
					if (properties != null) {
						for (String key : properties.keySet())
							transformer.setParameter(key, properties.get(key));
					}
					transformer.transform(new StreamSource(input), new StreamResult(output));
				}
			}
		}
		catch (TransformerException e) {
			throw new FormatException(e);
		}
	}
}
