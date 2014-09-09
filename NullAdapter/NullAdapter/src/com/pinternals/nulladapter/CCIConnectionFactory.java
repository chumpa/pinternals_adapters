package com.pinternals.nulladapter;

import java.io.Serializable;

import javax.naming.Reference;
import javax.resource.NotSupportedException;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

import com.sap.aii.af.lib.ra.cci.XIConnectionFactory;
import com.sap.aii.af.lib.ra.cci.XIConnectionSpec;
import com.sap.aii.af.lib.ra.cci.XIRecordFactory;

public class CCIConnectionFactory implements XIConnectionFactory, Serializable, Referenceable {
	static final long serialVersionUID = 180419750001L;
	private static final XITrace TRACE = new XITrace(CCIConnectionFactory.class.getName());
	
	private ManagedConnectionFactory mcf = null;
	private ConnectionManager cm = null;
	private XIRecordFactory rf = null;
	private Reference reference = null;

	public CCIConnectionFactory() throws ResourceException {
		String SIGNATURE = "CciConnectionFactory()";
		TRACE.entering(SIGNATURE);
		SPIManagedConnectionFactory smcf = new SPIManagedConnectionFactory();
		this.mcf = smcf;
		this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace());
		this.cm = new SPIConnectionManager();
		TRACE.exiting(SIGNATURE);
	}

	public CCIConnectionFactory(ManagedConnectionFactory mcf) throws ResourceException {
		String SIGNATURE = "CciConnectionFactory(ManagedConnectionFactory mcf)";
		TRACE.entering(SIGNATURE, new Object[] { mcf });

		SPIManagedConnectionFactory smcf = null;
		if (mcf == null) {
			TRACE.warningT(SIGNATURE, AdapterConstants.LogCategoryServer, "ManagedConnectionFactory was null, local instance created instead!");
			smcf = new SPIManagedConnectionFactory();
		} else {
			if (!(mcf instanceof SPIManagedConnectionFactory)) {
				ResourceException re = new ResourceException("Received ManagedConnectionFactory is not the one of the sample adapter.");
				TRACE.throwing(SIGNATURE, re);
				throw re;
			}
			smcf = (SPIManagedConnectionFactory) mcf;
		}
		this.mcf = smcf;
		this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace());
		this.cm = new SPIConnectionManager();
		TRACE.exiting(SIGNATURE);
	}

	public CCIConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm) throws ResourceException {
		String SIGNATURE = "CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)";
		TRACE.entering(SIGNATURE, new Object[] { mcf, cm });

		SPIManagedConnectionFactory smcf = null;
		if (mcf == null) {
			TRACE.warningT(SIGNATURE, AdapterConstants.LogCategoryServer, "ManagedConnectionFactory was null, local instance created instead!");
			smcf = new SPIManagedConnectionFactory();
		} else {
			if (!(mcf instanceof SPIManagedConnectionFactory)) {
				ResourceException re = new ResourceException("Received ManagedConnectionFactory is not the one of the sample adapter.");
				TRACE.throwing(SIGNATURE, re);
				throw re;
			}
			smcf = (SPIManagedConnectionFactory) mcf;
		}
		this.mcf = smcf;
		this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace());
		if (cm == null) {
			TRACE.warningT(SIGNATURE, AdapterConstants.LogCategoryServer, "ConnectionManager was null, local instance created instead (two-tier)!");
			this.cm = new SPIConnectionManager();
		} else {
			this.cm = cm;
		}
		TRACE.exiting(SIGNATURE);
	}

	public Connection getConnection() throws ResourceException {
		String SIGNATURE = "getConnection()";
		TRACE.entering(SIGNATURE);
		Connection con = null;
		con = (Connection) this.cm.allocateConnection(this.mcf, null);
		TRACE.exiting(SIGNATURE);
		return con;
	}

	public Connection getConnection(ConnectionSpec spec) throws ResourceException {
		String SIGNATURE = "getConnection(ConnectionSpec spec)";
		TRACE.entering(SIGNATURE, new Object[] { spec });
		if (!(spec instanceof XIConnectionSpec)) {
			ResourceException re = new ResourceException("ConnectionSpec is not instance of CciConnectionSpec.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		Connection con = null;
		ConnectionRequestInfo info = new CCIConnectionRequestInfo(((XIConnectionSpec) spec).getUserName(), ((XIConnectionSpec) spec).getPassword(), ((XIConnectionSpec) spec).getChannelId());
		con = (Connection) this.cm.allocateConnection(this.mcf, info);
		TRACE.exiting(SIGNATURE);
		return con;
	}

	public ResourceAdapterMetaData getMetaData() throws ResourceException {
		String SIGNATURE = "getMetaData()";
		TRACE.entering(SIGNATURE);
		CCIResourceAdapterMetaData meta = new CCIResourceAdapterMetaData();
		TRACE.exiting(SIGNATURE);
		return meta;
	}

	public RecordFactory getRecordFactory() throws ResourceException {
		return this.rf;
	}

	public void setReference(Reference ref) {
		String SIGNATURE = "setReference()";
		TRACE.entering(SIGNATURE, new Object[] { ref });
		this.reference = ref;
		TRACE.exiting(SIGNATURE);
	}

	public Reference getReference() {
		return this.reference;
	}

	public XIConnectionSpec getXIConnectionSpec() throws NotSupportedException {
		String SIGNATURE = "setReference()";
		TRACE.entering(SIGNATURE);
		XIConnectionSpecImpl x = new XIConnectionSpecImpl();
		TRACE.entering(SIGNATURE);
		return x;
	}

	public XIRecordFactory getXIRecordFactory() throws NotSupportedException, ResourceException {
		return this.rf;
	}
}
