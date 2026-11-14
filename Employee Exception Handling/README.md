# SAP CPI Employee Exception Handling -- Email Notifications

A practice project demonstrating how to build an SAP Cloud Integration
(SAP CPI) iFlow that receives employee data in JSON format, converts it
to XML, validates the employee details, and sends an email notification
for successful or failed processing.

## Project Overview

**Integration flow:** `Employee_Exception_Handling`

**Objective:** - Receive employee data through an HTTPS sender. -
Convert the incoming JSON payload into XML. - Validate required employee
fields. - Send a success email when validation and processing succeed. -
Catch processing exceptions in an Exception Subprocess and send an error
email. - Preserve failed status in SAP CPI monitoring when an error
occurs.

**Approach:** Use standard SAP CPI components. Groovy is not required
for the planned validation approach.

## High-Level iFlow Design

### Main Integration Process

``` text
HTTPS Sender
    |
JSON to XML Converter
    |
Router – Employee Validation
    |
    +-- Valid route
    |      |
    |   Prepare_Success_Email (Content Modifier)
    |      |
    |   Send step → Gmail Mail Receiver
    |      |
    |   End Message
    |
    +-- Invalid/default route
           |
        Error End Event
```

### Exception Subprocess

``` text
Exception Subprocess: Handle_Employee_Exception

Error Start Event
    |
Prepare_Error_Email (Content Modifier)
    |
Send step → Gmail Error Mail Receiver
    |
Error End Event
```

The Exception Subprocess is intended to catch exceptions raised during
the main process, including failures from external receiver calls. The
Error End Event keeps the message outcome as failed in monitoring after
the error notification is sent.

## Components Used

  -----------------------------------------------------------------------
  Component                           Purpose
  ----------------------------------- -----------------------------------
  HTTPS Sender Adapter                Receives the employee JSON request

  JSON to XML Converter               Converts the JSON request body into
                                      XML

  Router                              Routes valid employee data to the
                                      success path and invalid data to
                                      the default/error path

  Content Modifier --                 Prepares the success email subject
  `Prepare_Success_Email`             and body

  Send step + Mail Receiver           Sends the success notification
                                      through Gmail SMTP

  Error End Event                     Raises an exception for invalid
                                      employee data

  Exception Subprocess --             Handles exceptions from the main
  `Handle_Employee_Exception`         process

  Content Modifier --                 Prepares the error notification
  `Prepare_Error_Email`               

  Send step + Mail Receiver           Sends the error notification
                                      through Gmail SMTP

  Error End Event in Exception        Ends the exception handling while
  Subprocess                          retaining failed status
  -----------------------------------------------------------------------

## Sample Employee Payload

Send this JSON from Postman:

``` json
{
  "employeeId": "EMP1001",
  "employeeName": "Rahul Sharma",
  "department": "IT",
  "designation": "Software Developer",
  "email": "rahul@example.com",
  "salary": 50000,
  "location": "Mumbai"
}
```

### Validation Rules

The Router's valid route is intended to require:

-   Employee ID is not empty.
-   Employee Name is not empty.
-   Salary is greater than zero.

Example XPath condition used during configuration:

``` xpath
/Employee[normalize-space(employeeId) != '' and normalize-space(employeeName) != '' and number(salary) > 0]
```

**Important:** Confirm the actual XML produced by the JSON to XML
Converter before relying on this XPath. The XML root element and element
names must match the XPath exactly. If the generated XML structure
differs, adjust the Router condition accordingly.

The default route is intended to handle data that does not satisfy the
valid-route condition.

## Email Notifications

### Success Email

**Suggested subject:**

``` text
SAP CPI - Employee Processed Successfully
```

**Body configured:**

``` text
Hello Team,

Employee data has been processed successfully.

Status: SUCCESS

Regards,
SAP CPI Integration Team
```

The initial dynamic XPath placeholders for employee ID and employee name
were removed after deployment failed because the expression was not
accepted in the Content Modifier configuration.

### Error Email

**Suggested subject:**

``` text
SAP CPI - Employee Processing Failed
```

**Body configured:**

``` text
Hello Team,

An error occurred while processing employee data.

Status: FAILED

Error Details: ${exception.message}

Please check SAP CPI Message Processing Log
for further details.

Regards,
SAP CPI Integration Team
```

**Known item to verify:** In one test, the received email displayed
`${exception.message}` literally instead of the actual exception text.
Verify the Content Modifier body type and the supported
exception-expression syntax for the current CPI configuration/runtime
before considering dynamic error details complete.

## Gmail SMTP Configuration

Both the success and error Mail Receiver Adapters can use the same Gmail
SMTP account and the same deployed CPI User Credentials security
artifact.

  Setting              Intended value
  -------------------- --------------------------
  SMTP address         `smtp.gmail.com:587`
  Proxy Type           Internet
  Transport protocol   SMTP
  Authentication       Plain User Name/Password
  Credential Name      `GMAIL_SMTP_CREDENTIALS`
  Protection           STARTTLS Mandatory
  Body MIME type       Text/Plain
  Body encoding        UTF-8

Use a Gmail App Password stored in the CPI User Credentials security
artifact. Do not use the normal Gmail account password. A second Mail
Receiver using the same Gmail account does not require a new App
Password if it references the same credential artifact.

**Troubleshooting note from testing:** The CPI MPL showed a connection
timeout to `smtp.gmail.com` on port `857`. Verify the Address field in
both Mail Receiver adapters and ensure it is exactly
`smtp.gmail.com:587`. STARTTLS Mandatory is a separate setting and
should remain enabled for this configuration.

## Postman Test Procedure

1.  Deploy the iFlow.

2.  Copy the deployed HTTPS endpoint from the iFlow details/monitoring
    view.

3.  In Postman, create a `POST` request to that endpoint.

4.  Set the header:

    ``` text
    Content-Type: application/json
    ```

5.  Select **Body → raw → JSON**.

6.  Send the valid sample payload above.

7.  Check the CPI Message Processing Log (MPL) and the receiving Gmail
    inbox.

For the HTTPS sender, use the authentication method configured on the
adapter. The exact Postman Authorization settings depend on that
configuration.

### Test Case 1 -- Valid Employee

Use the sample payload with: - `employeeId`: `EMP1001` - `employeeName`:
`Rahul Sharma` - `salary`: `50000`

Expected behavior: - JSON is converted to XML. - Router selects the
valid route, provided the XPath matches the actual XML. - Success email
is sent. - Main process reaches End Message. - MPL should show
successful completion if all steps complete.

### Test Case 2 -- Invalid Employee

Example invalid payload:

``` json
{
  "employeeId": "",
  "employeeName": "Rahul Sharma",
  "department": "IT",
  "designation": "Software Developer",
  "email": "rahul@example.com",
  "salary": -5000,
  "location": "Mumbai"
}
```

Expected behavior: - Router does not select the valid route. -
Default/invalid route reaches Error End Event. - Exception Subprocess is
triggered. - Error notification is sent. - Exception Subprocess reaches
Error End Event. - MPL should retain a failed outcome.

## Monitoring and Troubleshooting

Use **Monitor → Integrations and APIs → Message Processing** (menu
labels may vary by tenant) to inspect the message.

Open the relevant message and review the processing steps. Identify the
exact failed step before changing the iFlow.

### SMTP connection timeout

Example MPL error observed:

``` text
com.sun.mail.util.MailConnectException:
Couldn't connect to host, port: smtp.gmail.com, 857;
timeout 30000;
nested exception is:
java.net.SocketTimeoutException: Connect timed out
```

Actions: - Verify the SMTP address in both Mail Receiver adapters. -
Confirm the port is `587`, not `857`. - Confirm Proxy Type is
Internet. - Confirm STARTTLS Mandatory is selected. - Confirm the
deployed credential artifact name is correct. - Check the failing
Mail/Send step in MPL.

### Error email contains `${exception.message}` literally

If the placeholder appears unchanged: - Check the Content Modifier
Message Body type. - Confirm the expression is supported in that exact
field and runtime. - Inspect the exception subprocess MPL details to
identify the original exception. - Do not assume the employee payload is
invalid just because an error email arrived; the error may be from an
outbound SMTP connection.

### Postman Cloud Agent times out

A Postman Cloud Agent timeout after 30 seconds does not by itself prove
that CPI failed. Check the CPI MPL before resending to avoid duplicate
processing or duplicate emails. If appropriate, use Postman Desktop with
the Desktop Agent.

## Current Project Status

The iFlow was deployed successfully after removing unsupported XPath
placeholders from the success email body.

During testing: - The error notification email was received. - The MPL
identified an SMTP connection timeout. - The error details showed an
attempted connection to `smtp.gmail.com` on port `857`. - The intended
correction is to verify and set the SMTP address to `smtp.gmail.com:587`
in **both** Mail Receiver adapters. - The `${exception.message}`
placeholder was observed literally in one received error email and still
requires verification.

Therefore, the project is **configured and deployed, but end-to-end
success and failure tests are not yet confirmed complete**.

## Security Notes

-   Never commit Gmail App Passwords, CPI credential values, tenant URLs
    containing secrets, or personal employee data to GitHub.
-   Keep credentials in SAP CPI Security Material/User Credentials.
-   Use dummy employee data for practice.
-   Do not include real salary or personal information in public test
    payloads.
