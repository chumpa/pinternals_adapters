package com.pinternals.mailclientadapter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.sap.tc.logging.Category;
import com.sap.tc.logging.Location;
import com.sap.tc.logging.SimpleLogger;
 
 public class XITrace
 {
   private String className = null;
   protected Location location = null;
   protected static boolean tracing = true;
   public static final int SEVERITY_ALL = 0;
   public static final int SEVERITY_DEBUG = 100;
   public static final int SEVERITY_ERROR = 500;
   public static final int SEVERITY_FATAL = 600;
   public static final int SEVERITY_GROUP = 800;
   public static final int SEVERITY_INFO = 300;
   public static final int SEVERITY_MAX = 700;
   public static final int SEVERITY_MIN = 0;
   public static final int SEVERITY_NONE = 701;
   public static final int SEVERITY_PATH = 200;
   public static final int SEVERITY_WARNING = 400;
   
   public XITrace(String className)
   {
     try
     {
       this.className = className;
       this.location = Location.getLocation(className);
     }
     catch (Exception t)
     {
       t.printStackTrace();
     }
   }
   
   public String toString()
   {
     return this.className;
   }
   
   public void entering(String signature)
   {
     if (this.location != null) {
       this.location.entering(signature);
     }
   }
   
   public void entering(String signature, Object[] args)
   {
     if (this.location != null) {
       this.location.entering(signature, args);
     }
   }
   
   public void exiting(String signature)
   {
     if (this.location != null) {
       this.location.exiting(signature);
     }
   }
   
   public void exiting(String signature, Object res)
   {
     if (this.location != null) {
       this.location.exiting(signature, res);
     }
   }
   
   public void throwing(String signature, Throwable t)
   {
     if (this.location != null) {
       this.location.throwing(signature, t);
     }
   }
   
   public void catching(String signature, Throwable t)
   {
     if ((this.location != null) && 
       (beLogged(400)))
     {
       ByteArrayOutputStream oStream = new ByteArrayOutputStream(1024);
       PrintStream pStream = new PrintStream(oStream);
       t.printStackTrace(pStream);
       pStream.close();
       String stackTrace = oStream.toString();
       this.location.warningT(signature, "Catching {0}", new Object[] { stackTrace });
     }
   }
   
   public void debugT(String signature, Category category, String msg)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.debugT(category, signature, msg);
       } else {
         this.location.debugT(signature, msg);
       }
     }
   }
   
   public void debugT(String signature, String msg)
   {
     if (this.location != null) {
       this.location.debugT(signature, msg);
     }
   }
   
   public void debugT(String signature, Category category, String msg, Object[] args)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.debugT(category, signature, msg, args);
       } else {
         this.location.debugT(signature, msg, args);
       }
     }
   }
   
   public void debugT(String signature, String msg, Object[] args)
   {
     if (this.location != null) {
       this.location.debugT(signature, msg, args);
     }
   }
   
   public void infoT(String signature, Category category, String msg)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.infoT(category, signature, msg);
       } else {
         this.location.infoT(signature, msg);
       }
     }
   }
   
   public void infoT(String signature, String msg)
   {
     if (this.location != null) {
       this.location.infoT(signature, msg);
     }
   }
   
   public void infoT(String signature, Category category, String msg, Object[] args)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.infoT(category, signature, msg, args);
       } else {
         this.location.infoT(signature, msg, args);
       }
     }
   }
   
   public void infoT(String signature, String msg, Object[] args)
   {
     if (this.location != null) {
       this.location.infoT(signature, msg, args);
     }
   }
   
   public void warningT(String signature, Category category, String msg)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.warningT(category, signature, msg);
       } else {
         this.location.warningT(signature, msg);
       }
     }
   }
   
   public void warningT(String signature, String msg)
   {
     if (this.location != null) {
       this.location.warningT(signature, msg);
     }
   }
   
   public void warningT(String signature, Category category, String msg, Object[] args)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.warningT(category, signature, msg, args);
       } else {
         this.location.warningT(signature, msg, args);
       }
     }
   }
   
   public void warningT(String signature, String msg, Object[] args)
   {
     if (this.location != null) {
       this.location.warningT(signature, msg, args);
     }
   }
   
   public void errorT(String signature, Category category, String msg)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.errorT(category, signature, msg);
       } else {
         this.location.errorT(signature, msg);
       }
     }
   }
   
   public void errorT(String signature, String msg)
   {
     if (this.location != null) {
       this.location.errorT(signature, msg);
     }
   }
   
   public void errorT(String signature, Category category, String msg, Object[] args)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.errorT(category, signature, msg, args);
       } else {
         this.location.errorT(signature, msg, args);
       }
     }
   }
   
   public void errorT(String signature, String msg, Object[] args)
   {
     if (this.location != null) {
       this.location.errorT(signature, msg, args);
     }
   }
   
   public void fatalT(String signature, Category category, String msg)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.fatalT(category, signature, msg);
       } else {
         this.location.fatalT(signature, msg);
       }
     }
   }
   
   public void fatalT(String signature, String msg)
   {
     if (this.location != null) {
       this.location.fatalT(signature, msg);
     }
   }
   
   public void fatalT(String signature, Category category, String msg, Object[] args)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.fatalT(category, signature, msg, args);
       } else {
         this.location.fatalT(signature, msg, args);
       }
     }
   }
   
   public void fatalT(String signature, String msg, Object[] args)
   {
     if (this.location != null) {
       this.location.fatalT(signature, msg, args);
     }
   }
   
   public void assertion(String signature, Category category, boolean assertion, String msg)
   {
     if (this.location != null) {
       if (category != null) {
         this.location.assertion(category, signature, assertion, msg);
       } else {
         this.location.assertion(signature, assertion, msg);
       }
     }
   }
   
   public boolean beLogged(int severity)
   {
     if (this.location != null) {
       return this.location.beLogged(severity);
     }
     return false;
   }
   
   public void errorT(String SIGNATURE, Category category, String messageID, String msg)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       if (category != null) {
         SimpleLogger.log(500, category, subloc, messageID, msg);
       } else {
         SimpleLogger.trace(500, subloc, messageID, msg);
       }
     }
   }
   
   public void errorT(String SIGNATURE, String messageID, String msg)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       SimpleLogger.trace(500, subloc, messageID, msg);
     }
   }
   
   public void errorT(String SIGNATURE, Category category, String messageID, String msg, Object... args)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       if (category != null) {
         SimpleLogger.log(500, category, subloc, messageID, msg, args);
       } else {
         SimpleLogger.trace(500, subloc, messageID, msg, args);
       }
     }
   }
   
   public void errorT(String SIGNATURE, String messageID, String msg, Object... args)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       SimpleLogger.trace(500, subloc, messageID, msg, args);
     }
   }
   
   public void fatalT(String SIGNATURE, Category category, String messageID, String msg)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       if (category != null) {
         SimpleLogger.log(600, category, subloc, messageID, msg);
       } else {
         SimpleLogger.trace(500, subloc, messageID, msg);
       }
     }
   }
   
   public void fatalT(String SIGNATURE, String messageID, String msg)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       SimpleLogger.trace(600, subloc, messageID, msg);
     }
   }
   
   public void fatalT(String SIGNATURE, Category category, String messageID, String msg, Object... args)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       if (category != null) {
         SimpleLogger.log(600, category, subloc, messageID, msg, args);
       } else {
         SimpleLogger.trace(600, subloc, messageID, msg, args);
       }
     }
   }
   
   public void fatalT(String SIGNATURE, String messageID, String msg, Object... args)
   {
     if (this.location != null)
     {
       Location subloc = Location.getLocation(this.location, SIGNATURE);
       SimpleLogger.trace(600, subloc, messageID, msg, args);
     }
   }
 }
