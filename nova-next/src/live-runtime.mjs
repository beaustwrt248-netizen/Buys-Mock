import { RUNTIME_CONFIG, assertPublicRuntimeConfig } from './runtime-config.mjs';
import { createSupabaseAuthClient } from './adapters/supabase-auth-client.mjs';
import { createSessionStore } from './session-store.mjs';
import { createAuthController } from './auth-controller.mjs';
import { createEdgeFunctionClient } from './adapters/edge-function-client.mjs';
import { createChatAdapter } from './adapters/chat-adapter.mjs';
import { createTurnstileController } from './turnstile.mjs';

function text(value) {
  return String(value ?? '');
}

function readableAuthError(error) {
  const message = text(error?.message || error);
  if (message === 'AUTH_FORBIDDEN') return 'This account is not authorised for Nova AI.';
  if (message === 'CAPTCHA_REQUIRED') return 'Complete the security check first.';
  if (message === 'AUTH_TIMEOUT') return 'Sign-in timed out. Check your connection and try again.';
  return 'Sign-in failed. Check your details and try again.';
}

function createElement(documentObj, tag, className = '', content = '') {
  const el = documentObj.createElement(tag);
  if (className) el.className = className;
  if (content) el.textContent = content;
  return el;
}

export function createLiveRuntime({
  config = RUNTIME_CONFIG,
  windowObj = globalThis.window,
  documentObj = globalThis.document,
  fetchImpl = globalThis.fetch?.bind?.(globalThis) || globalThis.fetch,
  storage = globalThis.sessionStorage,
  callbacks = {}
} = {}) {
  assertPublicRuntimeConfig(config);
  if (!documentObj) throw new Error('LIVE_RUNTIME_DOCUMENT_REQUIRED');
  if (!storage) throw new Error('LIVE_RUNTIME_SESSION_STORAGE_REQUIRED');

  const authClient = createSupabaseAuthClient({
    baseUrl: config.supabaseUrl,
    publishableKey: config.supabasePublishableKey,
    fetchImpl
  });
  const sessionStore = createSessionStore({ storage, key: config.sessionKey });
  const authController = createAuthController({
    authClient,
    sessionStore,
    onState: state => callbacks.onAuthState?.(state)
  });
  const edgeClient = createEdgeFunctionClient({
    baseUrl: config.supabaseUrl,
    publishableKey: config.supabasePublishableKey,
    getAccessToken: () => authController.getAccessToken(),
    fetchImpl
  });
  const chat = createChatAdapter({ edgeClient });
  const turnstile = createTurnstileController({ siteKey: config.turnstileSiteKey });

  let captchaToken = '';
  let chatBusy = false;
  let loginBound = false;
  let chatBound = false;

  function setLoginStatus(message = '', tone = '') {
    const status = documentObj.getElementById('novaNextLoginStatus');
    if (!status) return;
    status.textContent = message;
    status.className = `live-status${tone ? ` ${tone}` : ''}`;
  }

  function setSubmitReady(ready, label = 'Sign In') {
    const button = documentObj.querySelector('#loginForm button[type="submit"]');
    if (!button) return;
    button.disabled = !ready;
    button.textContent = label;
  }

  function prepareLoginUi() {
    if (loginBound) return;
    const form = documentObj.getElementById('loginForm');
    if (!form) throw new Error('LOGIN_FORM_MISSING');
    loginBound = true;

    const identity = form.querySelector('input[name="identity"]');
    const password = form.querySelector('input[name="password"]');
    if (identity) {
      identity.type = 'email';
      identity.required = true;
      identity.placeholder = 'Admin email';
    }
    if (password) password.required = true;

    documentObj.querySelector('.divider')?.classList.add('is-hidden');
    documentObj.querySelector('.social-row')?.classList.add('is-hidden');
    documentObj.querySelector('.signup-copy')?.classList.add('is-hidden');
    const loginOptions = documentObj.querySelector('.login-options');
    if (loginOptions) {
      loginOptions.innerHTML = '<span class="security-note">Enabled Morley Admin accounts only</span>';
    }
    const preview = documentObj.getElementById('previewNote');
    if (preview) preview.textContent = 'Secure sign-in uses Morley Supabase Auth and Cloudflare Turnstile. Passwords are never stored by Nova.';

    const captchaWrap = createElement(documentObj, 'div', 'turnstile-wrap');
    captchaWrap.id = 'novaNextTurnstile';
    const submit = form.querySelector('button[type="submit"]');
    if (submit) form.insertBefore(captchaWrap, submit);
    const status = createElement(documentObj, 'p', 'live-status');
    status.id = 'novaNextLoginStatus';
    status.setAttribute('role', 'status');
    status.setAttribute('aria-live', 'polite');
    form.append(status);
    setSubmitReady(false, 'Complete security check');

    form.addEventListener('submit', async event => {
      event.preventDefault();
      if (!captchaToken) {
        setLoginStatus('Complete the security check first.', 'error');
        return;
      }
      const email = text(identity?.value).trim();
      const secret = text(password?.value);
      if (!email || !secret) {
        setLoginStatus('Enter your Admin email and password.', 'error');
        return;
      }
      setSubmitReady(false, 'Verifying…');
      setLoginStatus('Checking your Admin access…');
      try {
        const result = await authController.signIn({ email, password: secret, captchaToken });
        if (password) password.value = '';
        captchaToken = '';
        callbacks.onAuthenticated?.(result.session, { restored: false });
      } catch (error) {
        if (password) password.value = '';
        captchaToken = '';
        setLoginStatus(readableAuthError(error), 'error');
        setSubmitReady(false, 'Complete security check');
        turnstile.reset();
      }
    });
  }

  async function mountTurnstile() {
    prepareLoginUi();
    try {
      await turnstile.mount('#novaNextTurnstile', {
        onToken: token => {
          captchaToken = token;
          setLoginStatus('Security check complete.', 'ok');
          setSubmitReady(Boolean(token), 'Sign In');
        },
        onExpired: () => {
          captchaToken = '';
          setLoginStatus('Security check expired. Complete it again.', 'error');
          setSubmitReady(false, 'Complete security check');
        },
        onError: () => {
          captchaToken = '';
          setLoginStatus('Security check could not be completed. Refresh and try again.', 'error');
          setSubmitReady(false, 'Security check unavailable');
        }
      });
    } catch (error) {
      setLoginStatus('Security check failed to load. Access remains locked.', 'error');
      setSubmitReady(false, 'Security check unavailable');
      callbacks.onRuntimeError?.(error);
    }
  }

  function updateProfile(session) {
    const email = text(session?.user?.email);
    const name = text(session?.user?.user_metadata?.full_name || session?.user?.user_metadata?.name || 'Morley Admin');
    for (const el of documentObj.querySelectorAll('.profile-copy strong')) el.textContent = name;
    for (const el of documentObj.querySelectorAll('.profile-copy small')) el.textContent = email;
    for (const el of documentObj.querySelectorAll('.avatar')) el.textContent = (name.trim()[0] || email.trim()[0] || 'N').toUpperCase();
  }

  function appendChatMessage(role, content, { tone = '', meta = '' } = {}) {
    const list = documentObj.getElementById('novaNextChatMessages');
    if (!list) return null;
    const article = createElement(documentObj, 'article', `chat-message ${role}${tone ? ` ${tone}` : ''}`);
    const body = createElement(documentObj, 'div', 'chat-message-body', content);
    article.append(body);
    if (meta) article.append(createElement(documentObj, 'small', 'chat-message-meta', meta));
    list.append(article);
    article.scrollIntoView?.({ block: 'end', behavior: 'smooth' });
    return article;
  }

  function setChatBusy(busy) {
    chatBusy = busy;
    const input = documentObj.getElementById('novaNextChatInput');
    const button = documentObj.getElementById('novaNextChatSend');
    if (input) input.disabled = busy;
    if (button) button.disabled = busy;
  }

  async function sendChat(prompt) {
    const clean = text(prompt).trim();
    if (!clean || chatBusy) return null;
    appendChatMessage('user', clean);
    setChatBusy(true);
    const pending = appendChatMessage('assistant pending', 'Nova is thinking…');
    try {
      const result = await chat.ask(clean, { mode: 'auto', provider: 'auto' });
      pending?.remove();
      const meta = [result.guarded ? 'Guarded' : '', result.degraded ? 'Degraded' : '', ...result.modelsUsed].filter(Boolean).join(' · ');
      appendChatMessage('assistant', result.answer || 'Nova returned no response.', {
        tone: result.ok ? (result.degraded ? 'warning' : '') : 'error',
        meta
      });
      callbacks.onChatResult?.(result);
      return result;
    } catch (error) {
      pending?.remove();
      const status = Number(error?.status || 0);
      if (status === 401 || status === 403) {
        authController.signOut();
        appendChatMessage('assistant', 'Your Nova session is no longer authorised. Sign in again.', { tone: 'error' });
        callbacks.onLocked?.('AUTH_REQUIRED');
      } else {
        appendChatMessage('assistant', 'Nova could not reach the guarded AI service. No answer was fabricated.', { tone: 'error' });
      }
      callbacks.onRuntimeError?.(error);
      throw error;
    } finally {
      setChatBusy(false);
    }
  }

  function prepareChatUi() {
    if (chatBound) return;
    const page = documentObj.querySelector('.page[data-route="chat"]');
    const composer = page?.querySelector('.composer');
    const suggestionList = page?.querySelector('.suggestion-list');
    if (!page || !composer || !suggestionList) return;
    chatBound = true;

    const messages = createElement(documentObj, 'div', 'chat-messages');
    messages.id = 'novaNextChatMessages';
    suggestionList.before(messages);

    const input = composer.querySelector('input');
    const send = composer.querySelector('button');
    if (input) {
      input.id = 'novaNextChatInput';
      input.autocomplete = 'off';
      input.placeholder = 'Ask Nova anything…';
      input.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey) {
          event.preventDefault();
          const value = input.value;
          input.value = '';
          sendChat(value).catch(() => {});
        }
      });
    }
    if (send) {
      send.id = 'novaNextChatSend';
      send.textContent = '↑';
      send.setAttribute('aria-label', 'Send message');
      send.addEventListener('click', () => {
        const value = input?.value || '';
        if (input) input.value = '';
        sendChat(value).catch(() => {});
      });
    }
    for (const button of suggestionList.querySelectorAll('button')) {
      button.addEventListener('click', () => {
        const value = button.textContent.trim().replace(/^[^A-Za-z]+/, '');
        if (input) input.value = value;
        sendChat(value).catch(() => {});
      });
    }
  }

  async function start() {
    prepareLoginUi();
    prepareChatUi();
    const state = await authController.restore();
    if (state.status === 'authenticated') {
      updateProfile(state.session);
      callbacks.onAuthenticated?.(state.session, { restored: true });
      return state;
    }
    callbacks.onLocked?.(state.error);
    await mountTurnstile();
    return state;
  }

  async function showLogin() {
    callbacks.onLocked?.();
    await mountTurnstile();
  }

  function signOut() {
    authController.signOut();
    captchaToken = '';
    callbacks.onLocked?.();
    mountTurnstile().catch(error => callbacks.onRuntimeError?.(error));
  }

  return Object.freeze({ start, showLogin, signOut, sendChat, getAccessToken: () => authController.getAccessToken() });
}
