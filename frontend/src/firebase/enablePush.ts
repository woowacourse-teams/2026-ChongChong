import { getMessaging, isSupported, onRegistered, register } from 'firebase/messaging';
import { app } from './settingFCM';

export async function enablePush() {
  if (!('Notification' in window) || !('serviceWorker' in navigator)) {
    throw new Error('알림을 지원하지 않는 환경입니다.');
  }

  // vapidKey가 설정되어있는지
  const vapidKey = process.env.FIREBASE_VAPID_KEY;
  if (!vapidKey) {
    throw new Error('VAPID 공개 키가 없습니다.');
  }

  if (Notification.permission === 'denied') {
    throw new Error('기기 또는 브라우저 설정에서 알림을 허용해 주세요.');
  }

  const permission =
    Notification.permission === 'granted' ? 'granted' : await Notification.requestPermission();

  if (permission !== 'granted') return false;

  if (!(await isSupported())) {
    throw new Error('FCM을 지원하지 않는 환경입니다.');
  }

  const scope = new URL('/firebase-cloud-messaging-push-scope', window.location.origin).href;
  const sw = await navigator.serviceWorker.getRegistration(scope);

  // 준비전에 구독하는걸 방지한다 (SW)
  if (sw?.scope !== scope || sw.active?.state !== 'activated') {
    throw new Error('알림 준비 중입니다. 잠시 후 다시 눌러 주세요.');
  }

  const messaging = getMessaging(app);

  // 등록결과인 FID 받기
  onRegistered(messaging, (fid) => {
    console.log('FCM 등록 FID:', fid);
  });

  // 찐 등록
  await register(messaging, {
    vapidKey,
    serviceWorkerRegistration: sw,
  });

  return true;
}
