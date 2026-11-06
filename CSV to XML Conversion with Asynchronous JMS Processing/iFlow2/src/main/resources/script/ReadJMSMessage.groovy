import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    // Read the XML message consumed from the JMS queue
    String body = message.getBody(String)

    // Attach the received XML to the Message Processing Log
    def messageLog = messageLogFactory.getMessageLog(message)

    if (messageLog != null) {
        messageLog.addAttachmentAsString(
            "JMS-Received-XML",
            body,
            "application/xml"
        )
    }

    // Keep the original XML payload unchanged
    return message
}
