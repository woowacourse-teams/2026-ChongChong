import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('https://mock.chongchong.com/studies', () => {
    return HttpResponse.json({
      notifications: [
        {
          id: 1,
          title: '8월 스터디 운영 방식이 바뀝니다',
          content: '8월부터 스터디 운영 방식을 변경하려고 합니다.',
          createdAt: '2025-04-16 16:44:10',
          memberCount: 4,
          completeCount: 2,
          remindedAt: '2025-04-16 16:44:10',
          isComplete: false,
        },
      ],
    });
  }),
];
