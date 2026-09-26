import {
  getWebPushPublicKey,
  registerWebPushSubscription,
  deactivateWebPushSubscription,
} from './api';
import { clearLocalPushSubscription, PUSH_SUBSCRIPTION_ID_KEY } from './localPush';

export async function enablePush() {
  if (
    !('Notification' in window) ||
    !('serviceWorker' in navigator) ||
    !('PushManager' in window)
  ) {
    throw new Error(
      '알림을 지원하지 않는 환경입니다. iPhone에서는 홈 화면에 추가한 앱에서 사용해 주세요.',
    );
  }

  if (Notification.permission === 'denied') {
    throw new Error('기기 또는 브라우저 설정에서 알림을 허용해 주세요.');
  }

  const permission =
    Notification.permission === 'granted' ? 'granted' : await Notification.requestPermission();

  if (permission !== 'granted') return false;

  const scope = new URL('/push/', window.location.origin).href;
  const sw = await navigator.serviceWorker.getRegistration(scope);

  if (sw?.scope !== scope || sw.active?.state !== 'activated') {
    throw new Error('알림 준비 중입니다. 잠시 후 다시 눌러 주세요.');
  }

  const publicKey = await getWebPushPublicKey();

  const subscription = await sw.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: publicKey,
  });

  try {
    const { endpoint, keys } = subscription.toJSON();

    if (!endpoint || !keys?.p256dh || !keys.auth) {
      throw new Error('푸시 구독 정보를 확인하지 못했어요.');
    }

    const subscriptionId = await registerWebPushSubscription({
      endpoint,
      keys: {
        p256dh: keys.p256dh,
        auth: keys.auth,
      },
    });

    localStorage.setItem(PUSH_SUBSCRIPTION_ID_KEY, String(subscriptionId));
  } catch (error) {
    await subscription.unsubscribe().catch(console.error);
    throw error;
  }

  return true;
}

export async function disablePush() {
  const subscriptionId = localStorage.getItem(PUSH_SUBSCRIPTION_ID_KEY);

  if (subscriptionId) {
    await deactivateWebPushSubscription(Number(subscriptionId));
  }

  await clearLocalPushSubscription();
}
