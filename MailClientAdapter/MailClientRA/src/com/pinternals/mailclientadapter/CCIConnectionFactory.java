package com.pinternals.mailclientadapter;

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

public class CCIConnectionFactory
  implements XIConnectionFactory, Serializable, Referenceable
{
  static final long serialVersionUID = -6045039600076914577L;
  private static final XITrace TRACE = new XITrace(CCIConnectionFactory.class.getName());
  private ManagedConnectionFactory mcf = null;
  private ConnectionManager cm = null;
  private XIRecordFactory rf = null;
  private Reference reference = null;
  
  public CCIConnectionFactory()
    throws ResourceException
  {
    String SIGNATURE = "CciConnectionFactory()";
    TRACE.entering("CciConnectionFactory()");
    SPIManagedConnectionFactory smcf = new SPIManagedConnectionFactory();
    this.mcf = smcf;
    this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace());
    this.cm = new SPIConnectionManager();
    TRACE.exiting("CciConnectionFactory()");
  }
  
  public CCIConnectionFactory(ManagedConnectionFactory mcf)
    throws ResourceException
  {
    String SIGNATURE = "CciConnectionFactory(ManagedConnectionFactory mcf)";
    TRACE.entering("CciConnectionFactory(ManagedConnectionFactory mcf)", new Object[] { mcf });
    
    SPIManagedConnectionFactory smcf = null;
    if (mcf == null)
    {
      TRACE.warningT("CciConnectionFactory(ManagedConnectionFactory mcf)", XIAdapterCategories.SERVER_JCA, "ManagedConnectionFactory was null, local instance created instead!");
      smcf = new SPIManagedConnectionFactory();
    }
    else
    {
      if (!(mcf instanceof SPIManagedConnectionFactory))
      {
        ResourceException re = new ResourceException("Received ManagedConnectionFactory is not the one of the sample adapter.");
        TRACE.throwing("CciConnectionFactory(ManagedConnectionFactory mcf)", re);
        throw re;
      }
      smcf = (SPIManagedConnectionFactory)mcf;
    }
    this.mcf = smcf;
    this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace());
    this.cm = new SPIConnectionManager();
    TRACE.exiting("CciConnectionFactory(ManagedConnectionFactory mcf)");
  }
  
  public CCIConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)
    throws ResourceException
  {
    String SIGNATURE = "CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)";
    TRACE.entering("CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)", new Object[] { mcf, cm });
    
    SPIManagedConnectionFactory smcf = null;
    if (mcf == null)
    {
      TRACE.warningT("CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)", XIAdapterCategories.SERVER_JCA, "ManagedConnectionFactory was null, local instance created instead!");
      smcf = new SPIManagedConnectionFactory();
    }
    else
    {
      if (!(mcf instanceof SPIManagedConnectionFactory))
      {
        ResourceException re = new ResourceException("Received ManagedConnectionFactory is not the one of the sample adapter.");
        TRACE.throwing("CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)", re);
        throw re;
      }
      smcf = (SPIManagedConnectionFactory)mcf;
    }
    this.mcf = smcf;
    this.rf = new XIMessageFactoryImpl(smcf.getAdapterType(), smcf.getAdapterNamespace());
    if (cm == null)
    {
      TRACE.warningT("CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)", XIAdapterCategories.SERVER_JCA, "ConnectionManager was null, local instance created instead (two-tier)!");
      this.cm = new SPIConnectionManager();
    }
    else
    {
      this.cm = cm;
    }
    TRACE.exiting("CciConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)");
  }
  
  public Connection getConnection()
    throws ResourceException
  {
    String SIGNATURE = "getConnection()";
    TRACE.entering("getConnection()");
    Connection con = null;
    con = (Connection)this.cm.allocateConnection(this.mcf, null);
    TRACE.exiting("getConnection()");
    return con;
  }
  
  public Connection getConnection(ConnectionSpec spec)
    throws ResourceException
  {
    String SIGNATURE = "getConnection(ConnectionSpec spec)";
    TRACE.entering("getConnection(ConnectionSpec spec)", new Object[] { spec });
    if (!(spec instanceof XIConnectionSpec))
    {
      ResourceException re = new ResourceException("ConnectionSpec is not instance of CciConnectionSpec.");
      TRACE.throwing("getConnection(ConnectionSpec spec)", re);
      throw re;
    }
    Connection con = null;
    ConnectionRequestInfo info = new CCIConnectionRequestInfo(((XIConnectionSpec)spec).getUserName(), ((XIConnectionSpec)spec).getPassword(), ((XIConnectionSpec)spec).getChannelId());
    



    con = (Connection)this.cm.allocateConnection(this.mcf, info);
    
    TRACE.exiting("getConnection(ConnectionSpec spec)");
    return con;
  }
  
  public ResourceAdapterMetaData getMetaData()
    throws ResourceException
  {
    String SIGNATURE = "getMetaData()";
    TRACE.entering("getMetaData()");
    CCIResourceAdapterMetaData meta = new CCIResourceAdapterMetaData();
    TRACE.exiting("getMetaData()");
    return meta;
  }
  
  public RecordFactory getRecordFactory()
    throws ResourceException
  {
    return this.rf;
  }
  
  public void setReference(Reference ref)
  {
    String SIGNATURE = "setReference()";
    TRACE.entering("setReference()", new Object[] { ref });
    this.reference = ref;
    TRACE.exiting("setReference()");
  }
  
  public Reference getReference()
  {
    return this.reference;
  }
  
  public XIConnectionSpec getXIConnectionSpec()
    throws NotSupportedException
  {
    return new XIConnectionSpecImpl();
  }
  
  public XIRecordFactory getXIRecordFactory()
    throws NotSupportedException, ResourceException
  {
    return this.rf;
  }
}

