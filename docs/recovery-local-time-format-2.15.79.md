# Recovery timestamp format

Formatting uses Java/Android `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)` with `Locale.getDefault()` and `ZoneId.systemDefault()`. This avoids hard-coding Perth for other Morley users while still rendering Perth correctly when the device is set to Australia/Perth.
