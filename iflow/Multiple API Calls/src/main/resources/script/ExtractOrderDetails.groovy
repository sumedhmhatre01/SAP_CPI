import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {

    // Read original order JSON
    def body = message.getBody(String)
    def order = new JsonSlurper().parseText(body)

    // Store original order details as exchange properties
    message.setProperty("OrderID", order.OrderID)
    message.setProperty("CustomerID", order.CustomerID)
    message.setProperty("ProductID", order.ProductID)
    message.setProperty("Quantity", order.Quantity)

    return message
}
