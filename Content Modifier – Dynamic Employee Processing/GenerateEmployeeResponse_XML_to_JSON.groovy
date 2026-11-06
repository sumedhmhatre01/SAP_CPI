import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read the XML payload produced by the JSON to XML Converter
    String body = message.getBody(String)
    def xml = new XmlSlurper().parseText(body)

    // Copy the original employee fields from the XML into a map
    def employee = [:]

    xml.children().each { node ->
        employee[node.name().toString()] = node.text()
    }

    // Preserve Salary as a number when it is numeric
    if (employee.Salary != null &&
        employee.Salary.toString().isNumber()) {
        employee.Salary = new BigDecimal(employee.Salary.toString())
    }

    // Read values assigned by earlier iFlow steps
    String currency = message.getProperty("Currency") as String
    String processingDateTime =
        message.getProperty("ProcessingDateTime") as String

    // Add the dynamic fields
    employee["Currency"] = currency
    employee["ProcessingDateTime"] = processingDateTime

    // Convert the result to a formatted JSON response
    message.setBody(
        JsonOutput.prettyPrint(JsonOutput.toJson(employee))
    )

    return message
}
