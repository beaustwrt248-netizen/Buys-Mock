# Recovery timestamp fallback

If a backend timestamp is blank, the existing Recovery Centre `Never` fallback continues to apply. If a nonblank timestamp is malformed, the original text is retained. The formatter never throws into the UI rendering path.
