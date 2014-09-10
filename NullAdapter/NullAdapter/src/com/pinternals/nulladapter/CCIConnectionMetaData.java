package com.pinternals.nulladapter;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import com.sap.aii.af.lib.trace.Trace;

public class CCIConnectionMetaData implements ConnectionMetaData {
	private static final Trace TRACE = new Trace(CCIConnectionMetaData.class.getName());
	private SPIManagedConnection mc;
	private static final String version = new String(AdapterConstants.version);
	private static final String name = new String("Connection");

	public CCIConnectionMetaData(SPIManagedConnection mc) {
		this.mc = mc;
	}

	public String getEISProductName() throws ResourceException {
		return name;
	}

	public String getEISProductVersion() throws ResourceException {
		return version;
	}

	public String getUserName() throws ResourceException {
		String SIGNATURE = "getUserName()";
		TRACE.entering(SIGNATURE);

		String userName = null;
		if (this.mc.isDestroyed()) {
			throw new ResourceException("ManagedConnection is destroyed");
		}
		PasswordCredential cred = this.mc.getPasswordCredential();
		if (cred != null) {
			userName = cred.getUserName();
		}
		TRACE.entering(SIGNATURE);
		return userName;
	}
}
