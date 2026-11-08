import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

def Message processData(Message message) {

    // Read incoming booking JSON
    def body = message.getBody(String)
    def booking = new JsonSlurper().parseText(body)

    // Calculate 18% tax
    BigDecimal amount = new BigDecimal(booking.Amount.toString())
    BigDecimal tax = amount.multiply(0.18G)

    // Prepare Finance branch result as XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.FinanceResult {
        BookingID(booking.BookingID)
        GuestName(booking.GuestName)
        Tax(tax.stripTrailingZeros().toPlainString())
    }

    message.setBody(writer.toString())

    return message
}
