import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

def Message processData(Message message) {

    // Read incoming booking JSON
    def body = message.getBody(String)
    def booking = new JsonSlurper().parseText(body)

    // Simulate room availability verification
    String roomStatus = "AVAILABLE"

    // Prepare Hotel branch result as XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.HotelResult {
        BookingID(booking.BookingID)
        RoomStatus(roomStatus)
    }

    message.setBody(writer.toString())

    return message
}
