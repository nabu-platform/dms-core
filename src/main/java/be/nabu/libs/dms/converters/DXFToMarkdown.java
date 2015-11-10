package be.nabu.libs.dms.converters;

import java.util.regex.Matcher;

public class DXFToMarkdown extends DXFToWiki {

	@Override
	protected String getXSLT() {
		return "dxf2markdown.xsl";
	}

	@Override
	protected String postProcessQuotes(String result) {
		// quotes after one another do not have the appropriate linefeeds
		result = result.replace("``````", "```\n\n```");
		return result;
	}

	@Override
	protected String createQuote(String targetType, String quote) {
		return "\n```" + (targetType.equals("txt") ? "" : targetType) + "\n" + Matcher.quoteReplacement(quote) + "\n```\n";
	}
	
	@Override
	public String getOutputContentType() {
		return MarkdownToDXF.CONTENT_TYPE;
	}
}
