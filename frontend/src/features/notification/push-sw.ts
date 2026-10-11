/// <reference lib="webworker" />

export {};

declare const self: ServiceWorkerGlobalScope;

interface PushPayload {
  notificationId?: number;
  title?: string;
  body?: string;
  deepLink?: string;
}

// 서비스 워커 설치 이벤트
self.addEventListener('install', (event) => {
  // skipWaiting(기존워커와 관련없이 활성화한다)
  // 비동기 작업을 이벤트 처리 작업으로 연결해서 브라우저가 작업이 끝날때까지 기다리게 한다.
  event.waitUntil(self.skipWaiting());
});

self.addEventListener('push', (event) => {
  let payload: PushPayload;

  try {
    payload = event.data?.json() ?? {};
  } catch {
    // payload에 데이터가 없는경우에는 애초에 알림이 안가도록 하는 처리가 필요할듯
    payload = {};
  }

  event.waitUntil(
    self.registration.showNotification(payload.title ?? '총총', {
      body: payload.body ?? '새로운 알림이 도착했어요.',
      icon: '/pwa/images/icon-192.webp',
      data: {
        notificationId: payload.notificationId,
        deepLink: payload.deepLink ?? '/studies',
      },
    }),
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const url = new URL(event.notification.data?.deepLink ?? '/studies', self.location.origin);

  if (url.origin !== self.location.origin) return;

  event.waitUntil(self.clients.openWindow(url.href));
});
