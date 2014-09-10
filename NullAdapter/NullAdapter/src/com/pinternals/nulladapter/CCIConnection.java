package com.pinternals.nulladapter;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.IllegalStateException;

import com.sap.aii.af.lib.trace.Trace;

public class CCIConnection implements Connection {
	private static final Trace TRACE = new Trace(CCIConnection.class.getName());
	private SPIManagedConnection mc = null;

	CCIConnection(SPIManagedConnection mc) {
		String SIGNATURE = "CciConnection(SpiManagedConnection)";
		TRACE.entering(SIGNATURE, new Object[] { mc });
		this.mc = mc;
		TRACE.exiting(SIGNATURE);
	}

	public Interaction createInteraction() throws ResourceException {
		String SIGNATURE = "createInteraction()";
		TRACE.entering(SIGNATURE);
		if (this.mc == null) {
			throw new ResourceException("Connection is invalid");
		}
		CCIInteraction interaction = new CCIInteraction(this);
		TRACE.exiting(SIGNATURE);
		return interaction;
	}

	public LocalTransaction getLocalTransaction() throws ResourceException {
		throw new NotSupportedException("Local Transaction not supported!!");
	}

	public ResultSetInfo getResultSetInfo() throws ResourceException {
		throw new NotSupportedException("ResultSet is not supported.");
	}

	public void close() throws ResourceException {
		String SIGNATURE = "close()";
		TRACE.entering(SIGNATURE);
		if (this.mc == null) {
			return;
		}
		this.mc.removeCciConnection(this);
		this.mc.sendEvent(1, null, this);
		this.mc = null;
		TRACE.exiting(SIGNATURE);
	}

	public ConnectionMetaData getMetaData() throws ResourceException {
		String SIGNATURE = "getMetaData()";
		TRACE.entering(SIGNATURE);
		CCIConnectionMetaData cmd = new CCIConnectionMetaData(this.mc);
		TRACE.exiting(SIGNATURE);
		return cmd;
	}

	void associateConnection(SPIManagedConnection newMc) throws ResourceException {
		String SIGNATURE = "associateConnection(SPIManagedConnection newMc)";
		TRACE.entering(SIGNATURE);
		try {
			checkIfValid();
		} catch (ResourceException ex) {
			TRACE.catching("associateConnection(SPIManagedConnection newMc)", ex);
			throw new IllegalStateException("Connection is invalid");
		}
		this.mc.removeCciConnection(this);

		newMc.addCciConnection(this);
		this.mc = newMc;
		TRACE.exiting(SIGNATURE);
	}

	SPIManagedConnection getManagedConnection() {
		String SIGNATURE = "getManagedConnection()";
		TRACE.entering(SIGNATURE);
		TRACE.exiting(SIGNATURE);
		return this.mc;
	}

	void checkIfValid() throws ResourceException {
		String SIGNATURE = "checkIfValid()";
		TRACE.entering(SIGNATURE);
		if (this.mc == null) {
			throw new ResourceException("Connection is invalid");
		}
		TRACE.exiting(SIGNATURE);
	}

	void invalidate() {
		String SIGNATURE = "invalidate()";
		TRACE.entering(SIGNATURE);
		this.mc = null;
		TRACE.exiting(SIGNATURE);
	}
}
