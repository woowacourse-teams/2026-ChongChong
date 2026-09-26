export const PUSH_SUBSCRIPTION_ID_KEY = 'chongchong:push-subscription-id';
export const PUSH_ENABLED_KEY = 'chongchong:push-enabled';

export async function clearLocalPushSubscription() {
  if ('serviceWorker' in navigator) {
    const scope = new URL('/push/', window.location.origin).href;
    const sw = await navigator.serviceWorker.getRegistration(scope);

    if (sw?.scope === scope) {
      const subscription = await sw.pushManager.getSubscription();
      await subscription?.unsubscribe();
    }
  }

  localStorage.removeItem(PUSH_SUBSCRIPTION_ID_KEY);
  localStorage.removeItem(PUSH_ENABLED_KEY);
}
