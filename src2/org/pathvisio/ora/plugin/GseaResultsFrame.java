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

import java.awt.Component;
import java.awt.Frame;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.JFreeChart;
import org.pathvisio.gui.dialogs.OkCancelDialog;
import org.pathvisio.ora.gsea.GseaPathwayResult;
import org.pathvisio.ora.gsea.GseaResults;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class GseaResultsFrame extends OkCancelDialog{
	/**
	 * This class show the GSEA results in a frame
	 * and draw the ES plot by using JFreeChart library
	 * @param list contains the ES value for each gene
	 * @param name gene Set name
	 * @param es_max maximum value for ES
	 * @param geneSet gene Set list (pathway)
	 * @param perm number of permutation for calculating p-value
	 * @param pvalue for this gsea test
	 * @param list2 genes in core enrichment
	 */

	private GseaResults model;
	private JTable pathwayJList;
	private JTextArea results = new JTextArea();
	private JLabel pwChart = null;
	private FormLayout layout = new FormLayout (
    		"150dlu, 2dlu, 250dlu:grow",
    		"250dlu:grow, 2dlu, 100dlu:grow");
	private JPanel mainPanel = new JPanel(layout);
	private CellConstraints cc = new CellConstraints();
	private JScrollPane scrollingArea;
	
	public GseaResultsFrame(Frame frame, GseaResults gseaResults) {
		super(frame, "GSEA Results", frame, true, false);
		setDialogComponent(createDialogPane(gseaResults));
		pack();
		setLocationRelativeTo(frame);
	}	
	
	protected Component createDialogPane(GseaResults gseaResults) 
	{
		model = gseaResults;
	    pathwayJList = new JTable(model);
	    mainPanel.add(new JScrollPane(pathwayJList), cc.xywh(1,1,1,3));
		pwChart = new JLabel("Please choose a pathway to see the GSEA results");
		pwChart.setHorizontalAlignment(SwingConstants.CENTER);
	    mainPanel.add(pwChart, cc.xy(3, 1, "fill, fill"));	    
	    pathwayJList.getSelectionModel().addListSelectionListener(new ValueReporter());
		scrollingArea = new JScrollPane(results);
		
		return mainPanel;
	}
	
	private class ValueReporter implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			GseaPathwayResult found = null;
			
			found = model.getRow(pathwayJList.getSelectedRow());

			if (found == null) return;
			
			if (!event.getValueIsAdjusting()) {
				pwChart.setText(null);
				
				results.setText("");
				results.append(found.getPwText());
				results.setCaretPosition(0);
				results.setEditable(false);
				mainPanel.add(scrollingArea, cc.xy(3, 3));
				
				JFreeChart chart = found.getPwChart();
				BufferedImage image = chart.createBufferedImage(pwChart.getWidth(), pwChart.getHeight());
				pwChart.setIcon(new ImageIcon(image));
				
				mainPanel.revalidate();
				mainPanel.repaint();
			}
		}
	}
	
}