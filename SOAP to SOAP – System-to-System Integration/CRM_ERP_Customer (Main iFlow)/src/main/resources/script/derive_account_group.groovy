import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

def Message processData(Message message) {
    // Read the incoming CRM SOAP body.
    String body = message.getBody(String)

    if (body == null || body.trim().isEmpty()) {
        throw new IllegalArgumentException("Incoming SOAP body is empty.")
    }

    // Parse the CustomerRequest XML payload.
    def root = new XmlSlurper(false, false).parseText(body)

    if (root.name() != "CustomerRequest") {
        throw new IllegalArgumentException(
            "Expected CustomerRequest as the root element, but received: ${root.name()}"
        )
    }

    String city = root.City.text()?.trim()

    if (!city) {
        throw new IllegalArgumentException("City is required to derive AccountGroup.")
    }

    // Sample business rule for this assignment.
    String accountGroup = city.equalsIgnoreCase("Hyderabad") ? "DOMESTIC" : "GENERAL"

    // Add AccountGroup to the source XML so Message Mapping can map it.
    root.appendNode {
        AccountGroup(accountGroup)
    }

    message.setBody(XmlUtil.serialize(root))
    return message
}
