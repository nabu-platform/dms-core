package be.nabu.libs.dms.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.dms.api.Converter;
import be.nabu.libs.dms.api.ConverterResolver;
import be.nabu.libs.dms.converters.ChainConverter;
import be.nabu.libs.dms.converters.PassThroughConverter;
import be.nabu.utils.io.ContentTypeMap;

public class SPIConverterResolver implements ConverterResolver {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Map<String, Map<String, Converter>> converters;
	
	public SPIConverterResolver() {
		// make sure this one is active
		ContentTypeMap.register();
		loadConverters();
	}
	
	@Override
	public Converter getConverter(String fromContentType, String toContentType) {
		// can not find any converters that start with this type
		if (!converters.containsKey(fromContentType))
			return null;
		// there is no explicit converter and we have not checked for a chained converter yet
		else if (!converters.get(fromContentType).containsKey(toContentType)) {
			if (fromContentType.equals(toContentType))
				converters.get(fromContentType).put(toContentType, new PassThroughConverter(fromContentType, toContentType));
			else {
				List<Converter> chain = getOptimalPath(fromContentType, toContentType);
				if (chain == null || chain.size() == 0)
					converters.get(fromContentType).put(toContentType, null);
				else
					converters.get(fromContentType).put(toContentType, new ChainConverter(chain));
			}
		}
		return converters.get(fromContentType).get(toContentType);
	}
	
	private void loadConverters() {
		ServiceLoader<Converter> serviceLoader = ServiceLoader.load(Converter.class);
		converters = new HashMap<String, Map<String, Converter>>();
		for (Converter converter : serviceLoader) {
			for (String inputContentType : converter.getContentTypes()) {
				if (!converters.containsKey(inputContentType))
					converters.put(inputContentType, new HashMap<String, Converter>());
				// don't overwrite a registered converter
				if (!converters.get(inputContentType).containsKey(converter.getOutputContentType()))
					converters.get(inputContentType).put(converter.getOutputContentType(), converter);
				
				// register extensions if they are not yet present
				if (ContentTypeMap.getInstance().getExtensionFor(inputContentType) == null)
					ContentTypeMap.getInstance().registerContentType(inputContentType, guessExtension(inputContentType));
				if (ContentTypeMap.getInstance().getExtensionFor(converter.getOutputContentType()) == null)
					ContentTypeMap.getInstance().registerContentType(converter.getOutputContentType(), guessExtension(converter.getOutputContentType()));
			}
		}
	}
	
	/**
	 * It takes the last bit of the contentType and uses it as an extension
	 * For example, "text/html" would become "html"
	 */
	private String guessExtension(String contentType) {
		return contentType.replaceAll("^.*?([\\w]+)$", "$1").toLowerCase();
	}
	
	private List<Converter> getOptimalPath(final String fromContentType, final String toContentType) {
		List<List<Converter>> possiblePaths = getConverterPaths(fromContentType, toContentType, new ArrayList<String>());
		if (possiblePaths == null)
			return null;
		logger.debug("Evaluating " + possiblePaths.size() + " possible paths");
		Collections.sort(possiblePaths, new Comparator<List<Converter>>() {

			@Override
			public int compare(List<Converter> arg0, List<Converter> arg1) {
				// sort inversely, the smaller the score the better
				return getScore(arg0).compareTo(getScore(arg1));
			}
			
			private Double getScore(List<Converter> path) {
				double score = 0;
				for (Converter converter : path) {
					if (converter == null) {
						// increase the score by ..a lot.. so it moves to the end of the list
						score += 1000;
						logger.error("Found a 'null' converter in path from " + fromContentType + " to " + toContentType + " path: " + path);
					}
					else if (converter instanceof ChainConverter)
						score += getScore(((ChainConverter) converter).getChain());
					else if (converter.isLossless())
						score += 1;
					else
						// why 1.6? it 'felt' like the right number...
						score += 1.6;
				}
				logger.debug("Path {}: {}", path, score);
				return score;
			}
		});
		logger.debug("Paths: {}", possiblePaths);
		return possiblePaths.size() > 0 ? possiblePaths.get(0) : null;
	}
	
	private List<List<Converter>> getConverterPaths(String fromContentType, String toContentType, List<String> alreadyChecked) {
		List<List<Converter>> converterPaths = new ArrayList<List<Converter>>();
		if (!converters.containsKey(fromContentType))
			return null;
		// direct path always wins
		else if (converters.get(fromContentType).containsKey(toContentType))
			converterPaths.add(
					new ArrayList<Converter>(
							Arrays.asList(new Converter [] { converters.get(fromContentType).get(toContentType) }))
			);
		else {
			for (String intermediate : converters.get(fromContentType).keySet()) {
				// the converter can be null if it has been resolved before and found to be impossible
				if (!alreadyChecked.contains(intermediate) && converters.get(fromContentType).get(intermediate) != null) {
					List<String> alreadyCheckedCopy = new ArrayList<String>();
					alreadyCheckedCopy.addAll(alreadyChecked);
					alreadyCheckedCopy.add(intermediate);
					List<List<Converter>> partialPaths = getConverterPaths(intermediate, toContentType, alreadyCheckedCopy);
					if (partialPaths != null) {
						// add the current converter to each path
						for (List<Converter> partialPath : partialPaths)
							partialPath.add(0, converters.get(fromContentType).get(intermediate));
						converterPaths.addAll(partialPaths);
//						List<Converter> path = new ArrayList<Converter>();
//						path.add(converters.get(fromContentType).get(intermediate));
//						path.addAll(partialPath);
//						return path;
					}
				}
			}
		}
		return converterPaths;
	}
}
