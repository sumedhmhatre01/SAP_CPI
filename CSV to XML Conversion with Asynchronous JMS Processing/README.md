# CSV to XML Conversion with Asynchronous JMS Processing – SAP CPI

## Project Overview

This project demonstrates how to convert CSV data into XML in **SAP Cloud Integration (SAP CPI)** and transfer the converted message asynchronously using a JMS queue.

The solution uses two integration flows:

1. **`CSV_TO_XML_ASYNC`** – Producer iFlow: creates CSV data, converts it to XML, verifies the converted structure, and sends the XML message to a JMS queue.
2. **`JMS_XML_CONSUMER`** – Consumer iFlow: receives the message from the JMS queue and records the received XML in the Message Processing Log (MPL).

The two iFlows communicate through the same JMS queue: `CSV_XML_QUEUE`.

## Architecture

```text
                    iFlow 1 – Producer
                    CSV_TO_XML_ASYNC

Timer Start
    |
    v
Content Modifier
(CSV message body)
    |
    v
CSV to XML Converter
    |
    v
Groovy Script
(Verify XML and record summary in MPL)
    |
    v
JMS Receiver
    |
    v
+--------------------------+
| JMS Queue                |
| CSV_XML_QUEUE            |
+--------------------------+
    |
    v
                    iFlow 2 – Consumer
                    JMS_XML_CONSUMER

JMS Sender
(Message Start Event)
    |
    v
Groovy Script
(Attach received XML to MPL)
    |
    v
End Event
```

## iFlow 1: CSV_TO_XML_ASYNC

### Purpose

This iFlow generates CSV data internally, converts it into XML, performs a basic verification, and sends the XML message to the JMS queue.

### Processing steps

1. **Timer Start Event**
   - Starts the producer iFlow according to its configured schedule.
   - It is not an on-demand HTTP or Postman trigger.

2. **Content Modifier**
   - Supplies the CSV content in the Message Body.
   - The sample data uses a semicolon (`;`) as the field separator.

3. **CSV to XML Converter**
   - Converts the CSV records into XML using the configured XSD.
   - The converter is configured to use the semicolon field separator and to exclude the first CSV header row from the data records.
   - The XSD defines the XML root and repeating record structure.

4. **Groovy Script – `VerifyXML.groovy`**
   - Reads the converted XML payload.
   - Parses the XML and counts the `USER` records.
   - Adds a verification summary as an attachment to the Message Processing Log (MPL), when the log is available.
   - Leaves the original XML message body unchanged.

5. **JMS Receiver**
   - Sends the converted XML message to the JMS queue named `CSV_XML_QUEUE`.
   - This queue is the hand-off point between the producer and consumer iFlows.

### Sample CSV input

The CSV headers must match the configured XSD element names when header-to-XSD matching is enabled.

```csv
USERNAME;IDENTIFIER;FIRSTNAME;LASTNAME
booker12;9012;Rachel;Booker
grey07;2070;Laura;Grey
johnson81;4081;Craig;Johnson
jenkins46;9346;Mary;Jenkins
smith79;5079;Jamie;Smith
```

### XSD structure

The XSD defines the following structure:

```xml
<USERS>
    <USER>
        <USERNAME>booker12</USERNAME>
        <IDENTIFIER>9012</IDENTIFIER>
        <FIRSTNAME>Rachel</FIRSTNAME>
        <LASTNAME>Booker</LASTNAME>
    </USER>
</USERS>
```

`USER` is a repeating element, allowing multiple CSV records to be represented in the XML document.

## iFlow 2: JMS_XML_CONSUMER

### Purpose

This iFlow consumes XML messages placed in the JMS queue by the producer iFlow.

### Processing steps

1. **JMS Sender Adapter / Message Start Event**
   - Listens to the queue `CSV_XML_QUEUE`.
   - A message arriving in the queue triggers the consumer iFlow.
   - No Timer Start Event or Content Modifier is required in this iFlow.

2. **Groovy Script – `ReadJMSMessage.groovy`**
   - Reads the XML message received from JMS.
   - Adds the received XML as an attachment named `JMS-Received-XML` to the Message Processing Log (MPL), when the log is available.
   - Does not change the message body.

3. **End Event**
   - Completes processing of the consumed message.

## JMS Configuration

Both iFlows must use the same queue name:

```text
CSV_XML_QUEUE
```

- The **JMS Receiver** in `CSV_TO_XML_ASYNC` sends messages to this queue.
- The **JMS Sender** in `JMS_XML_CONSUMER` consumes messages from this queue.

JMS provides asynchronous decoupling: the producer places a message on the queue, and the consumer processes it independently when the message is available and the consumer is active.

## Expected result

For the sample input containing five records:

- The CSV to XML Converter produces an XML document with one `USERS` root element and five `USER` records.
- The producer's MPL verification attachment reports the root element and record count.
- The consumer's MPL attachment `JMS-Received-XML` contains the XML message received from JMS.

Example verification summary:

```text
CSV to XML Conversion Successful
Root Element: USERS
Total Users: 5
```

## Deployment and testing

Recommended test sequence:

1. Save and deploy `JMS_XML_CONSUMER`.
2. Confirm that the consumer iFlow is started.
3. Save and deploy `CSV_TO_XML_ASYNC` if its configuration has changed.
4. Allow the producer's Timer to execute.
5. Open **Monitor → Integrations → Monitor Message Processing**.
6. Check the producer execution and its `CSV-to-XML-Verification` attachment.
7. Check the consumer execution and open its `JMS-Received-XML` attachment.
8. Confirm that the received XML contains all five records.

### Avoiding duplicate test messages

The producer uses a Timer. If the Timer is recurring, every scheduled execution can create another JMS message. After the required test, stop or undeploy the producer iFlow if no further scheduled messages are needed.

The consumer can remain deployed to receive future messages. It does not create messages by itself; it processes messages delivered to its JMS queue.

## Technologies and SAP CPI components

- SAP Cloud Integration (SAP CPI)
- Integration Flow (iFlow)
- Timer Start Event
- Content Modifier
- CSV to XML Converter
- XML Schema Definition (XSD)
- Groovy Script
- JMS Receiver Adapter
- JMS Sender Adapter
- JMS Queue
- Message Processing Log (MPL)

## Project learning outcomes

This exercise demonstrates:

- Supplying message content inside an iFlow using Content Modifier.
- Converting delimited CSV records into XML using an XSD.
- Matching CSV headers with XML schema elements.
- Using Groovy scripts to inspect payloads and add MPL attachments.
- Sending messages to a JMS queue with a JMS Receiver.
- Consuming queued messages through a JMS Sender in a separate iFlow.
- Understanding producer-consumer communication and asynchronous processing in SAP CPI.

## Notes

- The sample CSV uses semicolon-separated fields. The converter's field separator must therefore be configured as `;`.
- CSV header names and XSD element names must match according to the converter's header-matching configuration.
- The sample XML shown above illustrates the structure; the actual message contains all five records.
- Message Processing Log attachments are available only when message logging is enabled/available and the script obtains a non-null message log.
- Queue retention, expiration, retry, and dead-letter behavior depend on the JMS adapter settings configured in the SAP CPI tenant.
