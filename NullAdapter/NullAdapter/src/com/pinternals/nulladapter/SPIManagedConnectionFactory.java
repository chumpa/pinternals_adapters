package com.pinternals.nulladapter;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import com.sap.aii.af.lib.trace.Trace;
import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation;

@SuppressWarnings("deprecation")
public class SPIManagedConnectionFactory implements ManagedConnectionFactory, Serializable,
		ManagedConnectionFactoryActivation {
	private static ResourceException unsupported = new ResourceException("Unsupported method");
	private static final long serialVersionUID = 2197753211101170823L;
	private AdapterManager mgr = null;
	public static final Trace TRACE = new Trace(SPIManagedConnectionFactory.class.getName());

	@Override
	public Object createConnectionFactory() throws ResourceException {
		throw unsupported;
	}

	@Override
	public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
		String SIGNATURE = "createConnectionFactory(ConnectionManager cm)";
		TRACE.entering(SIGNATURE, new Object[] { cm });
		CCIConnectionFactory factory = new CCIConnectionFactory(this, cm);
		TRACE.exiting(SIGNATURE);
		return factory;
	}

	@Override
	public ManagedConnection createManagedConnection(Subject x, ConnectionRequestInfo y) throws ResourceException {
		throw unsupported;
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		throw unsupported;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ManagedConnection matchManagedConnections(Set x, Subject y, ConnectionRequestInfo z)
			throws ResourceException {
		throw unsupported;
	}

	@Override
	public void setLogWriter(PrintWriter var1) throws ResourceException {
		throw unsupported;
	}

	@Override
	public void start() {
		String SIGNATURE = "start()";
		TRACE.entering(SIGNATURE);
		mgr = new AdapterManager();
		mgr.start();
		TRACE.exiting(SIGNATURE);
	}

	@Override
	public void stop() {
		String SIGNATURE = "stop()";
		TRACE.entering(SIGNATURE);
		mgr.stop();
		TRACE.exiting(SIGNATURE);
	}

}
