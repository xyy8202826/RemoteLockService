package com.servlet.lock.startup;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.servlet.lock.client.LockManagerRemoteImpl;
import com.servlet.lock.server.LockManager;
import com.servlet.lock.server.LockManagerInMemoryImpl;
import com.servlet.lock.server.LockRuntimeException;

@Controller
@EnableAutoConfiguration
@Scope(value = "singleton")
public class Startup {
	private static Logger log = LoggerFactory.getLogger(Startup.class);
	public static LockManager lockmanager;
	static final String STR_LOCK_TIMEOUT = "lockTimeout";
	static final String STR_BLOCK_TIMEOUT = "blockTimeout";
	static final String STR_LOCK_MANAGER = "lockManager";
	private static long numRequests = 0;
	private static Throwable lastError = null;

	/**
	 * 
	 * @param strLockTimeout
	 * @param strBlockTimeout
	 */
	public static void init(String strLockTimeout, String strBlockTimeout) {
		// if lock manager was instantiated not yet
		log.info("Startup init ==========");
		if (lockmanager == null) {
			lastError = null;
			numRequests = 0;
			try {
				lockmanager = new LockManagerInMemoryImpl();
			} catch (Exception e) {
				lastError = new LockRuntimeException(
						"Can't instance lock manager, init parameter 'lockManager': "
								+ strLockTimeout);
				e.printStackTrace();
			}
			if (NumberUtils.isNumber(strLockTimeout)) {
				try {
					Long lockTimeout = NumberUtils.createLong(strLockTimeout);
					lockmanager.setLockTimeout(lockTimeout.longValue());
				} catch (Exception e) {
					if (lastError == null) {
						lastError = new LockRuntimeException(
								"Can't convert 'lockTimeout' init parameter: "
										+ strLockTimeout);
					}
					e.printStackTrace();
				}
			}
			if (NumberUtils.isNumber(strBlockTimeout)) {
				try {
					Long blockTimeout = NumberUtils.createLong(strBlockTimeout);
					lockmanager.setBlockTimeout(blockTimeout.longValue());
				} catch (Exception e) {
					if (lastError == null) {
						lastError = new LockRuntimeException(
								"Can't convert 'blockTimeout' init parameter: "
										+ strBlockTimeout);
					}
					e.printStackTrace();
				}
			}
		}
		log.info("Startup inited ==========");
	}

	@RequestMapping(value = "/my-lockserver", method = RequestMethod.GET)
	void doGet(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("text/html");
		response.setHeader("Pragma", "no-cache");

		PrintWriter out = response.getWriter();

		out.println("<html><head><title>My Distributed Locking Servlet Status Page</title>");
		out.println("</head><body><h1>My Distributed Locking Servlet</h1>");
		out.println("The servlet is running.<p>");

		if (lastError == null) {
			out.println("The LockServer is running.<p>");
			out.println("LockManager info: " + lockmanager.getLockInfo()
					+ "<p>");
			out.println("Processed Lock Request: " + numRequests + "<p>");
		} else {
			out.println("<h2>The LockServer has a problem!</h2>");
			out.println("The error message is:<p>");
			out.println(lastError.getMessage() + "<p>");
			lastError.printStackTrace(out);
			lastError = null;
		}
		out.println("</body></html>");
	}

	@RequestMapping(value = "/my-lockserver", method = RequestMethod.POST)
	void doPost(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.info("lockmanager{}", lockmanager);
		numRequests++;
		try {
			// read request:
			LockManagerRemoteImpl.LockInfo info = (LockManagerRemoteImpl.LockInfo) buildObjectFromRequest(request);
			log.info("LockManagerRemoteImpl.LockInfo{}", info);
			Object result = null;
			// now execute the command specified by the selector
			try {
				switch (info.methodName) {
				case LockManagerRemoteImpl.METHOD_READ_LOCK: {
					result = new Boolean(lockmanager.readLock(info.key,
							info.resourceId, info.isolationLevel));
					break;
				}
				case LockManagerRemoteImpl.METHOD_RELEASE_SINGLE_LOCK: {
					result = new Boolean(lockmanager.releaseLock(info.key,
							info.resourceId));
					break;
				}
				case LockManagerRemoteImpl.METHOD_RELEASE_LOCKS: {
					lockmanager.releaseLocks(info.key);
					result = Boolean.TRUE;
					break;
				}
				case LockManagerRemoteImpl.METHOD_WRITE_LOCK: {
					result = new Boolean(lockmanager.writeLock(info.key,
							info.resourceId, info.isolationLevel));
					break;
				}
				case LockManagerRemoteImpl.METHOD_UPGRADE_LOCK: {
					result = new Boolean(lockmanager.upgradeLock(info.key,
							info.resourceId, info.isolationLevel));
					break;
				}
				case LockManagerRemoteImpl.METHOD_CHECK_READ: {
					result = new Boolean(lockmanager.hasRead(info.key,
							info.resourceId));
					break;
				}
				case LockManagerRemoteImpl.METHOD_CHECK_WRITE: {
					result = new Boolean(lockmanager.hasWrite(info.key,
							info.resourceId));
					break;
				}
				case LockManagerRemoteImpl.METHOD_CHECK_UPGRADE: {
					result = new Boolean(lockmanager.hasUpgrade(info.key,
							info.resourceId));
					break;
				}
				case LockManagerRemoteImpl.METHOD_LOCK_INFO: {
					result = lockmanager.getLockInfo();
					break;
				}
				case LockManagerRemoteImpl.METHOD_LOCK_TIMEOUT: {
					result = new Long(lockmanager.getLockTimeout());
					break;
				}
				case LockManagerRemoteImpl.METHOD_BLOCK_TIMEOUT: {
					result = new Long(lockmanager.getBlockTimeout());
					break;
				}
				default: {
					throw new LockRuntimeException("Unknown command:"
							+ info.methodName);
				}
				}
			} catch (RuntimeException e) {
				result = new LockRuntimeException(
						"Error while invoke specified method in servlet.", e);
			}

			ObjectOutputStream oos = new ObjectOutputStream(
					response.getOutputStream());
			oos.writeObject(result);
			oos.flush();
			oos.close();
		} catch (Throwable t) {
			lastError = t;
			t.printStackTrace();
		}
	}

	private Object buildObjectFromRequest(HttpServletRequest request)
			throws IOException, ClassNotFoundException {
		Object obj = null;
		InputStream is = request.getInputStream();
		ObjectInputStream objInputStream = new ObjectInputStream(is);
		obj = objInputStream.readObject();
		objInputStream.close();
		is.close();
		return obj;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication springApplication = new SpringApplication(
				Startup.class);
		springApplication.addListeners(new ApplicationStartup());
		springApplication.run(args);
	}
}
