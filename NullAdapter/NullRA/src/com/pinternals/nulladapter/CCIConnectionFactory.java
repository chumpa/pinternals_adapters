package com.pinternals.nulladapter;

import com.sap.aii.af.lib.ra.cci.XIConnectionFactory;
import com.sap.aii.af.lib.ra.cci.XIConnectionSpec;
import com.sap.aii.af.lib.ra.cci.XIRecordFactory;
import com.sap.aii.af.lib.trace.Trace;
import java.io.Serializable;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.NotSupportedException;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

public class CCIConnectionFactory implements XIConnectionFactory, Serializable, Referenceable {
	private static final Trace TRACE = new Trace(CCIConnectionFactory.class.getName());
	private static final long serialVersionUID = 5353760424443859984L;

	public CCIConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm) throws ResourceException {
		String SIGNATURE = "CCIConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)";
		TRACE.entering(SIGNATURE, new Object[] { mcf, cm });
		TRACE.exiting(SIGNATURE);
	}

	@Override
	public XIConnectionSpec getXIConnectionSpec() throws NotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XIRecordFactory getXIRecordFactory() throws NotSupportedException, ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection() throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection(ConnectionSpec var1) throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceAdapterMetaData getMetaData() throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RecordFactory getRecordFactory() throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setReference(Reference var1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Reference getReference() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

}
