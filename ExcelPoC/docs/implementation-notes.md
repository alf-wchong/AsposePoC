# Implementation Notes

## Core approach

The application uses explicit marker cells in the Excel workbook to determine where values go.

Example marker:
```text
[{!Borrower Principal Name!}]
```

At runtime, the program:
- scans the workbook for marker cells
- extracts the marker name from the marker syntax
- compares that name to each JSON mapping's `key` and `aliases`
- replaces matching marker cells in place with the JSON `value`

## Why marker-based replacement

This avoids layout guessing.

The earlier approach of writing into the first empty cell to the right of a label is not reliable across different Excel templates. Marker-based replacement makes the destination explicit inside the workbook itself.

## Ambiguity handling

Before replacing anything, the application validates that each discovered marker resolves to at most one JSON entry.

If a marker matches multiple JSON entries, the application:
- emits one dense `ERROR` log event
- includes the sheet, cell, raw marker, logical marker name, and all competing JSON entries
- stops before modifying the workbook

## Logging behavior

The implementation now uses a low-noise logging model:
- `INFO` for lifecycle summaries only
- `DEBUG` for detailed scan and mapping activity
- `ERROR` for ambiguity failures

This keeps production logs compact while preserving detailed troubleshooting data when debug logging is enabled.

See [design-document.md](design-document.md) for the exact logging field names and event formats.
