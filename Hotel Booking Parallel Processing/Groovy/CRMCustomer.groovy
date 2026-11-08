import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

def Message processData(Message message) {

    // Read incoming booking JSON
    def body = message.getBody(String)
    def booking = new JsonSlurper().parseText(body)

    // Simulate customer creation in CRM
    String crmStatus = "CREATED"

    // Prepare CRM branch result as XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.CRMResult {
        BookingID(booking.BookingID)
        CustomerName(booking.GuestName)
        CRMStatus(crmStatus)
    }

    message.setBody(writer.toString())

    return message
}
