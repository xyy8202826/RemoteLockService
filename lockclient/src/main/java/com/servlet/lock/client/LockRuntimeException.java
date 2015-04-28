package com.servlet.lock.client;

public class LockRuntimeException extends RuntimeException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LockRuntimeException()
    {
    }

    public LockRuntimeException(String msg)
    {
        super(msg);
    }

    public LockRuntimeException(Throwable cause)
    {
        super(cause);
    }

    public LockRuntimeException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
