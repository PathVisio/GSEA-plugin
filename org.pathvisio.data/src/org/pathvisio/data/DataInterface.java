// PathVisio,
package org.pathvisio.data;

import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;

public interface DataInterface
{
    public ISample getSample(int id) throws DataException;
	
    public ISample findSample(String name) throws DataException;

    public List<String> getSampleNames();

    public List<String> getSampleNames(int dataType);
    
    public List<ISample> getOrderedSamples() throws DataException;

    /**
     * get all datasouces used in this gex.
     */
    public Set<DataSource> getUsedDatasources() throws DataException;

    public IRow getRow(int rowId) throws DataException;

    public int getNrRow() throws DataException;
}
