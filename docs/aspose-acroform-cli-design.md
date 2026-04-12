# Design: Java CLI to Fill Selected AcroForm Fields in the JLL Site Inspection Questionnaire

## Overview

This document describes a Maven-based Java CLI that uses **Aspose.PDF for Java** to populate three AcroForm text fields in the provided PDF form:

- `Property name`
- `Property address`
- `Company name`

The CLI accepts the three values on the command line, opens the source PDF, updates those fields, and saves a new filled PDF.

The target PDF is a standard **AcroForm** PDF, and the three target fields are present as text fields with these exact names:

- `Property name`
- `Property address`
- `Company name`

## Goals

- Provide a simple CLI for filling a small subset of fields.
- Use Aspose APIs that map cleanly to AcroForm field operations.
- Keep the implementation small, testable, and easy to extend.
- Preserve the original PDF and write a separate output file.

## Non-Goals

- Filling every field in the questionnaire.
- Flattening the form after write.
- Building a GUI or web service.
- Supporting XFA forms.

## Relevant Aspose APIs

- `Document.getForm()` returns the PDF's AcroForm object.
- `Form.getFields()` returns the lowest-level fields in the form hierarchy.
- `TextBoxField.setValue(String)` sets the value of a text field.
- `Field.getFullName()` returns the exact field name used for lookup.
- `Document.save(String)` saves the modified document.
- `com.aspose.pdf.facades.Form.fillField(String, String)` is a shorter alternative for name-based filling.

The preferred implementation uses the **core DOM-style API** because it is direct, type-aware, and easier to evolve into richer validation logic.

## Proposed CLI Contract

```bash
java -jar pdf-form-cli.jar \
  --input "JLL Site Inspection Questionnaire 2026.pdf" \
  --output "filled.pdf" \
  --property-name "Sunset Villas" \
  --property-address "123 Main Street" \
  --company-name "Acme Management LLC"
```

## Design Choice: DOM API vs Facades API

### Preferred: DOM API

The implementation uses:

1. `new Document(inputPath)`
2. `document.getForm()`
3. locate each field by exact name
4. cast to `TextBoxField`
5. `setValue(...)`
6. `document.save(outputPath)`

Why this is preferred:

- It makes field typing explicit.
- It is easy to fail fast if a field is not a text field.
- It is easy to extend later for checkboxes, dropdowns, signatures, and validation.
- It aligns well with a service class design.

### Acceptable Alternative: Facades API

An alternative implementation uses:

- `com.aspose.pdf.facades.Form`
- `fillField("Property name", value)`
- `fillField("Property address", value)`
- `fillField("Company name", value)`
- `save(...)`

This is slightly shorter, but the DOM API gives clearer control for a production-ready CLI.

## Processing Flow

```text
Parse CLI args
  -> validate required inputs
  -> load Aspose license if configured
  -> open PDF with Aspose Document
  -> resolve AcroForm
  -> resolve 3 target fields by exact name
  -> verify each field is a text field
  -> set values
  -> save to output path
  -> exit 0 on success
```

## Error Handling Model

The implementation distinguishes between:

1. **User input errors** such as missing CLI parameters
2. **Template contract errors** such as a renamed or missing PDF field
3. **Infrastructure errors** such as file I/O or license loading problems

This separation makes the CLI easier to automate in scripts and CI pipelines.

## Testing Strategy

### Unit Tests

Unit tests verify:

- CLI argument validation
- field-name constants
- stable defaults and option handling

### Integration Test

Recommended integration flow:

1. copy a known-good PDF fixture to a temp directory
2. run the CLI or `PdfFormFiller.fill(...)`
3. reopen the output PDF
4. read back the three field values
5. assert exact matches

## Conclusion

The simplest robust design is a Maven-structured Java CLI built on **Aspose.PDF's DOM API**. The CLI:

- accepts three required values on the command line
- opens the JLL questionnaire PDF
- locates the exact AcroForm fields `Property name`, `Property address`, and `Company name`
- sets each value through `TextBoxField.setValue(String)`
- saves a new output PDF

That approach is small enough for a proof of concept, while still providing a solid path to a production-grade form-filling tool.
