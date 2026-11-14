import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {

    // Read Product API response
    def body = message.getBody(String)

    // Parse JSON response
    def product = new JsonSlurper().parseText(body)

    // Store product details as Exchange Properties
    message.setProperty("ProductID", product.ProductID)
    message.setProperty("ProductName", product.ProductName)
    message.setProperty("Price", product.Price)

    return message
}
