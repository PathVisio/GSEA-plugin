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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.pathvisio.gui.dialogs.OkCancelDialog;
import org.pathvisio.data.DataInterface;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SampleFrame extends OkCancelDialog{

	/**
	 * A Frame with checboxes for determining the samples to use for the GSEA test
	 * @param gex current gex 
	 */
	public SampleFrame(Component locationComponent, DataInterface gex) {
		super(null, "Samples choice", null, true);
		setDialogComponent(createDialogPane(gex));
		setSize(390, 500);
		setLocationRelativeTo(locationComponent);
		validate();
	}
	
	private ButtonGroup groupe = null;
	private JRadioButton box1;
	private JRadioButton box2;
	private static Map <String, ArrayList<JRadioButton>> sampleres = new HashMap <String, ArrayList<JRadioButton>>();
	
	protected Component createDialogPane(DataInterface gex) {		
	    FormLayout layout = new FormLayout (
	    		"50dlu, 4dlu, 50dlu, 4dlu, 50dlu",
	    		"");
	    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
	    builder.setDefaultDialogBorder();

	    List<String> samplesNames = new ArrayList<String>();
	    for(int cf= 0;cf<gex.getSampleNames().size();cf++){
	    	samplesNames.add(gex.getSampleNames().get(cf));
		}
	    
	    for (int s=0 ; s<samplesNames.size();s++){
	    	List<JRadioButton> cb = new ArrayList<JRadioButton>();
	    	groupe=new ButtonGroup();
	    	JLabel samp = new JLabel(samplesNames.get(s));
	    	box1=new JRadioButton("Phen1");
	    	box2=new JRadioButton("Phen2");
	    	groupe.add(box1);
	    	groupe.add(box2);
	    	cb.add(box1);
	    	cb.add(box2);

	    	builder.append(samp);
	    	builder.append(box1);
	    	builder.append(box2);
	    	
	    	builder.nextLine();
	    	sampleres.put(samplesNames.get(s), (ArrayList<JRadioButton>) cb);
	    }
	    return new JScrollPane(builder.getPanel());
	}
	
	// return the selected samples in the two phenotypes
	public static HashMap<ArrayList<String>,ArrayList<String>> setSamples(){
		//  HashMap with the two lists of samplesId
		HashMap<ArrayList<String>,ArrayList<String>> kd = new HashMap<ArrayList<String>,ArrayList<String>>();
		// two lists contains samples names
		ArrayList<String> samplePhen1 = new ArrayList<String>();
		ArrayList<String> samplePhen2 = new ArrayList<String>();
		
		// UNCOMMENT AFTER TEST
		
		
		for (Entry<String, ArrayList<JRadioButton>> idsample : sampleres.entrySet()){
			for (int z=0 ; z<idsample.getValue().size();z++){
				if ((idsample.getValue().get(z).isSelected())){
					if(idsample.getValue().get(z).getText().equals("Phen1")){
						samplePhen1.add(idsample.getKey());
					}
					else {
						samplePhen2.add(idsample.getKey());
					}
				}
			}
		}
		
		
		
		// DELETE AFTER TEST
//		samplePhen1.add("ES2");
//		samplePhen1.add("ES6");
//		samplePhen1.add("ES8");
//		samplePhen1.add("ES10");
//		samplePhen2.add("EB1");
//		samplePhen2.add("EB3");
//		samplePhen2.add("EB5");
//		samplePhen2.add("EB7");
//		samplePhen2.add("EB9");
		// DELETE AFTER TEST
		
		kd.put(samplePhen1, samplePhen2);
		return kd;
	}
	
	protected void okPressed() {
		setSamples();
		super.okPressed();	
	}
	
}
