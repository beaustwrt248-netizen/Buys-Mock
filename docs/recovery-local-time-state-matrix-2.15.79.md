# Recovery local-time state matrix

| State | Display behaviour |
| --- | --- |
| Valid UTC/offset timestamp | Convert to device zone and locale |
| PostgreSQL space separator | Normalize to ISO `T` before parse |
| Blank timestamp | Preserve blank so existing `Never` UI fallback remains |
| Malformed timestamp | Preserve original value; no crash |
| Backup operations | Unchanged |
