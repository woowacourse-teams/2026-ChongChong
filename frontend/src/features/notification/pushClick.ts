import { getAccessToken } from '../login/accessToken';
import { recordNotificationDeliveryClick } from './api';

// 앱을 열 때 전달받은 번호만 읽고, 목적지 주소의 나머지 정보는 유지합니다.
export function readPushDeliveryId(): string | null {
  const url = new URL(window.location.href);
  const deliveryId = url.searchParams.get('pushDeliveryId');
  if (!deliveryId?.trim() || deliveryId === 'undefined' || deliveryId === 'null') return null;

  url.searchParams.delete('pushDeliveryId');
  window.history.replaceState(window.history.state, '', url.pathname + url.search + url.hash);
  return deliveryId;
}

// 기존 세션이 준비된 경우에만 기록하며, 응답을 기다리지 않습니다.
export function recordPushClick(deliveryId: string | null): void {
  if (!deliveryId || !getAccessToken()) return;

  try {
    recordNotificationDeliveryClick(deliveryId);
  } catch (error) {
    console.error('푸시 클릭 기록 실패', error);
  }
}
