package org.pathvisio.ora.plugin;

import java.util.HashMap;
import java.util.Map;

import org.pathvisio.desktop.gex.Sample;
import org.pathvisio.data.ISample;

public class SampleImpl implements ISample
{
	private final Sample parent;
	private static Map<Sample, SampleImpl> cache = new HashMap<Sample, SampleImpl>();

	/** 
	 * SampleImpl objects are cached. This ensures that there are no two SampleImpl objects wrapping the same Sample,
	 * and thus it is possible to test for equality on SampleImpl objects.
	 */
	public static SampleImpl getInstance(Sample parent)
	{
		if (!cache.containsKey(parent))
		{
			SampleImpl impl = new SampleImpl(parent);
			cache.put(parent, impl);
		}
		return cache.get(parent);
	}
	
	private SampleImpl(Sample parent)
	{
		this.parent = parent;
	}
	
	@Override
	public Integer getId()
	{
		return parent.getId();
	}

	@Override
	public String getName()
	{
		return parent.getName();
	}

	public Sample getParent()
	{
		return parent;
	}
}
