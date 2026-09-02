# Guardian repair worker

The Edge Function accepts either an authenticated Admin/Manager session or the matching Guardian incident dispatch nonce. Gateway JWT verification is disabled so database dispatch and browser preflight can reach the function; authorization is enforced inside the function before a requested repair can be claimed.
