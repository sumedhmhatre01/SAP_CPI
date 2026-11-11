import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {

    // Read incoming JSON payload
    def body = message.getBody(String)

    // Parse JSON
    def booking = new JsonSlurper().parseText(body)

    // Extract BookingID
    def bookingId = booking.BookingID?.toString()?.trim()

    // Validate BookingID
    if (!bookingId) {
        throw new IllegalArgumentException(
            "BookingID is missing or empty"
        )
    }

    // Store BookingID as an exchange property
    message.setProperty("BookingID", bookingId)

    return message
}
