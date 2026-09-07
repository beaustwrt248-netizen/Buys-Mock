# Morley 2.15.79 acceptance criteria

- Backup & Data no longer exposes raw `+00:00` database timestamps for valid backup records.
- The displayed time reflects the Android device time zone.
- Date/time presentation follows the Android device locale.
- Invalid timestamp input cannot crash the Recovery Centre.
- Backup, verify, restore and delete operations remain unchanged.
- Protected repository checks must pass before merge.
