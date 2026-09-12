export const CHANNELS = Object.freeze({
  development: Object.freeze({
    name: 'Nova Next',
    appId: 'com.buysloans.novanext',
    webScope: '/nova-next/',
    cacheNamespace: 'nova-next-dev-v1',
    requiresExplicitPromotion: false
  }),
  production: Object.freeze({
    name: 'Nova AI',
    appId: 'com.buysloans.nova',
    webScope: '/nova/',
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
