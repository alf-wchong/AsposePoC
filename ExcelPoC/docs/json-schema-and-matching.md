# JSON Schema and Matching Rules

## JSON shape
```json
[
  {
    "key": "Borrower",
    "value": "Acme Inc",
    "aliases": ["Borrower Principal Name"],
    "ReplaceAll": true,
    "CaseSensitivity": false
  }
]
```

## Defaults
- `aliases`: empty list when omitted
- `ReplaceAll`: `true` when omitted
- `CaseSensitivity`: `false` when omitted

## Ambiguous match example
This is invalid because the same marker could match both entries:
```json
[
  { "key": "Borrower", "value": "A", "aliases": ["Borrower Principal Name"] },
  { "key": "Borrower Principal Name", "value": "B", "aliases": [] }
]
```

The application stops and emits one error containing:
- marker text
- sheet name
- cell address
- all matching JSON entries
