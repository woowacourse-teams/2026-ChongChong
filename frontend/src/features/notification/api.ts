import api from '../../client';
import { isNotificationResponse } from './responseSchemas';

export const NOTIFICATION_API_URLS = {
  list: '/notifications',
};

export async function fetchNotifications() {
  try {
    const response = await api.get('/notifications');
    const data: unknown = await response.json();
    if (!isNotificationResponse(data)) {
      throw new Error('알림 목록 응답 형식이 올바르지 않습니다.');
    }
    return data;
  } catch (error) {
    throw new Error('알림 목록을 불러오는데 실패했습니다.', { cause: error });
  }
}
