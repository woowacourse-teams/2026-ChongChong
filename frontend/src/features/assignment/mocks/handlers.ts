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
            id: 2,
            name: '피즈',
            profileImage: 'https://example.com/profile.png',
          },
        ],
        incompleteMembers: [
          {
            id: 3,
            name: '바니',
            profileImage: 'https://example.com/profile2.png',
            lastRemindAt: '2025-04-16T16:44:10',
          },
          {
            id: 4,
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
      title: '이번주 그리디 3문제 풀이',
      closeAt: '2025-04-16T16:44:10',
      content:
        '백준에서 문제 푸시고 링크 올려주시면 됩니다. 그리디 문제집에서 원하는 세 문제를 풀고 올려주세요.',
      submissionType:
        'GitHub 저장소에 문제 번호로 폴더를 만들어 올린 뒤, 저장소나 PR 링크를 제출해주세요.',
    });
  }),

  http.get(`${BASE_URL}/studies/:studyId/assignments/:assignmentId/submissions`, () => {
    return HttpResponse.json({
      submissions: [
        {
          id: 1,
          name: '피즈',
          profileImage: 'http://localhost:8080',
          createdAt: '2025-04-16 16:44:10',
        },
        {
          id: 2,
          name: '이든',
          profileImage: 'http://localhost:8080',
          createdAt: '2025-04-16 16:44:10',
        },
        {
          id: 3,
          name: '바니',
          profileImage: 'http://localhost:8080',
          createdAt: '2025-04-16 16:44:10',
        },
      ],
    });
  }),
];
