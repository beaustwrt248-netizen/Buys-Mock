export function canEnterNovaNext(profile) {
  return Boolean(profile && profile.role === 'admin' && profile.is_enabled === true);
}
