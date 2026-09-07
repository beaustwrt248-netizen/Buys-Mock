# Time-zone behaviour

`ZoneId.systemDefault()` is evaluated on the Android device when the backup snapshot is loaded. A user travelling between time zones therefore sees the backup instant converted to the device's current local zone without changing the stored UTC/offset timestamp.
