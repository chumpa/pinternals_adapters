package com.pinternals;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.pinternals.nulladapter.XIConst;
import com.sap.aii.af.lib.trace.Trace;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.resource.SAPAdapterResources;
import com.sap.aii.af.service.util.transaction.api.TxManager;
import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;
import com.sap.guid.GUID;

public class AFUtil {
//	public InitialContext ctx = null;
//	public SAPAdapterResources msRes = null;
//	public GUID mcfLocalGuid = null;
//	private TxManager txMgr = null;

//	public ApplicationPropertiesAccess appCfgProps = null;             
//	java.util.Properties appProps = null;
//	private static final Trace TRACE = new Trace(AFUtil.class.getName());

//	public AFUtil() throws NamingException {
//		String SIGNATURE = "AFUtil()";
//        TRACE.exiting(SIGNATURE);
//	}
//
//	
//	public void getLocalGuid(Trace trace, String signature) {
////		mcfLocalGuid = new GUID();
////		TODO: try {
////		} catch (Exception e) {
////			TRACE.catching(SIGNATURE, e);
////			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, 
////					"Creation of MCF GUID failed. Thus no periodic status report possible! Reason: "
////					+ e.getMessage());
//
//	}
	
	// messages and error codes
	
	// formatting channel name
	static String labelLong = "%s channel %s|%s|%s with id=%s%s";
	public static String formatCcLong(Channel ch, String extra) {
		String s = String.format(labelLong, ch.getDirection(), 
				ch.getParty(), ch.getService(), ch.getChannelName(), ch.getObjectId(), extra);
		return s;
	}
}
