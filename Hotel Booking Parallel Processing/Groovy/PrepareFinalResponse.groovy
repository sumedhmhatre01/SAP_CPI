import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read gathered XML
    def body = message.getBody(String)
    def root = new XmlSlurper(false, false).parseText(body)

    // Find individual branch results
    def finance = root.depthFirst().find {
        it.name() == 'FinanceResult'
    }

    def hotel = root.depthFirst().find {
        it.name() == 'HotelResult'
    }

    def crm = root.depthFirst().find {
        it.name() == 'CRMResult'
    }

    // Prepare final JSON
    def result = [
        BookingID : finance.BookingID.text(),
        GuestName : finance.GuestName.text(),
        Tax       : finance.Tax.text().toBigDecimal(),
        RoomStatus: hotel.RoomStatus.text(),
        CRMStatus : crm.CRMStatus.text()
    ]

    // Convert to JSON
    message.setBody(JsonOutput.prettyPrint(
        JsonOutput.toJson(result)
    ))

    return message
}
