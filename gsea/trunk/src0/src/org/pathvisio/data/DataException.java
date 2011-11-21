package org.pathvisio.data;

public class DataException extends Exception
{
	public DataException(Throwable e)
	{
		super (e);
	}

	public DataException(String msg)
	{
		super (msg);
	}
}
