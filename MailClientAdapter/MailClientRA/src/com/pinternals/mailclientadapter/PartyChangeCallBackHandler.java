package com.pinternals.mailclientadapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.cpa.AbstractPartyCallBackHandler;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.Party;
import com.sap.aii.af.service.cpa.PartyIdentifier;

 public class PartyChangeCallBackHandler
   extends AbstractPartyCallBackHandler
 {
   private static final XITrace TRACE = new XITrace(PartyChangeCallBackHandler.class.getName());
   public static final String DUNS = "DUNS";
   private static PartyChangeCallBackHandler partyChangeCallBackHandler = new PartyChangeCallBackHandler();
   private HashMap<String, String> partyidentifierMap = null;
   
   private PartyChangeCallBackHandler()
   {
     this.partyidentifierMap = new HashMap();
   }
   
   public static PartyChangeCallBackHandler getInstance()
   {
     return partyChangeCallBackHandler;
   }
   
   public void partyrefreshEvent(Party arg0)
   {
     String SIGNATURE = "partyrefreshEvent(Party arg0)";
     TRACE.entering("partyrefreshEvent(Party arg0)", new Object[] { arg0 });
     try
     {
       String partyName = arg0.getParty();
       synchronized (this.partyidentifierMap)
       {
         if (this.partyidentifierMap.containsKey(partyName))
         {
           CPALookupManager lookupManager = CPAFactory.getInstance().getLookupManager();
           LinkedList<PartyIdentifier> list = lookupManager.getPartyIdentifiersByParty(arg0);
           Iterator<PartyIdentifier> it = list.iterator();
           String oldId = (String)this.partyidentifierMap.get(partyName);
           while (it.hasNext())
           {
             PartyIdentifier partyIdentifier = (PartyIdentifier)it.next();
             String id = partyIdentifier.getPartyIdentifier();
             String schema = partyIdentifier.getPartySchema();
             if ((schema.equals("DUNS")) && (!id.equals(oldId)))
             {
               TRACE.infoT("partyrefreshEvent(Party arg0)", "Received PartyChangeEvent for the Party " + arg0.getParty() + " The new Identifier is " + id);
               
               this.partyidentifierMap.put(partyName, id);
             }
           }
         }
       }
     }
     catch (CPAException e)
     {
       TRACE.catching("partyrefreshEvent(Party arg0)", e);
     }
     TRACE.exiting("partyrefreshEvent(Party arg0)");
   }
   
   public void addParty(String partyName, String identifier)
   {
     this.partyidentifierMap.put(partyName, identifier);
   }
   
   public void removeParty(String partyName)
   {
     this.partyidentifierMap.remove(partyName);
   }
   
   public Set<String> getRegisteredParties()
   {
     return this.partyidentifierMap.keySet();
   }
   
   public void clear()
   {
     this.partyidentifierMap.clear();
   }
 }

