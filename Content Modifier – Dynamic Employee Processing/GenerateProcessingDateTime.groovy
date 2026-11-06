import com.sap.gateway.ip.core.customdev.util.Message
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

def Message processData(Message message) {

    // Get current processing date and time in Indian Standard Time
    ZonedDateTime currentDateTime =
        ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))

    // Format date and time
    String processingDateTime =
        currentDateTime.format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        )

    // Store processing date and time as an exchange property
    message.setProperty("ProcessingDateTime", processingDateTime)

    // Return the original message without changing its body
    return message
}
