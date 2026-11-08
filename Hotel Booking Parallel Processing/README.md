# SAP CPI – Hotel Booking Parallel Processing

## Project Overview

This SAP Cloud Integration (CPI) iFlow demonstrates **Parallel Multicast, Join, and Gather** using a hotel booking scenario.

An incoming hotel booking JSON message is sent to three independent processing branches. Each branch performs a separate task, and the results are collected to prepare a final JSON response.

The three branches simulate:

1. **Finance** – calculates 18% tax on the booking amount.
2. **Hotel Management** – simulates a room availability check.
3. **CRM** – simulates customer creation.

The practical uses Groovy scripts to process each branch and prepare the final response. No external Finance, Hotel, or CRM systems are connected; their responses are simulated.

## Objectives

- Receive a booking request through an HTTPS sender.
- Execute three independent branches using Parallel Multicast.
- Process each branch independently.
- Synchronize branch completion using Join.
- Collect branch messages using Gather.
- Prepare a consolidated JSON response.
- Test the deployed iFlow using Postman.

## Technologies and Components

- SAP Integration Suite – Cloud Integration
- HTTPS Sender Adapter
- Parallel Multicast
- Groovy Script
- Join
- Gather
- Content Modifier
- End Message
- Postman

## iFlow Architecture

```text
HTTPS Sender
     |
   Start
     |
Parallel Multicast
     |
     +----------------------+----------------------+
     |                      |                      |
 Finance Branch        Hotel Branch             CRM Branch
     |                      |                      |
 FinanceTax.groovy   HotelAvailability.groovy  CRMCustomer.groovy
     |                      |                      |
     +----------------------+----------------------+
                            |
                    Join_Parallel_Branches
                            |
                    Gather_Branch_Results
                            |
                  PrepareFinalResponse.groovy
                            |
                    Set_Response_Headers
                     (Content Modifier)
                            |
                       End Message
```

## Input JSON

The iFlow receives a booking request in JSON format.

```json
{
  "BookingID": "BK1001",
  "GuestName": "Rahul",
  "HotelCode": "3001",
  "Amount": 12000
}
```

### Input fields

| Field | Description |
|---|---|
| `BookingID` | Unique booking reference |
| `GuestName` | Name of the guest |
| `HotelCode` | Hotel identifier |
| `Amount` | Booking amount used for tax calculation |

## Parallel Branch Processing

### Branch 1 – Finance

**Groovy script:** `FinanceTax.groovy`  
**iFlow step:** `Finance_Calculate_Tax`

The Finance branch reads the original booking JSON and calculates tax at 18%.

Formula:

```text
Tax = Amount × 18 / 100
Tax = 12000 × 18 / 100
Tax = 2160
```

It prepares an XML result:

```xml
<FinanceResult>
    <BookingID>BK1001</BookingID>
    <GuestName>Rahul</GuestName>
    <Tax>2160</Tax>
</FinanceResult>
```

### Branch 2 – Hotel Management

**Groovy script:** `HotelAvailability.groovy`  
**iFlow step:** `Hotel_Verify_Availability`

The Hotel branch reads the booking details and simulates a room availability check. For this practical, the status is hardcoded as `AVAILABLE`.

It prepares:

```xml
<HotelResult>
    <BookingID>BK1001</BookingID>
    <RoomStatus>AVAILABLE</RoomStatus>
</HotelResult>
```

> This is a simulated availability check. It does not query a real hotel inventory system.

### Branch 3 – CRM

**Groovy script:** `CRMCustomer.groovy`  
**iFlow step:** `CRM_Create_Customer`

The CRM branch reads the guest details and simulates customer creation. For this practical, the status is hardcoded as `CREATED`.

It prepares:

```xml
<CRMResult>
    <BookingID>BK1001</BookingID>
    <CustomerName>Rahul</CustomerName>
    <CRMStatus>CREATED</CRMStatus>
</CRMResult>
```

> This is a simulated CRM operation. It does not create a customer in an external CRM system.

## Join and Gather

### Join – `Join_Parallel_Branches`

Join synchronizes the parallel processing branches. It waits for the branches to complete before processing continues.

**Important:** Join is used for synchronization; it does not itself combine the branch payloads into one business response.

### Gather – `Gather_Branch_Results`

Gather collects the messages from the branches after synchronization. Its aggregation configuration determines the structure of the gathered payload.

The final response script expects the gathered XML to contain the `FinanceResult`, `HotelResult`, and `CRMResult` elements. Verify the actual Gather output in CPI if the flow is changed or behaves differently.

## Final Response Preparation

**Groovy script:** `PrepareFinalResponse.groovy`  
**iFlow step:** `Prepare_Final_Response`

This script:

1. Reads the gathered XML.
2. Locates the Finance, Hotel, and CRM result elements.
3. Extracts the required values.
4. Creates a consolidated JSON object.
5. Sets the message body to the formatted JSON response.

Expected response:

```json
{
  "BookingID": "BK1001",
  "GuestName": "Rahul",
  "Tax": 2160,
  "RoomStatus": "AVAILABLE",
  "CRMStatus": "CREATED"
}
```

## Response Header

A Content Modifier named `Set_Response_Headers` is used to set the HTTP response header:

| Header name | Value |
|---|---|
| `Content-Type` | `application/json` |

The End Message event completes the synchronous request-response processing.

## Postman Testing

### Request configuration

| Setting | Value |
|---|---|
| Method | `POST` |
| URL | Deployed iFlow HTTPS endpoint |
| Authorization | Use the authentication configured for the endpoint |
| Body | `raw` |
| Body format | `JSON` |

Send the input JSON shown above.

### Actual successful test response

The iFlow was tested successfully using Postman. The actual response received during the test was:

```json
{
  "BookingID": "BK1001",
  "GuestName": "SSM",
  "Tax": 2160,
  "RoomStatus": "AVAILABLE",
  "CRMStatus": "CREATED"
}
```

The guest name was `SSM` in the actual test request. The expected example earlier in this README uses `Rahul`.

The response confirms that the Finance, Hotel, and CRM branch results were present in the final JSON response.

## Project Structure in CPI

The iFlow contains the following logical steps and script resources:

```text
HotelBooking_ParallelProcessing
|
+-- HTTPS_HotelBooking
|
+-- Parallel_Multicast
|   |
|   +-- Finance_Calculate_Tax
|   |   +-- FinanceTax.groovy
|   |
|   +-- Hotel_Verify_Availability
|   |   +-- HotelAvailability.groovy
|   |
|   +-- CRM_Create_Customer
|       +-- CRMCustomer.groovy
|
+-- Join_Parallel_Branches
|
+-- Gather_Branch_Results
|
+-- Prepare_Final_Response
|   +-- PrepareFinalResponse.groovy
|
+-- Set_Response_Headers
|
+-- End Message
```

## Key Learning Outcomes

| CPI concept | What it does in this practical |
|---|---|
| HTTPS Sender | Receives the booking request |
| Parallel Multicast | Sends the same message to multiple branches for parallel processing |
| Groovy Script | Performs branch-specific processing and creates the final response |
| Join | Synchronizes completion of parallel branches |
| Gather | Collects branch messages according to its aggregation configuration |
| Content Modifier | Sets the response `Content-Type` header |
| End Message | Completes the synchronous request-response flow |

## Notes and Limitations

- Finance tax calculation uses a fixed rate of 18%.
- Hotel availability is simulated with a fixed `AVAILABLE` status.
- CRM customer creation is simulated with a fixed `CREATED` status.
- No external systems or databases are connected.
- The final response script depends on the XML structure produced by the configured Gather step. Inspect the Gather output in the CPI message processing log when troubleshooting.
- Configure endpoint authentication according to the security policy of your SAP CPI tenant.
- After testing, the iFlow can be undeployed if it is not required to remain active.

## Conclusion

This practical demonstrates how SAP CPI can process one incoming message through multiple parallel branches, synchronize the branch execution, gather their results, and return a consolidated JSON response.

It provides hands-on experience with **Parallel Multicast, Join, Gather, Groovy scripting, Content Modifier, and HTTPS request-response processing**.
