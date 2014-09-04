package com.pinternals.nulladapter;

//import com.sap.aii.af.idoc.ejb.SAPJRAUtilInvokerLocal;
//import com.sap.aii.af.idoc.exception.IDOCAdapterException;
//import com.sap.aii.af.idoc.ra.xi.ChannelConnectionData;
//import com.sap.aii.af.idoc.ra.xi.XIConfiguration;
//import com.sap.aii.af.idoc.utils.Categories;
//import com.sap.aii.af.idoc.utils.MultiRepositoryUtil;
//import com.sap.aii.af.idoc.utils.SAPJEEUtils;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;
import java.util.Timer;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.sap.aii.af.lib.trace.Category;
import com.sap.aii.af.lib.trace.MessageTrace;
import com.sap.aii.af.lib.trace.PublicCategories;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.guid.GUID;

public class First implements ManagedConnectionFactory, Serializable, ResourceAdapter {

	public static final String VERSION_ID = "$Id: //tc/xpi.adapters/NW731EXT_10_REL/src/_idoc_rar_module/rar/src/com/sap/aii/af/idoc/ra/IDOCManagedConnectionFactory.java#6 $";
	private static final MessageTrace TRACE = new MessageTrace("$Id: //tc/xpi.adapters/NW731EXT_10_REL/src/_idoc_rar_module/rar/src/com/sap/aii/af/idoc/ra/IDOCManagedConnectionFactory.java#6 $");
	private static final Category CATEGORY = Category.getCategory(PublicCategories.SERVICES_ROOT, "ADAPTER");
	static final long serialVersionUID = 18041975000000L;
//	private AuditAccess audit = null;
//	private GUID mcfLocalGuid = null;
	private Timer controlTimer = new Timer();
	public static final String JNDI_NAME = "deployedAdapters/zzz/shareable/zzz";
	transient PrintWriter logWriter;
	Thread t = null;
	private WorkManager workManager = null;

	public First() throws ResourceException {
		String SIGNATURE = "First()";
		TRACE.entering(SIGNATURE);
		TRACE.exiting(SIGNATURE);
	}

	private void traceSample(String someText) {
		String SIGNATURE = "traceSample()";
		TRACE.exiting("traceSample()", "some return value");
	}

	public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
		String SIGNATURE = "createConnectionFactory(ConnectionManager cxManager)";
		TRACE.entering("createConnectionFactory(ConnectionManager cxManager)", new Object[] { cm });
		TRACE.exiting("createConnectionFactory(ConnectionManager cxManager)");
		return null;
	}

	public Object createConnectionFactory() throws ResourceException {
		String SIGNATURE = "createConnectionFactory()";
		TRACE.entering("createConnectionFactory()");
		TRACE.exiting("createConnectionFactory()");
		return null;
	}

	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info)
			throws ResourceException {
		String SIGNATURE = "createManagedConnection(Subject subject, ConnectionRequestInfo info)";
		TRACE.entering("createManagedConnection(Subject subject, ConnectionRequestInfo info)", new Object[] { subject,
				info });
		TRACE.exiting("createManagedConnection(Subject subject, ConnectionRequestInfo info)");
		return null;
	}

	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)
			throws ResourceException {
		String SIGNATURE = "matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)";
		TRACE.entering("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)", new Object[] {
				connectionSet, subject, info });
		TRACE.exiting("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)");
		return null;
	}

	public void setLogWriter(PrintWriter out) throws ResourceException {
		String SIGNATURE = "setLogWriter(PrintWriter out)";
		TRACE.entering("setLogWriter(PrintWriter out)", new Object[] { out });
		TRACE.exiting("setLogWriter(PrintWriter out)");
	}

	public PrintWriter getLogWriter() throws ResourceException {
		return this.logWriter;
	}

//	public AuditAccess getAuditAccess() {
//		return this.audit;
//	}

	public boolean equals(Object obj) {
		String SIGNATURE = "equals(Object obj)";
		TRACE.entering("equals(Object obj)", new Object[] { obj });
		TRACE.exiting("equals(Object obj)");
		return super.equals(obj);
	}

	public int hashCode() {
		String SIGNATURE = "hashCode()";
		TRACE.entering("hashCode()");
		TRACE.exiting("hashCode()");
		return super.hashCode();
	}

	private void startInternal() {
		String SIGNATURE = "start()";
		TRACE.entering("start()");
		// this.t = new Thread(new Runnable() {
		// public void run() {
		// try {
		// XIConfiguration.getInstance(IDOCManagedConnectionFactory.this.workManager).init(IDOCManagedConnectionFactory.this);
		// IDOCManagedConnectionFactory.this.cleanAndRestartDefaultRA();
		// } catch (ResourceException var2) {
		// IDOCManagedConnectionFactory.TRACE.catching("start()",
		// "SOA.apt_idoc.2012", var2);
		// IDOCManagedConnectionFactory.TRACE.errorT("start()",
		// IDOCManagedConnectionFactory.CATEGORY, "SOA.apt_idoc.2012",
		// var2.toString());
		// } catch (IDOCAdapterException var3) {
		// IDOCManagedConnectionFactory.TRACE.catching("start()",
		// "SOA.apt_idoc.2012", var3);
		// IDOCManagedConnectionFactory.TRACE.errorT("start()",
		// IDOCManagedConnectionFactory.CATEGORY, "SOA.apt_idoc.2012",
		// var3.toString());
		// }
		//
		// }
		// });
		// this.t.start();
		TRACE.exiting("start()");
	}

	// private void cleanAndRestartDefaultRA() throws IDOCAdapterException {
	// String SIGNATURE = "cleanAndRestartDefaultRA()";
	// TRACE.entering("cleanAndRestartDefaultRA()");
	//
	// try {
	// SAPJRAUtilInvokerLocal e =
	// SAPJEEUtils.newSAPJRAUtilInvokerLocalInstance();
	// String rule = MultiRepositoryUtil.getInstance().getSafeRule();
	// e.updateDefaultIdocRAProperties(rule,
	// MultiRepositoryUtil.getInstance().getConnections());
	// } catch (Exception var4) {
	// throw new IDOCAdapterException(var4);
	// }
	//
	// SAPJEEUtils.getInstance().restartDefaultRA();
	// TRACE.exiting("cleanAndRestartDefaultRA()");
	// }

	private void stopInternal() {
		String SIGNATURE = "stop()";
		TRACE.entering("stop()");

		// try {
		// XIConfiguration.getInstance(this.workManager).stop();
		// ChannelConnectionData.getInstance().clear();
		// this.t.stop();
		// } catch (ResourceException var3) {
		// TRACE.catching("stop()", "SOA.apt_idoc.2013", var3);
		// TRACE.errorT("stop()", CATEGORY, "SOA.apt_idoc.2013",
		// var3.toString());
		// }

		TRACE.exiting("stop()");
	}

	// public void setLockWaitingTime(Integer waitTime) {
	// SAPJEEUtils.getInstance().setSLEEP_TIME((long)waitTime.intValue());
	// }
	//
	// public Integer getLockWaitingTime() {
	// return Integer.getInteger("" +
	// SAPJEEUtils.getInstance().getSLEEP_TIME());
	// }
	//
	// public void setLockMaxRetries(Integer maxRetries) {
	// SAPJEEUtils.getInstance().setRETRY(maxRetries.intValue());
	// }
	//
	// public Integer getLockMaxRetries() {
	// return Integer.valueOf(SAPJEEUtils.getInstance().getRETRY());
	// }
	//
	// public void setCustomDestMap(String customDestMapStr) {
	// SAPJEEUtils.getInstance().setCustomDestinations(customDestMapStr);
	// }
	//
	// public String getCustomDestMap() {
	// return SAPJEEUtils.getInstance().getCUSTOM_DEST_MAP_STR();
	// }
	//
	// public void setDeleteConnectionVal(String value) {
	// SAPJEEUtils.getInstance().setDeleteConnectionVal(value);
	// }
	//
	// public String getDeleteConnectionVal() {
	// return SAPJEEUtils.getInstance().getDeleteConnectionVal();
	// }
	//
	// public void setDefaultSenderPort(String defaultSenderPort) {
	// if(defaultSenderPort != null && !defaultSenderPort.isEmpty()) {
	// SAPJEEUtils.getInstance().setDEFAULT_SENDER_PORT(defaultSenderPort);
	// }
	//
	// }
	//
	// public String getDefaultSenderPort() {
	// return SAPJEEUtils.getInstance().getDEFAULT_SENDER_PORT();
	// }
	//
	// public void setDuplicateCheck(String sendersWithDuplicateCheck) {
	// SAPJEEUtils.getInstance().setDuplicateCheck(sendersWithDuplicateCheck);
	// }
	//
	// public String getDuplicateCheck() {
	// return SAPJEEUtils.getInstance().getDuplicateCheck();
	// }
	//
	// public void setPersistance(Boolean persistanceFlag) {
	// String SIGNATURE = "setPersistance(Boolean persistanceFlag)";
	// Properties p = new Properties();
	// p.put("domain", "true");
	//
	// try {
	// InitialContext e = new InitialContext(p);
	// e.rebind("Persistance", persistanceFlag);
	// } catch (NamingException var5) {
	// TRACE.errorT("setPersistance(Boolean persistanceFlag)", CATEGORY,
	// "Error While getting Initialcontext", var5.toString());
	// }
	//
	// }
	//
	// public Boolean getPersistance() {
	// String SIGNATURE = "getPersistance()";
	// Properties p = new Properties();
	// p.put("domain", "true");
	// Boolean persistanceFlag = SAPJEEUtils.PERSISTANCE_DEFAULT;
	//
	// try {
	// InitialContext e = new InitialContext(p);
	// persistanceFlag = (Boolean)e.lookup("Persistance");
	// } catch (Exception var5) {
	// TRACE.errorT("getPersistance()", CATEGORY,
	// "Error While getting Initialcontext", var5.toString());
	// }
	//
	// return persistanceFlag;
	// }
	//
	// public void setAutoNumberFromDB(Boolean autoNumberFromDBFlag) {
	// String SIGNATURE = "setAutoNumberFromDB(Boolean autoNumberFromDBFlag)";
	// Properties p = new Properties();
	// p.put("domain", "true");
	//
	// try {
	// InitialContext e = new InitialContext(p);
	// e.rebind("AutoNumberFromDB", autoNumberFromDBFlag);
	// } catch (NamingException var5) {
	// TRACE.errorT("setAutoNumberFromDB(Boolean autoNumberFromDBFlag)",
	// CATEGORY, "Error While getting Initialcontext", var5.toString());
	// }
	//
	// }

	// public Boolean getAutoNumberFromDB() {
	// String SIGNATURE = "setAutoNumberFromDB()";
	// Properties p = new Properties();
	// p.put("domain", "true");
	// Boolean autoNumberFromDBFlag = SAPJEEUtils.AUTO_NUMBER_FROM_DB_DEFAULT;
	//
	// try {
	// InitialContext e = new InitialContext(p);
	// autoNumberFromDBFlag = (Boolean)e.lookup("AutoNumberFromDB");
	// } catch (Exception var5) {
	// TRACE.errorT("setAutoNumberFromDB()", CATEGORY,
	// "Error While getting Initialcontext", var5.toString());
	// }
	//
	// return autoNumberFromDBFlag;
	// }
	//
	// public Boolean getConfirmTID() {
	// String SIGNATURE = "getConfirmTID()";
	// TRACE.entering("getConfirmTID()");
	// return (Boolean)TRACE.exiting("getConfirmTID()",
	// Boolean.valueOf(SAPJEEUtils.getInstance().isConfirmTID()));
	// }
	//
	// public void setConfirmTID(Boolean value) {
	// String SIGNATURE = "setConfirmTID(Boolean)";
	// TRACE.entering("setConfirmTID(Boolean)", new Object[]{value});
	// if(value != null) {
	// SAPJEEUtils.getInstance().setConfirmTID(value.booleanValue());
	// TRACE.debugT("setConfirmTID(Boolean)", " Set value = ", new
	// Object[]{value});
	// }
	//
	// TRACE.exiting("setConfirmTID(Boolean)");
	// }

	// public void setRemoveNS(Boolean value) {
	// String SIGNATURE = "setRemoveNS(Boolean)";
	// TRACE.entering("setRemoveNS(Boolean)", new Object[]{value});
	// if(value != null) {
	// SAPJEEUtils.setRemoveNS(value.booleanValue());
	// TRACE.debugT("setRemoveNS(Boolean)", " Set value = " + value);
	// }
	//
	// TRACE.exiting("setRemoveNS(Boolean)");
	// }
	//
	// public Boolean getRemoveNS() {
	// String SIGNATURE = "getRemoveNS()";
	// TRACE.entering("getRemoveNS()");
	// return (Boolean)TRACE.exiting("getRemoveNS()",
	// Boolean.valueOf(SAPJEEUtils.isRemoveNS()));
	// }
	//
	// public Integer getDuplicateCheckPersist() {
	// String SIGNATURE = "getDuplicateCheckPersist()";
	// TRACE.entering("getDuplicateCheckPersist()");
	// return (Integer)TRACE.exiting("getDuplicateCheckPersist()",
	// Integer.valueOf(SAPJEEUtils.getInstance().getDuplicateCheckPersist()));
	// }
	//
	// public void setDuplicateCheckPersist(Integer value) {
	// String SIGNATURE = "setDuplicateCheckPersist(Integer)";
	// TRACE.entering("setDuplicateCheckPersist(Integer)", new Object[]{value});
	// if(value != null) {
	// SAPJEEUtils.getInstance().setDuplicateCheckPersist(value.intValue());
	// TRACE.debugT("setDuplicateCheckPersist(Integer)", " Set value = ", new
	// Object[]{value});
	// }
	//
	// TRACE.exiting("setDuplicateCheckPersist(Integer)");
	// }
	//
	// public Boolean getHandleMSEvent() {
	// String SIGNATURE = "getHandleMSEvent()";
	// TRACE.entering("getHandleMSEvent()");
	// return (Boolean)TRACE.exiting("getHandleMSEvent()",
	// Boolean.valueOf(SAPJEEUtils.getInstance().getHandleMSEvent()));
	// }
	//
	// public void setHandleMSEvent(Boolean value) {
	// String SIGNATURE = "setHandleMSEvent(Boolean)";
	// TRACE.entering("setHandleMSEvent(Boolean)", new Object[]{value});
	// if(value != null) {
	// SAPJEEUtils.getInstance().setHandleMSEvent(value.booleanValue());
	// TRACE.debugT("setHandleMSEvent(Boolean)", " Set value = ", new
	// Object[]{value});
	// }
	//
	// TRACE.exiting("setHandleMSEvent(Boolean)");
	// }
	//
	// public Boolean getResolveVirtualReceiver() {
	// String SIGNATURE = "getResolveVirtualReceiver()";
	// TRACE.entering("getResolveVirtualReceiver()");
	// return (Boolean)TRACE.exiting("getResolveVirtualReceiver()",
	// Boolean.valueOf(SAPJEEUtils.getInstance().getResolveVirtualReceiver()));
	// }
	//
	// public void setResolveVirtualReceiver(Boolean value) {
	// String SIGNATURE = "setResolveVirtualReceiver(Boolean)";
	// TRACE.entering("setResolveVirtualReceiver(Boolean)", new
	// Object[]{value});
	// if(value != null) {
	// SAPJEEUtils.getInstance().setResolveVirtualReceiver(value.booleanValue());
	// TRACE.debugT("setResolveVirtualReceiver(Boolean)", " Set value = ", new
	// Object[]{value});
	// }
	//
	// TRACE.exiting("setResolveVirtualReceiver(Boolean)");
	// }
	@Override
	public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException {
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) {
	}

	@Override
	public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
		return null;
	}

	@Override
	public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {
		this.workManager = arg0.getWorkManager();
		this.startInternal();
	}

	@Override
	public void stop() {
		this.stopInternal();
	}

	// public String getIDocXMLHandler() {
	// String SIGNATURE = "getIdocXmlHandler()";
	// TRACE.entering("getIdocXmlHandler()");
	// MessageTrace var10000 = TRACE;
	// SAPJEEUtils.getInstance();
	// return (String)var10000.exiting("getIdocXmlHandler()",
	// SAPJEEUtils.getIdocXMLHandler());
	// }
	//
	// public void setIDocXMLHandler(String value) {
	// String SIGNATURE = "setIdocXmlHandler(String)";
	// TRACE.entering("setIdocXmlHandler(String)", new Object[]{value});
	// if(value != null) {
	// SAPJEEUtils.getInstance().setIdocXMLHandler(value);
	// TRACE.debugT("setIdocXmlHandler(String)", " Set value = ", new
	// Object[]{value});
	// }
	//
	// TRACE.exiting("setIdocXmlHandler(String)");
	// }
	//
	// public String getMonitorInputHelpTarget() {
	// String SIGNATURE = "getMonitorInputHelpTarget()";
	// TRACE.entering("getMonitorInputHelpTarget()");
	// MessageTrace var10000 = TRACE;
	// SAPJEEUtils.getInstance();
	// return (String)var10000.exiting("getMonitorInputHelpTarget()",
	// SAPJEEUtils.getMonitorInputHelpTarget());
	// }
	//
	// public void setMonitorInputHelpTarget(String target) {
	// String SIGNATURE = "setMonitorInputHelpTarget(String persistanceFlag)";
	// TRACE.entering("setMonitorInputHelpTarget(String persistanceFlag)", new
	// Object[]{target});
	// if(target != null) {
	// SAPJEEUtils.getInstance().setMonitorInputHelpTarget(target);
	// TRACE.debugT("setMonitorInputHelpTarget(String persistanceFlag)",
	// " Set value = ", new Object[]{target});
	// }
	//
	// TRACE.exiting("setMonitorInputHelpTarget(String persistanceFlag)");
	// }

}
