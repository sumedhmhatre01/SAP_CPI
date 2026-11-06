import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Ensures HotelBookings.Booking is always a JSON array.
 *
 * Expected input: JSON produced by the SAP CPI XML-to-JSON Converter.
 * - If Booking is a single JSON object, wrap it in a one-item list.
 * - If Booking is already a list, leave it unchanged.
 * - If Booking is missing or null, use an empty list.
 *
 * Other fields and their values (including Amount as a string) are preserved.
 */
Message processData(Message message) {
    String body = message.getBody(String)

    if (body == null || body.trim().isEmpty()) {
        throw new IllegalArgumentException("The message body is empty; expected JSON from the XML-to-JSON Converter.")
    }

    def parsedJson = new JsonSlurper().parseText(body)

    if (!(parsedJson instanceof Map)) {
        throw new IllegalArgumentException("Expected a JSON object at the root of the message.")
    }

    def hotelBookings = parsedJson.get("HotelBookings")

    if (!(hotelBookings instanceof Map)) {
        throw new IllegalArgumentException("Expected the JSON object 'HotelBookings' in the message body.")
    }

    def booking = hotelBookings.get("Booking")

    if (booking instanceof List) {
        // Multiple bookings are already represented as an array.
        // No change is required.
    } else if (booking instanceof Map) {
        // A single booking object is wrapped in an array.
        hotelBookings.put("Booking", [booking])
    } else if (booking == null) {
        // Keep the structure consistent when Booking is absent or null.
        hotelBookings.put("Booking", [])
    } else {
        throw new IllegalArgumentException("Unexpected type for 'HotelBookings.Booking'; expected an object, array, or null.")
    }

    message.setBody(JsonOutput.toJson(parsedJson))
    message.setHeader("Content-Type", "application/json")

    return message
}
