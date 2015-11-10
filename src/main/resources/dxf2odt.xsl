<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:drawing="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:xlink="http://www.w3.org/1999/xlink">

	<xsl:output omit-xml-declaration="yes"/>

	<xsl:template match="//body">
		<xsl:element name="office:body">
			<xsl:element name="office:text">
				<xsl:attribute name="text:use-soft-page-breaks">true</xsl:attribute>
				<xsl:apply-templates/>
			</xsl:element>
		</xsl:element>
	</xsl:template>
	
	<!-- tables -->
	<xsl:template match="table">
		<xsl:element name="table:table">
			<xsl:attribute name="table:style-name">Table</xsl:attribute>
			<xsl:call-template name="build-table-columns">
				<xsl:with-param name="table" select="."/>
			</xsl:call-template>
			<xsl:apply-templates/>	
		</xsl:element>
	</xsl:template>
	<xsl:template match="tr">
		<xsl:element name="table:table-row">
			<xsl:attribute name="table:style-name">TableRow</xsl:attribute>
			<xsl:apply-templates/>	
		</xsl:element>
	</xsl:template>
	<xsl:template match="td">
		<xsl:element name="table:table-cell">
			<xsl:attribute name="table:style-name">TableCell</xsl:attribute>
			<xsl:if test="@colspan > 1">
				<xsl:attribute name="table:number-columns-spanned"><xsl:value-of select="@colspan"/></xsl:attribute>
			</xsl:if>
			<xsl:if test="@rowspan > 1">
				<xsl:attribute name="table:number-rows-spanned"><xsl:value-of select="@rowspan"/></xsl:attribute>
			</xsl:if>
			<xsl:element name="text:p">
				<xsl:attribute name="text:style-name">paragraph</xsl:attribute>
				<xsl:apply-templates/>			
			</xsl:element>
		</xsl:element>
		<xsl:if test="@colspan > 1">
			<xsl:call-template name="loop-covered-cell">
				<xsl:with-param name="iteration" select="1"/>
				<xsl:with-param name="until" select="@colspan"/>
			</xsl:call-template>
		</xsl:if>		
	</xsl:template>
	
	<xsl:template name="loop-covered-cell">
		<xsl:param name="iteration"/>
		<xsl:param name="until"/>
		
		<xsl:if test="$iteration &lt; $until">
			<xsl:element name="table:covered-table-cell"/>
			<xsl:call-template name="loop-covered-cell">
				<xsl:with-param name="iteration" select="$iteration + 1"/>
				<xsl:with-param name="until" select="$until"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="build-table-columns">
		<xsl:param name="table"/>
		<xsl:element name="table:table-columns">
			<xsl:for-each select="$table/tr[1]/td">
				<xsl:element name="table:table-column">
					<xsl:attribute name="table:style-name">TableColumn</xsl:attribute>
				</xsl:element>
			</xsl:for-each>
		</xsl:element>
	</xsl:template>
	
	<!-- headers -->
	<xsl:template match="h1">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h1</xsl:attribute>
			<xsl:attribute name="text:outline-level">1</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="h2">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h2</xsl:attribute>
			<xsl:attribute name="text:outline-level">2</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="h3">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h3</xsl:attribute>
			<xsl:attribute name="text:outline-level">3</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="h4">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h4</xsl:attribute>
			<xsl:attribute name="text:outline-level">4</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="h5">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h5</xsl:attribute>
			<xsl:attribute name="text:outline-level">5</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="h6">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h6</xsl:attribute>
			<xsl:attribute name="text:outline-level">6</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="h7">
		<xsl:element name="text:h">
			<xsl:attribute name="text:style-name">h7</xsl:attribute>
			<xsl:attribute name="text:outline-level">7</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="u">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Underline</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="i">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Italic</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="b">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Bold</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="sub">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Subscript</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="sup">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Superscript</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="strong">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Strong</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="code">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Highlight</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="del">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Deleted</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="ins">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Inserted</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="em">
		<xsl:element name="text:span">
			<xsl:attribute name="text:style-name">Emphasized</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="hr">
		<xsl:element name="text:soft-page-break"/>
	</xsl:template>
		
	<xsl:template match="br">
		<!-- if you have a <br/> on the root (<body>) this should be shown with an empty paragraph instead of a linefeed because text:line-break is not allowed on the root of the document -->
		<xsl:choose>
			<xsl:when test="local-name(..) = 'body'"><xsl:element name="text:p"><xsl:attribute name="text:style-name">paragraph</xsl:attribute></xsl:element></xsl:when>
			<xsl:otherwise><xsl:element name="text:line-break"/></xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
	
	<xsl:template match="a">
		<xsl:choose>
			<!-- ignore anchors -->
			<xsl:when test="@name">
				<xsl:element name="text:span">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<!-- internal links can not be properly translated without the root url, so just ignore them atm -->			
			<xsl:when test="substring-before(@href, ':') = 'internal+link'">
				<xsl:element name="text:span">
					<xsl:apply-templates/>
				</xsl:element>			
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="text:a">
					<xsl:attribute name="xlink:href"><xsl:value-of select="@href"/></xsl:attribute>
					<xsl:element name="text:span">
						<xsl:attribute name="text:style-name">Hyperlink</xsl:attribute>
						<xsl:apply-templates/>
					</xsl:element>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- predefined stuff (mostly for text files) -->
	<xsl:template match="pre">
		<xsl:element name="text:p">
			<xsl:attribute name="text:style-name">paragraph</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="p">
		<xsl:choose>
			<xsl:when test="local-name(*[1]) != 'ul'
				and local-name(*[1]) != 'ol'
				and local-name(*[1]) != 'h1'
				and local-name(*[1]) != 'h2'
				and local-name(*[1]) != 'h3'
				and local-name(*[1]) != 'h4'
				and local-name(*[1]) != 'h5'
				and local-name(*[1]) != 'h6'
				and local-name(*[1]) != 'h7'">
				<xsl:element name="text:p">
					<xsl:attribute name="text:style-name">paragraph</xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="ul|ol">
		<xsl:choose>
			<xsl:when test="local-name(..) = 'ul' or local-name(..) = 'ol'">
				<xsl:element name="text:list-item">
					<xsl:element name="text:list">
						<xsl:attribute name="text:style-name">List</xsl:attribute>
						<xsl:attribute name="text:continue-numbering">true</xsl:attribute>
						<xsl:apply-templates/>
					</xsl:element>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="text:list">
					<xsl:attribute name="text:style-name">List</xsl:attribute>
					<xsl:attribute name="text:continue-numbering">true</xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="li">
		<xsl:element name="text:list-item">
			<xsl:element name="text:p">
				<xsl:attribute name="text:style-name">ListParagraph</xsl:attribute>
				<xsl:apply-templates/>
			</xsl:element>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="div">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="span">
		<xsl:choose>
			<!-- this is merely a wrapper to indicate that it was imported from another item
			it can be ignored in the odt -->
			<xsl:when test="@reference">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="text:span">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- used for quotes & lines in quotes -->
	<xsl:template match="blockquote">
		<xsl:element name="text:p">
			<xsl:attribute name="text:style-name">paragraph</xsl:attribute>
			<xsl:element name="drawing:frame">
				<xsl:attribute name="text:anchor-type">as-char</xsl:attribute>
	 			<xsl:attribute name="svg:width">6.2327in</xsl:attribute>
	 			<xsl:attribute name="svg:height">1in</xsl:attribute>
				<xsl:attribute name="style:rel-width">90%</xsl:attribute>
				<xsl:attribute name="style:rel-height">scale-min</xsl:attribute>
				<xsl:attribute name="drawing:style-name">TextBox</xsl:attribute>
				<xsl:element name="drawing:text-box">
					<xsl:element name="text:p">
						<xsl:attribute name="text:style-name">paragraph</xsl:attribute>
						<xsl:apply-templates/>
					</xsl:element>
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="img">
		<xsl:element name="text:p">
			<xsl:attribute name="text:style-name">paragraph</xsl:attribute>
			<xsl:element name="text:span">
				<xsl:element name="drawing:frame">
					<xsl:attribute name="drawing:style-name">wrappedImage</xsl:attribute>
					<xsl:attribute name="text:anchor-type">as-char</xsl:attribute>
					<xsl:attribute name="style:rel-width">scale</xsl:attribute>
					<xsl:attribute name="style:rel-height">scale</xsl:attribute>
					<xsl:attribute name="svg:width"><xsl:value-of select="@width"/>in</xsl:attribute>
					<xsl:attribute name="svg:height"><xsl:value-of select="@height"/>in</xsl:attribute>
					<xsl:attribute name="svg:x">0px</xsl:attribute>
					<xsl:attribute name="svg:y">0px</xsl:attribute>
					<xsl:attribute name="drawing:z-index">0</xsl:attribute>
					<xsl:element name="drawing:image">
						<xsl:attribute name="xlink:href"><xsl:value-of select="@src"/></xsl:attribute>
						<xsl:attribute name="xlink:show">embed</xsl:attribute>
						<xsl:attribute name="xlink:type">simple</xsl:attribute>
						<xsl:attribute name="xlink:actuate">onLoad</xsl:attribute>
					</xsl:element>
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>
	
	<!-- Do not normalize space because if you have for example <p>test1 <b>test2</b></p> the normalize would make it test1test2 instead of test1 test2
	<xsl:template match="text()"><xsl:value-of select="normalize-space()"/></xsl:template>
	 -->
</xsl:stylesheet>