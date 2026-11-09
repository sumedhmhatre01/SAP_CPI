import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read XML body received from Gather
    def body = message.getBody(String)
    def xml = new XmlSlurper().parseText(body)

    def regions = []
    BigDecimal totalSales = 0

    // Find each regional record, even if Gather adds a wrapper element
    def records = xml.depthFirst().findAll { node ->
        def children = node.children()
        children.any { it.name() == 'Region' } &&
        children.any { it.name() == 'Sales' }
    }

    // Process available regional records
    records.each { record ->

        def region = record.children()
                          .find { it.name() == 'Region' }
                          ?.text()?.trim()

        def salesText = record.children()
                              .find { it.name() == 'Sales' }
                              ?.text()?.trim()

        // Skip incomplete records or invalid sales values
        if (region && salesText) {
            try {
                BigDecimal sales = new BigDecimal(salesText)

                regions.add([
                    Region: region,
                    Sales : sales
                ])

                totalSales += sales
            } catch (NumberFormatException ignored) {
                // Skip records with non-numeric sales values
            }
        }
    }

    // Generate consolidated response
    def response = [
        TotalSales: totalSales,
        Regions   : regions
    ]

    message.setBody(
        JsonOutput.prettyPrint(JsonOutput.toJson(response))
    )
    message.setHeader("Content-Type", "application/json")

    return message
}
