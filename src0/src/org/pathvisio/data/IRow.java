package org.pathvisio.data;

import org.bridgedb.Xref;

public interface IRow
{

	Xref getXref();
	Object getSampleData(ISample iSample);

}
