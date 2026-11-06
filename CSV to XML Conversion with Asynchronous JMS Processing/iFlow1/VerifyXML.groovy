import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper

def Message processData(Message message) {

    // Read the XML produced by the CSV to XML Converter
    String body = message.getBody(String)

    // Parse the XML and count User elements
    def xml = new XmlSlurper(false, false).parseText(body)
    int userCount = xml.User.size()

    // Create a verification summary
    String result = """CSV to XML Conversion Successful
Root Element: ${xml.name()}
Total Users: ${userCount}
"""

    // Attach the summary to the Message Processing Log (MPL)
    def messageLog = messageLogFactory.getMessageLog(message)
    if (messageLog != null) {
        messageLog.addAttachmentAsString(
            "CSV-to-XML-Verification",
            result,
            "text/plain"
        )
    }

    // Leave the original XML payload unchanged for the JMS receiver
    return message
}
