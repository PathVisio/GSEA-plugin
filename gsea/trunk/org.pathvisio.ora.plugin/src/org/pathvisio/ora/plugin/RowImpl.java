package org.pathvisio.ora.plugin;

import java.util.HashMap;
import java.util.Map;

import org.bridgedb.Xref;
import org.pathvisio.desktop.gex.ReporterData;
import org.pathvisio.data.IRow;
import org.pathvisio.data.ISample;

public class RowImpl implements IRow
{
	private final ReporterData parent;

	private static Map<ReporterData, RowImpl> cache = new HashMap<ReporterData, RowImpl>();
	
	/** RowImpl are cached. This ensures that there are no two RowImpl objects 
	 * for the same ReporterData, and thus equality testing is possible */
	public static RowImpl getInstance(ReporterData parent)
	{
		if (!cache.containsKey(parent))
		{
			RowImpl impl = new RowImpl(parent);
			cache.put(parent, impl);
		}
		return cache.get(parent);
	}

	private RowImpl (ReporterData parent)
	{
		this.parent = parent;
	}
	
	@Override
	public Object getSampleData(ISample key)
	{
		if (key instanceof SampleImpl)
		{
			return parent.getSampleData(((SampleImpl)key).getParent());
		}
		else return null;
	}

	@Override
	public Xref getXref()
	{
		return parent.getXref();
	}

}
