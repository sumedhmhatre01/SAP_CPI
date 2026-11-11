# Hotel Booking – Asynchronous Processing with SAP CPI

## Overview

This practical demonstrates asynchronous hotel-booking processing using the SAP Cloud Integration (CPI) Data Store.

Two independent iFlows are used:
- `HotelBookingStorage` receives a booking request and stores it.
- `HotelBookingProcessor` retrieves stored bookings on a timer, processes them, updates their status, and deletes successfully processed entries.

## Business scenario

Hotel booking requests should be accepted and stored immediately, then processed independently at a later time.

### Sample input

```json
{
  "BookingID": "BK1001",
  "HotelCode": "3001",
  "GuestName": "Mahesh Kale",
  "RoomType": "Deluxe",
  "CheckIn": "2026-12-01",
  "CheckOut": "2026-12-05",
  "Amount": 12000,
  "Status": "NEW"
}
```

## Architecture

### 1. HotelBookingStorage

**Purpose:** Receive booking details and store them in the CPI Data Store.

Processing sequence:

1. HTTPS Sender receives the JSON request.
2. Groovy Script `Extract_Booking_ID` parses the JSON, validates `BookingID`, and saves it as an exchange property.
3. JSON to XML Converter converts the payload to XML.
4. Data Store Write `Store_Hotel_Booking` stores the XML in the global Data Store `HotelBookingStore`.
5. The Entry ID is `${property.BookingID}`.

**Data Store Write configuration**

| Setting | Value |
|---|---|
| Data Store Name | `HotelBookingStore` |
| Visibility | Global |
| Entry ID | `${property.BookingID}` |
| Overwrite Existing Message | Unchecked |
| Encrypt Stored Message | Enabled |
| Include Message Headers | Unchecked |

Overwrite is disabled so a duplicate booking ID does not silently replace an existing booking. A duplicate write is expected to fail.

### 2. HotelBookingProcessor

**Purpose:** Retrieve and process stored bookings independently of the incoming request.

Processing sequence:

1. Timer Start Event triggers the iFlow on a recurring schedule (one-minute interval used for practical testing).
2. Data Store Select `Retrieve_Hotel_Bookings` selects entries from `HotelBookingStore`.
3. General Splitter `Split_Booking_Records` separates the selected booking messages.
4. Content Modifier `Extract_Booking_Details` extracts booking properties using XPath.
5. Router `Check_Booking_Status` sends bookings with status `NEW` to the processing route. Other statuses follow the default route.
6. Groovy Script `Process_Hotel_Booking` validates required values and changes the booking status to `PROCESSED`.
7. Data Store Write `Update_Booking_Status` writes the updated booking using the same Entry ID, with overwrite enabled.
8. Data Store Delete `Delete_Processed_Booking` deletes the entry after the update succeeds.
9. An Exception Subprocess captures processing errors.

**Data Store Select and Splitter settings**

| Setting | Value |
|---|---|
| Data Store Name | `HotelBookingStore` |
| Visibility | Global |
| Number of Polled Messages | 10 |
| Delete on Completion | Unchecked |
| Splitter Expression Type | XPath |
| Splitter XPath | `/messages/message` |
| Grouping | 1 |
| Streaming | Enabled |
| Parallel Processing | Disabled |
| Stop on Exception | Enabled |

**Content Modifier exchange properties**

The following XPath expressions are used for the Select message structure:

| Property | Source Type | Source Value |
|---|---|---|
| `BookingAmount` | XPath | `/messages/message/root/Amount` |
| `BookingStatus` | XPath | `/messages/message/root/Status` |
| `HotelCode` | XPath | `/messages/message/root/HotelCode` |
| `GuestName` | XPath | `/messages/message/root/GuestName` |
| `BookingEntryID` | XPath | `/messages/message/@id` |

These expressions are based on the XML structure observed in the Select output. If the actual payload structure changes, inspect the message in Trace and adjust the XPath accordingly.

**Router condition**

- Processing route: `${property.BookingStatus} = 'NEW'`
- Default route: handles messages that do not match the processing condition.

## Groovy processing

The processing script simulates booking processing; it does not call an external hotel reservation system.

For a booking with status `NEW`, it:

1. Reads the XML payload.
2. Locates the individual booking message and its `root` element.
3. Validates the booking ID, guest name, and amount exchange properties.
4. Changes the XML `Status` element to `PROCESSED`.
5. Sets the `ProcessingStatus` exchange property.
6. Places the individual booking XML in the message body for the subsequent Data Store Write.

## Exception handling and recovery

The Exception Subprocess captures processing errors and records the exception message in the `ErrorMessage` exchange property.

Data Store Select is configured with **Delete on Completion unchecked**. The design explicitly updates and deletes an entry only after successful processing.

If processing fails before the explicit Delete operation, the entry should remain available for investigation and a later retry, subject to Data Store retention and expiry settings. Verify the entry and message-processing logs when testing failure scenarios.

Duplicate booking IDs are protected against in the storage iFlow by leaving **Overwrite Existing Message unchecked**. A duplicate write should fail rather than replace the existing booking.

## Testing with Postman

### Request

- Method: `POST`
- URL: Use the deployed HTTPS Sender endpoint from `HotelBookingStorage`.
- Header: `Content-Type: application/json`
- Authorization: Use the authentication configured for the HTTPS endpoint (Basic Auth in this practical setup).

### Sample request body

```json
{
  "BookingID": "BK1001",
  "HotelCode": "3001",
  "GuestName": "Mahesh Kale",
  "RoomType": "Deluxe",
  "CheckIn": "2026-12-01",
  "CheckOut": "2026-12-05",
  "Amount": 12000,
  "Status": "NEW"
}
```

### Verify storage

1. Confirm that the `HotelBookingStorage` message processing run completed successfully.
2. Open **Monitor → Manage Stores → Data Stores**.
3. Select `HotelBookingStore`.
4. Confirm that entry `BK1001` exists.
5. Inspect the stored payload and confirm that it is XML, with `Status` set to `NEW`.

### Verify asynchronous processing

1. Confirm that `HotelBookingProcessor` is deployed and started.
2. Wait for its next timer execution.
3. Open **Monitor → Message Processing** and inspect the latest run.
4. Verify that the booking properties were extracted correctly.
5. Verify that the processing payload contains `Status` set to `PROCESSED`.
6. Verify that `Update_Booking_Status` and `Delete_Processed_Booking` executed.
7. Reopen `HotelBookingStore` and confirm that `BK1001` is no longer present after successful processing.

A completed timer-triggered message alone does not prove that the booking was processed. Check the run steps and the Data Store entry as well.

## Expected result

| Stage | Expected status |
|---|---|
| Booking received | `NEW` |
| Booking stored | `NEW` |
| Booking selected | `NEW` |
| Booking processed | `PROCESSED` |
| Updated entry written | `PROCESSED` |
| Successful processing completed | Entry deleted |

The entry is intentionally deleted after successful processing, so `PROCESSED` is an intermediate stored state rather than a record that remains in the Data Store.

## Monitoring and troubleshooting

### JSON/XML parsing error

If Select reports an XML parsing error such as an unexpected `{` character, the stored entry is likely JSON. Ensure that the JSON to XML Converter executes before Data Store Write, delete the old test entry, and submit the request again.

### Exchange properties are empty

Inspect the message payload at the Content Modifier or Router in Trace. Compare the actual XML structure with the configured XPath expressions. The Select output may include wrapper elements such as `messages` and `message`.

### Groovy status update error

If the script reports a null-object error while accessing `Status`, inspect the actual XML body. The script must locate the booking's `message` wrapper and `root` element before updating the status.

### Entry remains in the Data Store

Inspect the latest `HotelBookingProcessor` run steps. Confirm that the Router selected the `NEW` processing route and that both Data Store Write and Data Store Delete executed. A completed iFlow run does not necessarily mean the booking followed the intended processing route.

## Key SAP CPI concepts demonstrated

- HTTPS Sender Adapter
- Groovy Script
- JSON to XML Converter
- CPI Data Store Write, Select, and Delete
- Global Data Store visibility
- Timer Start Event
- General Splitter
- Content Modifier and exchange properties
- XPath expressions
- Router and default route
- Exception Subprocess
- Trace and Message Processing monitoring
- Asynchronous processing and recovery design

## Notes

- Booking processing in this practical is simulated; no external hotel booking system is called.
- The one-minute timer interval is for testing. Choose a production interval according to business and tenant requirements.
- Data Store retention and expiry settings should follow the required business retention policy.
- Disable Trace after debugging because trace logs can expose message payloads and sensitive information.
