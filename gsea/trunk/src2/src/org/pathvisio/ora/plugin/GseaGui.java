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

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.jdesktop.swingworker.SwingWorker;
import org.pathvisio.core.data.XrefWithSymbol;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.util.PathwayParser;
import org.pathvisio.core.util.PathwayParser.ParseException;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.data.DataInterface;
import org.pathvisio.ora.gsea.GseaImpl;
import org.pathvisio.ora.gsea.GseaPathwayResult;
import org.pathvisio.ora.gsea.GseaResults;
import org.pathvisio.ora.gsea.GseaImpl.DataSet;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.stanford.ejalbert.BrowserLauncher;

public class GseaGui
{
	private final PvDesktop desktop;
	
	public GseaGui(PvDesktop desktop)
	{
		this.desktop = desktop;
	}

	private JTextField permutationNumber;
	private String geneSetName;
	private Checkbox genePermutationBox;
	private Checkbox samplePermutationBox;

	protected Component getConfigPanel() {
		FormLayout layout = new FormLayout("pref, 4dlu, 150dlu, 4dlu, min",
				"40dlu, 1dlu, 20dlu, 1dlu, 20dlu, 1dlu, 20dlu,1dlu, 20dlu, 1dlu, 20dlu, 1dlu, 20dlu");
		JPanel panel = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		JLabel chooseSamples = new JLabel(
				" Choose your samples by clicking Samples : ");
		final JButton samplesChoice = new JButton("Samples");
		samplesChoice.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == samplesChoice) {
					desktop.loadGexCache();
					SampleFrame sf = new SampleFrame(desktop.getFrame(), desktop.getGexManager()
							.getCurrentGex());
					sf.setVisible(true);
					sf.validate();
					sf.repaint();
				}
			}
		});
		panel.add(chooseSamples, cc.xy(3, 5));
		panel.add(samplesChoice, cc.xy(5, 5));

		JLabel nb_of_perm = new JLabel(" Number of permutations");
		permutationNumber = new JTextField();
		permutationNumber.setText("100");
		panel.add(nb_of_perm, cc.xy(1, 7));
		panel.add(permutationNumber, cc.xy(3, 7));

		JLabel permutation_type = new JLabel(" Type of permutations ");
		CheckboxGroup perm_type = new CheckboxGroup();
		samplePermutationBox = new Checkbox(" Samples permutation ", perm_type, true);
		genePermutationBox = new Checkbox(" Genes permutation ", perm_type, false);
		panel.add(permutation_type, cc.xy(1, 9));
		panel.add(samplePermutationBox, cc.xy(3, 9));
		panel.add(genePermutationBox, cc.xy(3, 11));
		
		final JButton helpButton = new JButton("Help");
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String url = "http://www.pathvisio.org/wiki/GseaPluginHelp";
				try
				{
					BrowserLauncher bl = new BrowserLauncher(null);
					bl.openURLinBrowser(url);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});
		panel.add(helpButton, cc.xy(5, 13));
		
		return panel;
	}

	// openPathway returns the list of the genes in the pathway
	private List<String> openPathway(File gpmlFile, IDMapper sgdb, DataSource targetDs) throws ParseException, SAXException, IDMapperException, IOException{
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

	public void finishedPressed(final File pwDir)
	{
		Frame progressFrame = new Frame();

		final ProgressKeeper pk = new ProgressKeeper(100);
		final ProgressDialog d = new ProgressDialog(progressFrame,
				"GSEA plugin progress", pk, false, true);

		SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
			protected Boolean doInBackground() {
				pk.setTaskName("Please wait while GSEA plugin is running...");
				try {
					pk.setProgress(0);
					pk.setTaskName("Reading expression data");
					int permCount = 0;

					GseaPathwayResult result;
					if (!(permutationNumber.getText().equals(""))) {
						permCount = Integer.parseInt(permutationNumber.getText());
					}

					// two list contains the idsample for each sample
					List<String> idsample1 = new ArrayList<String>();
					List<String> idsample2 = new ArrayList<String>();

					for (Entry<ArrayList<String>, ArrayList<String>> idlist : SampleFrame
							.setSamples().entrySet()) {
						idsample1 = idlist.getKey();
						idsample2 = idlist.getValue();
					}

					File[] gpmlFilesList = pwDir.listFiles();

					IDMapper sgdb = desktop.getSwingEngine().getGdbManager()
							.getCurrentGdb();
					DataInterface gex = desktop.getGexManager().getCurrentGex();

					List<String> allSamples = new ArrayList<String>();
					allSamples.addAll(idsample1);
					allSamples.addAll(idsample2);
					DataSet dataset = new DataSet (gex, allSamples);
					List<GseaPathwayResult> pwList = new ArrayList<GseaPathwayResult>();
					
					// Running GSEA for the list of pathways
					for (int i = 0; i < gpmlFilesList.length; i++) {
						try {
							pk.report("Processing pathway "+ gpmlFilesList[i].getName());
							pk.setProgress(10 + (i * 90 / gpmlFilesList.length));
							GseaImpl gseaImpl = new GseaImpl();
							List<String> geneIds = openPathway(gpmlFilesList[i], sgdb, dataset.getDataSource());
							result = gseaImpl.permuted_gsea(permCount, geneIds, gex, sgdb, idsample1, idsample2, samplePermutationBox.getState() == true, dataset);

							geneSetName = gpmlFilesList[i].getName();
							
							result.createInfo((String) (geneSetName.subSequence(0,
									(geneSetName.length() - 5))),
							permCount);
							pwList.add(result);
						} catch (Exception ex) {
							String msg = "Unable to perform gene set enrichment analysis \n Please check your samples";
							JOptionPane.showMessageDialog(null,
									"Error: " + msg + "\n\n"
											+ "See the error log for details.",
									"Error", JOptionPane.ERROR_MESSAGE);
							Logger.log.error(msg, ex);
							return false;
						}
					}

					Collections.sort(pwList);
					GseaResults gseaResults = new GseaResults();
					for (GseaPathwayResult i : pwList) gseaResults.addRow(i);
					d.setVisible(false);
					GseaResultsFrame resultFrame = new GseaResultsFrame(desktop.getFrame(), gseaResults);
					resultFrame.setVisible(true);

					return true;
				} catch (Exception e) {
					String msg = "Unable to perform gene set enrichment analysis \n Please check your files ";
					JOptionPane.showMessageDialog(null,
							"Error: " + msg + "\n\n"
									+ "See the error log for details.",
							"Error", JOptionPane.ERROR_MESSAGE);
					Logger.log.error(msg, e);
					return false;
				} finally {
					pk.setProgress(100);
					pk.finished();
				}
			}
	
		};
		sw.execute();
		d.setVisible(true);
	}
	
}
