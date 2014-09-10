package com.pinternals.nulladapter;

import java.io.Serializable;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import com.sap.aii.af.lib.trace.Trace;

public class SPIConnectionManager implements ConnectionManager, Serializable {
	private static final long serialVersionUID = -4890204421275269508L;
	private static final Trace TRACE = new Trace(SPIConnectionManager.class.getName());

	public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)
			throws ResourceException {
		String SIGNATURE = "allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)";
		TRACE.entering(SIGNATURE, new Object[] { mcf });
		ManagedConnection mc = mcf.createManagedConnection(null, info);
		Object cciConnection = mc.getConnection(null, info);
		TRACE.exiting(SIGNATURE);
		return cciConnection;
	}
}
