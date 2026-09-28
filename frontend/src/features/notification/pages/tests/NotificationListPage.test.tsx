import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { login, logout } from '../../../../test/render';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { assignmentTable } from '../../../assignment/mocks/db';
import { notificationTable } from '../../mocks/db';
import NotificationListPage from '../NotificationListPage';
import { createWrapper, setup } from '../../../../test/render';
import * as notificationApi from '../../api';

describe('알림 목록 페이지 테스트', () => {
  const testerId = 1;

  beforeEach(async () => {
    await userTable.create({
      id: testerId,
      name: '깡이',
      profileImage: 'http://localhost:8000',
    });
    login('깡이');
  });

  afterEach(() => {
    notificationTable.clear();
    logout();
  });

  describe('알림이 있는 경우', () => {
    beforeEach(async () => {
      await studyTable.create({
        id: 1,
        name: '치와와 스터디',
        description: '말 안듣고 사나운 치와와는 왜그럴까?',
        inviteLink: 'kkang',
      });
      await assignmentTable.create({
        id: 1,
        studyId: 1,
        title: '주인말 안듣기',
        content: '주인말 안듣고 밥만 달라 하기',
        submissionMethod: '말 안들은걸 음성으로 제출하세요',
        closeAt: '2025-12-31T23:59:59',
        submissionTarget: 'MEMBERS_AND_LEADER',
        completeUserIds: [],
      });
      await notificationTable.create({
        id: 1,
        userId: testerId,
        title: '[치와와 스터디] 새 과제',
        body: '삼겹살 얻어먹기',
        type: 'NEW',
        resourceType: 'ASSIGNMENT',
        resourceId: 1,
        deepLink: '/studies/1/assignments/1',
        isRead: false,
        createdAt: '2025-09-21T12:00:00',
      });
      await notificationTable.create({
        id: 2,
        userId: testerId,
        title: '[치와와 스터디] 새 공지',
        body: '소금빵 얻어먹기',
        type: 'NEW',
        resourceType: 'NOTICE',
        resourceId: 2,
        deepLink: '/studies/1/notices/2',
        isRead: true,
        createdAt: '2025-09-21T12:00:00',
      });
      await notificationTable.create({
        id: 3,
        userId: testerId,
        title: '[치와와 스터디] 과제 리마인드',
        body: '백숙 얻어먹기',
        type: 'REMIND',
        resourceType: 'ASSIGNMENT',
        resourceId: 3,
        deepLink: '/studies/1/assignments/3',
        isRead: false,
        createdAt: '2025-09-21T12:00:00',
      });
    });

    test('각 알림의 제목과 해당 알림의 상세 링크를 표시한다', async () => {
      setup(<NotificationListPage />, { wrapper: createWrapper() });

      const expectedNotifications = [
        { title: '[치와와 스터디] 새 과제', deepLink: '/studies/1/assignments/1' },
        { title: '[치와와 스터디] 새 공지', deepLink: '/studies/1/notices/2' },
        { title: '[치와와 스터디] 과제 리마인드', deepLink: '/studies/1/assignments/3' },
      ];

      for (const { title, deepLink } of expectedNotifications) {
        const link = await screen.findByRole('link', {
          name: (name) => name.includes(title),
        });

        expect(within(link).getByText(title)).toBeVisible();
        expect(link).toHaveAttribute('href', deepLink);
      }
    });

    test(`확인하지 않은 알림은 '미확인' 알림 아이콘이 표시된다`, async () => {
      setup(<NotificationListPage />, { wrapper: createWrapper() });

      const unreadNotification = await screen.findByRole('link', {
        name: (name) => name.includes('삼겹살 얻어먹기'),
      });

      expect(within(unreadNotification).getByRole('img', { name: '읽지 않은 알림' })).toBeVisible();
    });

    test(`확인한 알림은 '미확인' 알림 아이콘이 표시되지 않는다`, async () => {
      setup(<NotificationListPage />, { wrapper: createWrapper() });

      const readNotification = await screen.findByRole('link', {
        name: (name) => name.includes('소금빵 얻어먹기'),
      });

      expect(
        within(readNotification).queryByRole('img', { name: '읽지 않은 알림' }),
      ).not.toBeInTheDocument();
    });

    describe('알림을 클릭한 경우', () => {
      test('미확인 알림의 읽음 처리를 요청한다', async () => {
        const markAsRead = jest.spyOn(notificationApi, 'markNotificationAsRead');
        const { user } = setup(<NotificationListPage />, { wrapper: createWrapper() });
        const notification = await screen.findByRole('link', {
          name: (name) => name.includes('삼겹살 얻어먹기'),
        });

        await user.click(notification);

        await waitFor(() =>
          expect(notificationTable.findFirst((q) => q.where({ id: 1 }))?.isRead).toBe(true),
        );
        expect(markAsRead.mock.calls.map(([notificationId]) => notificationId)).toEqual([1]);
      });

      test('이미 확인한 알림은 읽음 요청을 생략한다', async () => {
        const markAsRead = jest.spyOn(notificationApi, 'markNotificationAsRead');
        const { user } = setup(<NotificationListPage />, { wrapper: createWrapper() });
        const notification = await screen.findByRole('link', {
          name: (name) => name.includes('소금빵 얻어먹기'),
        });

        await user.click(notification);

        expect(markAsRead).not.toHaveBeenCalled();
      });

      test('읽음 처리에 성공하면 미확인 표시를 갱신한다', async () => {
        const { user } = setup(<NotificationListPage />, { wrapper: createWrapper() });
        const notification = await screen.findByRole('link', {
          name: (name) => name.includes('삼겹살 얻어먹기'),
        });
        expect(within(notification).getByRole('img', { name: '읽지 않은 알림' })).toBeVisible();

        await user.click(notification);

        await waitFor(() => {
          const updatedNotification = screen.getByRole('link', {
            name: (name) => name.includes('삼겹살 얻어먹기'),
          });
          expect(updatedNotification).toBeVisible();
          expect(
            within(updatedNotification).queryByRole('img', { name: '읽지 않은 알림' }),
          ).not.toBeInTheDocument();
        });
      });

      test('읽음 처리에 실패하면 오류 토스트를 표시한다', async () => {
        server.use(
          http.patch(`${API_URL}/notifications/:notificationId`, () => HttpResponse.error()),
        );
        const { user } = setup(<NotificationListPage />, { wrapper: createWrapper() });
        const notification = await screen.findByRole('link', {
          name: (name) => name.includes('삼겹살 얻어먹기'),
        });

        await user.click(notification);

        const toast = await screen.findByRole('status');
        expect(toast).toHaveTextContent('알림을 읽음으로 처리하지 못했습니다.');
        expect(toast).toBeVisible();
      });
    });

    test('리마인드 알림 아이콘은 리마인드 알림으로 안내한다', async () => {
      setup(<NotificationListPage />, { wrapper: createWrapper() });

      const reminderNotification = await screen.findByRole('link', {
        name: (name) => name.includes('백숙 얻어먹기'),
      });

      expect(
        within(reminderNotification).getByRole('img', { name: '리마인드 알림' }),
      ).toBeVisible();
    });

    test('일반 알림 아이콘은 일반 알림으로 안내한다', async () => {
      setup(<NotificationListPage />, { wrapper: createWrapper() });

      const notification = await screen.findByRole('link', {
        name: (name) => name.includes('삼겹살 얻어먹기'),
      });

      expect(within(notification).getByRole('img', { name: '일반 알림' })).toBeVisible();
    });
  });

  test('알림이 존재하지 않는 경우 빈 컨텐츠를 렌더링한다', async () => {
    setup(<NotificationListPage />, { wrapper: createWrapper() });

    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
    expect(await screen.findByText('아직 알림이 없어요')).toBeInTheDocument();
  });

  test('알림을 불러오는데 실패한 경우 에러 컨텐츠를 렌더링한다', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(http.get(`${API_URL}/notifications`, () => HttpResponse.error()));
    setup(<NotificationListPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('알림 목록을 불러오는데 실패했습니다.'));
  });
});
