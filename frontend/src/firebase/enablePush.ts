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

  // 알림 요청 권한 받는다.
  const permission = await Notification.requestPermission();
  // 알림 안받음
  if (permission !== 'granted') return;

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
}
