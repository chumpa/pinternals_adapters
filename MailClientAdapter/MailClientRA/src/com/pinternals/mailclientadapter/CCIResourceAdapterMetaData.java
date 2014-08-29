package com.pinternals.mailclientadapter;

import javax.resource.cci.ResourceAdapterMetaData;

public class CCIResourceAdapterMetaData implements ResourceAdapterMetaData {
	private static final XITrace TRACE = new XITrace(CCIResourceAdapterMetaData.class.getName());
	private String vendorName = "SAP AG Germany";
	private String adapterVersion = "1.0";
	private String specVersion = "1.0";
	private String adapterName = "XI 3.0 AF Adapter Skeleton";
	private String description = "Sample adapter for the XI 3.0 Adapter Framework";

	public String getAdapterVersion() {
		return this.adapterVersion;
	}

	public String getSpecVersion() {
		return this.specVersion;
	}

	public String getAdapterName() {
		return this.adapterName;
	}

	public String getAdapterVendorName() {
		return this.vendorName;
	}

	public String getAdapterShortDescription() {
		return this.description;
	}

	public void setAdapterVersion(String version) {
		this.adapterVersion = version;
	}

	public void setSpecVersion(String version) {
		this.specVersion = version;
	}

	public void setAdapterName(String name) {
		this.adapterName = name;
	}

	public void setAdapterVendorName(String name) {
		this.vendorName = name;
	}

	public void setAdapterShortDescription(String description) {
		this.description = description;
	}

	public String[] getInteractionSpecsSupported() {
		String SIGNATURE = "CciConnection(SpiManagedConnection)";
		TRACE.entering(SIGNATURE);
		String[] str = new String[1];
		str[0] = new String("com.sap.aii.af.ra.ms.cci.XiInteractionSpec");
		TRACE.exiting(SIGNATURE);
		return str;
	}

	public boolean supportsExecuteWithInputAndOutputRecord() {
		return true;
	}

	public boolean supportsExecuteWithInputRecordOnly() {
		return true;
	}

	public boolean supportsLocalTransactionDemarcation() {
		return false;
	}
}
