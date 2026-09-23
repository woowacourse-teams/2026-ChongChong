import { QueryClient } from '@tanstack/react-query';
import { screen, within } from '@testing-library/react';
import { notificationTable, type NotificationSchemaType } from '../../mocks/db';
import { userTable } from '../../../user/mocks/db';
import notificationQueries from '../../queries';
import NotificationLink from '../NotificationLink';
import { login, logout, setup, createWrapper } from '../../../../test/render';
import { waitForQuerySuccess } from '../../../../test/query';

const notification = {
  id: 1,
  userId: 1,
  title: '알림',
  body: '...',
  type: 'CREATED',
  resourceType: 'ASSIGNMENT',
  resourceId: 1,
  deepLink: '/studies/1/assignments/1',
  createdAt: '2025-09-21T12:00:00',
} satisfies Partial<NotificationSchemaType>;

const readNotification = {
  ...notification,
  id: 2,
  isRead: true,
};

const unreadNotification = {
  ...notification,
  isRead: false,
};

describe('알림 링크 테스트', () => {
  let queryClient: QueryClient;

  beforeEach(async () => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    await userTable.create({
      id: 1,
      name: '깡이',
      profileImage: 'http://localhost:8000',
    });
    login('깡이');
  });

  afterEach(() => {
    queryClient.clear();
    notificationTable.clear();
    logout();
  });

  describe('읽지 않은 알림이 존재한다', () => {
    beforeEach(async () => {
      await notificationTable.create(unreadNotification);
      await notificationTable.create(readNotification);
    });

    test('특별한 알림 아이콘이 렌더링 된다', async () => {
      setup(<NotificationLink />, { wrapper: createWrapper({ queryClient }) });
      await waitForQuerySuccess(queryClient, notificationQueries.lists());

      const link = screen.getByRole('link');
      expect(within(link).getByTestId('has-unread-icon')).toBeVisible();
    });

    test('읽지 않은 알림이 존재한다는걸 안내한다', async () => {
      setup(<NotificationLink />, { wrapper: createWrapper({ queryClient }) });
      await waitForQuerySuccess(queryClient, notificationQueries.lists());

      const link = screen.getByRole('link');
      expect(link).toHaveAccessibleName('알림, 읽지 않은 알림 있음');
    });
  });

  describe('읽지 않은 알림이 존재하지 않는다', () => {
    beforeEach(async () => {
      await notificationTable.create(readNotification);
    });

    test('일반 알림 아이콘이 렌더링 된다', async () => {
      setup(<NotificationLink />, { wrapper: createWrapper({ queryClient }) });
      await waitForQuerySuccess(queryClient, notificationQueries.lists());

      const link = screen.getByRole('link');
      expect(within(link).getByTestId('no-unread-icon')).toBeVisible();
    });

    test('알림이라는걸 안내한다', async () => {
      setup(<NotificationLink />, { wrapper: createWrapper({ queryClient }) });
      await waitForQuerySuccess(queryClient, notificationQueries.lists());

      const link = screen.getByRole('link');
      expect(link).toHaveAccessibleName('알림');
    });
  });

  describe('알림 자체가 존재하지 않는다', () => {
    test('일반 알림 아이콘이 렌더링 된다', async () => {
      setup(<NotificationLink />, { wrapper: createWrapper({ queryClient }) });
      await waitForQuerySuccess(queryClient, notificationQueries.lists());

      const link = screen.getByRole('link');
      expect(within(link).getByTestId('no-unread-icon')).toBeVisible();
    });

    test('알림이라는걸 안내한다', async () => {
      setup(<NotificationLink />, { wrapper: createWrapper({ queryClient }) });
      await waitForQuerySuccess(queryClient, notificationQueries.lists());

      const link = screen.getByRole('link');
      expect(link).toHaveAccessibleName('알림');
    });
  });
});
