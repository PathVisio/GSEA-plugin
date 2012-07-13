package org.pathvisio.gsea.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;


public class GmtParser {

	public List<GeneSet> parseGmtFile(File file, IDMapper sgdb, DataSource dataSource) throws IOException, IDMapperException {
		List<GeneSet> list = new ArrayList<GeneSet>();
		if (file.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				int countGenesTotal = 0;
				int countGenesNotMapped = 0;
				GeneSet geneSet = new GeneSet();
				String[] lineSplit = line.split("\t");
				geneSet.setName(lineSplit[0]);
				geneSet.setDescription(lineSplit[1]);
				for (int i = 2; i < lineSplit.length; i++) {
					countGenesTotal++;
					Xref xref = new Xref(lineSplit[i], DataSource.getBySystemCode("L"));
					Set<Xref> set = sgdb.mapID(xref, dataSource);
					System.out.println(set.size());
					if(set.size() == 0) {
						countGenesNotMapped++;
					} else {
						for (Xref dest : set) {
							System.out.println("\t>> add gene + " + dest.getId());
							geneSet.getGenes().add(dest.getId());
						}
					}
				}
				geneSet.setNumGenes(countGenesTotal);
				geneSet.setNumGenesNotMapped(countGenesNotMapped);
				
				list.add(geneSet);
			}

			return list;

		} else {

		}
		return list;
	}
}
