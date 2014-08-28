package com.pinternals.mailclientadapter;

import com.sap.tc.logging.Category;
 
 public class XIAdapterCategories
 {
   public static final Category MY_ADAPTER_ROOT = Category.getCategory(Category.getRoot(), "Applications/ExchangeInfrastructure/Adapter/MailClientAdapter");
   public static final Category CONFIG = Category.getCategory(MY_ADAPTER_ROOT, "Configuration");
   public static final Category SERVER = Category.getCategory(MY_ADAPTER_ROOT, "Server");
   public static final Category SERVER_HTTP = Category.getCategory(SERVER, "HTTP");
   public static final Category SERVER_JNDI = Category.getCategory(SERVER, "Naming");
   public static final Category SERVER_JCA = Category.getCategory(SERVER, "JCA~~~");
   public static final Category CONNECT = Category.getCategory(MY_ADAPTER_ROOT, "Connection");
   public static final Category CONNECT_EIS = Category.getCategory(SERVER, "EIS");
   public static final Category CONNECT_AF = Category.getCategory(SERVER, "Adapter Framework");
 }

