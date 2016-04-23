package be.nabu.libs.dms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.datastore.api.WritableDatastore;
import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.ConverterResolver;
import be.nabu.libs.dms.api.DocumentCacheManager;
import be.nabu.libs.dms.api.DocumentManager;
import be.nabu.libs.dms.api.FormatException;
import be.nabu.libs.vfs.api.File;
import be.nabu.utils.io.IOUtils;

public class SimpleDocumentManager implements DocumentManager {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private ConverterResolver converterResolver;
	private DocumentCacheManager cacheManager;
	private WritableDatastore datastore;
	
	/**
	 * The maximum size a file can be while still being cached. If bigger, the file is never cached
	 * Note that <= 0 means that all files are cached
	 */
	private long cacheSizeLimit = 0;
	
	/**
	 * The list of target content types that are cached, e.g. if you use a html interface you may want to cache all conversions to "text/html"
	 * An empty list means all target content types are cached
	 */
	private List<String> cacheContentTypes = new ArrayList<String>();
	
	public SimpleDocumentManager() {
		this(new SPIConverterResolver());
	}
	
	public SimpleDocumentManager(DocumentCacheManager cacheManager) {
		this.cacheManager = cacheManager;
		this.converterResolver = new SPIConverterResolver();
	}
	
	public SimpleDocumentManager(ConverterResolver converterResolver) {
		this.converterResolver = converterResolver;
	}
	
	@Override
	public Converter getConverter(String fromContentType, String toContentType) {
		return converterResolver.getConverter(fromContentType, toContentType);
	}
	
	@Override
	public boolean canConvert(String fromContentType, String toContentType) {
		return getConverter(fromContentType, toContentType) != null;
	}

	/**
	 * The cache manager is only used if properties are null, otherwise it can not guarantee the correct result
	 */
	@Override
	public void convert(File file, String toContentType, OutputStream output, Map<String, String> properties) throws IOException, FormatException {
		DocumentCacheManager cacheManager = getCacheManager();
		if (properties == null && cacheManager != null) {
			byte [] content = cacheManager.getCached(file, toContentType);
			if (content != null) {
				logger.debug("Cache hit for {} to " + toContentType, file);
				IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(content)), IOUtils.wrap(output));
				return;
			}
		}
		
		Converter converter = converterResolver.getConverter(file.getContentType(), toContentType);
		if (converter == null)
			throw new IllegalArgumentException("Can not convert " + file.getContentType() + " to " + toContentType + ", no converter exists");
		
		if (properties == null && cacheManager != null && (cacheSizeLimit <= 0 || file.getSize() < cacheSizeLimit) && (cacheContentTypes.size() == 0 || cacheContentTypes.contains(toContentType))) {
			logger.debug("Cache miss for {} to " + toContentType, file);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			converter.convert(this, file, buffer, properties);
			cacheManager.setCached(file, toContentType, buffer.toByteArray());
			IOUtils.copyBytes(IOUtils.wrap(new ByteArrayInputStream(buffer.toByteArray())), IOUtils.wrap(output));
		}
		else
			converter.convert(this, file, output, properties);
	}

	public DocumentCacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(DocumentCacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public long getCacheSizeLimit() {
		return cacheSizeLimit;
	}

	public void setCacheSizeLimit(long cacheSizeLimit) {
		this.cacheSizeLimit = cacheSizeLimit;
	}

	public List<String> getCacheContentTypes() {
		return cacheContentTypes;
	}

	@Override
	public WritableDatastore getDatastore(File file) {
		return datastore == null ? new FileDatastore(file) : datastore;
	}

	public void setDatastore(WritableDatastore datastore) {
		this.datastore = datastore;
	}
}
