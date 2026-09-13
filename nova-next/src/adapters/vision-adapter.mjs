const MAX_IMAGES = 6;
const MAX_SINGLE = 8_000_000;
const MAX_TOTAL = 20_000_000;
const IMAGE_RE = /^data:image\/(?:jpeg|jpg|png|webp);base64,/i;

export function createVisionAdapter({ edgeClient } = {}) {
  if (!edgeClient || typeof edgeClient.invoke !== 'function') throw new TypeError('EDGE_CLIENT_REQUIRED');

  async function analyse(images, { hint = '' } = {}) {
    if (!Array.isArray(images) || images.length === 0) throw new Error('IMAGE_REQUIRED');
    if (images.length > MAX_IMAGES) throw new Error('IMAGE_COUNT');
    let total = 0;
    const normalized = images.map(value => String(value || ''));
    for (const image of normalized) {
      if (!IMAGE_RE.test(image)) throw new Error('IMAGE_TYPE');
      if (image.length > MAX_SINGLE) throw new Error('IMAGE_SIZE');
      total += image.length;
    }
    if (total > MAX_TOTAL) throw new Error('IMAGE_TOTAL_SIZE');
    return edgeClient.invoke('nova-vision', {
      image_data_urls: normalized,
      hint: String(hint || '').trim().slice(0, 500)
    });
  }

  return Object.freeze({ analyse });
}
