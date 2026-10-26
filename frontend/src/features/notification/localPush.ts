function createInstallationId() {
  const browserCrypto = window.crypto;

  if (typeof browserCrypto?.randomUUID === 'function') {
    return browserCrypto.randomUUID();
  }

  if (typeof browserCrypto?.getRandomValues === 'function') {
    const bytes = browserCrypto.getRandomValues(new Uint8Array(16));
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  // 인증용 비밀값이 아닌 브라우저 구분용 ID이므로, 암호화 API가 없는 환경도 지원한다.
  return `legacy-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}

export function getOrCreateInstallationId() {
  const key = 'installationId';
  let id = localStorage.getItem(key);

  if (!id) {
    id = createInstallationId();
    localStorage.setItem(key, id);
  }

  return id;
}

const PUSH_SUBSCRIPTION_ID_KEY = 'chongchong:push-subscription-id';
const PUSH_ENABLED_KEY = 'chongchong:push-enabled';

export async function clearLocalPushSubscription() {
  if ('serviceWorker' in navigator) {
    const scope = new URL('/push/', window.location.origin).href;
    const sw = await navigator.serviceWorker.getRegistration(scope);

    if (sw?.scope === scope && sw.pushManager) {
      const subscription = await sw.pushManager.getSubscription();
      await subscription?.unsubscribe();
    }
  }

  localStorage.removeItem(PUSH_SUBSCRIPTION_ID_KEY);
  localStorage.removeItem(PUSH_ENABLED_KEY);
}
