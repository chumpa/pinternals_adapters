package com.pinternals.nulladapter;

import com.sap.tc.logging.Category;

public class AdapterConstants {
	public static String version = "0.1";
	public static String ADAPTER_TYPE = "Null";
	public static String ADAPTER_NAMESPACE = "urn:pinternals-adapters";

	public static final Category LogCategoryRoot = Category.getCategory(Category.getRoot(), "Applications/ExchangeInfrastructure/Adapter/" + ADAPTER_TYPE);
	public static final Category LogCategoryConfig = Category.getCategory(LogCategoryRoot, "Configuration");
	public static final Category LogCategoryServer = Category.getCategory(LogCategoryRoot, "Server");
	public static final Category LogCategoryConnect = Category.getCategory(LogCategoryRoot, "Connection");
	public static final Category LogCategoryCONNECT_AF = Category.getCategory(LogCategoryServer, "Adapter Framework");

}
