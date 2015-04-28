package com.servlet.lock.client;



import org.junit.Ignore;
import org.junit.Test;
@Ignore
public class LockManagerRemoteImplTest {

	@Test
	public void testReadLock() throws Exception {
		LockManager lockManager=new LockManagerRemoteImpl("http://127.0.0.1:8080/my-lockserver");
		String key="key";
		String key2="key2";
		String resourceId="resourceId";
		System.out.println(lockManager.writeLock(key, resourceId, IsolationLevels.IL_DEFAULT));
		//System.out.println(lockManager.writeLock(key2, resourceId, IsolationLevels.IL_DEFAULT));
	}

}
