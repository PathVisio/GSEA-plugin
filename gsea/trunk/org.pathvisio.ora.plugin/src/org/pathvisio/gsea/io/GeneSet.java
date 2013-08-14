package org.pathvisio.gsea.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GeneSet {

	private String name;
	private String description;
	private List<String> genes;
	private int numGenes = 0;
	private int numGenesNotMapped = 0;
	private File source;
	
	public GeneSet() {
		genes = new ArrayList<String>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getGenes() {
		return genes;
	}

	public void setGenes(List<String> genes) {
		this.genes = genes;
	}

	public int getNumGenes() {
		return numGenes;
	}

	public void setNumGenes(int numGenes) {
		this.numGenes = numGenes;
	}

	public int getNumGenesNotMapped() {
		return numGenesNotMapped;
	}

	public void setNumGenesNotMapped(int numGenesNotMapped) {
		this.numGenesNotMapped = numGenesNotMapped;
	}

	public File getSource() {
		return source;
	}

	public void setSource(File source) {
		this.source = source;
	}
}
