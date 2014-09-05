package com.pinternals.nulladapter;

//import com.sap.aii.adapter.axis.ra.AbstractAdapterManager;
//import com.sap.aii.adapter.axis.ra.Adapter;
//import com.sap.aii.adapter.axis.ra.ModuleProcessorEngine;
//import com.sap.aii.adapter.axis.ra.transport.message.MSEventHandler;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import javax.naming.InitialContext;

import com.sap.aii.af.lib.trace.Trace;
import com.sap.aii.af.service.administration.api.AdapterCallback;
import com.sap.aii.af.service.administration.api.AdapterCapability;
import com.sap.aii.af.service.administration.api.AdapterRegistry;
import com.sap.aii.af.service.administration.api.AdapterRegistryFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.administration.api.cpa.ChannelLifecycleCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.ResourceBundleLocalizationCallback;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.resource.SAPAdapterResources;

public class AFUtil {
	private static LocalizationCallback localizer = null;
	public static String NS = "urn:pinternals-adapters", T = "NullAdapter";
	static AdapterRegistry ar = AdapterRegistryFactory.getInstance().getAdapterRegistry();
	public static void registerAdapter(ChannelLifecycleCallback cb) {
		AdapterCapability[] cp = {AdapterCapability.PUSH_CHANNEL_STATUS, AdapterCapability.PUSH_PROCESS_STATUS};
		ar.registerAdapter(NS, T, cp, new AdapterCallback[] {cb});
		localizer = XILocalizationUtilities.getLocalizationCallback();
	}
	public static void unregisterAdapter(ChannelLifecycleCallback cb) {
		AdapterCapability[] cp = {AdapterCapability.PUSH_CHANNEL_STATUS, AdapterCapability.PUSH_PROCESS_STATUS};
		ar.unregisterAdapter(NS, T);
	}
}

class AdapterManager implements ChannelLifecycleCallback {
	private static AdapterManager manager = null;
	private Hashtable<Object,Object> adapterList = null;
	private SAPAdapterResources resources = null;

	// // private ModuleProcessorEngine engine;
	// private ThreadDispatcher dispatcher;
	// private UserTransaction userTransaction;
	// private SPIAccess msadmin;
	// private Map idmap;
	// private static Method managedPropertiesSetProperty;
	// private static final String JNDI_NAME_ADAPTER_RESOURCES =
	// "SAPAdapterResources";
	// private static final String JNDI_NAME_AFLC_CALLBACK_AXIS =
	// "AFLCCallbackAxis";
	// private static final long ID_MAP_RETENTION_PERIOD = 2592000000L;
	// private static final String JNDI_NAME_USER_TRANSACTION =
	// "UserTransaction";
	// public static final String VERSION_ID =
	// "$Id: //tc/xpi.adapters/NW731EXT_10_REL/src/_axis_lib_module/rar/src/com/sap/aii/adapter/axis/ra/AdapterManager.java#1 $";
	 private static final Trace TRACE = new Trace(AdapterManager.class.getName());
	// private static final Category CATEGORY = Adapter.CATEGORY;

	public AdapterManager() {
		this.adapterList = new Hashtable<Object,Object>();
		// String SIGNATURE = "AdapterManager(ApplicationServiceContext)";
		// TRACE.entering("AdapterManager(ApplicationServiceContext)", new Object[0]);
		// TRACE.exiting("AdapterManager(ApplicationServiceContext)");
	}

	public static synchronized AdapterManager getInstance() {
		if(manager == null) 
			manager = new AdapterManager();
		return manager;
	}

	private void setup() {
	}

	synchronized void start() {
		String SIGNATURE = "start()";

		try {
			TRACE.entering(SIGNATURE);
			AFUtil.registerAdapter(this);
			TRACE.infoT(SIGNATURE, "Callback registered for adapter type {0}/{1}", new Object[] {
					"http://sap.com/xi/XI/System", "Axis" });
			InitialContext t = new InitialContext();
			this.resources = (SAPAdapterResources) t.lookup("SAPAdapterResources");
//			this.userTransaction = (UserTransaction) t.lookup("UserTransaction");

//			try {
//				t.rebind("AFLCCallbackAxis", this);
//			} catch (NamingException var11) {
//				TRACE.getLocation().traceThrowableT(500, "start()", "Unable to rebind the adapter manager to {0}", new Object[] { "AFLCCallbackAxis" }, var11);
//			}

			CPALookupManager cpa = CPAFactory.getInstance().getLookupManager();
//			this.msadmin = (SPIAccess) t.lookup("com.sap.engine.interfaces.messaging.spi");
//			this.msadmin.registerEventHandler("XIAxis", new MSEventHandler());
//			this.idmap = new AdapterManager.AFMap(2592000000L);
//			this.registerManagedProperties();
			LinkedList adapterLinkedList = cpa.getChannelsByAdapterType(AFUtil.T, AFUtil.NS);
//			TRACE.infoT("start()", CATEGORY, "CPA content: {0} adapter {1} {2} channel found", new Object[] {
//					new Integer(adapterLinkedList.size()), "http://sap.com/xi/XI/System", "Axis" });
			Iterator it = adapterLinkedList.iterator();

			while (it.hasNext()) {
				Channel channelObject = (Channel) it.next();
				this.addAdapter(channelObject);
			}

//			this.addNullAdapter();
		} catch (Exception var12) {
			TRACE.getLocation().traceThrowableT(500, SIGNATURE, "Startup error occurred.", var12);
		} finally {
			TRACE.exiting(SIGNATURE);
		}
	}
	synchronized void stop() {
		String SIGNATURE = "stop()";
		TRACE.entering(SIGNATURE);
		AFUtil.unregisterAdapter(this);
		TRACE.exiting(SIGNATURE);
	}

	private void addAdapter(final Channel channelObject) {
		String SIGNATURE = "addAdapter(Channel)";
		TRACE.entering(SIGNATURE);

		try {
			if (TRACE.beLogged(100)) {
				TRACE.debugT(SIGNATURE, "CPAObject: {0}", new Object[] { channelObject.getStringRepresentation() });
			}

			String chid = channelObject.getObjectId();
			String chname = channelObject.getChannelName();
			if (this.adapterList.get(chid) != null) {
				TRACE.infoT(SIGNATURE, "WARNING! adapter channel {0} {1} already exists! - remove it", new Object[] {
						chid, chname });
				this.stopAdapter(chid, true);
				TRACE.infoT(SIGNATURE, "WARNING! stopped old adapter channel {0} {1}", new Object[] {
						chid, chname });
			}

			TRACE.infoT(SIGNATURE, "Add adapter channel: {0} {1}", new Object[] { chid, chname });
//			final Adapter adapter = new Adapter();
//			this.adapterList.put(chid, adapter);
//			ThreadSystem ts = RuntimeManager.getInstance().getApplicationServiceContext().getCoreContext().getThreadSystem();
//			TRACE.infoT(SIGNATURE, "Initialize and start the channel {0} in new application thread through the ThreadSystem {1}", new Object[] {
//					channelObject, ts });
//			ts.startCleanThread(new Runnable() {
//				public void run() {
//					String SIG = "addAdapter(Channel).run()";
//					AdapterManager.TRACE.entering("addAdapter(Channel).run()");
//
//					try {
//						AdapterManager.TRACE.infoT("addAdapter(Channel).run()", "Initialize channel {0}/{1}", new Object[] {
//								channelObject.getAdapterType(), channelObject.getChannelName() });
//						adapter.init(channelObject, AdapterManager.this);
//						AdapterManager.TRACE.infoT("addAdapter(Channel).run()", "Starting channel {0}/{1}", new Object[] {
//								channelObject.getAdapterType(), channelObject.getChannelName() });
//						adapter.start();
//					} finally {
//						AdapterManager.TRACE.exiting("addAdapter(Channel).run()");
//					}
//
//				}
//			}, false, true);
			TRACE.infoT(SIGNATURE, "Channel {0} start-up forked.", new Object[] { channelObject });
		} finally {
			TRACE.exiting(SIGNATURE);
		}

	}

	private void stopAdapter(String channel, boolean completed) {
		String SIGNATURE = "stopAdapter(String,boolean)";
		TRACE.entering(SIGNATURE);
		TRACE.infoT(SIGNATURE, "Stopping adapter {0} with completion {1}", new Object[] {
				channel, completed ? Boolean.TRUE : Boolean.FALSE });
//		Adapter adapter = (Adapter) this.adapterList.get(channel);
//		if (adapter != null) {
//			adapter.stop(completed);
//			adapter.cleanup();
//		}

		TRACE.exiting(SIGNATURE);
	}
//
//	private void addNullAdapter() {
//		String SIGNATURE = "addNullAdapter()";
//		TRACE.entering("addNullAdapter()");
//		if (this.adapterList.get("00000000000000000000000000000000") != null) {
//			TRACE.infoT("addNullAdapter()", CATEGORY, "WARNING! adapter null channel already exists! - remove it");
//			this.stopAdapter("00000000000000000000000000000000", true);
//			TRACE.infoT("addNullAdapter()", CATEGORY, "WARNING! stopped old null adapter channel");
//		}
//
//		TRACE.infoT("addNullAdapter()", CATEGORY, "Add null adapter channel");
//		Adapter adapter = new Adapter();
//		this.adapterList.put("00000000000000000000000000000000", adapter);
//		adapter.init(Adapter.CHAMELEON_CHANNEL, this);
//		adapter.start();
//		TRACE.exiting("addNullAdapter()");
//	}
//
//	protected synchronized void stop() {
//		String SIGNATURE = "stop()";
//
//		try {
//			TRACE.entering("stop()");
//			Enumeration t = this.adapterList.keys();
//
//			while (t.hasMoreElements()) {
//				String chid = (String) t.nextElement();
//				this.stopAdapter(chid, false);
//			}
//
//			this.adapterList.clear();
//			this.unregisterManagedProperties();
//
//			try {
//				InitialContext t1 = new InitialContext();
//				t1.unbind("AFLCCallbackAxis");
//			} catch (NamingException var4) {
//				TRACE.getLocation().traceThrowableT(500, "stop()", "Error unbinding the entry {0}", new Object[] { "AFLCCallbackAxis" }, var4);
//			}
//
//			if (this.msadmin != null) {
//				this.msadmin.unregisterEventHandler("XIAxis");
//			}
//
//			AdapterRegistryFactory.getInstance().getAdapterRegistry().unregisterAdapter("http://sap.com/xi/XI/System", "Axis");
//			TRACE.exiting("stop()");
//		} catch (Exception var5) {
//			TRACE.getLocation().traceThrowableT(500, "stop()", "Stop error occurred.", var5);
//		}
//
//	}
//
//	public Adapter getAdapter(String channel) {
//		String SIGNATURE = "getAdapter(String)";
//		TRACE.entering("getAdapter(String)");
//		TRACE.infoT("getAdapter(String)", CATEGORY, "get Adapter {0}", new Object[] { channel });
//		return (Adapter) this.adapterList.get(channel);
//	}
//
	public synchronized void channelAdded(Channel channel) {
		String SIGNATURE = "channelAdded(Channel)";

		try {
			TRACE.entering("channelAdded(Channel)");
			String t = channel.getObjectId();
			TRACE.infoT("channelAdded(Channel)", "add adapter {0}", new Object[] { t });
			this.addAdapter(channel);
			TRACE.exiting("channelAdded(Channel)");
		} catch (Exception var4) {
			TRACE.getLocation().traceThrowableT(500, "channelAdded(Channel)", "Error occurred while adding channel {0}", new Object[] { channel }, var4);
		}

	}
//
	public synchronized void channelRemoved(Channel channel) {
		String SIGNATURE = "channelRemoved(Channel)";

		try {
			TRACE.entering("channelRemoved(Channel)");
//			String t = channel.getObjectId();
//			this.removeAdapter(t);
			TRACE.exiting("channelRemoved(Channel)");
		} catch (Exception var4) {
			TRACE.getLocation().traceThrowableT(500, "channelRemoved(Channel)", "Error occurred while removing channel {0}", new Object[] { channel }, var4);
		}

	}

	public synchronized void channelUpdated(Channel channel) {
		String SIGNATURE = "channelUpdated(Channel)";

		try {
			TRACE.entering("channelUpdated(Channel)");
			String t = channel.getObjectId();
//			this.removeAdapter(t);
//			this.addAdapter(channel);
			TRACE.exiting("channelUpdated(Channel)");
		} catch (Exception var4) {
			TRACE.getLocation().traceThrowableT(500, "channelUpdated(Channel)", "Error occurred while updating channel {0}", new Object[] { channel }, var4);
		}

	}
//
//	private Adapter createAdapter(String channelId) {
//		String SIGNATURE = "createAdapter(String)";
//
//		try {
//			TRACE.entering("createAdapter(String)");
//			CPALookupManager t = CPAFactory.getInstance().getLookupManager();
//			Channel channelObject = (Channel) t.getCPAObject(CPAObjectType.CHANNEL, channelId);
//			TRACE.infoT("createAdapter(String)", CATEGORY, "add adapter {0}", new Object[] { channelId });
//			this.addAdapter(channelObject);
//			TRACE.exiting("createAdapter(String)");
//			return this.getAdapter(channelId);
//		} catch (Exception var5) {
//			TRACE.catching("createAdapter(String)", var5);
//			return null;
//		}
//	}
//
//	private void removeAdapter(String channelId) {
//		String SIGNATURE = "removeAdapter(Channel)";
//
//		try {
//			TRACE.entering("removeAdapter(Channel)", new Object[] { channelId });
//			this.stopAdapter(channelId, true);
//			this.adapterList.remove(channelId);
//		} catch (Exception var4) {
//			TRACE.getLocation().traceThrowableT(500, "removeAdapter(Channel)", "Error occurred while removing adapter for channel ID {0}", new Object[] { channelId }, var4);
//		}
//
//	}
//
//	public synchronized ModuleProcessorEngine getAxisEngine() {
//		if (this.engine == null) {
//			Thread thread = Thread.currentThread();
//			ClassLoader cl = thread.getContextClassLoader();
//
//			try {
//				thread.setContextClassLoader(Adapter.class.getClassLoader());
//				this.engine = new ModuleProcessorEngine();
//			} finally {
//				thread.setContextClassLoader(cl);
//			}
//		}
//
//		return this.engine;
//	}
//
//	public synchronized ThreadDispatcher getThreadDispatcher() {
//		if (this.dispatcher == null) {
//			this.dispatcher = new AdapterManager.AdapterThreadDispatcher(this.resources);
//		}
//
//		return this.dispatcher;
//	}
//
//	UserTransaction getUserTransaction() {
//		return this.userTransaction;
//	}
//
//	Map getIDMap() {
//		return this.idmap;
//	}
//
//	private void registerManagedProperties() {
//		String SIGNATURE = "registerManagedProperties()";
//		Thread th = Thread.currentThread();
//		ClassLoader cl = th.getContextClassLoader();
//
//		try {
//			th.setContextClassLoader(Adapter.class.getClassLoader());
//			managedPropertiesSetProperty.invoke((Object) null, new Object[] {
//					"org.apache.axis.components.net.SecureSocketFactory",
//					"com.sap.aii.axis.transport.net.IAIKSocketFactory" });
//			managedPropertiesSetProperty.invoke((Object) null, new Object[] {
//					"org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory",
//					"com.sap.aii.axis.transport.net.IAIKCommonsSocketFactory" });
//		} catch (Exception var8) {
//			TRACE.getLocation().traceThrowableT(500, "registerManagedProperties()", "Error registering the managed properties", var8);
//		} finally {
//			th.setContextClassLoader(cl);
//		}
//
//	}
//
//	private void unregisterManagedProperties() {
//		String SIGNATURE = "registerManagedProperties()";
//		Thread th = Thread.currentThread();
//		ClassLoader cl = th.getContextClassLoader();
//
//		try {
//			th.setContextClassLoader(Adapter.class.getClassLoader());
//			managedPropertiesSetProperty.invoke((Object) null, new Object[] {
//					"org.apache.axis.components.net.SecureSocketFactory", null });
//			managedPropertiesSetProperty.invoke((Object) null, new Object[] {
//					"org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory", null });
//		} catch (Exception var8) {
//			TRACE.getLocation().traceThrowableT(500, "registerManagedProperties()", "Failed to unregister the managed properties", var8);
//		} finally {
//			th.setContextClassLoader(cl);
//		}
//
//	}
//
//	static {
//		try {
//			Class e = Class.forName("org.apache.commons.discovery.tools.ManagedProperties");
//			managedPropertiesSetProperty = e.getMethod("setProperty", new Class[] { String.class, String.class });
//		} catch (Throwable var1) {
//			TRACE.getLocation().traceThrowableT(500, "<clinit>", "unable to get ManagedProperties", var1);
//		}
//
//	}
//
//	private static class AdapterThreadDispatcher implements ThreadDispatcher {
//
//		private SAPAdapterResources resources;
//
//		public AdapterThreadDispatcher(SAPAdapterResources resources) {
//			this.resources = resources;
//		}
//
//		public void start(Runnable runnable) {
//			this.resources.startRunnable(runnable);
//		}
//
//		public void start(Runnable runnable, String taskName, String threadName) {
//			this.resources.startRunnable(runnable);
//		}
//	}
//
//	private static class AFMap implements Map {
//
//		private MessageIDMapper mapper = MessageIDMapper.getInstance();
//		private long period;
//
//		public AFMap(long period) {
//			this.period = period;
//		}
//
//		public void clear() {
//			Set keys = this.entrySet();
//			Iterator it = keys.iterator();
//
//			while (it.hasNext()) {
//				String key = (String) it.next();
//				this.mapper.remove(key);
//			}
//
//		}
//
//		public boolean containsKey(Object key) {
//			return this.get(key) != null;
//		}
//
//		public boolean containsValue(Object value) {
//			LinkedList entries = this.mapper.readIDMapEntries();
//			Iterator it = entries.iterator();
//
//			String[] entry;
//			do {
//				if (!it.hasNext()) {
//					return false;
//				}
//
//				entry = (String[]) ((String[]) it.next());
//			} while (!value.equals(entry[1]));
//
//			return true;
//		}
//
//		public Set entrySet() {
//			LinkedList entries = this.mapper.readIDMapEntries();
//			HashSet entriez = new HashSet();
//			Iterator it = entries.iterator();
//
//			while (it.hasNext()) {
//				String[] entry = (String[]) ((String[]) it.next());
//				entriez.add(new AdapterManager.AFMap.AFMapEntry(entry[0], entry[1]));
//			}
//
//			return entriez;
//		}
//
//		public Object get(Object key) {
//			return this.mapper.getMappedId(key.toString());
//		}
//
//		public boolean isEmpty() {
//			return this.size() == 0;
//		}
//
//		public Set keySet() {
//			LinkedList entries = this.mapper.readIDMapEntries();
//			HashSet keys = new HashSet();
//			Iterator it = entries.iterator();
//
//			while (it.hasNext()) {
//				String[] entry = (String[]) ((String[]) it.next());
//				keys.add(entry[0]);
//			}
//
//			return keys;
//		}
//
//		public Object put(Object key, Object value) {
//			try {
//				this.mapper.createIDMap(key.toString(), value.toString(), this.period + System.currentTimeMillis());
//				return value;
//			} catch (UtilException var4) {
//				throw new IllegalArgumentException(var4.toString());
//			}
//		}
//
//		public void putAll(Map t) {
//			try {
//				Iterator e = t.keySet().iterator();
//
//				while (e.hasNext()) {
//					String key = (String) e.next();
//					String value = (String) t.get(key);
//					if (value == null) {
//						this.mapper.remove(key);
//					} else {
//						this.mapper.createIDMap(key, value, this.period + System.currentTimeMillis());
//					}
//				}
//
//			} catch (UtilException var5) {
//				throw new IllegalArgumentException(var5.toString());
//			}
//		}
//
//		public Object remove(Object key) {
//			String value = this.mapper.getMappedId(key.toString());
//			if (value != null) {
//				this.mapper.remove(key.toString());
//			}
//
//			return value;
//		}
//
//		public int size() {
//			LinkedList entries = this.mapper.readIDMapEntries();
//			return entries.size();
//		}
//
//		public Collection values() {
//			LinkedList entries = this.mapper.readIDMapEntries();
//			LinkedList values = new LinkedList();
//			Iterator it = entries.iterator();
//
//			while (it.hasNext()) {
//				String[] entry = (String[]) ((String[]) it.next());
//				values.add(entry[1]);
//			}
//
//			return values;
//		}
//
//		private class AFMapEntry implements Entry {
//
//			private String key;
//			private String value;
//
//			public AFMapEntry(String key, String value) {
//				this.key = key;
//				this.value = value;
//			}
//
//			public Object getKey() {
//				return this.key;
//			}
//
//			public Object getValue() {
//				return this.value;
//			}
//
//			public Object setValue(Object value) {
//				String val = AFMap.this.mapper.getMappedId(this.key.toString());
//				if (val != null && !val.equals(value)) {
//					try {
//						AFMap.this.mapper.createIDMap(this.key, value.toString(), AFMap.this.period
//								+ System.currentTimeMillis());
//					} catch (UtilException var4) {
//						throw new IllegalArgumentException(var4.toString());
//					}
//				}
//
//				return val;
//			}
//		}
//	}
}

class XILocalizationUtilities {

   public static LocalizationCallback getLocalizationCallback() {
      return new ResourceBundleLocalizationCallback(XILocalizationUtilities.class.getPackage().getName() + ".rb_pimon", XILocalizationUtilities.class.getClassLoader());
   }
}
