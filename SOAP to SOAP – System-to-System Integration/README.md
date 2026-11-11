# CRM to ERP Customer Integration – SAP CPI

## Overview

This project demonstrates a SOAP-to-SOAP system-to-system integration using SAP Integration Suite, Cloud Integration (SAP CPI).

The business scenario transfers customer details from a CRM system to an ERP system. SAP CPI receives the CRM SOAP request, derives an account group, maps the message into the ERP format, invokes a SOAP service, processes the ERP response, and returns the response to the original caller.

A second iFlow acts as a mock ERP SOAP service so the integration can be practised without a real ERP system.

## Business Scenario

**CRM → SAP CPI → Mock ERP SOAP Service**

Source customer information:

- Customer ID: `C1001`
- Customer Name: `Rahul Sharma`
- City: `Hyderabad`

The integration derives the account group using the sample business rule below.

| City | Account Group |
|---|---|
| Hyderabad | DOMESTIC |
| Any other city | GENERAL |

This is an example rule for this practice assignment. Replace it with the actual business rules required by a real implementation.

## Integration Architecture

Two iFlows are used:

### 1. CRM_ERP_CUSTOMER

The main integration flow performs the following steps:

1. Receives a SOAP request from CRM using a SOAP Sender Adapter.
2. Executes the `Derive AccountGroup` Groovy Script.
3. Maps the CRM message to the ERP message using Message Mapping.
4. Uses Request Reply to call the ERP SOAP service.
5. Receives the ERP response.
6. Returns the response to the original SOAP caller.

### 2. MOCK_ERP_SOAP_SERVICE

The mock ERP flow:

1. Exposes a SOAP endpoint using a SOAP Sender Adapter.
2. Receives the ERP customer creation request.
3. Uses a Content Modifier to return a fixed simulated ERP response.

The mock service currently returns a fixed success response. It does not persist customer data or perform an actual ERP customer creation.

## Message Transformation

### CRM Source Payload

The SOAP Body contains:

```xml
<CustomerRequest>
    <CustomerID>C1001</CustomerID>
    <CustomerName>Rahul Sharma</CustomerName>
    <City>Hyderabad</City>
</CustomerRequest>
```

### Payload After Groovy Script

The Groovy Script adds the `AccountGroup` element:

```xml
<CustomerRequest>
    <CustomerID>C1001</CustomerID>
    <CustomerName>Rahul Sharma</CustomerName>
    <City>Hyderabad</City>
    <AccountGroup>DOMESTIC</AccountGroup>
</CustomerRequest>
```

### ERP Target Payload

Message Mapping transforms the source fields as follows:

| CRM Source Field | ERP Target Field |
|---|---|
| `CustomerID` | `BusinessPartner` |
| `CustomerName` | `Name` |
| `City` | `Location` |
| `AccountGroup` | `AccountGroup` |

Expected mapped payload:

```xml
<ERPCreateCustomer>
    <BusinessPartner>C1001</BusinessPartner>
    <Name>Rahul Sharma</Name>
    <Location>Hyderabad</Location>
    <AccountGroup>DOMESTIC</AccountGroup>
</ERPCreateCustomer>
```

## Mock ERP Response

The mock ERP iFlow returns this response:

```xml
<ERPCreateCustomerResponse>
    <Status>SUCCESS</Status>
    <Message>Customer created successfully in ERP</Message>
    <ERPReference>ERP10001</ERPReference>
</ERPCreateCustomerResponse>
```

This is a simulated response and is returned regardless of the request contents in the current mock implementation.

## SAP CPI Artifacts

| Artifact | Purpose |
|---|---|
| `CRM_ERP_CUSTOMER` | Main CRM-to-ERP integration flow |
| `MOCK_ERP_SOAP_SERVICE` | Mock ERP SOAP endpoint |
| `Derive AccountGroup` | Groovy Script step |
| `CustomerRequest.xsd` | Source message structure |
| `ERPCreateCustomer.xsd` | Target message structure |
| `MOCK_ERP_CREDENTIALS` | User Credentials security artifact used by the SOAP receiver |

### Message Mapping XSD Structures

**Source:** `CustomerRequest.xsd`

Contains:

- `CustomerID`
- `CustomerName`
- `City`
- `AccountGroup`

**Target:** `ERPCreateCustomer.xsd`

Contains:

- `BusinessPartner`
- `Name`
- `Location`
- `AccountGroup`

The source XSD includes `AccountGroup` because the Groovy Script adds this element before Message Mapping runs.

## Groovy Script

The `Derive AccountGroup` script:

1. Reads the incoming XML body.
2. Parses the `CustomerRequest` payload.
3. Validates that the root element and City are present.
4. Sets `DOMESTIC` when City is Hyderabad (case-insensitive).
5. Sets `GENERAL` for other cities.
6. Adds `AccountGroup` to the XML.
7. Passes the updated payload to Message Mapping.

The script is included separately as `derive_account_group.groovy`.

## SOAP Adapter Configuration

### Main iFlow – SOAP Sender

The main iFlow receives the CRM request through a SOAP Sender Adapter.

The sender endpoint address configured in the iFlow is:

```text
/crm/customer
```

Use the complete deployed endpoint URL shown in the SAP CPI monitor when calling the iFlow.

### Mock ERP iFlow – SOAP Sender

The mock ERP SOAP Sender endpoint address is:

```text
/mock/erp/customer
```

The deployed endpoint URL is tenant-specific. Obtain the current URL from the endpoint details in SAP CPI Monitor rather than relying on a URL copied from another tenant.

The mock sender is protected by User Role authentication.

### Main iFlow – SOAP Receiver

The SOAP Receiver Adapter calls the mock ERP endpoint with:

- Proxy Type: Internet
- Authentication: Basic
- Credential Name: `MOCK_ERP_CREDENTIALS`
- Timeout: `60000` milliseconds
- SOAP Version: 1.1

The WSDL imported for the receiver contains the selected service, port, and operation. In the practice configuration, the selected operation is `invoke` and the selected port uses SOAP 1.1.

## Security Configuration

The receiver uses a CPI User Credentials security artifact named:

```text
MOCK_ERP_CREDENTIALS
```

It was populated using the `clientid` and `clientsecret` from an appropriate SAP BTP Integration Flow service key. The service key must include the `ESBMessaging.send` role.

**Security notes:**

- Never commit client secrets, passwords, access tokens, or private keys to GitHub.
- Do not include service key JSON files in the repository.
- Keep credentials in SAP CPI Security Material.
- Do not paste secrets into screenshots, source files, or documentation.
- Use separate credentials and appropriate permissions for production systems.

## Testing with Postman

After both iFlows are deployed and started:

1. Copy the main iFlow SOAP Sender endpoint URL from Monitor.
2. Create a POST request in Postman.
3. Use the authentication method configured on the main SOAP Sender.
4. Set the request body to raw XML.
5. Set the `Content-Type` header to `text/xml; charset=UTF-8` for SOAP 1.1.
6. Send the SOAP request and inspect the response.

Example SOAP 1.1 request:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Header/>
    <soapenv:Body>
        <CustomerRequest>
            <CustomerID>C1001</CustomerID>
            <CustomerName>Rahul Sharma</CustomerName>
            <City>Hyderabad</City>
        </CustomerRequest>
    </soapenv:Body>
</soapenv:Envelope>
```

Expected business response:

```xml
<ERPCreateCustomerResponse>
    <Status>SUCCESS</Status>
    <Message>Customer created successfully in ERP</Message>
    <ERPReference>ERP10001</ERPReference>
</ERPCreateCustomerResponse>
```

The actual HTTP response may contain this XML inside a SOAP Envelope. The exact response depends on the SOAP service contract and runtime configuration.

## Project Limitations

This is a learning exercise using a mock ERP service. It does not connect to a real CRM or ERP system, store customer records, or implement production-grade error handling and retry processing.

The mock ERP returns a fixed success response. A production implementation would require actual ERP connectivity, business validation, appropriate security, meaningful response handling, monitoring, and error/retry design.

## Technologies and Components

- SAP Integration Suite – Cloud Integration (SAP CPI)
- SOAP 1.1
- SOAP Sender and Receiver Adapters
- Groovy Script
- Message Mapping
- XML and XSD
- Content Modifier
- Request Reply
- SAP BTP Service Keys
- SAP CPI Security Material
- Postman

---

**Project:** CRM to ERP Customer Integration  
**Integration Pattern:** SOAP to SOAP – Synchronous Request-Reply  
**Purpose:** Practice system-to-system integration, conditional logic, message mapping, SOAP connectivity, and response handling in SAP CPI.
