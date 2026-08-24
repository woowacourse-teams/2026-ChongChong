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
          closeAt: '2026-08-25T23:59:59',
          memberCount: 4,
          completeCount: 2,
          isComplete: false,
        },
        {
          id: 2,
          title: '9월의 스터디',
          content: '치킨 피자',
          submissionType: '링크 제출',
          closeAt: '2026-08-25T23:59:59',
          memberCount: 4,
          completeCount: 2,
          isComplete: true,
        },
      ],
    });
  }),

  http.get(
    `${BASE_URL}/studies/:studyId/assignments/:assignmentId/completions-status`,
    ({ params }) => {
      return HttpResponse.json({
        id: Number(params.assignmentId),
        memberCount: 4,
        completeCount: 2,
        incompleteCount: 2,
        remindAt: '2025-04-16T16:44:10',
        completeMembers: [
          {
            id: 1,
            name: '안톨리니',
            profileImage: 'https://example.com/profile.png',
          },
          {
            id: 1,
            name: '피즈',
            profileImage: 'https://example.com/profile.png',
          },
        ],
        incompleteMembers: [
          {
            id: 2,
            name: '바니',
            profileImage: 'https://example.com/profile2.png',
            lastRemindAt: '2025-04-16T16:44:10',
          },
          {
            id: 1,
            name: '이든',
            profileImage: 'https://example.com/profile.png',
          },
        ],
      });
    },
  ),

  http.get(`${BASE_URL}/studies/:studyId/assignments/:assignmentId`, ({ params }) => {
    return HttpResponse.json({
      id: Number(params.assignmentId),
      title: '8월부터 스터디 운영 방식이 바뀝니다.',
      closeAt: '2025-04-16T16:44:10',
      content: '8월부터 스터디 운영 방식을 변경하려고 합니다.',
      submissionType: '링크를 통해 제출',
    });
  }),
];
