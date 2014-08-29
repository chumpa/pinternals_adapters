package com.pinternals.mailclientadapter;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.InitialContext;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.processor.ModuleProcessor;
import com.sap.aii.af.lib.mp.processor.ModuleProcessorFactory;
import com.sap.aii.af.lib.ra.cci.XIAdapterException;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.monitoring.ChannelDirection;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManager;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManagerFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContext;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContextFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessState;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.idmap.MessageIDMapper;
import com.sap.aii.af.service.resource.SAPAdapterResources;
import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation;
import com.sap.engine.interfaces.messaging.api.DeliverySemantics;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.TextPayload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.RetryControlException;
import com.sap.engine.interfaces.messaging.api.exception.RetryMode;
import com.sap.guid.GUID;
import com.sap.transaction.TransactionTicket;
import com.sap.transaction.TxException;
import com.sap.transaction.TxManager;
import com.sap.transaction.TxRollbackException;

public class SPIManagedConnectionFactory implements ManagedConnectionFactory, Serializable,
		Runnable, ManagedConnectionFactoryActivation {
	static final long serialVersionUID = -2387046407149571208L;
	private static final XITrace TRACE = new XITrace(SPIManagedConnectionFactory.class.getName());
	private AuditAccess audit = null;
	private GUID mcfLocalGuid = null;
	private Timer controlTimer = new Timer();
	private static final int TIMER_FIRST_RUN = 120000;
	private static final int TIMER_PERIOD = 60000;
	private static int fileCounter = 0;
	private static int waitTime = 5000;
	transient PrintWriter logWriter;
	private static Object synchronizer = new Object();
	private SAPAdapterResources msRes = null;
	private int threadStatus = 0;
	private static final int TH_INIT = 0;
	private static final int TH_STARTED = 1;
	private static final int TH_STOPPED = 2;
	private InitialContext ctx = null;
	private XIConfiguration xIConfiguration = null;
	private Map managedConnections = Collections.synchronizedMap(new HashMap());
	private transient MessageIDMapper messageIDMapper = null;
	private transient XIMessageFactoryImpl mf = null;
	static final String AS_ACTIVE = "active";
	static final String AS_INACTIVE = "inactive";
	// private static final String AM_CPA = "CPA";
	// private static final String AM_MSG = "MSG";
	// private static final String ADDR_AGENCY_EAN = "009";
	// private static final String ADDR_SCHEMA_GLN = "GLN";
	private String addressMode = null;
	private String adapterType = null;
	private String adapterNamespace = null;
	private int propWaitNum = 10;
	private int propWaitTime = 1000;
	// static final String OUT_DIR = "/temp";
	// static final String OUT_PREFIX = "sample_ra_output";
	// static final String IN_DIR = "/temp";
	// static final String IN_NAME = "sample_ra_input";
	// private static final String PM_TEST = "test";
	// private static final String PM_RENAME = "rename";
	// static final String FM_NEW = "new";
	// static final String FM_REPLACE = "replace";
	// private static final String QOS_EO = "EO";
	// private static final String QOS_EOIO = "EOIO";
	// private static final String QOS_BE = "BE";
	// private static final String ERR_NONE = "none";
	// private static final String ERR_ROLLBACK = "rollback";
	static final String ASMA_NAME = "JCAChannelID";

	public SPIManagedConnectionFactory() throws ResourceException {
		String SIGNATURE = "SpiManagedConnectionFactory()";
		TRACE.entering(SIGNATURE);
		try {
			ctx = new InitialContext();
			Object res = ctx.lookup("SAPAdapterResources");
			msRes = ((SAPAdapterResources) res);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0011",
					"Access to XI AF MS resource failed. Adapter cannot be started.");
		}
		try {
			synchronized (synchronizer) {
				mcfLocalGuid = new GUID();
				TRACE
						.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
								"This SPIManagedConnectionFactory has the GUID: "
										+ mcfLocalGuid.toString());
			}
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
					"Creation of MCF GUID failed. Thus no periodic status report possible! Reason: "
							+ e.getMessage());
		}
		TRACE.exiting(SIGNATURE);
	}

	private void traceSample(String someText) {
		String SIGNATURE = "traceSample()";

		TRACE.entering(SIGNATURE);

		TRACE.entering(SIGNATURE, new Object[] { someText });
		try {
			XIAdapterException te = new XIAdapterException("Test exception only");

			TRACE.throwing(SIGNATURE, te);
			throw te;
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.debugT(SIGNATURE, "A test TraceException was catched and ignored!");

			TRACE.exiting(SIGNATURE);

			TRACE.exiting(SIGNATURE, "some return value");
		}
	}

	private ModuleProcessor lookUpModuleProcessor(int retryNum) throws ResourceException {
		String SIGNATURE = "lookUpModuleProcessor()";
		TRACE.entering(SIGNATURE);
		ModuleProcessor mp = null;
		try {
			mp = ModuleProcessorFactory.getModuleProcessor(true, retryNum, propWaitTime);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE
					.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0012",
							"Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
			ResourceException re = new ResourceException(
					"Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
			throw re;
		}
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
				"Lookup of XI AF MP entry ejb was succesfully.");
		TRACE.exiting(SIGNATURE);
		return mp;
	}

	public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
		String SIGNATURE = "createConnectionFactory(ConnectionManager cxManager)";
		TRACE.entering(SIGNATURE, new Object[] { cm });
		CCIConnectionFactory factory = new CCIConnectionFactory(this, cm);
		TRACE.exiting(SIGNATURE);
		return factory;
	}

	public Object createConnectionFactory() throws ResourceException {
		String SIGNATURE = "createConnectionFactory()";
		TRACE.entering(SIGNATURE);
		CCIConnectionFactory factory = new CCIConnectionFactory(this, null);
		TRACE.exiting(SIGNATURE);
		return factory;
	}

	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info)
			throws ResourceException {
		String SIGNATURE = "createManagedConnection(Subject subject, ConnectionRequestInfo info)";
		TRACE.entering("createManagedConnection(Subject subject, ConnectionRequestInfo info)",
				new Object[] { subject, info });
		String channelID = null;
		Channel channel = null;

		SPIManagedConnection mc = null;
		if (!(info instanceof CCIConnectionRequestInfo)) {
			TRACE.errorT("createManagedConnection(Subject subject, ConnectionRequestInfo info)",
					XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0013",
					"Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
			ResourceException re = new ResourceException(
					"Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
			TRACE.throwing("createManagedConnection(Subject subject, ConnectionRequestInfo info)",
					re);
			throw re;
		}
		try {
			channelID = ((CCIConnectionRequestInfo) info).getChannelId();
			channel = (Channel) CPAFactory.getInstance().getLookupManager().getCPAObject(
					CPAObjectType.CHANNEL, channelID);
		} catch (Exception e) {
			TRACE.catching("createManagedConnection(Subject subject, ConnectionRequestInfo info)",
					e);
			TRACE
					.errorT(
							"createManagedConnection(Subject subject, ConnectionRequestInfo info)",
							XIAdapterCategories.CONNECT_AF,
							"SOA.apt_sample.0014",
							"Cannot access the channel parameters of channel: "
									+ channelID
									+ ". Check whether the channel is stopped in the administrator console.");
			ResourceException re = new ResourceException(
					"Cannot access the channel parameters of channel: "
							+ channelID
							+ ". Check whether the channel is stopped in the administrator console.");
			throw re;
		}
		PasswordCredential credential = XISecurityUtilities.getPasswordCredential(this, subject,
				info);
		mc = new SPIManagedConnection(this, credential, false, channelID, channel);
		if (mc != null) {
			managedConnections.put(channelID, mc);
			TRACE.debugT("createManagedConnection(Subject subject, ConnectionRequestInfo info)",
					XIAdapterCategories.CONNECT_AF,
					"For channelID {0} this managed connection is stored: {1}", new Object[] {
							channelID, mc });
		}
		TRACE.exiting("createManagedConnection(Subject subject, ConnectionRequestInfo info)");
		return mc;
	}

	void destroyManagedConnection(String channelID) throws ResourceException {
		String SIGNATURE = "destroyManagedConnection(String channelID)";
		TRACE.entering("destroyManagedConnection(String channelID)", new Object[] { channelID });
		SPIManagedConnection mc = null;
		try {
			mc = (SPIManagedConnection) managedConnections.get(channelID);
			if (mc != null) {
				mc.sendEvent(1, null, mc);
				managedConnections.remove(channelID);
				mc.destroy(true);
				TRACE.debugT("destroyManagedConnection(String channelID)",
						XIAdapterCategories.CONNECT_AF,
						"ManagedConnection for channel ID {0} found and destroyed.",
						new Object[] { channelID });
			} else {
				TRACE.warningT("destroyManagedConnection(String channelID)",
						XIAdapterCategories.CONNECT_AF,
						"ManagedConnection for channel ID {0} not found.",
						new Object[] { channelID });
			}
		} catch (Exception e) {
			TRACE.catching("destroyManagedConnection(String channelID)", e);
			TRACE.errorT("destroyManagedConnection(String channelID)",
					XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0015",
					"Received exception during ManagedConnection destroy: " + e.getMessage());
		}
		TRACE.exiting("destroyManagedConnection(String channelID)");
	}

	void removeManagedConnection(String channelID) {
		String SIGNATURE = "removeManagedConnection(String channelID)";
		TRACE.entering("removeManagedConnection(String channelID)", new Object[] { channelID });
		managedConnections.remove(channelID);
		TRACE.exiting("removeManagedConnection(String channelID)");
	}

	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject,
			ConnectionRequestInfo info) throws ResourceException {
		String SIGNATURE = "matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)";
		TRACE
				.entering(
						"matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)",
						new Object[] { connectionSet, subject, info });

		SPIManagedConnection mcFound = null;
		CCIConnectionRequestInfo cciInfo = null;
		PasswordCredential pc = XISecurityUtilities.getPasswordCredential(this, subject, info);
		if ((info instanceof CCIConnectionRequestInfo)) {
			cciInfo = (CCIConnectionRequestInfo) info;
		} else {
			TRACE
					.errorT(
							"matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)",
							XIAdapterCategories.CONNECT_AF,
							"Unknown ConnectionRequestInfo parameter received. Cannot match connection");
			return null;
		}
		Iterator it = connectionSet.iterator();
		while ((it.hasNext()) && (mcFound == null)) {
			Object obj = it.next();
			if ((obj instanceof SPIManagedConnection)) {
				SPIManagedConnection mc = (SPIManagedConnection) obj;
				if (!mc.isDestroyed()) {
					ManagedConnectionFactory mcf = mc.getManagedConnectionFactory();
					if ((XISecurityUtilities.isPasswordCredentialEqual(mc.getPasswordCredential(),
							pc))
							&& (mcf.equals(this))
							&& (mc.getChannelID().equalsIgnoreCase(cciInfo.getChannelId()))) {
						mcFound = mc;
						TRACE
								.debugT(
										"matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)",
										XIAdapterCategories.CONNECT,
										"Found existing ManagedConnection in container set for channel {0}.",
										new Object[] { mc.getChannelID() });
					} else {
						TRACE
								.debugT(
										"matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)",
										XIAdapterCategories.CONNECT,
										"ManagedConnection in container set does not fit. Ignore.");
					}
				} else {
					TRACE
							.debugT(
									"matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)",
									XIAdapterCategories.CONNECT,
									"Destroyed sample ManagedConnection in container set. Ignore.");
				}
			} else {
				TRACE
						.debugT(
								"matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)",
								XIAdapterCategories.CONNECT,
								"This is not a sample ManagedConnection in container set. Ignore.");
			}
		}
		TRACE
				.exiting("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)");
		return mcFound;
	}

	public void setLogWriter(PrintWriter out) throws ResourceException {
		String SIGNATURE = "setLogWriter(PrintWriter out)";
		TRACE.entering("setLogWriter(PrintWriter out)", new Object[] { out });
		out.print("XI AF Sample Adapter has received a J2EE container log writer.");
		out
				.print("XI AF Sample Adapter will not use the J2EE container log writer. See the trace file for details.");
		logWriter = out;
		TRACE.exiting("setLogWriter(PrintWriter out)");
	}

	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter;
	}

	AuditAccess getAuditAccess() {
		return audit;
	}

	XIMessageFactoryImpl getXIMessageFactoryImpl() {
		return mf;
	}

	// public String getOutFileName(String outFileNamePrefix)
	// {
	// String SIGNATURE = "getFileName()";
	// TRACE.entering("getFileName()");
	//    
	// int cnt = 0;
	// String fileName = null;
	// synchronized (synchronizer)
	// {
	// cnt = fileCounter;
	// fileCounter += 1;
	// }
	// fileName = new String(outFileNamePrefix + "." + Integer.toString(cnt) +
	// ".txt");
	// TRACE.debugT("getFileName()", XIAdapterCategories.CONNECT,
	// "Output file name =" + fileName);
	//    
	// TRACE.exiting("getFileName()");
	// return fileName;
	// }

	public boolean equals(Object obj) {
		String SIGNATURE = "equals(Object obj)";
		TRACE.entering("equals(Object obj)", new Object[] { obj });
		boolean equal = false;
		if ((obj instanceof SPIManagedConnectionFactory)) {
			SPIManagedConnectionFactory other = (SPIManagedConnectionFactory) obj;
			if ((adapterNamespace.equals(other.getAdapterNamespace()))
					&& (adapterType.equals(other.getAdapterType()))
					&& (addressMode.equals(other.getAddressMode()))) {
				equal = true;
			}
		}
		TRACE.exiting("equals(Object obj)");
		return equal;
	}

	public int hashCode() {
		String SIGNATURE = "hashCode()";
		TRACE.entering("hashCode()");
		int hash = 0;
		String propset = adapterNamespace + adapterType + addressMode;
		hash = propset.hashCode();
		TRACE.exiting("hashCode()");
		return hash;
	}

	public static int getFileCounter() {
		return fileCounter;
	}

	public String getAddressMode() {
		String SIGNATURE = "getAddressMode()";
		TRACE.entering("getAddressMode()");
		TRACE.debugT("getAddressMode()", XIAdapterCategories.CONNECT,
				"Address determination mode =" + addressMode);
		TRACE.exiting("getAddressMode()");
		return addressMode;
	}

	public void setAddressMode(String addressMode) {
		this.addressMode = addressMode;
	}

	public void startMCF() throws ResourceException {
		String SIGNATURE = "startMCF()";
		TRACE.entering("startMCF()");
		if (threadStatus != 1) {
			try {
				threadStatus = 1;
				msRes.startRunnable(this);
			} catch (Exception e) {
				TRACE.catching("startMCF()", e);
				threadStatus = 2;
				TRACE.errorT("startMCF()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0016",
						"Cannot start inbound message thread");
				ResourceException re = new ResourceException(e.getMessage());
				TRACE.throwing("startMCF()", re);
				throw re;
			}
		}
		TRACE.exiting("startMCF()");
	}

	public void stopMCF() throws ResourceException {
		String SIGNATURE = "stopMCF()";
		TRACE.entering("stopMCF()");

		threadStatus = 2;
		try {
			synchronized (this) {
				notify();

				wait(waitTime + 1000);
			}
			xIConfiguration.stop();
		} catch (Exception e) {
			TRACE.catching("stopMCF()", e);
			TRACE.errorT("stopMCF()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0017",
					"Cannot stop inbound message thread. Reason: " + e.getMessage());
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing("stopMCF()", re);
			throw re;
		}
		TRACE.exiting("stopMCF()");
	}

	public void startTimer() {
		String SIGNATURE = "startTimer()";
		TRACE.entering("startTimer()");
		if (mcfLocalGuid != null) {
			try {
				controlTimer.scheduleAtFixedRate(
						new XIManagedConnectionFactoryController(this, ctx), 120000L, 60000L);
			} catch (Exception e) {
				TRACE.catching("startTimer()", e);
				TRACE.debugT("startTimer()", XIAdapterCategories.CONNECT_AF,
						"Creation of MCF controller failed. No periodic MCF status reports available! Reason: "
								+ e.getMessage());
			}
		}
		TRACE.exiting("startTimer()");
	}

	public void stopTimer() {
		String SIGNATURE = "stopTimer()";
		TRACE.entering("stopTimer()");
		controlTimer.cancel();
		TRACE.exiting("stopTimer()");
	}

	public void run() {
		String SIGNATURE = "run()";
		TRACE.entering("run()");

		String oldThreadName = Thread.currentThread().getName();
		String newThreadName = "MailClientAdapter " + mcfLocalGuid;
		try {
			Thread.currentThread().setName(newThreadName);
			TRACE.debugT("run()", XIAdapterCategories.CONNECT_AF, "Switched thread name to: {0}",
					new Object[] { newThreadName });

			boolean notSet = true;
			int numTry = 0;
			int pollTime = -1;
			while ((notSet) && (numTry < propWaitNum)) {
				if ((addressMode != null) && (adapterType != null) && (adapterNamespace != null)) {
					notSet = false;
				}
				numTry++;
				TRACE.debugT("run()", XIAdapterCategories.CONNECT_AF,
						"MCF waits for setter completion. Try: {0} of {1}.", new Object[] {
								Integer.toString(numTry), Integer.toString(propWaitNum) });
				try {
					Thread.sleep(propWaitTime);
				} catch (Exception e) {
					TRACE.catching("run()", e);
				}
			}
			ModuleProcessor mp = null;
			try {
				mp = lookUpModuleProcessor(propWaitNum);
			} catch (Exception e) {
				TRACE.catching("run()", e);
				TRACE
						.errorT(
								"run()",
								XIAdapterCategories.CONNECT_AF,
								"Cannot instatiate the MailClientAdapter module processor bean. The inbound processing is stopped. Exception:"
										+ e.toString());
				threadStatus = 2;
			}
			if (xIConfiguration == null) {
				try {
					xIConfiguration = new XIConfiguration(adapterType, adapterNamespace);
					xIConfiguration.init(this);
				} catch (Exception e) {
					TRACE.catching("run()", e);
					TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0018",
							"Cannot instatiate the XI CPA handler. The inbound processing is stopped. Exception:"
									+ e.toString());
					threadStatus = 2;
				}
			}
			while (threadStatus == 1) {
				try {
					List<Channel> channels = xIConfiguration.getCopy(Direction.INBOUND);
					for (int i = 0; i < channels.size(); i++) {
						Channel channel = (Channel) channels.get(i);
						try {
							String processMode = null;
							String qos = null;
							String psec = null;
							String raiseError = null;
							String channelAddressMode = null;
							boolean set_asma = false;

							try {
								qos = channel.getValueAsString("qos");
								psec = channel.getValueAsString("pollInterval");
							} catch (Exception e) {
								TRACE.catching("run()", e);
							}
							int ptime = 0;
							if ((psec != null) && (psec.length() > 0)) {
								ptime = Integer.valueOf(psec).intValue() * 1000;
							}
							if ((pollTime < 0) || (ptime < pollTime)) {
								pollTime = ptime;
							}
							// sendMessageFromFile("/dev/null", channel,
							// processMode, qos, raiseError, channelAddressMode,
							// set_asma);
							// test code
							TRACE.warningT("XIManagedConnectionFactoryController.run()",
									XIAdapterCategories.CONNECT_AF,
									"Code for polling Mail Sender @@@");

							InitialContext ctx = new InitialContext();
							Session ses = (Session) ctx.lookup("java:comp/env/mail/MailSession");
							TRACE.debugT(SIGNATURE, "Session:" + ses + " "
									+ ses.getStore().toString());
							Store st = ses.getStore("imaps");
							st.connect();
							Folder f = st.getDefaultFolder();
							int mc;
							TRACE.debugT(SIGNATURE, "Store: {0}, Folder: {1},{2}", new Object[] {
									st, f, f.getFullName() });

							f = st.getFolder("INBOX");
							mc = f.getMessageCount();
							TRACE.debugT(SIGNATURE, "Store: {0}, Folder: {1}, messages count: {2}",
									new Object[] { st, f, mc });

							f = st.getFolder("INBOX/CurrencyRates");
							mc = f.getMessageCount();
							TRACE.debugT(SIGNATURE, "Store: {0}, Folder: {1}, messages count: {2}",
									new Object[] { st, f, mc });

							f = st.getFolder("Inbox/CurrencyRates");
							mc = f.getMessageCount();
							TRACE.debugT(SIGNATURE, "Store: {0}, Folder: {1}, messages count: {2}",
									new Object[] { st, f, mc });

							Binding b = CPAFactory.getInstance().getLookupManager()
									.getBindingByChannelId(channel.getObjectId());
							Message msg = mf.createMessageRecord(b.getFromParty(), b
									.getFromService(), b.getToParty(), b.getToService(), b
									.getActionName(), b.getActionNamespace());
							XMLPayload p = msg.createXMLPayload();
							p.setContent("<a/>".getBytes());
							msg.setDeliverySemantics(DeliverySemantics.ExactlyOnce);
							p.setName("MainDocument");
							p
									.setDescription("XI AF Sample Adapter Input: XML document as MainDocument");
							msg.setDocument(p);

						} catch (Exception e) {
							TRACE.catching("run()", e);
							TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF,
									"Cannot send message to channel {0}. Received exception: {1}",
									new Object[] { channel.getObjectId(), e.getMessage() });
						}
					}
				} catch (Exception e) {
					TRACE.catching("run()", e);
					TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0019",
							"Cannot access inbound channel configuration. Received exception: "
									+ e.getMessage());
				}
				try {
					synchronized (this) {
						if (pollTime <= 0) {
							wait(waitTime);
						} else {
							wait(pollTime);
						}
					}
				} catch (InterruptedException e1) {
					TRACE.catching("run()", e1);
					TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0020",
							"Inbound thread stopped. Received exception during wait period: "
									+ e1.getMessage());
					threadStatus = 2;
				}
			}
		} finally {
			Thread.currentThread().setName(oldThreadName);
			TRACE.debugT("run()", XIAdapterCategories.CONNECT_AF,
					"Switched thread name back to: {0}", new Object[] { oldThreadName });
		}
	}

	private void sendMessageFromFile1(String inFileName, Channel channel, String processMode,
			String qos, String raiseError, String channelAddressMode, boolean set_asma) {
		String SIGNATURE = "sendMessageFromFile(String inFileName)";
		String msgText = new String();

		File inputFile = null;
		String channelId = null;
		String xiMsgId = null;

		// boolean fileRead = true;
		// try
		// {
		// inputFile = new File(inFileName);
		// if (!inputFile.exists()) {
		// TRACE.warningT("sendMessageFromFile(String inFileName)",
		// "No filename '"+inFileName+"' exist");
		// fileRead = false;
		// }
		// }
		// catch (Exception e)
		// {
		// TRACE.catching("sendMessageFromFile(String inFileName)", e);
		// TRACE.errorT("sendMessageFromFile(String inFileName)",
		// XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0021", "Input file "
		// + inFileName + " attributes cannot be read. Received exception: " +
		// e.getMessage());
		// fileRead = false;
		// }
		String extMsgId = "MailClientAdapter" + Thread.currentThread().toString(); // getExternalMessageID(inputFile);
		// TRACE.infoT("sendMessageFromFile(String inFileName)",
		// "External message ID is '" + extMsgId +
		// "', fileRead="+fileRead+",processMode="+processMode+",qos="+qos+",inFileName="+inFileName);
		// if ((fileRead == true) && (0 !=
		// processMode.compareToIgnoreCase("test")) && (
		// (qos.equalsIgnoreCase("EOIO")) || (qos.equalsIgnoreCase("EO"))))
		// {
		// if ((xiMsgId = messageIDMapper.getMappedId(extMsgId)) != null)
		// {
		// TRACE.infoT("sendMessageFromFile(String inFileName)",
		// XIAdapterCategories.CONNECT_AF,
		// "Duplicated and already processed file (message) with id {0} detected.  It will be ignored.",
		// new Object[] { extMsgId });
		// MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
		// audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS,
		// "Duplicated and already processed file (message) with id {0} detected.  It will be ignored.",
		// new Object[] { extMsgId });
		// audit.flushAuditLogEntries(amk);
		// if (0 == processMode.compareToIgnoreCase("rename")) {
		// try
		// {
		// renameFile(inFileName, inputFile);
		// }
		// catch (Exception e)
		// {
		// TRACE.catching("sendMessageFromFile(String inFileName)", e);
		// }
		// }
		// TRACE.exiting("sendMessageFromFile(String inFileName)");
		// return;
		// }
		// TRACE.debugT("sendMessageFromFile(String inFileName)",
		// XIAdapterCategories.CONNECT_AF,
		// "Duplicate check passed succesfully. New message, no duplicate (id {0})",
		// new Object[] { extMsgId });
		// }
		// if (fileRead == true) {
		// try
		// {
		// BufferedReader in = new BufferedReader(new FileReader(inputFile));
		// String line = null;
		// while ((line = in.readLine()) != null) {
		// msgText = msgText + line + "\n";
		// }
		// in.close();
		// TRACE.debugT("sendMessageFromFile(String inFileName)",
		// XIAdapterCategories.CONNECT_AF, "File message text: " + msgText);
		// }
		// catch (Exception e)
		// {
		// TRACE.catching("sendMessageFromFile(String inFileName)", e);
		// TRACE.errorT("sendMessageFromFile(String inFileName)",
		// XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0022", "Input file "
		// + inFileName + " cannot be opened. Retry in " +
		// Integer.toString(waitTime) + " milliseconds!" +
		// " Received exception: " + e.getMessage());
		// fileRead = false;
		// }
		// }
		if (true == true) {
			try {
				// String fromParty = null;
				// String toParty = null;
				// String fromService = null;
				// String toService = null;
				// String action = null;
				// String actionNS = null;
				// if (channelAddressMode.equalsIgnoreCase("CPA"))
				// {
				// channelId = channel.getObjectId();
				//          
				//
				//
				//
				// Binding binding =
				// CPAFactory.getInstance().getLookupManager().getBindingByChannelId(channelId);
				// action = binding.getActionName();
				// actionNS = binding.getActionNamespace();
				// fromParty = binding.getFromParty();
				// fromService = binding.getFromService();
				// toParty = binding.getToParty();
				// toService = binding.getToService();
				// }
				// else
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT_AF, "Input file " + inFileName +
				// " was read.");
				//          
				// fromParty = findValue("FromParty:", msgText);
				// toParty = findValue("ToParty:", msgText);
				// fromService = findValue("FromService:", msgText);
				// toService = findValue("ToService:", msgText);
				// action = findValue("Action:", msgText);
				// actionNS = findValue("ActionNS:", msgText);
				// String areGLN = findValue("GLNMode:", msgText);
				// if ((areGLN != null) && (areGLN.compareToIgnoreCase("true")
				// == 0))
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Access the normalization manager now.");
				// NormalizationManager normalizer =
				// NormalizationManager.getInstance();
				//            
				// com.sap.aii.af.service.cpa.Service fromXIService =
				// normalizer.getXIService(fromParty, "GLN", fromService);
				// if ((fromXIService != null) && (fromXIService.getService() !=
				// null) && (fromXIService.getService().length() > 0))
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization for service: {0} is: {1}", new
				// Object[] { fromService, fromXIService.getService() });
				// fromService = fromXIService.getService();
				// }
				// else
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization is not defined for service: {0}", new
				// Object[] { fromService });
				// }
				// com.sap.aii.af.service.cpa.Party fromXIParty =
				// normalizer.getXIParty("009", "GLN", fromParty);
				// if ((fromXIParty != null) && (fromXIParty.getParty() != null)
				// && (fromXIParty.getParty().length() > 0))
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization for party: {0} is: {1}", new Object[]
				// { fromParty, fromXIParty.getParty() });
				// fromParty = fromXIParty.getParty();
				// }
				// else
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization is not defined for party: {0}", new
				// Object[] { fromParty });
				// }
				// com.sap.aii.af.service.cpa.Service toXIService =
				// normalizer.getXIService(toParty, "GLN", toService);
				// if ((toXIService != null) && (toXIService.getService() !=
				// null) && (toXIService.getService().length() > 0))
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization for service: {0} is: {1}", new
				// Object[] { toService, toXIService.getService() });
				// toService = toXIService.getService();
				// }
				// else
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization is not defined for service: {0}", new
				// Object[] { toService });
				// }
				// com.sap.aii.af.service.cpa.Party toXIParty =
				// normalizer.getXIParty("009", "GLN", toParty);
				// if ((toXIParty != null) && (toXIParty.getParty() != null) &&
				// (toXIParty.getParty().length() > 0))
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization for party: {0} is: {1}", new Object[]
				// { toParty, toXIParty.getParty() });
				// toParty = toXIParty.getParty();
				// }
				// else
				// {
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT,
				// "Address normalization is not defined for party: {0}", new
				// Object[] { toParty });
				// }
				// }
				// CPAInboundRuntimeLookupManager channelLookup =
				// CPAFactory.getInstance().createInboundRuntimeLookupManager(adapterType,
				// adapterNamespace, fromParty, toParty, fromService, toService,
				// action, actionNS);
				// channel = channelLookup.getChannel();
				// if (channel != null)
				// {
				// channelId = channel.getObjectId();
				// }
				// else
				// {
				// TRACE.errorT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0023",
				// "The channel ID cannot be determined. Reason: No agreement (binding) for the FP,TP,FS,TS,A combination available. Message will be processed later!");
				// return;
				// }
				// }
				// if ((fromParty == null) || (fromParty.equals("*"))) {
				// fromParty = new String("");
				// }
				// if ((fromService == null) || (fromService.equals("*"))) {
				// fromService = new String("");
				// }
				// if ((toParty == null) || (toParty.equals("*"))) {
				// toParty = new String("");
				// }
				// if ((toService == null) || (toService.equals("*"))) {
				// toService = new String("");
				// }
				// if ((action == null) || (action.equals("*"))) {
				// action = new String("");
				// }
				// if ((actionNS == null) || (actionNS.equals("*"))) {
				// actionNS = new String("");
				// }
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT_AF,
				// "The following address data were extracted (FP,TP,FS,TS,A): "
				// + fromParty + "," + toParty + "," + fromService + "," +
				// toService + "," + action);
				//        
				// TRACE.debugT("sendMessageFromFile(String inFileName)",
				// XIAdapterCategories.CONNECT_AF, "The channel ID is: " +
				// channelId);
				//        

				Message msg = mf.createMessageRecord("TNF", "SVNSI_D", "", "", "AA", "urn:AA");
				if (qos.equalsIgnoreCase("BE")) {
					msg.setDeliverySemantics(DeliverySemantics.BestEffort);
				} else if (qos.equalsIgnoreCase("EOIO")) {
					msg.setDeliverySemantics(DeliverySemantics.ExactlyOnceInOrder);
				} else {
					msg.setDeliverySemantics(DeliverySemantics.ExactlyOnce);
				}
				XMLPayload xp = msg.createXMLPayload();
				if (msgText.indexOf("<?xml") != -1) {
					xp.setText(msgText);
					xp.setName("MainDocument");
					xp.setDescription("XI AF Sample Adapter Input: XML document as MainDocument");
				} else {
					xp.setContent(msgText.getBytes("UTF-8"));
					xp.setContentType("application/octet-stream");
					xp.setName("MainDocument");
					xp.setDescription("XI AF Sample Adapter Input: Binary as MainDocument");
				}
				if (set_asma) {
					msg.setMessageProperty(adapterNamespace + "/" + adapterType, "JCAChannelID",
							channelId);
					TRACE.debugT("sendMessageFromFile(String inFileName)",
							XIAdapterCategories.CONNECT_AF,
							"The adapter specific message attribute (ASMA) {0} was set.",
							new Object[] { "JCAChannelID" });
				} else {
					TRACE
							.debugT(
									"sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"The adapter specific message attribute (ASMA) {0} was not set since the setting is switched off in the channel configuration.",
									new Object[] { "JCAChannelID" });
				}
				msg.setDocument(xp);

				TRACE.debugT("sendMessageFromFile(String inFileName)",
						XIAdapterCategories.CONNECT_AF, "Message object created and filled.");

				ModuleData md = new ModuleData();
				md.setPrincipalData(msg);
				if (!qos.equalsIgnoreCase("BE")) {
					TransactionTicket txTicket = null;
					try {
						TRACE.debugT("sendMessageFromFile(String inFileName)",
								XIAdapterCategories.CONNECT_AF, "Get transaction ticket now.");
						txTicket = TxManager.required();
						TRACE.debugT("sendMessageFromFile(String inFileName)",
								XIAdapterCategories.CONNECT_AF, "Got transaction ticket: {0}",
								new Object[] { txTicket.toString() });
						xiMsgId = msg.getMessageId();
						MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
						md.setSupplementalData("audit.key", amk);
						if (MessageDirection.OUTBOUND == MessageDirection.valueOf("OUTBOUND")) {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"AuditDirection typesafe enum works well!");
						} else {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"AuditDirection typesafe enum works quite bad!");
						}
						if (AuditLogStatus.ERROR == AuditLogStatus.valueOf("ERR")) {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"AuditLogStatus typesafe enum works well!");
						} else {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"AuditLogStatus typesafe enum works quite bad!");
						}
						MessageKey amk2 = msg.getMessageKey();
						if (amk2.equals(amk)) {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"MessageKey amk and amk2 are equal!");
						} else {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"MessageKey amk and amk2 are not equal!");
						}
						TRACE
								.debugT(
										"sendMessageFromFile(String inFileName)",
										XIAdapterCategories.CONNECT_AF,
										"The last audit message key being used was: amk: {0}, dir: {1}, msgid: {2}, msgkey: {3}, stat: {4}.",
										new Object[] { amk.toString(),
												amk.getDirection().toString(),
												amk.getMessageId().toString(), amk.toString(),
												AuditLogStatus.SUCCESS.toString() });

						audit
								.addAuditLogEntry(amk, AuditLogStatus.SUCCESS,
										"Asynchronous message was read from file and will be forwarded to the XI AF MS now.");
						audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS,
								"Name of the processed file: {0}.", new Object[] { inFileName });
						audit.addAuditLogEntry(amk, AuditLogStatus.WARNING,
								"Demo: This is a warning audit log message");
						audit.flushAuditLogEntries(amk);
						audit.flushAuditLogEntries(amk);

						TRACE.debugT("sendMessageFromFile(String inFileName)",
								XIAdapterCategories.CONNECT_AF,
								"Message will be forwarded to XI AF MP and channel: " + channelId);

						lookUpModuleProcessor(1).process(channelId, md);

						TRACE.debugT("sendMessageFromFile(String inFileName)",
								XIAdapterCategories.CONNECT_AF, "The message with ID "
										+ msg.getMessageId()
										+ " was forwarded to the XI AF succesfully.");
						if (0 != processMode.compareToIgnoreCase("test")) {
							messageIDMapper.createIDMap(extMsgId, xiMsgId, System
									.currentTimeMillis() + 86400000L, true);
						}
						if (0 == raiseError.compareToIgnoreCase("rollback")) {
							audit
									.addAuditLogEntry(
											amk,
											AuditLogStatus.ERROR,
											"Channel error mode is set to rollback. An Exception is thrown now to demonstrate a rollback behavior.");
							audit.flushAuditLogEntries(amk);
							TRACE
									.infoT(
											"sendMessageFromFile(String inFileName)",
											XIAdapterCategories.CONNECT_AF,
											"Channel error mode is set to rollback. An Exception is thrown now to demonstrate a rollback behavior.");
							try {
								MonitoringManager mm = MonitoringManagerFactory.getInstance()
										.getMonitoringManager();
								ProcessContextFactory.ParamSet ps = ProcessContextFactory
										.getParamSet().message(msg).channel(channel);
								ProcessContext pc = ProcessContextFactory.getInstance()
										.createProcessContext(ps);
								mm
										.reportProcessStatus(
												adapterNamespace,
												adapterType,
												ChannelDirection.SENDER,
												ProcessState.FATAL,
												"Rollback triggered (as demo) since channel error mode was set to rollback",
												pc);
							} catch (Exception e) {
								TRACE.catching("sendMessageFromFile(String inFileName)", e);
								TRACE.errorT("sendMessageFromFile(String inFileName)",
										XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0024",
										"Process state propagation failed due to: {0}",
										new Object[] { e.getMessage() });
							}
							RetryControlException e = new RetryControlException(
									"Sample rollback simulation test exception",
									RetryMode.STOP_RETRIES);
							TRACE.throwing("sendMessageFromFile(String inFileName)", e);
							throw e;
						}
						// if (0 == processMode.compareToIgnoreCase("rename")) {
						// renameFile(inFileName, inputFile);
						// }
					} catch (TxRollbackException e) {
						TRACE.catching("sendMessageFromFile(String inFileName)", e);

						TRACE
								.errorT(
										"sendMessageFromFile(String inFileName)",
										XIAdapterCategories.CONNECT_AF,
										"SOA.apt_sample.0025",
										"Rollback was performed explicitly!. Reason: {0}. Message will be processed again later.",
										new Object[] { e.getMessage() });
					} catch (TxException e) {
						TRACE.catching("sendMessageFromFile(String inFileName)", e);

						TRACE
								.errorT(
										"sendMessageFromFile(String inFileName)",
										XIAdapterCategories.CONNECT_AF,
										"SOA.apt_sample.0026",
										"Internal transaction manager exception received. Rollback is performed!. Reason: {0}. Message will be processed again later.",
										new Object[] { e.getMessage() });
					} catch (Exception e) {
						TRACE.catching("sendMessageFromFile(String inFileName)", e);

						TRACE
								.errorT(
										"sendMessageFromFile(String inFileName)",
										XIAdapterCategories.CONNECT_AF,
										"SOA.apt_sample.0027",
										"Inbound processing failed, transaction is being rollback'ed. Reason: {0}.Message will be processed again later.",
										new Object[] { e.getMessage() });
						TxManager.setRollbackOnly();
					} finally {
						if (txTicket == null) {
							TRACE.errorT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0028",
									"Got no valid transaction ticket (was null).");
						} else {
							TRACE.debugT("sendMessageFromFile(String inFileName)",
									XIAdapterCategories.CONNECT_AF,
									"Transaction level will be committed now.");
							try {
								TxManager.commitLevel(txTicket);
							} catch (Exception e) {
								TRACE
										.errorT(
												SIGNATURE,
												XIAdapterCategories.CONNECT_AF,
												"SOA.apt_sample.0029",
												"Internal transaction manager exception received. Rollback is performed!. Reason: {0}. Message will be processed again later.",
												new Object[] { e.getMessage() });
							}
							TRACE.debugT(SIGNATURE,
									XIAdapterCategories.CONNECT_AF,
									"Transaction level was committed succesfully.");
						}
					}
				} else {
					try {
						xiMsgId = msg.getMessageId();
						MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
						md.setSupplementalData("audit.key", amk);
						audit
								.addAuditLogEntry(amk, AuditLogStatus.SUCCESS,
										"Synchronous message was read from file and will be forwarded to the XI AF MS now.");

						TRACE.debugT(SIGNATURE,
								XIAdapterCategories.CONNECT_AF,
								"Message will be forwarded to XI AF MP and channel: " + channelId);

						ModuleData result = lookUpModuleProcessor(1).process(channelId, md);

						TRACE.debugT(SIGNATURE,
								XIAdapterCategories.CONNECT_AF, "The synchronous message with ID "
										+ msg.getMessageId()
										+ " was processed by the XI AF succesfully.");
						Object principal = result.getPrincipalData();
						if ((principal instanceof Message)) {
							Message response = (Message) principal;
							TRACE.infoT(SIGNATURE,
											XIAdapterCategories.CONNECT_AF,
											"Got back a response message. ID/FP/FS/TP/TS/IF/IFNS/Class: {0}/{1}/{2}/{3}/{4}/{5}/{6}/{7}",
											new Object[] { response.getMessageId(),
													response.getFromParty().toString(),
													response.getFromService().toString(),
													response.getToParty().toString(),
													response.getToService().toString(),
													response.getAction().getName(),
													response.getAction().getType(),
													response.getMessageClass().toString() });

							Payload payload = response.getDocument();
							if ((payload instanceof TextPayload)) {
								TextPayload text = (TextPayload) payload;
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
										"Payload: {0}", new Object[] { text.getText() });
							} else {
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
										"Received a binary response {0}",
										new Object[] { new String(payload.getContent()) });
							}
							Payload att = response.getAttachment("Attachment");
							if ((att != null & att instanceof TextPayload)) {
								TextPayload text = (TextPayload) att;
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
										"Payload: {0}", new Object[] { text.getText() });
							} else if (att != null) {
								TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
										"Received a binary response {0}",
										new Object[] { new String(att.getContent()) });
							}
						} else {
							TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
									"SOA.apt_sample.0030",
									"Received not a XI message as response. Class is: {0}",
									new Object[] { principal.getClass().getName() });
						}
						// if (0 == processMode.compareToIgnoreCase("rename")) {
						// renameFile(inFileName, inputFile);
						// }
					} catch (Exception e) {
						TRACE.catching(SIGNATURE, e);
						TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
								"SOA.apt_sample.0031",
								"Synchronous inbound processing failed. Received exception: "
										+ e.getMessage());
					}
				}
			} catch (Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0032",
						"Received exception: " + e.getMessage());
			}
		}
	}

	public static String getExternalMessageID(File f) {
		int keymaxlen = 127;
		String extMsgId = "JCASample::"
				+ (f == null ? "NULL" : new StringBuilder().append(f.getAbsolutePath()).append("_")
						.append(f.lastModified()).toString());
		if (extMsgId.length() > 127) {
			String digest = "....." + XISecurityUtilities.digest(extMsgId);
			extMsgId = extMsgId.substring(0, 127 - digest.length()).concat(digest);
		}
		return extMsgId;
	}

	// private void renameFile(String inFileName, File inputFile)
	// throws Exception
	// {
	// String SIGNATURE = "renameFile(String inFileName, File inputFile)";
	// try
	// {
	// File renamed = new File(inFileName + ".sent");
	// renamed.delete();
	// if (false == inputFile.renameTo(renamed)) {
	// TRACE.errorT("renameFile(String inFileName, File inputFile)",
	// XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0033", "Input file " +
	// inFileName + " cannot be renamed. It will be sent again!");
	// }
	// }
	// catch (Exception e)
	// {
	// TRACE.catching("renameFile(String inFileName, File inputFile)", e);
	// TRACE.errorT("renameFile(String inFileName, File inputFile)",
	// XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0034", "Input file " +
	// inFileName + " cannot be renamed. Received exception: " +
	// e.getMessage());
	// throw e;
	// }
	// }

	private String findValue(String key, String text) {
		String SIGNATURE = "findValue(String key, String text)";
		int startIndex = text.indexOf(key);
		if (startIndex < 0) {
			return new String("");
		}
		startIndex += key.length();

		int endIndex = text.indexOf(";", startIndex);
		if (endIndex < 0) {
			endIndex = text.lastIndexOf(text);
		}
		String value = text.substring(startIndex, endIndex);
		TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
				"findValue data (key,value,start,end): " + key + "," + value + ","
						+ Integer.toString(startIndex) + "," + Integer.toString(endIndex));
		return value;
	}

	public String getAdapterNamespace() {
		String SIGNATURE = "getAdapterNamespace()";
		TRACE.entering(SIGNATURE);
		TRACE.exiting(SIGNATURE);
		return adapterNamespace;
	}

	public String getAdapterType() {
		return adapterType;
	}

	public void setAdapterNamespace(String adapterNamespace) {
		String SIGNATURE = "setAdapterNamespace(String adapterNamespace)";
		TRACE.entering(SIGNATURE, new Object[] { adapterNamespace });
		this.adapterNamespace = adapterNamespace;
		TRACE.exiting(SIGNATURE);
	}

	public void setAdapterType(String adapterType) {
		String SIGNATURE = "setAdapterType(String adapterType)";
		TRACE.entering(SIGNATURE, new Object[] { adapterType });
		this.adapterType = adapterType;
		TRACE.exiting(SIGNATURE);
	}

	public GUID getMcfLocalGuid() {
		return mcfLocalGuid;
	}

	class XIManagedConnectionFactoryController extends TimerTask {
		private SPIManagedConnectionFactory controlledMcf;

		public XIManagedConnectionFactoryController(SPIManagedConnectionFactory mcf,
				InitialContext ctx) {
			controlledMcf = mcf;
		}

		public void run() {
			String SIGNATURE = "XIManagedConnectionFactoryController.run()";
			String controlledMcfGuid = null;
			try {
				if (controlledMcf != null) {
					controlledMcfGuid = controlledMcf.getMcfLocalGuid().toHexString();
				}
				SPIManagedConnectionFactory.TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
						"MCF with GUID {0} is running. ({1})", new Object[] {
								controlledMcfGuid.toString(),
								SPIManagedConnectionFactory.class.getClassLoader() });

			} catch (Exception e) {
				SPIManagedConnectionFactory.TRACE.catching(SIGNATURE, e);
				SPIManagedConnectionFactory.TRACE.warningT(SIGNATURE,
						XIAdapterCategories.CONNECT_AF,
						"Processing of control timer failed. Reason: " + e.getMessage());
			}
		}
	}

	public void start() {
		String SIGNATURE = "start()";
		TRACE.entering(SIGNATURE);
		String controlledMcfGuid = getMcfLocalGuid().toHexString();
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
				"MCF with GUID {0} is started now. ({1})", new Object[] {
						controlledMcfGuid.toString(),
						SPIManagedConnectionFactory.class.getClassLoader() });
		try {
			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE
					.errorT(
							SIGNATURE,
							XIAdapterCategories.CONNECT,
							"SOA.apt_sample.0035",
							"Unable to access the XI AF audit log. Reason: {0}. Adapter cannot not start the inbound processing!",
							new Object[] { e });
			TRACE.exiting(SIGNATURE);
			return;
		}
		messageIDMapper = MessageIDMapper.getInstance();
		if (messageIDMapper == null) {
			TRACE
					.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "SOA.apt_sample.0036",
							"Gut null as MessageIDMapper singleton instance. Adapter cannot not start the inbound processing!");
			TRACE.exiting(SIGNATURE);
			return;
		}
		try {
			mf = new XIMessageFactoryImpl(adapterType, adapterNamespace);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE
					.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "SOA.apt_sample.0037",
							"Unable to create XI message factory. Adapter cannot not start the inbound processing!");
			TRACE.exiting(SIGNATURE);
			return;
		}
		try {
			startMCF();
			startTimer();
			TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
					"MCF with GUID {0} was started successfully.", new Object[] { controlledMcfGuid
							.toString() });
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0038",
					"Start of MCF failed. Reason: {0}", new Object[] { e.getMessage() });
		}
		// try
		// {
		// ClassUtil.setClassLoader("com.sap.aii.af.sample.module.ConvertCRLFfromToLF0",
		// ConvertCRLFfromToLF0.class.getClassLoader());
		// }
		// catch (Exception e)
		// {
		// TRACE.catching("start()", e);
		// TRACE.errorT("start()", XIAdapterCategories.CONNECT_AF,
		// "SOA.apt_sample.0039",
		// "Unable to register pojo modules. Reason: {0}", new Object[] {
		// e.getMessage() });
		// }
		TRACE.exiting(SIGNATURE);
	}

	public void stop() {
		String SIGNATURE = "stop()";
		TRACE.entering(SIGNATURE);
		String controlledMcfGuid = getMcfLocalGuid().toHexString();
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
				"The running MCF with GUID {0} will be stopped now",
				new Object[] { controlledMcfGuid.toString() });

		// ClassUtil.removeClassLoader("com.sap.aii.af.sample.module.ConvertCRLFfromToLF0");
		try {
			stopMCF();
			stopTimer();
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}
		TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF,
				"MCF with GUID {0} was stopped successfully.", new Object[] { controlledMcfGuid
						.toString() });
		TRACE.exiting(SIGNATURE);
	}

	public boolean isRunning() {
		if (threadStatus == 1) {
			return true;
		}
		return false;
	}
}
