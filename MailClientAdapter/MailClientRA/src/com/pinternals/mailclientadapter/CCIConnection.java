package com.pinternals.mailclientadapter;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.IllegalStateException;
 
 public class CCIConnection
   implements Connection
 {
   private static final XITrace TRACE = new XITrace(CCIConnection.class.getName());
   private SPIManagedConnection mc = null;
   
   CCIConnection(SPIManagedConnection mc)
   {
     String SIGNATURE = "CciConnection(SpiManagedConnection)";
     TRACE.entering("CciConnection(SpiManagedConnection)", new Object[] { mc });
     this.mc = mc;
     TRACE.exiting("CciConnection(SpiManagedConnection)");
   }
   
   public Interaction createInteraction()
     throws ResourceException
   {
     String SIGNATURE = "createInteraction()";
     TRACE.entering("createInteraction()");
     if (this.mc == null) {
       throw new ResourceException("Connection is invalid");
     }
     CCIInteraction interaction = new CCIInteraction(this);
     TRACE.exiting("createInteraction()");
     return interaction;
   }
   
   public LocalTransaction getLocalTransaction()
     throws ResourceException
   {
     throw new NotSupportedException("Local Transaction not supported!!");
   }
   
   public ResultSetInfo getResultSetInfo()
     throws ResourceException
   {
     throw new NotSupportedException("ResultSet is not supported.");
   }
   
   public void close()
     throws ResourceException
   {
     String SIGNATURE = "close()";
     TRACE.entering("close()");
     if (this.mc == null) {
       return;
     }
     this.mc.removeCciConnection(this);
     this.mc.sendEvent(1, null, this);
     this.mc = null;
     TRACE.exiting("close()");
   }
   
   public ConnectionMetaData getMetaData()
     throws ResourceException
   {
     String SIGNATURE = "getMetaData()";
     TRACE.entering("getMetaData()");
     CCIConnectionMetaData cmd = new CCIConnectionMetaData(this.mc);
     TRACE.exiting("getMetaData()");
     return cmd;
   }
   
   void associateConnection(SPIManagedConnection newMc)
     throws ResourceException
   {
     String SIGNATURE = "associateConnection(SPIManagedConnection newMc)";
     TRACE.entering("associateConnection(SPIManagedConnection newMc)");
     try
     {
       checkIfValid();
     }
     catch (ResourceException ex)
     {
       TRACE.catching("associateConnection(SPIManagedConnection newMc)", ex);
       throw new IllegalStateException("Connection is invalid");
     }
     this.mc.removeCciConnection(this);
     
     newMc.addCciConnection(this);
     this.mc = newMc;
     TRACE.exiting("associateConnection(SPIManagedConnection newMc)");
   }
   
   SPIManagedConnection getManagedConnection()
   {
     String SIGNATURE = "getManagedConnection()";
     TRACE.entering("getManagedConnection()");
     TRACE.exiting("getManagedConnection()");
     return this.mc;
   }
   
   void checkIfValid()
     throws ResourceException
   {
     String SIGNATURE = "checkIfValid()";
     TRACE.entering("checkIfValid()");
     if (this.mc == null) {
       throw new ResourceException("Connection is invalid");
     }
     TRACE.exiting("checkIfValid()");
   }
   
   void invalidate()
   {
     String SIGNATURE = "invalidate()";
     TRACE.entering("invalidate()");
     this.mc = null;
     TRACE.exiting("invalidate()");
   }
 }

