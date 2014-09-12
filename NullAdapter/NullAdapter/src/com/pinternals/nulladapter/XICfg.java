package com.pinternals.nulladapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.resource.ResourceException;
import com.pinternals.AFUtil;
import com.sap.aii.af.lib.trace.Trace;
import com.sap.aii.af.service.administration.api.AdapterCallback;
import com.sap.aii.af.service.administration.api.AdapterCapability;
import com.sap.aii.af.service.administration.api.AdapterRegistry;
import com.sap.aii.af.service.administration.api.AdapterRegistryFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.administration.api.cpa.ChannelLifecycleCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationNotPossibleException;
import com.sap.aii.af.service.administration.api.i18n.ResourceBundleLocalizationCallback;
import com.sap.aii.af.service.administration.api.monitoring.ChannelDirection;
import com.sap.aii.af.service.administration.api.monitoring.ChannelSelfTestCallback;
import com.sap.aii.af.service.administration.api.monitoring.ChannelState;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatus;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusCallback;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusFactory;
import com.sap.aii.af.service.administration.api.monitoring.ChannelUnknownException;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManager;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManagerFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContext;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContextFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessState;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.pmi.PMI;
import com.sap.aii.utilxi.rtcheck.base.TestSuitResult;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;

public class XICfg implements ChannelLifecycleCallback, ChannelStatusCallback,
		LocalizationCallback, ChannelSelfTestCallback {
	private static final Trace TR = new Trace(XICfg.class.getName());
	private String aType, aNS;
	private List<Channel> outboundChannels = null, inboundChannels = null;
	private CPALookupManager lookupManager = null;
	private AdapterRegistry adapterRegistry = null;
	private LocalizationCallback lclzer = null;
	private SPIManagedConnectionFactory mcf = null;

	// public XICfg() {
	// this(AdapterConstants.ADAPTER_TYPE, AdapterConstants.ADAPTER_NS);
	// String SIGNATURE = "XICfg()";
	// TR.debugT(SIGNATURE, "empty constructor was called");
	// }
	public XICfg(String adType, String adNS) {
		String SIGNATURE = "XICfg(String adType, String adNS)";
		TR.entering(SIGNATURE, new Object[] { adType, adNS });
		try {
			CPAFactory cf = CPAFactory.getInstance();
			this.lookupManager = cf.getLookupManager();
		} catch (Exception e) {
			TR.catching(SIGNATURE, e);
			TR.errorT(SIGNATURE, XIConst.lcConfig, "", "CPALookupManager cannot be instantiated due to {0}", new Object[] { e.getMessage() });
			TR.errorT(SIGNATURE, XIConst.lcConfig, "", "No channel configuration can be read, no message exchange possible!");
		}
		this.aType = adType;
		this.aNS = adNS;
		TR.exiting(SIGNATURE);
	}

	@Override
	// @ChannelLifecycleCallback
	public void channelAdded(Channel ch) {
		String SIGNATURE = "channelAdded(Channel ch)";
		TR.entering(SIGNATURE, new Object[] { ch });
		Direction d = ch.getDirection();
		synchronized (this) {
			if (d == Direction.INBOUND)
				this.inboundChannels.add(ch);
			else if (d == Direction.OUTBOUND) this.outboundChannels.add(ch);
		}
		TR.infoT(SIGNATURE, XIConst.lcAF, AFUtil.formatCcLong(ch, " was added"));
		TR.exiting(SIGNATURE);
	}

	@Override
	// @ChannelLifecycleCallback
	public void channelUpdated(Channel channel) {
		String SIGNATURE = "channelUpdated(Channel channel)";
		TR.entering(SIGNATURE);
		channelRemoved(channel);
		channelAdded(channel);
		TR.exiting(SIGNATURE);
	}

	@Override
	// @ChannelLifecycleCallback
	public void channelRemoved(Channel ch) {
		String SIGNATURE = "channelRemoved(Channel ch)";
		TR.entering(SIGNATURE, new Object[] { ch });
		List<Channel> channels = null;
		TR.infoT(SIGNATURE, XIConst.lcAF, AFUtil.formatCcLong(ch, " was removed"));
		String channelID = ch.getObjectId();
		Direction d = ch.getDirection();
		if (d == Direction.INBOUND) {
			channels = this.inboundChannels;
		} else {
			channels = this.outboundChannels;
		}
		synchronized (this) {
			for (Channel x : channels)
				if (x.getObjectId().equals(ch.getObjectId())) {
					channels.remove(x);
					if (d == Direction.INBOUND) break;
					try {
						this.mcf.destroyManagedConnection(channelID);
					} catch (Exception e) {
						TR.catching(SIGNATURE, e);
						TR.warningT(SIGNATURE, XIConst.lcAF, "The ManagedConnection for channel {0} cannot be destroyed. Configuration update might not work.", new Object[] { channelID });
					}
				}
		}
		TR.exiting(SIGNATURE);
	}

	public void init(SPIManagedConnectionFactory mcf) throws ResourceException {
		String SIGNATURE = "init(mcf)";
		TR.entering(SIGNATURE, mcf.nickGuid);
		String dir = null;
		String name = null;
		this.mcf = mcf;
		try {
			this.lclzer = XILocalizer.getLocalizationCallback();
			AdapterRegistryFactory arf = AdapterRegistryFactory.getInstance();
			this.adapterRegistry = arf.getAdapterRegistry();
			this.adapterRegistry.registerAdapter(this.aNS, this.aType, new AdapterCapability[] { AdapterCapability.PUSH_PROCESS_STATUS }, new AdapterCallback[] { this });
		} catch (Exception e) {
			TR.catching(SIGNATURE, e);
			ResourceException re = new ResourceException("XI AAM registration failed due to: "
					+ e.getMessage());
			TR.throwing(SIGNATURE, re);
			throw re;
		}
		synchronized (this) {
			this.inboundChannels = new LinkedList<Channel>();
			this.outboundChannels = new LinkedList<Channel>();
			try {
				List<Channel> allChannels = this.lookupManager.getChannelsByAdapterType(this.aType, this.aNS);
				TR.debugT(SIGNATURE, XIConst.lcAF, "The XI AAM service returned {0} channels "
						+ "for adapter type {1} with namespace {2}", new Object[] {
						new Integer(allChannels.size()), this.aType, this.aNS });
				for (int i = 0; i < allChannels.size(); i++) {
					Channel channel = (Channel) allChannels.get(i);
					if (channel.getDirection() == Direction.INBOUND) {
						this.inboundChannels.add(channel);
						// dir = channel.getValueAsString("fileInDir");
						// name = channel.getValueAsString("fileInName");
					} else {
						if (channel.getDirection() != Direction.OUTBOUND) {
							continue;
						}
						this.outboundChannels.add(channel);
						// dir = channel.getValueAsString("fileOutDir");
						// name = channel.getValueAsString("fileOutPrefix");
					}
					TR.infoT(SIGNATURE, XIConst.lcAF, "Channel with ID {0} for party {1} and service {2"
							+ "} added (direction is {3}, directory: {4}, name: {5}).", new Object[] {
							channel.getObjectId(), channel.getParty(),
							channel.getService(),
							channel.getDirection().toString(), dir, name });
				}
			} catch (Exception e) {
				TR.catching(SIGNATURE, e);
				ResourceException re = new ResourceException("XI CPA lookup failed due to: "
						+ e.getMessage());
				TR.throwing(SIGNATURE, re);
				throw re;
			}
		}
		TR.exiting(SIGNATURE, mcf.nickGuid);
	}

	public void stop() throws ResourceException {
		String SIGNATURE = "stop()";
		TR.entering(SIGNATURE, mcf.nickGuid);
		try {
			// try
			// {
			// Set<String> parties =
			// this.partyChangeCallBackHandler.getRegisteredParties();
			// Iterator<String> partyIterator = parties.iterator();
			// while (partyIterator.hasNext())
			// {
			// String partyName = (String)partyIterator.next();
			// PartyCallBackController.getInstance().unregisterForPartyEvent(partyName,
			// this.partyChangeCallBackHandler);
			// }
			// this.partyChangeCallBackHandler.clear();
			// }
			// catch (CPAException e)
			// {
			// TRACE.catching("stop()", e);
			// }
			this.adapterRegistry.unregisterAdapter(this.aNS, this.aType);
		} catch (Exception e) {
			TR.catching(SIGNATURE, e);
			ResourceException re = new ResourceException("XI AAM unregistration failed due to: "
					+ e.getMessage());
			TR.throwing(SIGNATURE, re);
			throw re;
		}
		TR.exiting(SIGNATURE, mcf.nickGuid);
	}

	@SuppressWarnings("unchecked")
	public List<Channel> getCopy(Direction direction) throws ResourceException {
		String SIGNATURE = "getCopy(Direction direction)";
		List<Channel> out = null;
		if ((this.inboundChannels == null) || (this.outboundChannels == null)) {
			init(this.mcf);
		}
		synchronized (this) {
			if (direction == Direction.INBOUND) {
				out = (List<Channel>) ((LinkedList<Channel>) this.inboundChannels).clone();
			} else if (direction == Direction.OUTBOUND) {
				out = (List<Channel>) ((LinkedList<Channel>) this.outboundChannels).clone();
			} else {
				ResourceException re = new ResourceException("Direction invalid");
				TR.throwing(SIGNATURE, re);
				throw re;
			}
		}
		return out;
	}

	public ChannelStatus getChannelStatus(Channel channel, Locale locale)
			throws ChannelUnknownException {
		String SIGNATURE = "getChannelStatus(Channel channel, Locale locale)";
		TR.entering(SIGNATURE, new Object[] { channel, locale });
		boolean channelFound = false;
		Channel storedChannel = null;
		String channelID = "<unknown>";
		Exception cause = null;
		ChannelStatus cs = null;
		try {
			channelID = channel.getObjectId();
			List<Channel> channels = null;
			if (channel.getDirection() == Direction.INBOUND) {
				channels = this.inboundChannels;
			} else {
				channels = this.outboundChannels;
			}
			synchronized (this) {
				for (int i = 0; i < channels.size(); i++) {
					storedChannel = (Channel) channels.get(i);
					if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
						channelFound = true;
						break;
					}
				}
			}
		} catch (Exception e) {
			TR.catching(SIGNATURE, e);
			cause = e;
			TR.errorT(SIGNATURE, XIConst.lcConfig, "", "Channel lookup failed due to {0}.", new Object[] { e.getMessage() });
		}
		if (!channelFound) {
			ChannelUnknownException cue = new ChannelUnknownException("Channel with ID "
					+ channelID + " is not known.", cause);
			TR.errorT(SIGNATURE, XIConst.lcConfig, "", "Channel {0} is not known.", new Object[] { channelID });
			TR.throwing(SIGNATURE, cue);
			throw cue;
		}
		ChannelStatusFactory csf = ChannelStatusFactory.getInstance();
		if (csf == null) {
			ChannelUnknownException cue = new ChannelUnknownException("Internal error: Unable to get instance of ChannelStatusFactory.", cause);
			TR.errorT(SIGNATURE, XIConst.lcConfig, "", "Unable to get instance of ChannelStatusFactory.");
			TR.throwing(SIGNATURE, cue);
			throw cue;
		}
		try {
			if (storedChannel.getDirection() == Direction.INBOUND) {
				// String directory = channel.getValueAsString("fileInDir");
				// if ((directory == null) || (directory.length() == 0))
				// {
				// TRACE.warningT("getChannelStatus(Channel channel, Locale locale)",
				// XIAdapterCategories.CONFIG,
				// "Unable to determine input file directory. Take default: c:/temp");
				// directory = "c:/temp";
				// }
				// String name = channel.getValueAsString("fileInName");
				// if ((name == null) || (name.length() == 0))
				// {
				// TRACE.warningT("getChannelStatus(Channel channel, Locale locale)",
				// XIAdapterCategories.CONFIG,
				// "Unable to determine input file prefix. Take default: sample_ra_input");
				// name = "sample_ra_input";
				// }
				// File dir = new File(directory);
				// if (!dir.exists())
				// {
				// cs = csf.createChannelStatus(channel, ChannelState.ERROR,
				// "Input file directory " + directory + " does not exists.");
				// TRACE.exiting("getChannelStatus(Channel channel, Locale locale)",
				// new Object[] { cs });
				// return cs;
				// }
				// if (!this.mcf.isRunning())
				// {
				// cs = csf.createChannelStatus(channel, ChannelState.ERROR,
				// "The JCA adapter inbound thread is not working correctly. No inbound messages possible!");
				// TRACE.exiting("getChannelStatus(Channel channel, Locale locale)",
				// new Object[] { cs });
				// return cs;
				// }
			} else {
				// String directory = channel.getValueAsString("fileOutDir");
				// if ((directory == null) || (directory.length() == 0))
				// {
				// cs = csf.createChannelStatus(channel, ChannelState.ERROR,
				// "Output file directory name is not set.");
				// TRACE.exiting("getChannelStatus(Channel channel, Locale locale)",
				// new Object[] { cs });
				// return cs;
				// }
				// File dir = new File(directory);
				// if (!dir.exists())
				// {
				// cs = csf.createChannelStatus(channel, ChannelState.ERROR,
				// "Output file directory " + directory + " does not exists.");
				// TRACE.exiting("getChannelStatus(Channel channel, Locale locale)",
				// new Object[] { cs });
				// return cs;
				// }
			}
			cs = csf.createChannelStatus(channel, ChannelState.OK, this.lclzer.localizeString("CHANNEL_OK", locale));
			// cs = csf.createChannelStatus(channel, ChannelState.OK,
			// "CHANNEL_OK!");
		} catch (Exception e) {
			TR.catching(SIGNATURE, e);
			TR.errorT(SIGNATURE, XIConst.lcAF, "", "Cannot retrieve status for channel {0}. Received exception: {1}", new Object[] {
					channel.getChannelName(), e.getMessage() });
			cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Cannot retrieve status for this channel due to: "
					+ e.getMessage());
			TR.exiting(SIGNATURE, new Object[] { cs });
			return cs;
		}
		TR.exiting(SIGNATURE, new Object[] { cs });
		return cs;
	}

	public String localizeString(String str, Locale locale)
			throws LocalizationNotPossibleException {
		return this.lclzer.localizeString(str, locale);
	}

	@Override
	public TestSuitResult testChannel(Channel var1, Locale var2) {
		// TODO Auto-generated method stub
		// StatusReporter reporter;
		MessageKey pmiid = new MessageKey("", MessageDirection.INBOUND);
		// Object[] params = new Object[9];
		// reporter.report(0, params);
		MonitoringManager mon = MonitoringManagerFactory.getInstance().getMonitoringManager();
		mon.reportChannelStatus(aNS, aType, var1, ChannelState.ERROR, "var2");
		ProcessContextFactory.ParamSet ps = ProcessContextFactory.getParamSet().channel(var1);// message(msg).channel(var1);
		ProcessContext pc = ProcessContextFactory.getInstance().createProcessContext(ps);
		mon.reportProcessStatus(aNS, aType, ChannelDirection.SENDER, ProcessState.ERROR, "zzz!", pc);
		mon.reportProcessStatus(aNS, aType, ChannelDirection.RECEIVER, ProcessState.FATAL, "xxx!", pc);
		PMI.invokeAFStatusAgent(pmiid, "0", null, null, "ping");
		//		
		// addStatusReportEntry(statusreporter, 0, pmiid, false, xiinfo,
		// "ZZZ!!!");
		return null;
	}
	// public boolean channelActiveTest(TestSuitResult suite) {
	// boolean bActive = false;
	// ClusterChannelRuntimeStatus channelStatus = null;
	//
	// try {
	// channelStatus =
	// MonitoringAdapterAdminManagerFactory.getInstance().getMonitoringAdapterAdminManager().getClusterChannelRuntimeStatus(this.channel,
	// Locale.getDefault());
	// boolean e =
	// !channelStatus.getChannelState().equals(ChannelState.INACTIVE);
	// if(e && this.isConfigActive) {
	// this.addToSuiteResult(suite, 2, "CHANN_ACTIVE_INACTIVE_TEST",
	// "CHANN_ACTIVE_INACTIVE_TEST_SUCC");
	// bActive = true;
	// } else {
	// this.addToSuiteResult(suite, 1, "CHANN_ACTIVE_INACTIVE_TEST",
	// "CHANN_ACTIVE_INACTIVE_TEST_FAIL");
	// }
	// } catch (Exception var5) {
	// this.addToSuiteResult(suite, 0, "CHANN_ACTIVE_INACTIVE_TEST",
	// "CHANN_ACTIVE_INACTIVE_TEST_ERROR", new Object[]{var5.toString()});
	// }
	// return bActive;
	// }
}

class XILocalizer {
	static ResourceBundleLocalizationCallback RBC = new ResourceBundleLocalizationCallback(XIConst.rbName, XILocalizer.class.getClassLoader());

	public static LocalizationCallback getLocalizationCallback() {
		return RBC;
	}
}