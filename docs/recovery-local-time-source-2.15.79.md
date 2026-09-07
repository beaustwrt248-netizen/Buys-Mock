# Source ownership

The implementation lives in the native Android `DriveBackupClient` because that is where Recovery Centre backup API timestamps are converted into the display model. Keeping formatting there avoids coupling the backend API to a single user locale or time zone.
