<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:o="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:t="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:d="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:x="http://www.w3.org/1999/xlink" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0">
	
	<xsl:output omit-xml-declaration="yes"/>
	
	<xsl:param name="url"/>
	
	<xsl:template match="/o:document-content/o:body/o:text">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="t:h">
		<xsl:variable name="level"><xsl:value-of select="@t:outline-level"/></xsl:variable>
		<xsl:element name="h{$level}">
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="t:table-of-content">
		<!-- do nothing -->
	</xsl:template>
	
	<!-- an empty space -->
	<xsl:template match="t:s">
		<xsl:text>&#160;</xsl:text>
	</xsl:template>
	<!-- a tab -->
	<xsl:template match="t:tab">
		<xsl:text>	</xsl:text>
	</xsl:template>
	<!-- a line break -->
	<xsl:template match="t:line-break">
		<xsl:element name="br"/>
	</xsl:template>
	
	<xsl:template match="t:soft-page-break">
		<xsl:element name="hr"/>
	</xsl:template>
	
	<!-- a formatted number -->
	<xsl:template match="t:number">
		<xsl:apply-templates/>
	</xsl:template>

	<!-- tables -->	
	<xsl:template match="table:table">
		<xsl:element name="table">
			<xsl:apply-templates/>	
		</xsl:element>
	</xsl:template>
	<xsl:template match="table:table-row">
		<xsl:element name="tr">
			<xsl:apply-templates/>	
		</xsl:element>
	</xsl:template>
	<xsl:template match="table:table-cell">
		<xsl:element name="td">
			<xsl:if test="@table:number-columns-spanned > 1">
				<xsl:attribute name="colspan"><xsl:value-of select="@table:number-columns-spanned"/></xsl:attribute>
			</xsl:if>
			<xsl:if test="@table:number-rows-spanned > 1">
				<xsl:attribute name="rowspan"><xsl:value-of select="@table:number-rows-spanned"/></xsl:attribute>
			</xsl:if>
			<xsl:apply-templates/>	
		</xsl:element>
	</xsl:template>
	<xsl:template match="table:table-header-rows">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="table:table-rows">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="table:table-row-group">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="table:table-column-group">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="table:table-header-columns">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="table:table-columns">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="t:a">
		<xsl:element name="a">
			<!-- always external links when coming from odt -->
			<xsl:attribute name="href"><xsl:value-of select="@x:href"/></xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="t:p">
		<!-- the table-of-content "header" so to speak is kept in an inconspicuous 'p' element, this clears it -->
		<xsl:if test="local-name(following-sibling::*[1]) != 'table-of-content'">
			<xsl:choose>
				<!-- odt will always nest elements inside a list-item in another tag, like p, skip it (it is added again when transforming from html to odt) -->
				<xsl:when test="local-name(parent::*) = 'list-item' or local-name(parent::*) = 'table-cell' or local-name(parent::*) = 'text-box' or local-name(child::*) = 'frame'">
					<xsl:apply-templates/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:element name="p">
						<xsl:if test="@t:style-name">
							<xsl:attribute name="style"><xsl:value-of select="@t:style-name"/></xsl:attribute>
						</xsl:if>
						<xsl:apply-templates/>
					</xsl:element>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	
	
	<xsl:template match="t:list">
		<xsl:element name="ul">
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="t:list-item">
		<xsl:choose>
			<!-- openoffice does not accept <ul><li></li><ul></ul></ul>, it requires each nested ul to be inside a li
			(note that word does not make this distinction)
			however in the html spec the reverse is true, a ul element must not be inside a li element
			this renders rather oddly in both firefox & chrome so if the li only exists exists to express the ul, ignore its -->
			<xsl:when test="local-name(child::*) = 'list'">
				<xsl:apply-templates select="t:list"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="li">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>	
	
	<xsl:template match="t:span">
		<xsl:choose>
			<xsl:when test="local-name(parent::*) = 'a'">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Bold'">
				<xsl:element name="b">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Underline'">
				<xsl:element name="u">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Italic'">
				<xsl:element name="i">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Subscript'">
				<xsl:element name="sub">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Superscript'">
				<xsl:element name="sup">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Strong'">
				<xsl:element name="strong">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Highlight'">
				<xsl:element name="code">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Deleted'">
				<xsl:element name="del">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:when test="@t:style-name = 'Emphasized'">
				<xsl:element name="em">
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="span">
					<xsl:attribute name="class"><xsl:value-of select="@t:style-name"/></xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="d:frame">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="d:text-box">
		<xsl:element name="blockquote">
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="d:image">
		<xsl:element name="img">
			<xsl:attribute name="src"><xsl:value-of select="@x:href"/></xsl:attribute>
		</xsl:element>
	</xsl:template>
	
	<!-- 
	<xsl:template match="text()"><xsl:value-of select="normalize-space()"/></xsl:template>
	-->
</xsl:stylesheet>