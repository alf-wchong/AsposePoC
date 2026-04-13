# Aspose Cells Marker Filler

This Maven project fills Excel templates by replacing explicit marker cells like `[{!Borrower Principal Name!}]` with values from a JSON mapping file.

## Marker rules
- Markers must be in the exact form `[{!...!}]`
- Matching is `key OR aliases[]`
- `CaseSensitivity` is optional and defaults to `false`
- `ReplaceAll` is optional and defaults to `true`
- If a single marker matches more than one JSON entry, the application fails fast and logs the marker, sheet, cell, and competing JSON entries in one error message

## Logging model
- `INFO` = lifecycle summaries only
- `DEBUG` = per-sheet, per-marker, and per-mapping detail
- `ERROR` = fail-fast ambiguity event with full competing-match context

See [docs/design-document.md](docs/design-document.md) for the concrete logging specification.

## Run
```bash
mvn exec:java -Dexec.args='--input "src/main/resources/Contingent Liabilities.xlsx" --config src/main/resources/sample-field-data.json --output target/Contingent-Liabilities-filled.xlsx --scan-all-sheets'
```

## Package
```bash
mvn clean package
```

## Output
- filled workbook at the `--output` path
- logs to console and to `target/excel-marker-filler.log`
