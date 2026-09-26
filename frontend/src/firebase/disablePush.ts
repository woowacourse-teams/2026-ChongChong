import { getMessaging, isSupported, unregister } from 'firebase/messaging';
import { app } from './settingFCM';

export async function disablePush() {
  if (!(await isSupported())) {
    throw new Error('알림을 지원하지 않는 환경입니다.');
  }

  // FCM 등록 해제
  await unregister(getMessaging(app));

  const scope = new URL('/firebase-cloud-messaging-push-scope', window.location.origin).href;

  const sw = await navigator.serviceWorker.getRegistration(scope);

  if (sw?.scope !== scope) return;

  // 브라우저 구독 해제, 허용 권한은 유지한채 푸시 수신 등록만 해제
  const subscription = await sw.pushManager.getSubscription();
  await subscription?.unsubscribe();
}
