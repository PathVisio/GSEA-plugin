package org.pathvisio.ora.gsea;

import org.pathvisio.ora.util.PropertyColumn;

public enum Column implements PropertyColumn
{
	PATHWAY_NAME("Pathway"),
	ES_MAX("Max. Enrichment Score"),
	PVAL("P-value"),
	;

	private String title;
	
	private Column(String title) { this.title = title; }
	
	@Override
	public String getTitle()
	{
		return title;
	}
}
