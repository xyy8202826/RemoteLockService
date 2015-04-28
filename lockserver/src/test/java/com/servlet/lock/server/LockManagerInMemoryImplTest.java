package com.servlet.lock.server;

import org.junit.Ignore;
import org.junit.Test;


@Ignore
public class LockManagerInMemoryImplTest {
	@Test
	public void testReadLock() throws Exception {
		LockManagerInMemoryImpl lockManager=new LockManagerInMemoryImpl();
		String key="strLock";
		String key1="key1";
		String resourceId="resourceId";
		System.out.println(lockManager.writeLock(key, resourceId, IsolationLevels.IL_DEFAULT));
		System.out.println(lockManager.writeLock(key1, resourceId, IsolationLevels.IL_DEFAULT));
	}
}
