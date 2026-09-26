import { API_URL } from '../../../config';
import api from '../../client';
import { handleError, ValidationError } from '../../shared/api/error';
import { isNotificationResponse } from './responseSchemas';
import ky from 'ky';

interface WebPushSubscriptionRequest {
  endpoint: string;
  keys: {
    p256dh: string;
    auth: string;
  };
}

export const NOTIFICATION_API_URLS = {
  list: '/notifications',
  detail: (notificationId: number) => `/notifications/${notificationId}`,
};

export async function fetchNotifications() {
  try {
    const response = await api.get(NOTIFICATION_API_URLS.list);
    const data: unknown = await response.json();
    if (!isNotificationResponse(data)) {
      throw new Error('알림 목록 응답 형식이 올바르지 않습니다.');
    }
    return data.notifications;
  } catch (error) {
    throw new Error('알림 목록을 불러오는데 실패했습니다.', { cause: error });
  }
}

export async function markNotificationAsRead(notificationId: number) {
  try {
    await api.patch(NOTIFICATION_API_URLS.detail(notificationId));
  } catch (error) {
    throw handleError(error, {
      mappers: [ValidationError],
      fallback: new Error('알림을 읽음으로 처리하지 못했습니다.', { cause: error }),
    });
  }
}

export async function getWebPushPublicKey() {
  const { publicKey } = await ky.get(`${API_URL}/web-push/config`).json<{ publicKey: string }>();

  return publicKey;
}

export async function registerWebPushSubscription(subscription: WebPushSubscriptionRequest) {
  const { subscriptionId } = await api
    .post('/web-push-subscriptions', {
      json: subscription,
    })
    .json<{ subscriptionId: number }>();

  return subscriptionId;
}

export async function deactivateWebPushSubscription(subscriptionId: number) {
  await api.delete(`/web-push-subscriptions/${subscriptionId}`);
}
