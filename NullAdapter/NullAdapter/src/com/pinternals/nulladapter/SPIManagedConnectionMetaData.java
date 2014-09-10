package com.pinternals.nulladapter;

import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ManagedConnectionMetaData;
import com.sap.aii.af.lib.trace.Trace;

public class SPIManagedConnectionMetaData implements ManagedConnectionMetaData {
	private static final Trace TRACE = new Trace(SPIManagedConnectionMetaData.class.getName());
	private SPIManagedConnection mc;
	private static final String version = new String("1.0");
	private static final String name = new String(
			"SAP XI JCA 1.0 Sample Resource Adapter File System Connection");

	public SPIManagedConnectionMetaData(SPIManagedConnection mc) {
		this.mc = mc;
	}

	public String getEISProductName() throws ResourceException {
		return name;
	}

	public String getEISProductVersion() throws ResourceException {
		return version;
	}

	public int getMaxConnections() throws ResourceException {
		return 0;
	}

	public String getUserName() throws ResourceException {
		String SIGNATURE = "getUserName()";
		TRACE.entering(SIGNATURE);
		if (this.mc.isDestroyed()) {
			throw new IllegalStateException("ManagedConnection has been destroyed");
		}
		String userName = null;
		if (this.mc.getPasswordCredential() != null) {
			userName = this.mc.getPasswordCredential().getUserName();
		}
		TRACE.exiting(SIGNATURE);
		return userName;
	}
}
