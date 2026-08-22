import { http, HttpResponse } from 'msw';
import { BASE_URL } from '../../../../config';

export const handlers = [
  http.get(`${BASE_URL}/studies/:studyId/assignments`, () => {
    return HttpResponse.json({
      assignments: [
        {
          id: 1,
          title: '8월 스터디 운영 방식이 바뀝니다',
          content: '8월부터 스터디 운영 방식을 변경하려고 합니다.',
          submissionType: '링크 제출',
          closeAt: '2025-04-16 16:44:10',
          memberCount: 4,
          completeCount: 2,
          remindAt: '2025-04-16 16:44:10',
          isComplete: false,
        },
        {
          id: 2,
          title: '9월의 스터디',
          content: '치킨 피자',
          submissionType: '링크 제출',
          closeAt: '2025-04-16 16:44:10',
          memberCount: 4,
          completeCount: 2,
          remindAt: '2025-04-16 16:44:10',
          isComplete: true,
        },
      ],
    });
  }),
];
