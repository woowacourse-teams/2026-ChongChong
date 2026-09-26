import { http, HttpResponse, passthrough } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { notificationTable } from './db';

export const handlers = [
  http.get(`${API_URL}/notifications`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const notifications = await notificationTable.findMany((q) =>
      q.where({ userId: Number(user.id) }),
    );
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const notificationsResponse = notifications.map(({ userId, ...rest }) => rest);
    return HttpResponse.json({
      notifications: notificationsResponse,
    });
  }),

  http.patch(`${API_URL}/notifications/:notificationId`, async ({ request, params }) => {
    const { notificationId } = params;
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const notification = notificationTable.findFirst((q) =>
      q.where({ id: Number(notificationId), userId: Number(user.id) }),
    );
    if (!notification) return new HttpResponse(null, { status: 404 });
    await notificationTable.update(notification, {
      data(currentNotification) {
        currentNotification.isRead = true;
      },
    });
    return new HttpResponse(null, { status: 204 });
  }),
  http.get(`${API_URL}/web-push/config`, () => passthrough()),
];
