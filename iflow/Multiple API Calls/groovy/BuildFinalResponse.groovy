import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read stored Exchange Properties
    def orderID = message.getProperty("OrderID")
    def customerID = message.getProperty("CustomerID")
    def customerName = message.getProperty("CustomerName")
    def customerCity = message.getProperty("CustomerCity")

    def productID = message.getProperty("ProductID")
    def productName = message.getProperty("ProductName")
    def price = message.getProperty("Price")

    def quantity = message.getProperty("Quantity")

    // Calculate total amount
    def totalAmount = price * quantity

    // Build final response
    def response = [
        OrderID: orderID,
        Customer: [
            CustomerID: customerID,
            CustomerName: customerName,
            City: customerCity
        ],
        Product: [
            ProductID: productID,
            ProductName: productName,
            Price: price
        ],
        Quantity: quantity,
        TotalAmount: totalAmount
    ]

    // Convert response to JSON
    message.setBody(JsonOutput.prettyPrint(
        JsonOutput.toJson(response)
    ))

    message.setHeader("Content-Type", "application/json")

    return message
}
