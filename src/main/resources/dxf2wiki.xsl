<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:drawing="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:xlink="http://www.w3.org/1999/xlink">

	<xsl:strip-space elements="*"/>
	<xsl:param name="imageRoot"/>
	
	<xsl:output omit-xml-declaration="yes" method="text"/>


	<xsl:template name="lf"><xsl:text>
</xsl:text></xsl:template>

	<xsl:template match="/body">
		<xsl:apply-templates>
			<xsl:with-param name="depth" select="0"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="meta">
		<xsl:text>@</xsl:text><xsl:value-of select="@name"/>
		<xsl:if test="@content">
			<xsl:text>=</xsl:text><xsl:value-of select="@content"/>
		</xsl:if>
		<xsl:call-template name="lf"/>
	</xsl:template>
	
	<!-- tables -->
	<xsl:template match="table">
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/>
			<xsl:apply-templates/>
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/>
	</xsl:template>

	<!-- for a thead, add an additional | at the front, the tr will add another -->	
	<xsl:template match="thead">
		<xsl:text>|</xsl:text>
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="tr">
		<xsl:choose>
			<xsl:when test="@class = 'header'">
				<xsl:text>||</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>|</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:apply-templates/>
		<xsl:call-template name="lf"/>	
	</xsl:template>
	
	<xsl:template match="td">
		<xsl:if test="@class='important'">
			<xsl:text>!</xsl:text>
		</xsl:if>
		<xsl:if test="@class='highlight'">
			<xsl:text>@</xsl:text>
		</xsl:if>
		<xsl:value-of select="translate(., '&#10;&#13;', '')"/>
		<xsl:if test="@class='important'">
			<xsl:text>!</xsl:text>
		</xsl:if>		
		<xsl:if test="@class='highlight'">
			<xsl:text>@</xsl:text>
		</xsl:if>
		<xsl:text>|</xsl:text>			
	</xsl:template>
	<xsl:template match="col">
		<!-- do nothing -->
	</xsl:template>
	<xsl:template match="tbody">
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- headers -->
	<xsl:template match="h1">
		<xsl:call-template name="lf"/><xsl:text>h1. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	<xsl:template match="h2">
		<xsl:call-template name="lf"/><xsl:text>h2. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	<xsl:template match="h3">
		<xsl:call-template name="lf"/><xsl:text>h3. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	<xsl:template match="h4">
		<xsl:call-template name="lf"/><xsl:text>h4. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	<xsl:template match="h5">
		<xsl:call-template name="lf"/><xsl:text>h5. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	<xsl:template match="h6">
		<xsl:call-template name="lf"/><xsl:text>h6. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	<xsl:template match="h7">
		<xsl:call-template name="lf"/><xsl:text>h7. </xsl:text><xsl:value-of select="."/><xsl:call-template name="lf"/>
	</xsl:template>
	
	<xsl:template match="u">
		<xsl:text>__</xsl:text><xsl:value-of select="."/><xsl:text>__</xsl:text>
	</xsl:template>
	<xsl:template match="i">
		<xsl:text>++</xsl:text><xsl:value-of select="."/><xsl:text>++</xsl:text>
	</xsl:template>
	<xsl:template match="b">
		<xsl:text>**</xsl:text><xsl:value-of select="."/><xsl:text>**</xsl:text>
	</xsl:template>
	<xsl:template match="del">
		<xsl:text>~~</xsl:text><xsl:value-of select="."/><xsl:text>~~</xsl:text>
	</xsl:template>
	<xsl:template match="sup">
		<xsl:text>+^</xsl:text><xsl:value-of select="."/><xsl:text>^</xsl:text>
	</xsl:template>
	<xsl:template match="sub">
		<xsl:text>-^</xsl:text><xsl:value-of select="."/><xsl:text>^</xsl:text>
	</xsl:template>
	<xsl:template match="strong">
		<xsl:text>!!</xsl:text><xsl:apply-templates/><xsl:text>!!</xsl:text>
	</xsl:template>
	<xsl:template match="code">
		<xsl:text>@@</xsl:text><xsl:value-of select="."/><xsl:text>@@</xsl:text>
	</xsl:template>
	<!-- this is used to emphasize headers in a list, e.g. <li><em>header</em>: text</li>
	the wiki syntax is:
	* :header: text -->
	<xsl:template match="em">
		<xsl:text>:</xsl:text><xsl:value-of select="."/>
	</xsl:template>
		
	<xsl:template match="br">
		<xsl:call-template name="lf"/>
	</xsl:template>
	
	<xsl:template match="a">
		<xsl:choose>
			<!-- an anchor -->
			<xsl:when test="@name">
				<xsl:if test="@original">
					<xsl:text>[#</xsl:text><xsl:value-of select="@original"/><xsl:text>]</xsl:text>
				</xsl:if>
			</xsl:when>
			<!-- when the original link is interpreted to generate the html, this attribute will contain the original link -->
			<xsl:when test="@original">
				<xsl:choose>
					<!-- an anchor reference -->
					<xsl:when test="substring(@href, 0, 1) = '#'">
						<xsl:text>[</xsl:text>
						<!-- only write the display name if it was explicitly set, otherwise it is a generated one -->
						<xsl:if test="@hasDisplayName = 'true'">
							<xsl:value-of select="normalize-space()"/><xsl:text>|</xsl:text>
						</xsl:if>
						<xsl:text>#</xsl:text><xsl:value-of select="@original"/><xsl:text>]</xsl:text> 
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>[</xsl:text>
						<xsl:if test="@hasDisplayName = 'true'">
							<xsl:value-of select="normalize-space()"/><xsl:text>|</xsl:text>
						</xsl:if>
						<xsl:text>$</xsl:text><xsl:value-of select="@original"/><xsl:text>]</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>[</xsl:text><xsl:value-of select="normalize-space()"/><xsl:text>|</xsl:text><xsl:value-of select="@href"/><xsl:text>]</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- predefined stuff (mostly for text files) -->
	<xsl:template match="pre">
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/><xsl:value-of select="."/><xsl:call-template name="lf"/><xsl:call-template name="lf"/>
	</xsl:template>
	
	<xsl:template match="p">
		<!-- <xsl:call-template name="lf"/><xsl:call-template name="lf"/><xsl:value-of select="."/><xsl:apply-templates select="span | p"/><xsl:call-template name="lf"/><xsl:call-template name="lf"/> -->
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/><xsl:apply-templates/><xsl:call-template name="lf"/><xsl:call-template name="lf"/>
	</xsl:template>
	
	<xsl:template match="hr">
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/><xsl:text>--</xsl:text><xsl:call-template name="lf"/><xsl:call-template name="lf"/>
	</xsl:template>
	
	<xsl:template name="print-list-depth">
		<xsl:param name="list"/>
		<xsl:text>*</xsl:text>
		<xsl:if test="local-name(parent::*[1]) = 'ul'">
			<xsl:call-template name="print-list-depth">
				<xsl:with-param name="list" select="parent::*[1]"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="print-list-depth2">
		<xsl:param name="depth"/>
		<xsl:param name="character"/>
		<xsl:text><xsl:value-of select="$character"/></xsl:text>
		<xsl:if test="$depth &gt; 1">
			<xsl:call-template name="print-list-depth2">
				<xsl:with-param name="depth" select="$depth - 1"/>
				<xsl:with-param name="character" select="$character"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="ul|ol">
		<xsl:param name="depth"/>
		<!-- a leading linefeed for the opening of the list -->
		<xsl:if test="$depth = 0">
			<xsl:call-template name="lf"/>
		</xsl:if>
		<xsl:apply-templates>
			<xsl:with-param name="depth" select="$depth + 1"/>
		</xsl:apply-templates>
	</xsl:template>
	
	<xsl:template match="li">
		<xsl:param name="depth"/>
		<xsl:choose>
			<xsl:when test="local-name(parent::*[1]) = 'ol'">
				<xsl:call-template name="print-list-depth2">
					<xsl:with-param name="depth" select="$depth"/>
					<xsl:with-param name="character">#</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="print-list-depth2">
					<xsl:with-param name="depth" select="$depth"/>
					<xsl:with-param name="character">*</xsl:with-param>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text> </xsl:text>
		<xsl:apply-templates>
			<xsl:with-param name="depth" select="$depth"/>
		</xsl:apply-templates><xsl:call-template name="lf"/>
	</xsl:template>
	
	<!-- divs should not be generated by the browser (update: chrome generates divs if you don't explicitly set it to paragraph)
	They can be used (like the wiki format) to convey special blocks though
	However they should be reverted to their original format, NOT from the html to the wiki format otherwise they will lose all value
	For this reason the parser must pick these up and convert them manually
	They can be referenced by the "quote-id" attribute and replaced by searching for [quote=id]
	 -->
	<xsl:template match="blockquote">
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/>
		<xsl:choose>
			<!-- it could be that the quote block itself is added, just use that then -->
			<xsl:when test="substring(normalize-space(string(.)), 1, 6) = '[quote'">
				<xsl:call-template name="lf"/>
				<xsl:apply-templates/>
			</xsl:when>
			<!-- this is if there is no quote block around it yet -->
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="@format">
						<xsl:text>[quote|</xsl:text><xsl:value-of select="@format"/><xsl:text>]</xsl:text>
					</xsl:when>
					<xsl:otherwise><xsl:text>[quote]</xsl:text></xsl:otherwise>
				</xsl:choose>
				<xsl:call-template name="lf"/>
				<xsl:choose>
					<xsl:when test="@quote-id">
						<xsl:text>[quote=</xsl:text><xsl:value-of select="@quote-id"/><xsl:text>]</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates/>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:call-template name="lf"/>
				<xsl:text>[/quote]</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/>
	</xsl:template>
	
	<xsl:template match="span">
		<xsl:param name="depth"/>
		<xsl:choose>
			<!-- DEPRECATED: use link
			the "reference" attribute is used by the wiki to indicate external articles
			They should not be transformed when generating wiki format, instead, use the include statement -->
			<xsl:when test="@reference">
				<xsl:call-template name="lf"/><xsl:call-template name="lf"/><xsl:text>[:</xsl:text><xsl:value-of select="@reference"/><xsl:text>]</xsl:text><xsl:call-template name="lf"/><xsl:call-template name="lf"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates>
					<xsl:with-param name="depth" select="$depth"/>
				</xsl:apply-templates>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="link">
		<xsl:call-template name="lf"/><xsl:call-template name="lf"/><xsl:text>[:</xsl:text><xsl:value-of select="@href"/><xsl:text>]</xsl:text><xsl:call-template name="lf"/><xsl:call-template name="lf"/>	
	</xsl:template>
	
	<xsl:template match="img">
		<xsl:choose>
			<xsl:when test="starts-with(@src, $imageRoot)">
				<xsl:text>[:</xsl:text><xsl:value-of select="substring-after(@src, $imageRoot)"/><xsl:text>]</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>[:</xsl:text><xsl:value-of select="@src"/><xsl:text>]</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- generated by chrome instead of <p> so make it a paragraph (not always?)
	also generated by chrome instead of <br> in the case of a paste... -->
	<xsl:template match="div">
		<!-- <xsl:call-template name="lf"/> --><xsl:call-template name="lf"/><xsl:apply-templates/><xsl:call-template name="lf"/><!-- <xsl:call-template name="lf"/> -->
	</xsl:template>
	
</xsl:stylesheet>