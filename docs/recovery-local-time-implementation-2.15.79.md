# Implementation detail

`localTimestamp(value, zoneId, locale)` normalizes the PostgreSQL date/time separator, parses with `OffsetDateTime`, shifts the instant with `atZoneSameInstant(zoneId)`, and formats using localized medium date plus short time. Default parameters bind production to device settings while tests inject deterministic settings.
