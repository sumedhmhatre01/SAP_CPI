
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlParser
import groovy.xml.XmlUtil

def Message processData(Message message) {

    // Read incoming XML
    def body = message.getBody(String)

    // Parse XML
    def document = new XmlParser().parseText(body)

    // Find individual booking message
    def bookingMessage

    if (document.name() == 'message') {
        bookingMessage = document
    } else {
        bookingMessage = document.depthFirst().find {
            it instanceof Node && it.name() == 'message'
        }
    }

    if (bookingMessage == null) {
        throw new Exception("Booking message element not found")
    }

    // Extract booking XML
    def bookingRoot = bookingMessage.get('root')?.getAt(0)

    if (bookingRoot == null) {
        throw new Exception("Booking root element not found")
    }

    // Extract booking details
    def bookingId = message.getProperty("BookingEntryID")
    def guestName = message.getProperty("GuestName")
    def amount = message.getProperty("BookingAmount")

    // Validate booking information
    if (!bookingId || !guestName || !amount) {
        throw new Exception("Required booking details are missing")
    }

    // Find and update booking status
    def statusNode = bookingRoot.get('Status')?.getAt(0)

    if (statusNode == null) {
        throw new Exception("Booking Status element not found")
    }

    statusNode.setValue("PROCESSED")

    // Store only the individual booking XML
    message.setBody(XmlUtil.serialize(bookingRoot))

    // Set processing result
    message.setProperty("ProcessingStatus", "PROCESSED")

    return message
}
