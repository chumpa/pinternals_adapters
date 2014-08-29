package com.pinternals;

import com.sap.aii.af.service.cpa.Channel;

public class AFUtil {
	static String labelLong = "%s channel %s|%s|%s with id=%s%s";
	public static String formatCcLong(Channel ch, String extra) {
		String s = String.format(labelLong, ch.getDirection(), 
				ch.getParty(), ch.getService(), ch.getChannelName(), ch.getObjectId(), extra);
		return s;
	}
}
