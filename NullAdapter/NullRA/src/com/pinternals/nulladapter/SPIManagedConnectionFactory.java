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

import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation;

@SuppressWarnings("deprecation")
public class SPIManagedConnectionFactory implements ManagedConnectionFactory, Serializable, ManagedConnectionFactoryActivation {

	private static final long serialVersionUID = 2197753211101170823L;

	@Override
	public Object createConnectionFactory() throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object createConnectionFactory(ConnectionManager var1) throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ManagedConnection createManagedConnection(Subject var1, ConnectionRequestInfo var2) throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return null;
	}

	@Override
	public ManagedConnection matchManagedConnections(Set var1, Subject var2, ConnectionRequestInfo var3)
			throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter var1) throws ResourceException {
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}
