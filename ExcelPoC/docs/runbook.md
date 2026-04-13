# Runbook

## Purpose

Run the Excel marker filler against a workbook template and a JSON mapping file.

## Command

```bash
mvn exec:java -Dexec.args='--input "src/main/resources/Contingent Liabilities.xlsx" --config src/main/resources/sample-field-data.json --output target/Contingent-Liabilities-filled.xlsx --scan-all-sheets'
```

## Required arguments

- `--input`
  - path to the source workbook
- `--config`
  - path to the JSON mapping file
- `--output`
  - path for the filled workbook

## Optional arguments

- `--log-file`
  - custom application log file path
  - default: `target/excel-marker-filler.log`
- `--scan-all-sheets`
  - scan all worksheets instead of only the first worksheet

## Expected log flow at INFO

A normal successful run emits these lifecycle summaries:

```text
event=run_start ...
event=mappings_loaded ...
event=marker_discovery_complete ...
event=validation_complete ...
event=replacement_complete ...
event=workbook_saved ...
event=run_complete ...
```

## Expected log flow at DEBUG

With debug logging enabled, the application also emits:
- `event=worksheet_scan_start`
- `event=marker_discovered`
- `event=mapping_evaluation_start`
- `event=mapping_result`
- `event=cell_replaced`

## Failure behavior

If one marker matches more than one JSON entry, the application logs:

```text
event=validation_failed errorType=ambiguous_marker_match ...
```

and exits without saving a modified workbook.

## Notes

- JSON entries with no matching markers are normal and appear only in `DEBUG`
- Only cells containing valid marker syntax are modified
- Replacement happens in place
