package org.pathvisio.ora.gsea;

import org.pathvisio.ora.util.ListWithPropertiesTableModel;

public class GseaResults extends ListWithPropertiesTableModel<Column, GseaPathwayResult>
{
	public GseaResults()
	{
		setColumns(new Column[] { Column.PATHWAY_NAME, Column.ES_MAX, Column.PVAL });
	}
}
