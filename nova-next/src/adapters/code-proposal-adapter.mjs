function safeNovaNextPath(value) {
  const path = String(value || '').trim().replace(/^\/+/, '');
  if (!path.startsWith('nova-next/') || path.includes('..')) return false;
  const lower = path.toLowerCase();
  if (/(?:^|\/)(?:auth|security|guardian|pricing|release|ota)(?:[\/_.-]|$)/.test(lower)) return false;
  if (/keystore|signing|credential|secret|\.env(?:$|\.)/.test(lower)) return false;
  return /\.(?:js|mjs|css|html|json|md|txt|java|kt|xml)$/i.test(path);
}

export function createCodeProposalAdapter({ edgeClient } = {}) {
  if (!edgeClient || typeof edgeClient.invoke !== 'function') throw new TypeError('EDGE_CLIENT_REQUIRED');

  async function propose({ diagnostic, candidateFiles } = {}) {
    const detail = String(diagnostic || '').trim();
    if (detail.length < 8) throw new Error('DIAGNOSTIC_REQUIRED');
    if (!Array.isArray(candidateFiles) || candidateFiles.length === 0) throw new Error('CANDIDATE_FILES_REQUIRED');
    const files = candidateFiles.map(value => String(value || '').trim()).filter(Boolean);
    if (files.length > 4) throw new Error('TOO_MANY_CANDIDATE_FILES');
    if (files.some(path => !safeNovaNextPath(path))) throw new Error('PROTECTED_PATH');
    return edgeClient.invoke('nova-code-proposal', {
      diagnostic: detail.slice(0, 8000),
      candidate_files: files
    });
  }

  return Object.freeze({ propose });
}
