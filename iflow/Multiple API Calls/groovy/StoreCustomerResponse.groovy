import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {

    // Read Customer API response
    def body = message.getBody(String)

    // Parse JSON response
    def customer = new JsonSlurper().parseText(body)

    // Store customer details as Exchange Properties
    message.setProperty("CustomerID", customer.CustomerID)
    message.setProperty("CustomerName", customer.CustomerName)
    message.setProperty("CustomerCity", customer.City)

    return message
}
