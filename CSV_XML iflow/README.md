# SAP CPI – CSV to XML iFlow

## Overview

This demonstrates an SAP Cloud Integration (CPI) iFlow that converts employee records from CSV format into XML format for a receiving application. It also sets the source-system identifier using a Content Modifier.

## Scenario

An external HR system sends employee records in CSV format, but the receiving application requires XML.

### Input Payload

```csv
EmployeeID,Name,Department
103,Amit,Finance
104,Neha,IT
```

## iFlow Diagram

![SAP CPI CSV to XML iFlow](images/csv-xml-iflow.png)

## iFlow Steps

1. **Start Timer 1:** Starts the integration process based on the configured timer schedule.
2. **Content Modifier 3:** Allows message headers, exchange properties, or the message body to be configured before conversion, depending on its settings.
3. **CSV to XML Converter 1:** Converts the incoming CSV data into XML format.
4. **Content Modifier 4:** Sets the exchange property `SourceSystem` to `ExternalHR`.
5. **End:** Marks the end of the integration process.

## Expected XML Output

The following is an illustrative XML output, assuming the converter is configured to use the CSV header names as XML field names and to create an `Employees` root element with an `Employee` element for each record.

```xml
<Employees>
    <Employee>
        <EmployeeID>103</EmployeeID>
        <Name>Amit</Name>
        <Department>Finance</Department>
    </Employee>
    <Employee>
        <EmployeeID>104</EmployeeID>
        <Name>Neha</Name>
        <Department>IT</Department>
    </Employee>
</Employees>
```

The exchange property is:

```text
SourceSystem = ExternalHR
```

**Note:** Setting `SourceSystem` as an exchange property does not automatically add it to the XML body. If the receiving application requires the source-system identifier inside the XML, an additional step must insert it into the message body.

For example, the required XML could include:

```xml
<Employees>
    <SourceSystem>ExternalHR</SourceSystem>
    <Employee>
        <EmployeeID>103</EmployeeID>
        <Name>Amit</Name>
        <Department>Finance</Department>
    </Employee>
    <Employee>
        <EmployeeID>104</EmployeeID>
        <Name>Neha</Name>
        <Department>IT</Department>
    </Employee>
</Employees>
```

## How the CSV to XML Converter Works

- **Column names:** The first row contains `EmployeeID`, `Name`, and `Department`. When the converter is configured to recognize the first row as headers, these names identify the fields.
- **Records:** Each subsequent row represents one employee. In this example, there are two employee records.
- **Field mapping:** Values are mapped according to their column positions. For example, `103` is the employee ID, `Amit` is the name, and `Finance` is the department.
- **XML structure:** The converter configuration determines how CSV columns and records are represented as XML elements. The exact XML structure depends on the configured options.

## Why CSV Structure and Converter Configuration Must Be Compatible

The CSV input structure must match the converter configuration for the conversion to work correctly.

- **Delimiter:** The delimiter configured in the converter must match the input file. For example, comma-separated data requires a comma delimiter.
- **Header handling:** The converter must correctly identify whether the first row contains column names.
- **Column consistency:** Each employee record should contain the expected number of fields.
- **Field mapping:** The column names and values must match the structure expected by the receiving application.
- **Special characters:** Quotes, commas within values, and other special characters must be handled according to the CSV format and converter settings.

If these settings are incorrect, the XML output may contain incorrect fields, misplaced values, or conversion errors.

## Testing and Validation

- [ ] Verify that the iFlow starts successfully.
- [ ] Provide the sample CSV payload.
- [ ] Confirm that the CSV to XML Converter produces XML.
- [ ] Verify that both employee records are present.
- [ ] Check that employee IDs, names, and departments map correctly.
- [ ] Verify that the exchange property `SourceSystem` is set to `ExternalHR`.
- [ ] If required, confirm that `SourceSystem` is also present in the final XML body.

## Technologies Used

- SAP Cloud Integration (SAP CPI)
- Integration Flow (iFlow)
- Content Modifier
- CSV to XML Converter
- XML
- CSV

## Conclusion

This project demonstrates how SAP CPI can transform CSV employee data into XML using a CSV to XML Converter. A Content Modifier sets the `SourceSystem` exchange property to `ExternalHR`. Correct converter configuration ensures that employee records and their fields are represented as expected by the receiving application.

## Repository Structure

```text
CSV_XML iflow/
├── README.md
└── images/
    └── csv-xml-iflow.png
```