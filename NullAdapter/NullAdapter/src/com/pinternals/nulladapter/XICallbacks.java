package com.pinternals.nulladapter;

import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.ResourceBundleLocalizationCallback;

public class XICallbacks {

}

class XILocalizationUtilities2 {
	static ResourceBundleLocalizationCallback RBLC = new ResourceBundleLocalizationCallback(
			AdapterConstants.rbName, XILocalizationUtilities2.class.getClassLoader());

	public static LocalizationCallback getLocalizationCallback() {
		return RBLC;
	}
}