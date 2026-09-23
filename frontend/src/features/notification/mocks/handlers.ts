import { http, HttpResponse } from 'msw';
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
    const notificationsResponse = notifications.map(({ userId, ...rest }) => rest);
    return HttpResponse.json(notificationsResponse);
  }),
];
