// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//

package org.pathvisio.ora.gsea;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.pathvisio.ora.util.RowWithProperties;

public class GseaPathwayResult implements RowWithProperties<Column>, Comparable<GseaPathwayResult>
{
	private String pathwayName;
	private File pathwayFile;
	private String pathwayText;
	private JFreeChart pathwayChart;

	private double esMax = 0;
	private double pValue = 0;
	private List<Double> esDrawing;
	private List<String> core;
	private List<String> ids;
	
	private List<String> idsNotMapped;

	public GseaPathwayResult(List<Double> esDrawing, List<String> coreEnrichment,
			List<String> ids, double esMax, double pValue)
	{
		this.esDrawing = esDrawing;
		this.core = coreEnrichment;
		this.ids = ids;
		this.esMax = esMax;
		this.pValue = pValue;
		idsNotMapped = new ArrayList<String>();
	}

	public void createInfo(String setName, int perm) 
	{
		DecimalFormat df = new DecimalFormat("0.000");

		StringBuilder results = new StringBuilder();
		String res;
		if (getEsMax() > 0) {
			res = " The gene set \" " + setName
					+ " \" is upregulated in phenotype 1 ";
		} else {
			res = " The gene set \" " + setName
					+ " \" is upregulated in phenotype 2 ";
		}

		results.append(" GSEA results for " + setName + "\n Number of genes : "
				+ ids.size() + "\n Number of permutations : " + perm
				+ "\n P-Value : " + df.format(getPValue()) + "\n ES max : "
				+ df.format(getEsMax()) + "\n" + res
				+ "\n The genes in core enrichment are : ");

		for (int s = 0; s < core.size(); s++) {
			results.append("\n" + core.get(s));
		}

		// using JFreeChart for ES plot
		XYSeries series = new XYSeries(" ES value ");
		for (int i = 0; i < esDrawing.size(); i++) {
			series.add(i, esDrawing.get(i));
		}

		final NumberAxis xAxis = new NumberAxis();
		xAxis.setRange(0, esDrawing.size());
		XYDataset xyDataset = new XYSeriesCollection(series);

		pathwayChart = ChartFactory.createXYLineChart(
				" Enrichment plot for " + setName, " ranked gene list",
				" ES score ", xyDataset, PlotOrientation.VERTICAL, true, false,
				false);

		this.pathwayName = setName;
		this.pathwayText = results.toString();
	}
	
	public void addIdNotMapped(String id) {
		if(id != null && !idsNotMapped.contains(id)) {
			idsNotMapped.add(id);
		}
	}

	public String getPwName(){
		return pathwayName;
	}
	
	public String getPwText(){
		return pathwayText;
	}
	
	public JFreeChart getPwChart(){
		return pathwayChart;
	}
	
	public String toString()
	{
		return pathwayName;
	}

	@Override
	public String getProperty(Column prop)
	{
		switch (prop)
		{
		case PATHWAY_NAME:
			return pathwayName;
		case ES_MAX:
			return "" + esMax;
		case PVAL:
			return "" + pValue;
		}
		
		return null;
	}

	public double getPValue()
	{
		return pValue;
	}

	public double getEsMax()
	{
		return esMax;
	}

	@Override
	public int compareTo(GseaPathwayResult arg0)
	{
		return Double.compare(arg0.esMax, esMax);
	}

	public File getPathwayFile() {
		return pathwayFile;
	}

	public void setPathwayFile(File pathwayFile) {
		this.pathwayFile = pathwayFile;
	}
}
