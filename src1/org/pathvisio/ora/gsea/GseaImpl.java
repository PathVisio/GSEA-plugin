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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.pathvisio.data.DataException;
import org.pathvisio.data.DataInterface;
import org.pathvisio.data.IRow;
import org.pathvisio.data.ISample;
import org.xml.sax.SAXException;

public class GseaImpl 
{
	private List<Double> esForChart ;
	private List<Double> esList ;
	private List<String> coreEnrichment ;
	private List<String> geneSetInDataset;
	private Map<String, Double>  snrMap;
	private double totalSnr;
	private static final DecimalFormat DF = new DecimalFormat("0.000");

	/** 
	 * Shuffle sample lists. returned lists have the same length, but 
	 * values may be interchanged
	 */
	private void permuteSamples(List<Integer> sampleIdx1, List<Integer> sampleIdx2)
	{
		int size1 = sampleIdx1.size();
		int size2 = sampleIdx1.size();
		
		List<Integer> tmpSamplesList = new ArrayList<Integer>(size1 + size2);
		tmpSamplesList.addAll(sampleIdx1);
		tmpSamplesList.addAll(sampleIdx2);
		
		Collections.shuffle(tmpSamplesList);		
		
		int pos = 0;
		for(int r = 0; r < size1; r++, pos++)
		{
			sampleIdx1.set(r, tmpSamplesList.get(pos));
		}
		for(int r = 0; r < size2; r++, pos++)
		{
			sampleIdx2.set(r, tmpSamplesList.get(pos));
		}
	}
	
	private Map<String, Double> permutateGenes(Map<String, Double> snr_list){
		Map<String, Double> shuffledSnr = new HashMap<String, Double>();
		List<String> geneName = new ArrayList<String>();
		List<Double> geneSnr = new ArrayList<Double>(); 
		for(Entry<String, Double> tmpShuffledSnr : snr_list.entrySet()){
			geneName.add(tmpShuffledSnr.getKey());
			geneSnr.add(tmpShuffledSnr.getValue());
		}
		Collections.shuffle(geneName);
		Collections.shuffle(geneSnr);
		
		for (int i=0; i<geneName.size();i++){
			shuffledSnr.put(geneName.get(i), geneSnr.get(i));
		}
		return shuffledSnr;
	}
	
	private static class SummaryStats
	{
		double mean;
		double sd;

		/**
		 * Calculate mean and standard deviation
		 */
		private void fill(double[] values)
		{
			double sum = 0;
			for (double i : values){
				sum += i;
			}
			mean = sum / values.length;
			
			double sumsq = 0;
			for (double i : values)
			{
				sumsq += ((i - mean) * (i - mean)) ;
			}
			
			double var = 0;
			var = (sumsq / (values.length - 1));
			
			// sd has a minimum value of (0.2 * mean), see the documentation at http://www.broadinstitute.org/gsea/doc/GSEAUserGuideFrame.html
			// it says : sd "has a minimum value of .2 * absolute"(mean)
			double sd1Tmp = Math.sqrt(var);
			if (sd1Tmp < (0.2 * mean)){
				sd = 0.2 * mean;
			}
			else{
				sd = sd1Tmp;
			}
		}
	
	}
	
	
	// calculate mean, standard deviation, snr (Signal to Noise Ratio) and snr total
	private double calculateMean(DataSet dataset, List<Integer> sampleIdx1, List<Integer> sampleIdx2, 
			List<String> geneSet, List<String> geneSetInDataset, Map<String, Double> snrMap) {
		double totalSnr = 0.00;
		
		SummaryStats stats1 = new SummaryStats();
		SummaryStats stats2 = new SummaryStats();
		
		for(int i = 0; i < dataset.getSize(); i++)
		{
			// phenotype 1 values
			double[] phen1data = dataset.rowSlice(i, sampleIdx1);
			// phenotype 2 values
			double[] phen2data = dataset.rowSlice(i, sampleIdx2);
			
			// gene name
			String rowKey = dataset.getRowId(i);

			if (geneSet.contains(rowKey)) {
				geneSetInDataset.add(rowKey);
			}
			stats1.fill(phen1data);
			stats2.fill(phen2data);
			double snr = (stats1.mean - stats2.mean) / (stats1.sd + stats2.sd);

			snrMap.put(rowKey, snr);

			if (geneSetInDataset.contains(rowKey)) {
				totalSnr += Math.abs(snr);	
			}
		}
		return totalSnr;
	}
	
	// We can use directly the ranking_list method for calculating ES max for the permutation
	// We don't need to recalculate SNR and ES values for each genes
	private double rankingList(boolean perm, Map<String, Double> snrMap, 
			List<String> geneSetInDataset, DataSet dataset, double totalSnr)
	{	
		if(perm) {
			snrMap = permutateGenes(snrMap);
		}
		
		// 'as' is a temporary List for ranking snr
		List<Map.Entry<String, Double>> as = new ArrayList<Map.Entry<String, Double>>(snrMap.entrySet());  
		Collections.sort( as , new Comparator<Map.Entry<String, Double>>() {  
			public int compare( Map.Entry<String, Double> e1 , Map.Entry<String, Double> e2 )  
			{  
				Double first = (Double)e1.getValue();  
				Double second = (Double)e2.getValue();  
				return second.compareTo(first);
			}  
		});  

		List<Double> esSign = new ArrayList<Double>();
		double esMax = 0;
		Map<String, Double> esMap = new HashMap<String, Double>();
		List<String> geneSetList = new ArrayList<String>();
		double es = 0;
		int count = 0;
		int countMax = 0;		
		for(Map.Entry<String, Double> entry : as)   
		{   
			if (geneSetInDataset.contains((String)entry.getKey())){
				es = es + (Math.abs((Double)entry.getValue()) / totalSnr);
				geneSetList.add((String)entry.getKey());
			}
			else {
				es = ( es - (1.0 / (dataset.getSize() - geneSetInDataset.size())) );

			}
			esMap.put((String) entry.getKey(), Double.parseDouble(DF.format(es)));
			esMax = Math.max(Math.abs(es), esMax);
			count++;
			// To define the sign ( - or + ) of ES_max 
			if (esMax == Math.abs(es)) {
				esSign.add(es);
				countMax = count;
			}
		}   

		// For correct ES_max sign
		esMax = esSign.get(esSign.size() - 1);

		// results
		List<String> datasetRanked = new ArrayList<String>();
		for(Map.Entry<String, Double> entry : as)   
		{   
			if (esMap.containsKey(entry.getKey())) {
				datasetRanked.add((String)entry.getKey());
			} 			 
			if(!perm) esList.add(esMap.get(entry.getKey()));
		}
		
		// determine core enrichment list
		if (!perm) {
			// if esMax is positive, get the genes before countMax
			if (esMax > 0) {
				for (int c = 0; c < countMax; c++) {
					if (geneSetList.contains(datasetRanked.get(c))){
						coreEnrichment.add(datasetRanked.get(c));
					}
				}
			}
			// if esMax is negative, get the genes after countMax
			else {
				for (int c = countMax; c < datasetRanked.size(); c++) {
					if (geneSetList.contains(datasetRanked.get(c))) {
						coreEnrichment.add(datasetRanked.get(c));
					}
				}
			}
		}
		return esMax;
	}
	
	/**
	 * Efficient memory cache of expression data.
	 */
	public static class DataSet
	{
		private final double[][] data;
		private final Map<Integer, Integer> sampleIndexMap = new HashMap<Integer, Integer>();
		private final List<String> rowIds = new ArrayList<String>();
		private DataSource targetDs;
		
		public int getSize()
		{
			return data.length;
		}
		
		public String getRowId(int row)
		{
			return rowIds.get(row);
		}
		
		public DataSource getDataSource()
		{
			return targetDs;
		}
		
		/**
		 * Sucks all data from the Gex for the given samples, and stores it in memory.
		 * This happens at construction time, afterwards there is no way to read more data.
		 */
		public DataSet(DataInterface gex, List<String> sampleLabels) throws DataException
		{
			data = new double[gex.getNrRow()][sampleLabels.size()];
			targetDs = gex.getRow(0).getXref().getDataSource();
			
			Map<String, ISample> samples = new HashMap<String, ISample>();
			for (ISample s : gex.getOrderedSamples())
			{
				samples.put(s.getName(), s);
			}
			
			for (int i = 0; i < sampleLabels.size(); ++i)
			{
				sampleIndexMap.put(samples.get(sampleLabels.get(i)).getId(), i);
			}
			
			for (int row = 0; row < gex.getNrRow(); ++row)
			{
				IRow rowData = gex.getRow(row);
				if (rowData.getXref().getDataSource() != targetDs)
				{
					throw new DataException("Can't perform GSEA, dataset contains multiple datasources;\n" + 
							rowData.getXref().getDataSource() + " and " + targetDs);
				}
				rowIds.add(rowData.getXref().getId());
				for (int col = 0; col < sampleLabels.size(); ++col)
				{
					data[row][col] = (Double)rowData.getSampleData(samples.get(sampleLabels.get(col)));
				}
			}			
		}
		
		public double[] rowSlice(int row, List<Integer> sampleIds)
		{
			double[] result = new double[sampleIds.size()];
			for (int col = 0; col < sampleIds.size(); ++col)
			{
				result[col] = data[row][sampleIndexMap.get(sampleIds.get(col))];
			}
			return result;
		}
		
	}
		
		
	/**
	 * calculate the Snr value and rank the dataset gene list
	 * @param gex current gex
	 * @param geneSet gene Set list
	 * @param sampleNames1 samples to use for phenotype1
	 * @param sampleNames2 samples to use for phenotype2
	 * @param perm number of permutations
	 * @param sample : this boolean define if samples permutation(true) or genes permutation(false) 
	 * @return Es maximum value
	 */
	private double calculateEsMax (DataInterface gex, List<String> geneSet, List<String> sampleNames1,
			List<String> sampleNames2, boolean perm, boolean sample, DataSet dataset) throws DataException{
		
		esList = new ArrayList<Double>();
		coreEnrichment = new ArrayList<String>();
		
		// to recover the sampleID for each sample
		// for example : (0 : ES1 , 1: ES2 ...)
		Map<Integer, String> gexSamples = new HashMap<Integer, String>();
		for(int cf = 0; cf < gex.getSampleNames().size(); cf++) {
			gexSamples.put(cf, gex.getSampleNames().get(cf));
		}
		
		// we just need the integer corresponding to the sampleID
		// sample1_id contains the sampleID for phenotype1, for example (0,1,2,3)
		// sample2_id contains the sampleID for phenotype2, for example (5,6,7,8,9)
		List<Integer> sampleIdx1 = new ArrayList<Integer>();
		List<Integer> sampleIdx2 = new ArrayList<Integer>();
		for (Entry<Integer, String> idsample : gexSamples.entrySet()){
			if(sampleNames1.contains(idsample.getValue())){
				sampleIdx1.add(idsample.getKey());
			}
			if(sampleNames2.contains(idsample.getValue())){
				sampleIdx2.add(idsample.getKey());
			}
		}

		// if samples permutation 
		// sample1_id and sample2_id are shuffled
		if (sample) {
			permuteSamples(sampleIdx1,sampleIdx2);
		}
		
		// the snr values list
		snrMap = new HashMap<String, Double>();
		// geneSetInDataset is the exact value for gene in the gene Set and in the Dataset
		geneSetInDataset = new ArrayList<String>();
		totalSnr = calculateMean (dataset, sampleIdx1, sampleIdx2, geneSet, geneSetInDataset, snrMap);		
		return rankingList (perm, snrMap, geneSetInDataset, dataset, totalSnr);
	}

	// calculation of the permuted GSEA depending on the number and type of permutations
	public GseaPathwayResult permuted_gsea(int permutationNum, List<String> geneIds, DataInterface gex, IDMapper sgdb, List<String> sampleNames1, 
			List<String> sampleNames2, boolean permuteSamples, DataSet dataset) throws SAXException, IDMapperException, IOException, DataException
	{
		int significantPermutationCount = 0;
		double esMax;
		double pValue = 1.0;
		esMax = calculateEsMax(gex, geneIds, sampleNames1, sampleNames2, false, false, dataset);
		// create backup copy before permutation
		esForChart = esList;
		if (permutationNum > 0)
		{
			if (!permuteSamples)
			{
				for (int p = 0; p < permutationNum; p++)
				{
					double esPermuted = rankingList(true, snrMap, geneSetInDataset, dataset, totalSnr);
					if ((Math.abs(esPermuted)) > (Math.abs(esMax)))
					{
						significantPermutationCount++;
					}
				}
			}
			else
			{
				for (int p = 0; p < permutationNum; p++)
				{
					double esPermuted = calculateEsMax(gex, geneIds, sampleNames1, sampleNames2, false, true, dataset);
					if ((Math.abs(esPermuted)) > (Math.abs(esMax)))
					{
						significantPermutationCount++;
					}
				}
			}
			pValue = (significantPermutationCount / (double) (permutationNum));
		}
		GseaPathwayResult result = new GseaPathwayResult(esForChart, coreEnrichment, geneIds, esMax, pValue);
		return result;
	}

}
