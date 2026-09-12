export const CHANNELS = Object.freeze({
  development: Object.freeze({
    name: 'Nova Next',
    appId: 'au.com.morley.novanext.dev',
    webScope: '/nova-next/',
    cacheNamespace: 'nova-next-dev-v1',
    requiresExplicitPromotion: false
  }),
  production: Object.freeze({
    name: 'Nova AI',
    appId: '__CURRENT_NOVA_PRODUCTION_APP_ID__',
    webScope: '__CURRENT_NOVA_PRODUCTION_SCOPE__',
    cacheNamespace: 'nova-production',
    requiresExplicitPromotion: true
  })
});

export function promotionChecklist() {
  return Object.freeze([
    'feature-parity',
    'auth-security-review',
    'guardian-boundary-review',
    'data-contract-compatibility',
    'pwa-cache-scope-review',
    'android-package-id-verification',
    'android-signing-key-verification',
    'version-upgrade-verification',
    'rollback-artifacts-recorded',
    'explicit-human-approval'
  ]);
}
