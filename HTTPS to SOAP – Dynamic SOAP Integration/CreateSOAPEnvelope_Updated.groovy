import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

def Message processData(Message message) {

    // Read the XML payload produced by the JSON to XML Converter.
    String body = message.getBody(String)

    // Parse the converted XML.
    def xml = new XmlSlurper(false, false).parseText(body)

    // Extract customer details and escape XML special characters.
    String customerID = XmlUtil.escapeXml(xml.CustomerID.text())
    String customerName = XmlUtil.escapeXml(xml.CustomerName.text())
    String city = XmlUtil.escapeXml(xml.City.text())
    String mobile = XmlUtil.escapeXml(xml.Mobile.text())

    // Build the SOAP 1.1 envelope.
    String soapBody = """
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:cus="urn:customer-service">
    <soapenv:Header/>
    <soapenv:Body>
        <cus:CreateCustomerRequest>
            <cus:CustomerID>${customerID}</cus:CustomerID>
            <cus:CustomerName>${customerName}</cus:CustomerName>
            <cus:City>${city}</cus:City>
            <cus:Mobile>${mobile}</cus:Mobile>
        </cus:CreateCustomerRequest>
    </soapenv:Body>
</soapenv:Envelope>
"""

    // Set the message body as a Java String, not a Groovy GString.
    message.setBody(soapBody.toString())

    return message
}
