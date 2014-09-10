package com.pinternals.nulladapter;

import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;

import com.sap.aii.af.lib.ra.cci.XIMessageRecord;
import com.sap.aii.af.lib.ra.cci.XIRecordFactory;
import com.sap.engine.interfaces.messaging.api.AckType;
import com.sap.engine.interfaces.messaging.api.Action;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageFactory;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Party;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.Service;
import com.sap.engine.interfaces.messaging.api.ack.AckFactory;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;
import com.sap.aii.af.lib.trace.Trace;

public class XIMessageFactoryImpl implements XIRecordFactory {
//	private static final String AF_MSGFCT_TYPE = "XI";
	private static final Trace TRACE = new Trace(XIMessageFactoryImpl.class.getName());
	private MessageFactory mf = null;
	private AckFactory af = null;
	private String ackfct = null;

	public XIMessageFactoryImpl(String adapterType, String adapterNamespace)
			throws ResourceException {
		String SIGNATURE = "XIMessageFactoryImpl(String adapterType, String adapterNamespace)";
		TRACE.entering(SIGNATURE, new Object[] { adapterType, adapterNamespace });
		try {
			this.mf = PublicAPIAccessFactory.getPublicAPIAccess().createMessageFactory("XI");
			this.af = PublicAPIAccessFactory.getPublicAPIAccess().createAckFactory();

			this.ackfct = (adapterType + "_" + adapterNamespace);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
	}

	public Message createMessageRecord(String fromParty, String toParty, String fromService,
			String toService, String action, String actionNS) throws ResourceException {
		String SIGNATURE = "createMessageRecord(String fromParty, String toParty, String fromService, String toService, String action)";
		TRACE.entering(SIGNATURE,
				new Object[] { fromParty, toParty, fromService, toService, action });
		Message msg = null;
		try {
			Party fp = new Party(fromParty);
			Party tp = new Party(toParty);
			Service fs = new Service(fromService);
			Service ts = new Service(toService);
			Action a = new Action(action, actionNS);
			msg = this.mf.createMessage(fp, tp, fs, ts, a);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

	public Message createMessageRecord(Party fromParty, Party toParty, Service fromService,
			Service toService, Action action) throws ResourceException {
		String SIGNATURE = "createMessageRecord(Party fromParty, Party toParty, Service fromService, Service toService, Action action)";
		TRACE.entering(SIGNATURE,
				new Object[] { fromParty, toParty, fromService, toService, action });
		Message msg = null;
		try {
			msg = this.mf.createMessage(fromParty, toParty, fromService, toService, action);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

	public Message createMessageRecord(String fromParty, String toParty, String fromService,
			String toService, String action, String actionNS, String messageId)
			throws ResourceException {
		String SIGNATURE = "createMessageRecord(String fromParty, String toParty, String fromService, String toService, String action, String messageId)";
		TRACE.entering(SIGNATURE, new Object[] { fromParty, toParty, fromService, toService,
				action, messageId });
		Message msg = null;
		try {
			Party fp = new Party(fromParty);
			Party tp = new Party(toParty);
			Service fs = new Service(fromService);
			Service ts = new Service(toService);
			Action a = new Action(action, actionNS);
			msg = this.mf.createMessage(fp, tp, fs, ts, a, messageId);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

	public Message createMessageRecord(Party fromParty, Party toParty, Service fromService,
			Service toService, Action action, String messageId) throws ResourceException {
		String SIGNATURE = "createMessageRecord(Party fromParty, Party toParty, Service fromService, Service toService, Action action, String messageId)";
		TRACE.entering(SIGNATURE, new Object[] { fromParty, toParty, fromService, toService,
				action, messageId });
		Message msg = null;
		try {
			msg = this.mf.createMessage(fromParty, toParty, fromService, toService, action,
					messageId);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException(e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		return msg;
	}

	public MappedRecord createMappedRecord(String arg0) throws ResourceException {
		throw new ResourceException("Map records are not supported.");
	}

	public IndexedRecord createIndexedRecord(String arg0) throws ResourceException {
		throw new ResourceException("Index records are not supported");
	}

	public XIMessageRecord createXIMessageRecord() {
		return new XIMessageRecordImpl(null);
	}

	public void ackNotSupported(MessageKey messageKey, AckType[] acksNotSupported)
			throws MessagingException {
		this.af.ackNotSupported(this.ackfct, messageKey, acksNotSupported);
	}

	public void applicationAck(MessageKey messageToAck) {
		String SIGNATURE = "applicationAck(MessageKey messageToAck)";
		try {
			this.af.applicationAck(this.ackfct, messageToAck);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}
	}

	public void applicationErrorAck(MessageKey messageToAck, Exception error) {
		String SIGNATURE = "applicationErrorAck(MessageKey messageToAck,java.lang.Exception error)";
		try {
			this.af.applicationErrorAck(this.ackfct, messageToAck, error);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}
	}

	public void deliveryAck(MessageKey messageToAck) {
		String SIGNATURE = "deliveryAck(MessageKey messageToAck)";
		try {
			this.af.deliveryAck(this.ackfct, messageToAck);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}
	}

	public void deliveryErrorAck(MessageKey messageToAck, Exception error) {
		String SIGNATURE = "deliveryErrorAck(MessageKey messageToAck,java.lang.Exception error)";
		try {
			this.af.deliveryErrorAck(this.ackfct, messageToAck, error);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
		}
	}
}
