package com.pinternals.mailclientadapter;
 
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.ResourceBundleLocalizationCallback;
 
public class XILocalizationUtilities
{
	public static LocalizationCallback getLocalizationCallback() {
		return new ResourceBundleLocalizationCallback(XILocalizationUtilities.class.getPackage().getName() + ".rb_pimon", XILocalizationUtilities.class.getClassLoader());
	}
}
