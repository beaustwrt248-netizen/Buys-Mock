# Admin user-control session revocation

`supabase.auth.admin.signOut(jwt, scope)` accepts a logged-in access-token JWT. It must never be called with a target user's UUID.

Target-user password reset, disable, and force-signout actions revoke that user's server-side sessions through the service-role-only `public.admin_revoke_user_sessions(uuid)` RPC instead. This prevents UUID values from reaching JWT parsing and preserves global sign-out semantics for refresh sessions.
