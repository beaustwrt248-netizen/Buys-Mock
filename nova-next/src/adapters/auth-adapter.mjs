import { canEnterNovaNext } from '../auth-policy.mjs';

function requiredFunction(name, value) {
  if (typeof value !== 'function') throw new TypeError(`Missing auth dependency: ${name}`);
  return value;
}

export function createAuthAdapter({ verifyHuman, signInPassword, loadProfile, signOutRemote }) {
  const verify = requiredFunction('verifyHuman', verifyHuman);
  const signIn = requiredFunction('signInPassword', signInPassword);
  const profileFor = requiredFunction('loadProfile', loadProfile);
  const remoteSignOut = requiredFunction('signOutRemote', signOutRemote);
  let activeSession = null;

  async function signInWithPolicy({ identity, password, turnstileToken }) {
    const human = await verify(turnstileToken);
    if (!human) throw new Error('HUMAN_VERIFICATION_FAILED');

    const authResult = await signIn({ identity, password });
    if (!authResult?.user?.id || !authResult?.accessToken) throw new Error('AUTH_EXCHANGE_FAILED');

    const profile = await profileFor(authResult.user.id, authResult.accessToken);
    if (!canEnterNovaNext(profile)) {
      await remoteSignOut(authResult.accessToken);
      throw new Error('AUTH_FORBIDDEN');
    }

    activeSession = Object.freeze({
      user: authResult.user,
      profile,
      accessToken: authResult.accessToken
    });
    return activeSession;
  }

  async function signOut() {
    const token = activeSession?.accessToken || null;
    activeSession = null;
    await remoteSignOut(token);
  }

  function session() {
    return activeSession;
  }

  return Object.freeze({ signIn: signInWithPolicy, signOut, session });
}
