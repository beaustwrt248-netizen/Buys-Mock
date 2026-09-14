const SHA256_PATTERN = /^[a-f0-9]{64}$/i;
const HTTPS_PATTERN = /^https:\/\//i;

export const NOVA_OTA_CONFIG = Object.freeze({
  appId: 'nova-next',
  packageName: 'com.buysloans.novanext',
  channel: 'stable',
  manifestUrl: 'https://buyshub.me/downloads/nova-next/stable/manifest.json'
});

function otaError(code) {
  const error = new Error(code);
  error.code = code;
  return error;
}

function requireString(value, code) {
  const text = String(value ?? '').trim();
  if (!text) throw otaError(code);
  return text;
}

export function validateNovaRelease(release, {
  currentVersionCode = 0,
  config = NOVA_OTA_CONFIG
} = {}) {
  if (!release || typeof release !== 'object' || Array.isArray(release)) {
    throw otaError('NOVA_OTA_INVALID_RELEASE');
  }

  const appId = requireString(release.appId, 'NOVA_OTA_APP_ID_REQUIRED');
  const channel = requireString(release.channel, 'NOVA_OTA_CHANNEL_REQUIRED');
  const packageName = requireString(release.packageName, 'NOVA_OTA_PACKAGE_REQUIRED');
  const versionName = requireString(release.versionName, 'NOVA_OTA_VERSION_NAME_REQUIRED');
  const sha256 = requireString(release.sha256, 'NOVA_OTA_SHA256_REQUIRED').toLowerCase();
  const downloadUrl = requireString(release.downloadUrl, 'NOVA_OTA_DOWNLOAD_URL_REQUIRED');
  const versionCode = Number(release.versionCode);
  const current = Number(currentVersionCode || 0);

  if (appId !== config.appId) throw otaError('NOVA_OTA_APP_ID_MISMATCH');
  if (channel !== config.channel) throw otaError('NOVA_OTA_CHANNEL_MISMATCH');
  if (packageName !== config.packageName) throw otaError('NOVA_OTA_PACKAGE_MISMATCH');
  if (!Number.isSafeInteger(versionCode) || versionCode <= 0) throw otaError('NOVA_OTA_VERSION_CODE_INVALID');
  if (!Number.isSafeInteger(current) || current < 0) throw otaError('NOVA_OTA_CURRENT_VERSION_INVALID');
  if (versionCode <= current) throw otaError('NOVA_OTA_NOT_NEWER');
  if (!SHA256_PATTERN.test(sha256)) throw otaError('NOVA_OTA_SHA256_INVALID');
  if (!HTTPS_PATTERN.test(downloadUrl)) throw otaError('NOVA_OTA_DOWNLOAD_URL_INVALID');

  const url = new URL(downloadUrl);
  if (url.hostname !== 'buyshub.me' && url.hostname !== 'www.buyshub.me') {
    throw otaError('NOVA_OTA_DOWNLOAD_HOST_INVALID');
  }
  if (!url.pathname.startsWith('/downloads/nova-next/')) {
    throw otaError('NOVA_OTA_DOWNLOAD_PATH_INVALID');
  }

  return Object.freeze({
    appId,
    channel,
    packageName,
    versionCode,
    versionName,
    sha256,
    downloadUrl: url.toString(),
    releaseNotes: String(release.releaseNotes ?? '').trim(),
    publishedAt: String(release.publishedAt ?? '').trim()
  });
}
