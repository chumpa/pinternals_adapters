package com.pinternals.mailclientadapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.resource.SAPSecurityResources;
import com.sap.aii.security.lib.KeyStoreManager;
import com.sap.aii.security.lib.PermissionMode;
import com.sap.security.api.ssf.ISsfProfile;
 
 public class SPIManagedConnection
   implements ManagedConnection
 {
   private static final XITrace TRACE = new XITrace(SPIManagedConnection.class.getName());
   private XIConnectionEventListenerManager cciListener;
   private PasswordCredential credential;
   private SPIManagedConnectionFactory mcf;
   private PrintWriter logWriter;
   private boolean supportsLocalTx;
   private boolean destroyed;
   private Set<Object> connectionSet;
   private FileOutputStream physicalConnection;
//   private String outFileNamePrefix = null;
   private String channelID = null;
   private Channel channel = null;
   
//   private String connectionIMAP = null;
   
//   private String fileMode = null;
//   private String directory = null;
//   private String prefix = null;
//   private File outFile = null;
//   private boolean asmaGet = false;
//   private boolean asmaError = false;
   
   SPIManagedConnection(SPIManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID, Channel channel)
     throws ResourceException, NotSupportedException
   {
     String SIGNATURE = "SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)";
     TRACE.entering("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", new Object[] { mcf, credential, new Boolean(supportsLocalTx), channelID });
//     String outFileName = "(not set)";
     if (supportsLocalTx == true) {
       throw new NotSupportedException("Local transactions are not supported!");
     }
     this.mcf = mcf;
     this.credential = credential;
     this.supportsLocalTx = supportsLocalTx;
     this.channelID = channelID;
     this.channel = channel;
     
     this.connectionSet = new HashSet<Object>();
     this.cciListener = new XIConnectionEventListenerManager(this);
     TRACE.infoT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT, "Physical connection, the file, was opened sucessfuly");
     TRACE.exiting("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)");
   }
   
   String getChannelID()
   {
     return this.channelID;
   }
   
   public void setSupportsLocalTx(boolean ltx)
     throws NotSupportedException
   {
     if (ltx == true) {
       throw new NotSupportedException("Local transactions are not supported!");
     }
     this.supportsLocalTx = ltx;
   }
   
   public boolean getSupportsLocalTx()
   {
     return this.supportsLocalTx;
   }
   
   public void setManagedConnectionFactory(SPIManagedConnectionFactory mcf)
   {
     this.mcf = mcf;
   }
   
   public ManagedConnectionFactory getManagedConnectionFactory()
   {
     return this.mcf;
   }
   
   public Object getConnection(Subject subject, ConnectionRequestInfo info)
     throws ResourceException
   {
     String SIGNATURE = "getConnection(Subject subject, ConnectionRequestInfo info)";
     TRACE.entering("getConnection(Subject subject, ConnectionRequestInfo info)", new Object[] { subject, info });
     
 
     checkIfDestroyed();
     CCIConnection cciConnection = new CCIConnection(this);
     addCciConnection(cciConnection);
     TRACE.exiting("getConnection(Subject subject, ConnectionRequestInfo info)");
     return cciConnection;
   }
   
   public void destroy()
     throws ResourceException
   {
     String SIGNATURE = "destroy()";
     TRACE.entering("destroy()");
     destroy(false);
     TRACE.exiting("destroy()");
   }
   
   void destroy(boolean fromMCF)
     throws ResourceException
   {
     String SIGNATURE = "destroy(boolean fromMCF)";
     TRACE.entering("destroy(boolean fromMCF)", new Object[] { new Boolean(fromMCF) });
     if (!this.destroyed) {
       try
       {
         this.destroyed = true;
         Iterator it = this.connectionSet.iterator();
         while (it.hasNext())
         {
           CCIConnection cciCon = (CCIConnection)it.next();
           cciCon.invalidate();
         }
         this.connectionSet.clear();
         this.physicalConnection.close();
       }
       catch (Exception ex)
       {
         TRACE.catching("destroy(boolean fromMCF)", ex);
         throw new ResourceException(ex.getMessage());
       }
     }
     if (!fromMCF) {
       this.mcf.removeManagedConnection(this.channelID);
     }
     TRACE.exiting("destroy(boolean fromMCF)");
   }
   
   public void cleanup()
     throws ResourceException
   {
     String SIGNATURE = "cleanup()";
     TRACE.entering("cleanup()");
//     try
//     {
//       checkIfDestroyed();
//       Iterator it = this.connectionSet.iterator();
//       while (it.hasNext())
//       {
//         CCIConnection cciCon = (CCIConnection)it.next();
//         cciCon.invalidate();
//       }
//       this.connectionSet.clear();
//       if (0 != 0)
//       {
//         this.physicalConnection.close();
//         String outFileName = this.mcf.getOutFileName(this.outFileNamePrefix);
//         this.physicalConnection = new FileOutputStream(outFileName);
//         TRACE.infoT("cleanup()", XIAdapterCategories.CONNECT, "Physical connection was cleaned and a new file was opened sucessfuly. Filename: " + outFileName);
//       }
//     }
//     catch (Exception ex)
//     {
//       TRACE.catching("cleanup()", ex);
//       throw new ResourceException(ex.getMessage());
//     }
     TRACE.exiting("cleanup()");
   }
   
   public void associateConnection(Object connection)
     throws ResourceException
   {
     String SIGNATURE = "associateConnection(Object connection)";
     TRACE.entering("associateConnection(Object connection)");
     
     checkIfDestroyed();
     if ((connection instanceof CCIConnection))
     {
       CCIConnection cciCon = (CCIConnection)connection;
       cciCon.associateConnection(this);
     }
     else
     {
       java.lang.IllegalStateException ise = new java.lang.IllegalStateException("Invalid connection object: " + connection);
       TRACE.throwing("associateConnection(Object connection)", ise);
       throw ise;
     }
     TRACE.exiting("associateConnection(Object connection)");
   }
   
   public void addConnectionEventListener(ConnectionEventListener listener)
   {
     String SIGNATURE = "addConnectionEventListener(ConnectionEventListener listener)";
     TRACE.entering("addConnectionEventListener(ConnectionEventListener listener)");
     this.cciListener.addConnectorListener(listener);
     TRACE.exiting("addConnectionEventListener(ConnectionEventListener listener)");
   }
   
   public void removeConnectionEventListener(ConnectionEventListener listener)
   {
     String SIGNATURE = "removeConnectionEventListener(ConnectionEventListener listener)";
     TRACE.entering("removeConnectionEventListener(ConnectionEventListener listener)");
     this.cciListener.removeConnectorListener(listener);
     TRACE.exiting("removeConnectionEventListener(ConnectionEventListener listener)");
   }
   
   public XAResource getXAResource()
     throws ResourceException
   {
     throw new NotSupportedException("XA transaction not supported");
   }
   
   public LocalTransaction getLocalTransaction()
     throws ResourceException
   {
     throw new NotSupportedException("Local transaction not supported");
   }
   
   public ManagedConnectionMetaData getMetaData()
     throws ResourceException
   {
     checkIfDestroyed();
     return new SPIManagedConnectionMetaData(this);
   }
   
   public void setLogWriter(PrintWriter out)
     throws ResourceException
   {
     String SIGNATURE = "setLogWriter(PrintWriter out)";
     TRACE.entering("setLogWriter(PrintWriter out)", new Object[] { out });
     this.logWriter = out;
     out.print("XI AF Sample Adapter has received a J2EE container log writer.");
     out.print("XI AF Sample Adapter will not use the J2EE container log writer. See the trace file for details.");
     TRACE.exiting("setLogWriter(PrintWriter out)");
   }
   
   public PrintWriter getLogWriter()
     throws ResourceException
   {
     return this.logWriter;
   }
   
   boolean isDestroyed()
   {
     return this.destroyed;
   }
   
   PasswordCredential getPasswordCredential()
   {
     return this.credential;
   }
   
   public void sendEvent(int eventType, Exception ex)
   {
     this.cciListener.sendEvent(eventType, ex, null);
   }
   
   public void sendEvent(int eventType, Exception ex, Object connectionHandle)
   {
     this.cciListener.sendEvent(eventType, ex, connectionHandle);
   }
   
   public void addCciConnection(CCIConnection cciCon)
   {
     this.connectionSet.add(cciCon);
   }
   
   public void removeCciConnection(CCIConnection cciCon)
   {
     this.connectionSet.remove(cciCon);
   }
   
   public void start()
     throws ResourceException
   {
     this.mcf.startMCF();
   }
   
   public void stop()
     throws ResourceException
   {
     this.mcf.stopMCF();
   }
   
   private void checkIfDestroyed()
     throws ResourceException
   {
     if (this.destroyed) {
       throw new javax.resource.spi.IllegalStateException("Managed connection is closed");
     }
   }
 }

