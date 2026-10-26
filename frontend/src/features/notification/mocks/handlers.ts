import { http, HttpResponse, passthrough } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { notificationTable, webPushSubscriptionTable } from './db';

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
  http.get(`${API_URL}/web-push-subscriptions/me`, ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const installationId = new URL(request.url).searchParams.get('installationId');
    if (!installationId?.trim()) return new HttpResponse(null, { status: 400 });
    const subscription = webPushSubscriptionTable.findFirst((q) =>
      q.where({ userId: user.id, installationId }),
    );
    return HttpResponse.json({
      isActive: subscription?.isActive ?? false,
      subscriptionId: subscription?.subscriptionId ?? null,
    });
  }),
  http.post(`${API_URL}/web-push-subscriptions`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const { installationId } = (await request.json()) as { installationId: string };
    if (!installationId?.trim()) return new HttpResponse(null, { status: 400 });
    const existing = webPushSubscriptionTable.findFirst((q) =>
      q.where({ userId: user.id, installationId }),
    );
    if (existing) {
      webPushSubscriptionTable.update(existing, {
        data(subscription) {
          subscription.isActive = true;
        },
      });
      return HttpResponse.json({ subscriptionId: existing.subscriptionId });
    }
    const subscriptionId = webPushSubscriptionTable.findMany().length + 1;
    await webPushSubscriptionTable.create({
      subscriptionId,
      userId: user.id,
      installationId,
      isActive: true,
    });
    return HttpResponse.json({ subscriptionId });
  }),
  http.delete(`${API_URL}/web-push-subscriptions/:subscriptionId`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const subscription = webPushSubscriptionTable.findFirst((q) =>
      q.where({ userId: user.id, subscriptionId: Number(params.subscriptionId) }),
    );
    if (subscription)
      webPushSubscriptionTable.update(subscription, {
        data(subscription) {
          subscription.isActive = false;
        },
      });
    return new HttpResponse(null, { status: 204 });
  }),
];
