/// <reference lib="webworker" />
import { recordNotificationDeliveryClick } from './api';
export {};

declare const self: ServiceWorkerGlobalScope;

// 서비스 워커 설치 이벤트
self.addEventListener('install', (event) => {
  // skipWaiting(기존워커와 관련없이 활성화한다)
  // 비동기 작업을 이벤트 처리 작업으로 연결해서 브라우저가 작업이 끝날때까지 기다리게 한다.
  event.waitUntil(self.skipWaiting());
});

self.addEventListener('push', (event) => {
  // TODO: event.data.json은 어떤 값이 오게될지 예상되지 않습니다. (런타임 타입검증이 필요합니다)
  const { title, body, deliveryId, clickToken, deepLink } = event.data!.json();

  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      data: { deliveryId, clickToken, deepLink },
    }),
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const { deliveryId, clickToken, deepLink } = event.notification.data;

  event.waitUntil(
    Promise.allSettled([
      recordNotificationDeliveryClick(deliveryId, clickToken),
      // TODO: deepLink또한 무조건 전달될지에 대한 보장이 판단되지 않습니다
      // fallback이 필요할 수 있으며 응답 타입에 대한 정확한 파악이 필요합니다
      self.clients.openWindow(deepLink),
    ]),
  );
});
