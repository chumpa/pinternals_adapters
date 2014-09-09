package com.pinternals.nulladapter;

import java.security.MessageDigest;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

public class XISecurityUtilities {
	private static final XITrace TRACE = new XITrace(XISecurityUtilities.class.getName());

	public static PasswordCredential getPasswordCredential(final ManagedConnectionFactory mcf,
			final Subject subject, ConnectionRequestInfo info) throws ResourceException {
		String SIGNATURE = "getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] { mcf, subject, info });

		PasswordCredential credential = null;

		// if (subject == null) {
		// if (info == null)
		// credential = null;
		// else {
		// CCIConnectionRequestInfo myinfo = (CCIConnectionRequestInfo) info;
		// credential = new PasswordCredential(myinfo.getUserName(),
		// myinfo.getPassword().toCharArray());
		// credential.setManagedConnectionFactory(mcf);
		// }
		// } else {
		// credential = (PasswordCredential) AccessController.doPrivileged(new
		// PrivilegedAction() {
		// public Object run() {
		// Set creds = subject.getPrivateCredentials(PasswordCredential.class);
		// Iterator iter = creds.iterator();
		// while (iter.hasNext()) {
		// PasswordCredential temp =(PasswordCredential) iter.next();
		// if (temp.getManagedConnectionFactory().equals(mcf)) {
		// return temp;
		// }
		// }
		// return null;
		// }
		// });
		// if (credential == null)
		// throw new SecurityException("No PasswordCredential found");
		// }

		TRACE.exiting(SIGNATURE);
		return credential;
	}

	public static boolean isEqual(String a, String b) {
		if (a == null) {
			return b == null;
		}
		return a.equals(b);
	}

	public static boolean isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b) {
		String SIGNATURE = "isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b)";
		TRACE.entering(SIGNATURE, new Object[] { a, b });
		boolean equal = false;
		if (a == b) {
			equal = true;
		} else if ((a == null) && (b != null)) {
			equal = false;
		} else if ((a != null) && (b == null)) {
			equal = false;
		} else if (!isEqual(a.getUserName(), b.getUserName())) {
			equal = false;
		} else {
			String p1 = null;
			String p2 = null;
			if (a.getPassword() != null) {
				p1 = new String(a.getPassword());
			}
			if (b.getPassword() != null) {
				p2 = new String(b.getPassword());
			}
			equal = isEqual(p1, p2);
		}
		TRACE.exiting(SIGNATURE);
		return equal;
	}

	public static String digest(String s) {
		String SIGNATURE = "digest(String)";
		TRACE.entering(SIGNATURE, new Object[] { s });
		String digestString = null;
		try {
			if ((s == null) || (s.length() == 0)) {
				return digestString;
			}
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(s.getBytes("UTF-8"));
			byte[] b = digest.digest();
			digestString = bytesToHexString(b);
			return digestString;
		} catch (Exception e) {
			byte[] b;
			TRACE.catching(SIGNATURE, e);
			digestString = s;
			return digestString;
		} finally {
			TRACE.exiting(SIGNATURE, digestString);
		}
	}

	public static String bytesToHexString(byte[] buf) {
		StringBuffer sb = new StringBuffer();
		for (byte b : buf) {
			sb.append(Integer.toHexString(b < 0 ? 256 + b : b));
		}
		return sb.toString();
	}
}
