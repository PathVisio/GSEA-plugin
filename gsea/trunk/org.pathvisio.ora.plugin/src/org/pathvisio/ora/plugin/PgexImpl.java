package org.pathvisio.ora.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.desktop.gex.SimpleGex;
import org.pathvisio.data.DataInterface;
import org.pathvisio.data.DataException;
import org.pathvisio.data.IRow;
import org.pathvisio.data.ISample;

public class PgexImpl implements DataInterface
{
	private final SimpleGex gex;
	
	public PgexImpl (SimpleGex gex)
	{
		this.gex = gex;
	}
	
	public List<String> getSampleNames()
	{
		return gex.getSampleNames();
	}
	
	public int getNrRow() throws DataException
	{
		try
		{
			return gex.getNrRow();
		}
		catch (DataException e)
		{
			throw new DataException (e);
		}
	}

	@Override
	public IRow getRow(int row) throws DataException
	{
		try
		{
			return RowImpl.getInstance(gex.getRow(row));
		}
		catch (DataException e)
		{
			throw new DataException (e);
		}
	}

	@Override
	public ISample findSample(String name) throws DataException
	{
		try
		{
			return SampleImpl.getInstance(gex.findSample(name));
		}
		catch (DataException e)
		{
			throw new DataException(e);
		}
	}

	@Override
	public ISample getSample(int id) throws DataException
	{
		try
		{
			return SampleImpl.getInstance(gex.getSample(id));
		}
		catch (DataException e)
		{
			throw new DataException(e);
		}
	}

	@Override
	public List<String> getSampleNames(int dataType)
	{
		return gex.getSampleNames(dataType);
	}

	@Override
	public Set<DataSource> getUsedDatasources() throws DataException
	{
		try
		{
			return gex.getUsedDatasources();
		}
		catch (DataException e)
		{
			throw new DataException(e);
		}
	}

	@Override
	public List<ISample> getOrderedSamples() throws DataException
	{
		List<ISample> result = new ArrayList<ISample>();
		try
		{
			for (ISample s : gex.getOrderedSamples())
			{
				result.add (SampleImpl.getInstance(s));
			}
		}
		catch (DataException e)
		{
			throw new DataException(e);
		}
		return result;
	}

	@Override
	public Map<Integer, ? extends ISample> getSamples() throws DataException
	{
		return gex.getSamples();
	}

	@Override
	public Collection<? extends IRow> getData(Set<Xref> destRefs) throws DataException
	{
		return gex.getData(destRefs);
	}
}
