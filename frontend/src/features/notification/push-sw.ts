/// <reference lib="webworker" />

export {};

declare const self: ServiceWorkerGlobalScope;

// 서비스 워커 설치 이벤트
self.addEventListener('install', (event) => {
  // skipWaiting(기존워커와 관련없이 활성화한다)
  // 비동기 작업을 이벤트 처리 작업으로 연결해서 브라우저가 작업이 끝날때까지 기다리게 한다.
  event.waitUntil(self.skipWaiting());
});
