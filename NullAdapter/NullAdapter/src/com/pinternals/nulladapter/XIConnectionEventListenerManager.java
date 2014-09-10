package com.pinternals.nulladapter;

import java.util.Vector;

import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ManagedConnection;
import com.sap.aii.af.lib.trace.Trace;

public class XIConnectionEventListenerManager {
	private static final Trace TRACE = new Trace(XIConnectionEventListenerManager.class.getName());
	private Vector listeners;
	private ManagedConnection mc = null;

	public XIConnectionEventListenerManager(ManagedConnection mc) {
		String SIGNATURE = "XIConnectionEventListenerManager(ManagedConnection mc)";
		TRACE.entering(SIGNATURE, new Object[] { mc });
		this.listeners = new Vector();
		this.mc = mc;
		TRACE.exiting(SIGNATURE);
	}

	public void sendEvent(int eventType, Exception ex, Object connectionHandle) {
		String SIGNATURE = "sendEvent(int eventType, Exception ex, Object connectionHandle)";
		TRACE.entering(SIGNATURE, new Object[] { new Integer(eventType), ex, connectionHandle });

		Vector list = (Vector) this.listeners.clone();

		ConnectionEvent ce = null;
		if (ex == null) {
			ce = new ConnectionEvent(this.mc, eventType);
		} else {
			ce = new ConnectionEvent(this.mc, eventType, ex);
		}
		if (connectionHandle != null) {
			ce.setConnectionHandle(connectionHandle);
		}
		int size = list.size();
		for (int i = 0; i < size; i++) {
			ConnectionEventListener l = (ConnectionEventListener) list.elementAt(i);
			switch (eventType) {
			case 1:
				l.connectionClosed(ce);
				break;
			case 2:
				l.localTransactionStarted(ce);
				break;
			case 3:
				l.localTransactionCommitted(ce);
				break;
			case 4:
				l.localTransactionRolledback(ce);
				break;
			case 5:
				l.connectionErrorOccurred(ce);
				break;
			default:
				throw new IllegalArgumentException("Illegal eventType: " + eventType);
			}
		}
		TRACE.exiting(SIGNATURE);
	}

	public void addConnectorListener(ConnectionEventListener listener) {
		String SIGNATURE = "addConnectorListener(ConnectionEventListener listener)";
		TRACE.entering(SIGNATURE, new Object[] { listener });
		if (listener != null) {
			this.listeners.addElement(listener);
		}
		TRACE.exiting(SIGNATURE);
	}

	public void removeConnectorListener(ConnectionEventListener listener) {
		String SIGNATURE = "removeConnectorListener(ConnectionEventListener listener)";
		TRACE.entering(SIGNATURE, new Object[] { listener });
		if (listener != null) {
			this.listeners.removeElement(listener);
		}
		TRACE.exiting(SIGNATURE);
	}
}
