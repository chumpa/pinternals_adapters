package com.pinternals.mailclientadapter;
import java.io.Serializable;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

public class SPIConnectionManager
  implements ConnectionManager, Serializable
{
  static final long serialVersionUID = 123L;
  private static final XITrace TRACE = new XITrace(SPIConnectionManager.class.getName());
  
  public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)
    throws ResourceException
  {
    String SIGNATURE = "allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)";
    TRACE.entering("allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)", new Object[] { mcf });
    ManagedConnection mc = mcf.createManagedConnection(null, info);
    
    Object cciConnection = mc.getConnection(null, info);
    TRACE.exiting("allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)");
    return cciConnection;
  }
}
