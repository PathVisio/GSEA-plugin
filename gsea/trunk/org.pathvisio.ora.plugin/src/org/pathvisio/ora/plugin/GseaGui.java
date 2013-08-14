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

package org.pathvisio.ora.plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.data.XrefWithSymbol;
import org.pathvisio.core.util.PathwayParser;
import org.pathvisio.core.util.PathwayParser.ParseException;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.data.DataException;
import org.pathvisio.data.DataInterface;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gsea.io.GeneSet;
import org.pathvisio.gsea.io.GmtParser;
import org.pathvisio.ora.gsea.GseaImpl;
import org.pathvisio.ora.gsea.GseaImpl.DataSet;
import org.pathvisio.ora.gsea.GseaPathwayResult;
import org.pathvisio.ora.gsea.GseaResults;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class GseaGui
{
	private final PvDesktop desktop;
	private CellConstraints cc = new CellConstraints();
	
	public GseaGui(PvDesktop desktop)
	{
		this.desktop = desktop;
	}

	private JTextField permutationNumber;
	private String geneSetName;
	private JCheckBox genePermutationBox;
	private JCheckBox samplePermutationBox;

	private JPanel config;
	private JPanel samples;
	
	protected Component getConfigPanel() {
		FormLayout mainLayout = new FormLayout("1dlu,175dlu,5dlu,160dlu,3dlu","5dlu, pref, 10dlu, top:200dlu");
		DefaultFormBuilder builder = new DefaultFormBuilder(mainLayout);
		
		samples = new JPanel();
		samples.add(getRightPanel());
		JScrollPane pane = new JScrollPane(samples);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		config = new JPanel();
		config.setLayout(new BorderLayout());
		config.add(getLeftPanel(), BorderLayout.NORTH);
		
		builder.add(config, cc.xy(2, 4));
		builder.add(pane, cc.xy(4, 4));
		builder.addSeparator("Configuration", cc.xyw(2, 2, 1));
		builder.addSeparator("Samples", cc.xyw(4, 2, 1));
		JPanel mainPanel = builder.getPanel();
		mainPanel.setMaximumSize(new Dimension(300,300));
		return mainPanel;
	}

	private JPanel getRightPanel() {
		desktop.loadGexCache();
		SampleFrame sf = new SampleFrame(desktop.getFrame(), desktop.getGexManager().getCurrentGex());
		JPanel panel = sf.createDialogPane(desktop.getGexManager().getCurrentGex());
		return panel;
	}

	private JPanel getLeftPanel() {
		FormLayout configLayout = new FormLayout("3dlu,pref,3dlu,pref","10dlu,pref,5dlu,pref,5dlu,pref");
		JPanel panel = new JPanel(configLayout);
		
		JLabel nb_of_perm = new JLabel(" Number of permutations");
		permutationNumber = new JTextField();
		permutationNumber.setText("100");
		panel.add(nb_of_perm, cc.xy(2, 2));
		panel.add(permutationNumber, cc.xy(4, 2));

		JLabel permutation_type = new JLabel(" Type of permutations ");
		samplePermutationBox = new JCheckBox(" Samples permutation ", true);
		genePermutationBox = new JCheckBox(" Genes permutation ", false);
		
		ButtonGroup bg=new ButtonGroup();
		bg.add(samplePermutationBox);
		bg.add(genePermutationBox);
		
		panel.add(permutation_type, cc.xy(2, 4));
		panel.add(samplePermutationBox, cc.xy(4, 4));
		panel.add(genePermutationBox, cc.xy(4, 6));
		return panel;
	}
	
	// openPathway returns the list of the genes in the pathway
	public List<String> openPathway(File gpmlFile, IDMapper sgdb, DataSource targetDs) throws ParseException, SAXException, IDMapperException, IOException{
		List<String> geneSet = new ArrayList<String>();
		try
		{
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			PathwayParser pwyParser = new PathwayParser(gpmlFile, xmlReader);
			for (XrefWithSymbol ref : pwyParser.getGenes()) 
			{
				for (Xref dest : sgdb.mapID(ref.asXref(), targetDs)) {
					geneSet.add(dest.getId());
				}
			}
		}
		catch(ParseException ex) {
			JOptionPane.showMessageDialog(null, "Error: Enable to open pathway " + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		return geneSet;
	}
	
	public Integer getPermCount() {
		if (!(permutationNumber.getText().equals(""))) {
			int permCount = Integer.parseInt(permutationNumber.getText());
			return permCount;
		}
		return null;
	}

	public boolean runCalculation(File pwDir, File geneSetFile, ProgressKeeper pk, JTextArea progressText) {
		try {
			progressText.append("Please wait while GSEA plugin is running...\n");
			pk.setProgress(0);
			progressText.append("Reading expression data\n");
			int permCount = 0;
	
			
			GseaImpl gseaImpl = new GseaImpl();
			
			if (!(permutationNumber.getText().equals(""))) {
				permCount = Integer.parseInt(permutationNumber.getText());
			}
			
			// get current gene database and gene expression dataset
			IDMapper sgdb = desktop.getSwingEngine().getGdbManager().getCurrentGdb();
			DataInterface gex = desktop.getGexManager().getCurrentGex();
	
			// prepare samples and dataset
			// two list contains the idsample for each sample
			List<String> idsample1 = new ArrayList<String>();
			List<String> idsample2 = new ArrayList<String>();
	
			for (Entry<ArrayList<String>, ArrayList<String>> idlist : SampleFrame.setSamples().entrySet()) {
				idsample1 = idlist.getKey();
				idsample2 = idlist.getValue();
			}
			List<String> allSamples = new ArrayList<String>();
			allSamples.addAll(idsample1);
			allSamples.addAll(idsample2);
			DataSet dataset = new DataSet(gex, allSamples);
			
			results = new GseaResults();
			List<GseaPathwayResult> pwList = new ArrayList<GseaPathwayResult>();
			pk.setProgress(10);
			List<GeneSet> geneSets = new ArrayList<GeneSet>();
			if(pwDir != null) {
				File[] gpmlFilesList = pwDir.listFiles();
				progressText.append("Reading " + gpmlFilesList.length + " pathway(s) from pathway directory " + pwDir.getName() + " ...\n");
				
				// load pathways in pathway directory
				for (int i = 0; i < gpmlFilesList.length; i++) {
					try {
						List<String> geneIds = openPathway(gpmlFilesList[i], sgdb, dataset.getDataSource());
						GeneSet set = new GeneSet();
						set.setName(gpmlFilesList[i].getName());
						set.setSource(gpmlFilesList[i]);
						set.setNumGenes(geneIds.size());
						set.setGenes(geneIds);
						geneSets.add(set);
					} catch (Exception e) {
						progressText.append("\tCould not open pathway " + gpmlFilesList[i] + "\n");
					}
				}
			}
			if(geneSetFile != null) {
				GmtParser parser = new GmtParser();
				try {
					List<GeneSet> list;
					list = parser.parseGmtFile(geneSetFile, sgdb, dataset.getDataSource());
					geneSets.addAll(list);
					if(list.size() != 0) {
						progressText.append("Reading " + list.size() + " gene set(s) from file " + geneSetFile.getName() + " ...\n");
					}
				} catch (IOException e) {
					progressText.append("\tCould not read gene set collection file " + geneSetFile.getName() + ".\n");
				} catch (IDMapperException e1) {
					progressText.append("\tCould not read gene set collection file " + geneSetFile.getName() + "\n");
				}
			}
			
			progressText.append("\n\nPerforming gene set analysis for " + geneSets.size() + " gene sets...\n");

			int step = geneSets.size()/90;
			int count = 1;
			// perform GSEA for all gene sets
			for(GeneSet set : geneSets) {
				if(!pk.isCancelled()) {
					try {
						GseaPathwayResult result = gseaImpl.permuted_gsea(permCount, set.getGenes() , gex, idsample1, idsample2, samplePermutationBox.isSelected() == true, dataset);
						result.createInfo(set.getName(), permCount);
						if(set.getSource() != null) {
							result.setPathwayFile(set.getSource());
						}
						pwList.add(result);
						progressText.append(".");
						pk.setProgress(10+(count*step));
						count++;
					} catch (IOException e) {
						progressText.append("\tCould not perform analysis with gene set " + geneSetFile.getName() + ".\n");
					} catch (IDMapperException e1) {
						progressText.append("\tCould not perform analysis with gene set " + geneSetFile.getName() + "\n");
					} catch (SAXException e) {
						progressText.append("\tCould not perform analysis with gene set " + geneSetFile.getName() + "\n");
					}
				}
			}
			progressText.append("\n");
			Collections.sort(pwList);
			
			for (GseaPathwayResult i : pwList) {
				results.addRow(i);
			}
			progressText.append("\n\nDone.\n\n");
			pk.setProgress(100);
		} catch (DataException e) {
			progressText.append("Unable to perform gene set enrichment analysis \n Please check your samples.\n");
			return false;
		}
		return true;
	}
	
	private GseaResults results = null; 
	
	public GseaResults getResults() {
		return results;
	}
}
