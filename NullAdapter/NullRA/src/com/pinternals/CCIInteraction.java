package com.pinternals.nulladapter;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

import com.sap.aii.af.lib.ra.cci.XIAdapterException;
import com.sap.aii.af.lib.ra.cci.XIInteraction;
import com.sap.aii.af.lib.ra.cci.XIInteractionSpec;
import com.sap.aii.af.lib.ra.cci.XIMessageRecord;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAOutboundRuntimeLookupManager;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.NormalizationManager;
import com.sap.aii.af.service.cpa.PartyIdentifier;
import com.sap.aii.af.service.cpa.ServiceIdentifier;
import com.sap.aii.af.service.headermapping.HeaderMapper;
import com.sap.aii.af.service.headermapping.HeaderMappingException;
import com.sap.aii.af.service.idmap.MessageIDMapper;
import com.sap.engine.interfaces.messaging.api.AckType;
import com.sap.engine.interfaces.messaging.api.Action;
import com.sap.engine.interfaces.messaging.api.ErrorInfo;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;

public class CCIInteraction implements XIInteraction {
	private static final XITrace TRACE = new XITrace(CCIInteraction.class.getName());
	// private static final String ADDR_AGENCY_EAN = "009";
	// private static final String ADDR_SCHEMA_GLN = "GLN";
	private Connection connection;
	private XIMessageFactoryImpl mf = null;
	private SPIManagedConnection mc = null;
	private SPIManagedConnectionFactory mcf = null;
	private AuditAccess audit = null;

	public CCIInteraction(Connection cciConnection) throws ResourceException {
		String SIGNATURE = "CciInteraction(javax.resource.cci.Connection)";
		TRACE.entering(SIGNATURE, new Object[] { cciConnection });
		if (cciConnection == null) {
			ResourceException re = new ResourceException("No related CCI connection in Interaction (cciConnection is null).");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		this.connection = cciConnection;
		this.mc = ((CCIConnection) this.connection).getManagedConnection();
		if (this.mc == null) {
			ResourceException re = new ResourceException("No related managed connection in CCI connection (mc is null).");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		this.mcf = ((SPIManagedConnectionFactory) this.mc.getManagedConnectionFactory());
		if (this.mcf == null) {
			ResourceException re = new ResourceException("No related managed connection factory in managed connection (mcf is null).");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		this.audit = this.mcf.getAuditAccess();

		this.mf = this.mcf.getXIMessageFactoryImpl();

		TRACE.exiting(SIGNATURE);
	}

	public Connection getConnection() {
		return this.connection;
	}

	public void close() throws ResourceException {
		String SIGNATURE = "close()";
		TRACE.entering(SIGNATURE);
		this.connection = null;
		TRACE.exiting(SIGNATURE);
	}

	public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
		String SIGNATURE = "execute(InteractionSpec ispec, Record input, Record output)";
		TRACE.entering(SIGNATURE);
		if (!(output instanceof XIMessageRecord)) {
			ResourceException re = new ResourceException("Output record is no XI AF XIMessageRecord.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		XIMessageRecord callerOutput = (XIMessageRecord) output;
		XIMessageRecord localOutput = (XIMessageRecord) execute(ispec, input);
		try {
			callerOutput.setXIMessage(localOutput.getXIMessage());
			callerOutput.setRecordName(localOutput.getRecordName());
			callerOutput.setRecordShortDescription(localOutput.getRecordShortDescription());
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, MCAConstants.LogCategoryConnect, "SOA.apt_sample.0002", "Exception during output record transfer. Reason: {0}", new Object[] { e.getMessage() });
			ResourceException re = new ResourceException("Output record cannot be filled. Reason: " + e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
		return true;
	}

	public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
		String SIGNATURE = "execute(InteractionSpec ispec, Record input)";
		TRACE.entering(SIGNATURE, new Object[] { ispec, input });
		Record output = null;
		if (ispec == null) {
			ResourceException re = new ResourceException("Input ispec is null.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		if (!(ispec instanceof XIInteractionSpec)) {
			ResourceException re = new ResourceException("Input ispec is no XI AF InteractionSpec.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		XIInteractionSpec XIIspec = (XIInteractionSpec) ispec;
		String method = XIIspec.getFunctionName();
		if (method.compareTo("Send") == 0) {
			output = send(XIIspec, input, this.mc);
		} else if (method.compareTo("Call") == 0) {
			output = call(XIIspec, input, this.mc);
		} else {
			ResourceException re = new ResourceException("Unknown function name in ispec: " + method);
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE);
		return output;
	}

	public ResourceWarning getWarnings() throws ResourceException {
		return null;
	}

	public void clearWarnings() throws ResourceException {
	}

	private Record send(InteractionSpec ispec, Record input, SPIManagedConnection mc) throws ResourceException {
		String SIGNATURE = "send(InteractionSpec ispec, Record input, SpiManagedConnection mc)";
		TRACE.entering(SIGNATURE, new Object[] { ispec, input, mc });

		Record output = null;

		if (!(input instanceof XIMessageRecord)) {
			ResourceException re = new ResourceException("Input record is not instance of Message.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		Message msg = ((XIMessageRecordImpl) input).getXIMessage();

		MessageKey amk = new MessageKey(msg.getMessageId(), MessageDirection.INBOUND);
		// if (mc.getAsmaGet())
		// {
		// String value = msg.getMessageProperty(this.mcf.getAdapterNamespace()
		// + "/" + this.mcf.getAdapterType(), "JCAChannelID");
		// if (value != null)
		// {
		// TRACE.debugT("send(InteractionSpec ispec, Record input, SpiManagedConnection mc)",
		// XIAdapterCategories.CONNECT, "Detected ASMA {0} with value: {1}", new
		// Object[] { "JCAChannelID", value });
		// }
		// else
		// {
		// if (mc.getAsmaError())
		// {
		// TRACE.errorT("send(InteractionSpec ispec, Record input, SpiManagedConnection mc)",
		// XIAdapterCategories.CONNECT, "SOA.apt_sample.0003",
		// "ASMA {0} not found in the current message. Channel is configured to throw an error in this case.",
		// new Object[] { "JCAChannelID" });
		// XIAdapterException de = new
		// XIAdapterException("ASMA not found in the current message. Channel is configured to throw an error in this case.");
		// this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR,
		// "ASMA not found in the current message. Channel is configured to throw an error in this case.");
		// this.audit.flushAuditLogEntries(amk);
		// throw de;
		// }
		// TRACE.debugT("send(InteractionSpec ispec, Record input, SpiManagedConnection mc)",
		// XIAdapterCategories.CONNECT,
		// "ASMA {0} not found in the current message. Channel is configured to continue the processing.",
		// new Object[] { "JCAChannelID" });
		// }
		// }
		// else
		// {
		// TRACE.debugT("send(InteractionSpec ispec, Record input, SpiManagedConnection mc)",
		// XIAdapterCategories.CONNECT,
		// "ASMA are switched off for this channel.");
		// }
		String[] result = getMappedHeaderFieldsAndNormalize(mc.getChannelID(), msg);
		String fromParty = result[0];
		String fromService = result[1];
		String toParty = result[2];
		String toService = result[3];

		Payload appPayLoad = msg.getDocument();
		String payText = new String(appPayLoad.getContent());
		if (payText.indexOf("<DeliveryException>") != -1) {
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Payload contains the <DeliverableException> tag "
					+ "that causes a XIDeliveryException for testing purposes!");
			XIAdapterException de = new XIAdapterException("XI AF JCA sample ra cannot deliver the message (test)");
			this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Payload contains the <DeliverableException> tag that causes "
					+ "a XIDeliveryException for testing purposes!");
			this.audit.flushAuditLogEntries(amk);
			TRACE.throwing(SIGNATURE, de);
			throw de;
		}
		if (payText.indexOf("<RecoverableException>") != -1) {
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Payload contains the <RecoverableException> tag that causes a"
					+ " XIRecoverableException for testing purposes!");
			XIAdapterException re = new XIAdapterException("XI AF JCA sample ra cannot temporarily deliver the message (test)");
			this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Payload contains the <RecoverableException> tag that causes a"
					+ " XIRecoverableException for testing purposes!");
			this.audit.flushAuditLogEntries(amk);
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		if (payText.indexOf("<FatalTraceOn>") != -1) {
			TRACE.fatalT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "SOA.apt_sample.0050", "Ignore this and the following trace messages in the method send()."
					+ " It is just a sample for using the trace API!");

			TRACE.fatalT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "SOA.apt_sample.0051", "A fatal trace message with signature, category and text");

			TRACE.fatalT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "SOA.apt_sample.0052", "A fatal trace message with signature, category, text and {0}.", new Object[] { "parameters" });

			TRACE.fatalT(SIGNATURE, "SOA.apt_sample.0053", "A fatal trace message with signature and text only");

			TRACE.fatalT(SIGNATURE, "SOA.apt_sample.0054", "A fatal trace message with signature, text and {0}", new Object[] { "parameters" });
		}
		String newMsgIndicator = new String("***** Start of async. message *****");
		try {
			// PrintWriter printWriter = new PrintWriter(fWriter);
			// printWriter.println(newMsgIndicator);
			// printWriter.println("From (P/S): " + fromParty + "/" +
			// fromService);
			// printWriter.println("To (P/S): " + toParty + "/" + toService);
			// printWriter.println("Payload: ");
			// printWriter.println(payText);
			// fWriter.flush();

			MessageIDMapper messageIDMapper = MessageIDMapper.getInstance();

			String extMsgId = "123"; //SPIManagedConnectionFactory.getExternalMessageID(null);
			TRACE.infoT(SIGNATURE, "External message ID is '" + extMsgId + "'");

			String jcaMessageId = "JCASample_" + msg.getMessageId();

			messageIDMapper.createIDMap(jcaMessageId, extMsgId, System.currentTimeMillis() + 86400000L, false);

			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Lookup of {0} returns: {1}", new Object[] {
					jcaMessageId, messageIDMapper.getMappedId(jcaMessageId) });

			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Lookup of {0} returns: {1}", new Object[] {
					extMsgId, messageIDMapper.getMappedId(extMsgId) });

			messageIDMapper.remove(jcaMessageId);
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			XIAdapterException de = new XIAdapterException("System error: " + e.getMessage());
			this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Unable to write message into file.");
			this.audit.flushAuditLogEntries(amk);
			TRACE.throwing(SIGNATURE, de);
			throw de;
		}
		this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Async. message was forwarded succesfully to the file system");
		this.audit.flushAuditLogEntries(amk);

		MessageKey msgKey = msg.getMessageKey();
		TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Message key {0} with this data received: ID: {1} Direction: {2}", new Object[] {
				msgKey.toString(), msgKey.getMessageId(), msgKey.getDirection().toString() });
		if (payText.indexOf("<AppAckOn>") == -1) {
			try {
				AckType[] notSupportedAcks = { AckType.APPLICATION, AckType.APPLICATION_ERROR };
				this.mf.ackNotSupported(msgKey, notSupportedAcks);
			} catch (Exception e) {
				TRACE.catching(SIGNATURE, e);
				TRACE.warningT(SIGNATURE, MCAConstants.LogCategoryConnect, "Not supported Acks cannot be published!");
			}
		} else {
			this.mf.applicationAck(msgKey);
		}
		this.mf.deliveryAck(msgKey);

		TRACE.exiting(SIGNATURE);
		return output;
	}

	private String[] getMappedHeaderFieldsAndNormalize(String channelID, Message msg) {
		String SIGNATURE = "getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)";
		TRACE.entering(SIGNATURE);

		String fromParty = null;
		String fromService = null;
		String toParty = null;
		String toService = null;
		PartyIdentifier fromPartyIdentifier = null;
		PartyIdentifier toPartyIdentifier = null;
		ServiceIdentifier fromServiceIdentifier = null;
		ServiceIdentifier toServiceIdentifier = null;
		try {
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Get receiver agreement with OutboundRuntimeLookup now.");
			CPAOutboundRuntimeLookupManager outLookup = CPAFactory.getInstance().createOutboundRuntimeLookupManager(this.mcf.getAdapterType(), this.mcf.getAdapterNamespace(), msg.getFromParty().toString(), msg.getToParty().toString(), msg.getFromService().toString(), msg.getToService().toString(), msg.getAction().getName(), msg.getAction().getType());

			Binding binding = outLookup.getBinding();

			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Get receiver agreement for channel ID {0} now.", new Object[] { channelID });
			Binding bindingByChannel = CPAFactory.getInstance().getLookupManager().getBindingByChannelId(channelID);
			// readSampleConfiguration(outLookup, bindingByChannel);
			Channel channelFromBinding = outLookup.getChannel();
			byte[] rawHeaderMappingData = outLookup.getHeaderMappingConfig();

			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Get header mappings for message with ID {0} and receiver agreement with ID {1} now.", new Object[] {
					msg.getMessageId(), binding.getObjectId() });
			try {
				HeaderMapper hm = new HeaderMapper();

				Map mappedFields = HeaderMapper.getMappedHeader(msg, binding);
				if ((mappedFields != null) && (!mappedFields.isEmpty())) {
					if ((fromParty = (String) mappedFields.get(HeaderMapper.FROM_PARTY)) != null) {
						TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Header mapping: From party {0} is mapped to {1}", new Object[] {
								msg.getFromParty().toString(), fromParty });
					}
					if ((fromService = (String) mappedFields.get(HeaderMapper.FROM_SERVICE)) != null) {
						TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Header mapping: From service {0} is mapped to {1}", new Object[] {
								msg.getFromService().toString(), fromService });
					}
					if ((toParty = (String) mappedFields.get(HeaderMapper.TO_PARTY)) != null) {
						TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Header mapping: To party {0} is mapped to {1}", new Object[] {
								msg.getToParty().toString(), toParty });
					}
					if ((toService = (String) mappedFields.get(HeaderMapper.TO_SERVICE)) != null) {
						TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Header mapping: To service {0} is mapped to {1}", new Object[] {
								msg.getToService().toString(), toService });
					}
				} else {
					TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Header mapping is not defined for receiver agreement: {0}", new Object[] { binding.getStringRepresentation() });
				}
			} catch (HeaderMappingException he) {
				TRACE.catching(SIGNATURE, he);
				throw new HeaderMappingException(he.getMessage());
			}
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, MCAConstants.LogCategoryConnect, "SOA.apt_sample.0004", "Exception during header mapping. Reason: {0}. Error will be ignored.", new Object[] { e.getMessage() });
		}
		if (fromParty == null) {
			fromParty = msg.getFromParty().toString();
		}
		if (fromService == null) {
			fromService = msg.getFromService().toString();
		}
		if (toParty == null) {
			toParty = msg.getToParty().toString();
		}
		if (toService == null) {
			toService = msg.getToService().toString();
		}
		try {
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Access the normalization manager now.");
			NormalizationManager normalizer = NormalizationManager.getInstance();

			fromServiceIdentifier = normalizer.getAlternativeServiceIdentifier(fromParty, fromService, "GLN");
			if ((fromServiceIdentifier != null) && (fromServiceIdentifier.getServiceIdentifier() != null)
					&& (fromServiceIdentifier.getServiceIdentifier().length() > 0)) {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization for service: {0} is: {1}", new Object[] {
						fromService, fromServiceIdentifier.getServiceIdentifier() });
				fromService = fromServiceIdentifier.getServiceIdentifier();
			} else {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization is not defined for service: {0}", new Object[] { fromService });
			}
			fromPartyIdentifier = normalizer.getAlternativePartyIdentifier("009", "GLN", fromParty);
			if ((fromPartyIdentifier != null) && (fromPartyIdentifier.getParty() != null)
					&& (fromPartyIdentifier.getParty().length() > 0)) {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization for party: {0} is: {1}", new Object[] {
						fromParty, fromPartyIdentifier.getPartyIdentifier() });
				fromParty = fromPartyIdentifier.getPartyIdentifier();
			} else {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization is not defined for party: {0}", new Object[] { fromParty });
			}
			toServiceIdentifier = normalizer.getAlternativeServiceIdentifier(toParty, toService, "GLN");
			if ((toServiceIdentifier != null) && (toServiceIdentifier.getServiceIdentifier() != null)
					&& (toServiceIdentifier.getServiceIdentifier().length() > 0)) {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization for service: {0} is: {1}", new Object[] {
						toService, toServiceIdentifier.getServiceIdentifier() });
				toService = toServiceIdentifier.getServiceIdentifier();
			} else {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization is not defined for service: {0}", new Object[] { toService });
			}
			toPartyIdentifier = normalizer.getAlternativePartyIdentifier("009", "GLN", toParty);
			if ((toPartyIdentifier != null) && (toPartyIdentifier.getParty() != null)
					&& (toPartyIdentifier.getParty().length() > 0)) {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization for party: {0} is: {1}", new Object[] {
						toParty, toPartyIdentifier.getPartyIdentifier() });
				toParty = toPartyIdentifier.getPartyIdentifier();
			} else {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Address normalization is not defined for party: {0}", new Object[] { toParty });
			}
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			TRACE.errorT(SIGNATURE, MCAConstants.LogCategoryConnect, "SOA.apt_sample.0005", "Exception during address normalization. Reason: {0}. Error will be ignored.", new Object[] { e.getMessage() });
		}
		String[] result = new String[4];
		result[0] = fromParty;
		result[1] = fromService;
		result[2] = toParty;
		result[3] = toService;

		TRACE.exiting(SIGNATURE);
		return result;
	}

	private String[] getFaultIF(String channelID) {
		String SIGNATURE = "getFaultIF(String channelID)";
		TRACE.entering(SIGNATURE, new Object[] { channelID });
		String[] result = new String[2];
		try {
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Get channel CPA object with channelID {0}", new Object[] { channelID });
			Channel channel = (Channel) CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelID);
			result[0] = channel.getValueAsString("faultInterface");
			result[1] = channel.getValueAsString("faultInterfaceNamespace");
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Read this fault interface value: Name: {0} Namespace: {1}", new Object[] {
					result[0], result[1] });
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			result[0] = "XIAFJCASampleFault";
			result[1] = "http://sap.com/xi/XI/sample/JCA";
			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Fault interface cannot be read from channel configuration due to {0}. Take defaults value: Name: {1} Namespace: {2}", new Object[] {
					e.getMessage(), result[0], result[1] });
		}
		TRACE.exiting(SIGNATURE);
		return result;
	}

	// private void readSampleConfiguration(CPAOutboundRuntimeLookupManager
	// outLookup, Binding binding)
	// {
	// String SIGNATURE =
	// "readSampleConfiguration(OutboundRuntimeLookup outLookup)";
	// TRACE.entering(SIGNATURE, new Object[] { outLookup });
	// try
	// {
	// String sampleStringValue =
	// outLookup.getBindingValueAsString("sampleString");
	// long sampleLongValue = outLookup.getBindingValueAsLong("sampleLong");
	// int sampleIntValue = outLookup.getBindingValueAsInt("sampleInteger");
	// boolean sampleBooleanValue =
	// outLookup.getBindingValueAsBoolean("sampleBoolean");
	// Object sampleObjectValue =
	// outLookup.getBindingValueAsString("sampleString");
	// BinaryData sampleBinaryValue =
	// outLookup.getBindingValueAsBinary("sampleBinary");
	// TableData sampleTableValue =
	// outLookup.getBindingValueAsTable("sampleTable");
	//       
	// 
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Sample binding configuration read: String {0}, long {1}, int {2}, boolean {3}, Object {4}",
	// new Object[] { sampleStringValue, String.valueOf(sampleLongValue),
	// String.valueOf(sampleIntValue), String.valueOf(sampleBooleanValue),
	// sampleObjectValue.toString() });
	//       
	// 
	// 
	// String samplePasswordValue =
	// outLookup.getBindingValueAsString("samplePassword");
	// if (true == outLookup.isBindingValuePassword("samplePassword")) {
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Binding passwords must not be printed out in logfiles!");
	// } else {
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "The binding 'samplePassword' parameter was no password? Value: {0}", new
	// Object[] { samplePasswordValue });
	// }
	// sampleStringValue = outLookup.getChannelValueAsString("sampleString");
	// sampleLongValue = outLookup.getChannelValueAsLong("sampleLong");
	// sampleIntValue = outLookup.getChannelValueAsInt("sampleInteger");
	// sampleBooleanValue = outLookup.getChannelValueAsBoolean("sampleBoolean");
	// sampleObjectValue = outLookup.getChannelValueAsString("sampleString");
	// sampleBinaryValue = outLookup.getChannelValueAsBinary("sampleBinary");
	// sampleTableValue = outLookup.getChannelValueAsTable("sampleTable");
	//       
	// 
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Sample channel configuration read: String {0}, long {1}, int {2}, boolean {3}, Object {4}",
	// new Object[] { sampleStringValue, String.valueOf(sampleLongValue),
	// String.valueOf(sampleIntValue), String.valueOf(sampleBooleanValue),
	// sampleObjectValue.toString() });
	//       
	// 
	// 
	// samplePasswordValue =
	// outLookup.getChannelValueAsString("samplePassword");
	// if (true == outLookup.isChannelValuePassword("samplePassword")) {
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Channel passwords must not be printed out in logfiles!");
	// } else {
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "The channel 'samplePassword' parameter was no password? Value: {0}", new
	// Object[] { samplePasswordValue });
	// }
	// }
	// catch (Exception e)
	// {
	// TRACE.catching(SIGNATURE, e);
	// TRACE.errorT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "SOA.apt_sample.0006",
	// "Sample configuration read with OutboundRuntimeLookup failed. Error is ignored!");
	// }
	// try
	// {
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "This binding is received: Channel/Direction/Attributes = {0}/{1}/{2}",
	// new Object[] { binding.getChannelId(), binding.getDirection().toString(),
	// binding.getAttributes() });
	//       
	// 
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Binding address data: FP/FS/TP/TS/IF/IFNS = {0}/{1}/{2}/{3}/{4}/{5}",
	// new Object[] { binding.getFromParty(), binding.getFromService(),
	// binding.getToParty(), binding.getToService(), binding.getActionName(),
	// binding.getAdapterNamespace() });
	//       
	// 
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Binding adapter data: Type/NameSpace/SWCV = {0}/{1}/{2}", new Object[] {
	// binding.getAdapterType(), binding.getAdapterNamespace(),
	// binding.getAdapterSWCV() });
	//       
	// 
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Binding mapping data: Id/Class/NewIF/NewIFNS = {0}/{1}/{2}/{3}", new
	// Object[] { binding.getMappingId(), binding.getMappingClassName(),
	// binding.getMappedActionName(), binding.getMappedActionNamespace() });
	//       
	// 
	// byte[] mapping = binding.getHeaderMappingConfig();
	//       
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Binding object data: Id/Name = {0}/{1}", new Object[] {
	// binding.getObjectId(), binding.getObjectName() });
	//       
	// 
	// Direction direction = binding.getDirection();
	// String strDir = "Unknown";
	// if (direction == Direction.INBOUND) {
	// strDir = "INBOUND";
	// }
	// if (direction == Direction.OUTBOUND) {
	// strDir = "OUTBOUND";
	// }
	// TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "Binding direction data: DB/String = {0}/{1}", new Object[] {
	// direction.getDBFlag(), strDir });
	// }
	// catch (Exception e)
	// {
	// TRACE.catching(SIGNATURE, e);
	// TRACE.errorT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF,
	// "SOA.apt_sample.0007",
	// "Sample configuration read with Binding failed. Error is ignored!");
	// }
	// TRACE.exiting(SIGNATURE);
	// }

	private Record call(InteractionSpec ispec, Record input, SPIManagedConnection mc) throws ResourceException {
		String SIGNATURE = "call(InteractionSpec ispec, Record input, SpiManagedConnection mc)";
		TRACE.entering(SIGNATURE);

		FileOutputStream file = null; // mc.getOutFile();
		XIMessageRecordImpl output = null;
		if (file == null) {
			ResourceException re = new ResourceException("No related file stream resource in managed connection (file is null).");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		OutputStreamWriter fWriter = new OutputStreamWriter(file);
		if (input == null) {
			ResourceException re = new ResourceException("Input record is null.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		if (!(input instanceof XIMessageRecord)) {
			ResourceException re = new ResourceException("Input record is not instance of Message.");
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		Message msg = ((XIMessageRecord) input).getXIMessage();

		MessageKey amk = new MessageKey(msg.getMessageId(), MessageDirection.INBOUND);

		String[] result = getMappedHeaderFieldsAndNormalize(mc.getChannelID(), msg);
		String fromParty = result[0];
		String fromService = result[1];
		String toParty = result[2];
		String toService = result[3];

		Payload appPayLoad = msg.getDocument();
		String payText = new String(appPayLoad.getContent());
		String newMsgIndicator = new String("***** Start of sync. message *****");
		try {
			PrintWriter printWriter = new PrintWriter(fWriter);
			printWriter.println(newMsgIndicator);
			printWriter.println("From (P/S): " + fromParty + "/" + fromService);
			printWriter.println("To (P/S): " + toParty + "/" + toService);
			printWriter.println("Payload: ");
			printWriter.println(payText);
			fWriter.flush();
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException("System error: " + e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		try {

			TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Create synchronous response.");
			if (payText.indexOf("<ApplicationError>") == -1) {
				output = new XIMessageRecordImpl(msg.getToParty(), msg.getFromParty(), msg.getToService(), msg.getFromService(), msg.getAction());

				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Retrieve XI message from output: "
						+ output.toString());
				Message response = output.getXIMessage();

				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Create payload of synchronous response: "
						+ response.toString());
				XMLPayload xp = response.createXMLPayload();

				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Fill payload of synchronous response: "
						+ xp.toString());
				xp.setText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response>OK</Response>");
				xp.setName("MainDocument");
				xp.setDescription("XI AF Sample Adapter Sync Response");
				xp.setContentType("application/xml");

				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Set payload of synchronous response.");
				response.setDocument(xp);
				String requestId = msg.getMessageId();
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Set RefToMsgId of synchronous response to: "
						+ requestId);
				response.setRefToMessageId(requestId);

				this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Sync. message was forwarded succesfully to the file system");
			} else {
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryConnect, "Payload contains the <ApplicationError> tag that causes a application error response for testing purposes!");
				this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Simulate application error response now.");

				String[] faultIF = getFaultIF(mc.getChannelID());
				Action action = new Action(faultIF[0], faultIF[1]);
				output = new XIMessageRecordImpl(msg.getToParty(), msg.getFromParty(), msg.getToService(), msg.getFromService(), action);

				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Retrieve XI message from output: "
						+ output.toString());
				Message response = output.getXIMessage();

				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Create payload of synchronous error response: "
						+ response.toString());

				XMLPayload xp = response.createXMLPayload();
				xp.setName("MainDocument");
				xp.setDescription("XI AF Sample Adapter Sync Error Response");
				if (payText.indexOf("ApplicationErrorBinaryPayload") != -1) {
					TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Fill binary payload of synchronous ApplicationError response");
					xp.setContent(new byte[] { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57 });
					xp.setContentType("application/octet-stream");
				} else if (payText.indexOf("ApplicationErrorTextPayload") != -1) {
					TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Fill text payload of synchronous ApplicationError response");
					xp.setText("Error simulated, ApplicationError contains text payload only");
					xp.setContentType("text/plain");
				} else if (payText.indexOf("ApplicationErrorXMLPayloadWithAtt") != -1) {
					TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Fill XML payload of synchronous ApplicationError response with binary attachment");
					xp.setText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Failure><Error>Error simulated, ApplicationError contains XML payload with binary attachment</Error></Failure>");
					xp.setContentType("application/xml");
					Payload p = response.createPayload();
					p.setContent(new byte[] { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57 });
					p.setContentType("application/octet-stream");
					p.setName("Attachment");
					p.setDescription("XI AF Sample Adapter Sync Error Response binary attachment");
					response.addAttachment(p);
				} else {
					TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Fill XML payload of synchronous ApplicationError response");
					xp.setText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Failure><Error>Error simulated, ApplicationError contains XML payload only</Error></Failure>");
					xp.setContentType("application/xml");
				}
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Set payload of synchronous error response.");
				response.setDocument(xp);

				String requestId = msg.getMessageId();
				TRACE.debugT(SIGNATURE, MCAConstants.LogCategoryCONNECT_AF, "Set RefToMsgId of synchronous error response to: "
						+ requestId);
				response.setRefToMessageId(requestId);

				ErrorInfo errorInfo = response.createErrorInfo();
				errorInfo.setAttribute("ErrorCode", "SOME_APP_ERR_CODE");
				errorInfo.setAttribute("ErrorArea", "JCA");
				errorInfo.setAttribute("ErrorCategory", "Application");
				errorInfo.setAttribute("AdditionalErrorText", "MainDocument has contained the <ApplicationError> element that triggers the JCA adapter to create an app error response as demo!");
				errorInfo.setAttribute("ApplicationFaultInterface", faultIF[0]);
				errorInfo.setAttribute("ApplicationFaultInterfaceNamespace", faultIF[1]);
				response.setErrorInfo(errorInfo);
			}
		} catch (Exception e) {
			TRACE.catching(SIGNATURE, e);
			ResourceException re = new ResourceException("System error: " + e.getMessage());
			TRACE.throwing(SIGNATURE, re);
			throw re;
		}
		TRACE.exiting(SIGNATURE, output);
		return output;
	}

	public XIInteractionSpec getXIInteractionSpec() throws NotSupportedException {
		return new XIInteractionSpecImpl();
	}
}
