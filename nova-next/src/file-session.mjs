export const MAX_FILES = 20;
export const MAX_FILE_BYTES = 20 * 1024 * 1024;
export const MAX_TEXT_BYTES = 1024 * 1024;

const TEXT_TYPES = new Set(['text/plain', 'text/markdown', 'application/json', 'text/csv', 'application/csv']);
const TEXT_EXTENSIONS = new Set(['txt', 'md', 'markdown', 'json', 'csv']);
const IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function extension(name = '') {
  const parts = String(name).toLowerCase().split('.');
  return parts.length > 1 ? parts.pop() : '';
}

function isTextLike(file) {
  return TEXT_TYPES.has(String(file?.type || '').toLowerCase()) || TEXT_EXTENSIONS.has(extension(file?.name));
}

function isImage(file) {
  return IMAGE_TYPES.has(String(file?.type || '').toLowerCase());
}

function fileSize(file) {
  const size = Number(file?.size);
  if (!Number.isFinite(size) || size < 0) throw new Error('FILE_SIZE_INVALID');
  return size;
}

function toIso(now) {
  const value = now();
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error('FILE_CLOCK_INVALID');
  return date.toISOString();
}

function publicMetadata(id, entry) {
  const file = entry.file;
  const size = fileSize(file);
  return Object.freeze({
    id,
    name: String(file?.name || 'Unnamed file'),
    type: String(file?.type || ''),
    size,
    addedAt: entry.addedAt,
    canReadText: isTextLike(file) && size <= MAX_TEXT_BYTES,
    canAnalyseImage: isImage(file)
  });
}

export function createFileSession({
  now = () => new Date(),
  idFactory = () => globalThis.crypto?.randomUUID?.() || `file-${Date.now()}-${Math.random().toString(16).slice(2)}`
} = {}) {
  if (typeof now !== 'function') throw new TypeError('FILE_CLOCK_REQUIRED');
  if (typeof idFactory !== 'function') throw new TypeError('FILE_ID_FACTORY_REQUIRED');

  const entries = new Map();

  function metadata(id) {
    const entry = entries.get(id);
    return entry ? publicMetadata(id, entry) : null;
  }

  function addFiles(files) {
    const incoming = [...(files || [])];
    if (entries.size + incoming.length > MAX_FILES) throw new Error('FILE_SESSION_LIMIT');
    for (const file of incoming) {
      if (!file || typeof file !== 'object') throw new Error('FILE_INVALID');
      if (fileSize(file) > MAX_FILE_BYTES) throw new Error('FILE_TOO_LARGE');
    }

    const prepared = incoming.map(file => {
      const id = String(idFactory() || '').trim();
      if (!id || entries.has(id)) throw new Error('FILE_ID_INVALID');
      return { id, file, addedAt: toIso(now) };
    });
    const batchIds = new Set();
    for (const item of prepared) {
      if (batchIds.has(item.id)) throw new Error('FILE_ID_INVALID');
      batchIds.add(item.id);
    }

    for (const item of prepared) entries.set(item.id, { file: item.file, addedAt: item.addedAt });
    return prepared.map(item => metadata(item.id));
  }

  function list() {
    return [...entries.keys()].map(id => metadata(id));
  }

  function get(id) {
    const entry = entries.get(id);
    if (!entry) return null;
    return Object.freeze({ metadata: metadata(id), file: entry.file });
  }

  function remove(id) {
    return entries.delete(id);
  }

  function clear() {
    entries.clear();
  }

  async function readText(id) {
    const entry = entries.get(id);
    if (!entry) throw new Error('FILE_NOT_FOUND');
    if (!isTextLike(entry.file)) throw new Error('FILE_TEXT_UNSUPPORTED');
    if (fileSize(entry.file) > MAX_TEXT_BYTES) throw new Error('FILE_TEXT_TOO_LARGE');
    if (typeof entry.file.text !== 'function') throw new Error('FILE_TEXT_UNAVAILABLE');
    return String(await entry.file.text());
  }

  function readDataUrl(id) {
    const entry = entries.get(id);
    if (!entry) return Promise.reject(new Error('FILE_NOT_FOUND'));
    if (!isImage(entry.file)) return Promise.reject(new Error('FILE_IMAGE_UNSUPPORTED'));
    if (typeof globalThis.FileReader !== 'function') return Promise.reject(new Error('FILE_READER_UNAVAILABLE'));
    return new Promise((resolve, reject) => {
      const reader = new globalThis.FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(reader.error || new Error('FILE_READ_FAILED'));
      reader.readAsDataURL(entry.file);
    });
  }

  const toChatText = id => readText(id);
  const toVisionDataUrl = id => readDataUrl(id);

  return Object.freeze({ addFiles, list, get, remove, clear, readText, readDataUrl, toChatText, toVisionDataUrl });
}
