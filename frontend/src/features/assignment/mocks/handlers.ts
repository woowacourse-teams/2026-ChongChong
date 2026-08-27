import { http, HttpResponse } from 'msw';
import { BASE_URL } from '../../../../config';

const assignmentList = Array.from({ length: 12 }, (_, index) => {
  const id = 12 - index;

  return {
    id,
    title: `${id}주차 과제`,
    content: `${id}주차 학습 내용을 정리하고 풀이 링크를 제출해주세요.`,
    submissionMethod: id % 2 === 0 ? 'GitHub PR' : '링크 제출',
    closeAt: `2026-09-${String(id).padStart(2, '0')}T23:59:59`,
    memberCount: 4,
    completeCount: id % 5,
    isComplete: id % 4 === 0,
  };
});

export const handlers = [
  http.get(`${BASE_URL}/studies/:studyId/assignments`, ({ request }) => {
    const searchParams = new URL(request.url).searchParams;
    const cursor = searchParams.get('cursor');
    const requestedSize = Number(searchParams.get('size') ?? 4);
    const size = Number.isInteger(requestedSize) && requestedSize > 0 ? requestedSize : 4;
    const cursorIndex = cursor
      ? assignmentList.findIndex((assignment) => assignment.id === Number(cursor))
      : 0;
    const startIndex = cursorIndex >= 0 ? cursorIndex : 0;
    const assignments = assignmentList.slice(startIndex, startIndex + size);
    const nextIndex = startIndex + assignments.length;
    const hasNext = nextIndex < assignmentList.length;
    const nextCursor = hasNext
      ? assignmentList[nextIndex].id
      : (assignments[assignments.length - 1]?.id ?? 0);

    return HttpResponse.json({ nextCursor, hasNext, assignments });
  }),

  http.post(`${BASE_URL}/studies/:studyId/assignments`, () =>
    HttpResponse.json({ assignmentId: 3 }, { status: 201 }),
  ),

  http.patch(
    `${BASE_URL}/studies/:studyId/assignments/:assignmentId`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.delete(`${BASE_URL}/studies/:studyId/assignments/:assignmentId`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  http.get(`${BASE_URL}/studies/:studyId/assignments/:assignmentId/status`, ({ params }) => {
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
  }),

  http.get(`${BASE_URL}/studies/:studyId/assignments/:assignmentId`, ({ params }) => {
    const assignmentId = Number(params.assignmentId);

    return HttpResponse.json({
      id: assignmentId,
      title: '이번주 그리디 3문제 풀이',
      closeAt: '2025-04-16T16:44:10',
      content:
        '백준에서 문제 푸시고 링크 올려주시면 됩니다. 그리디 문제집에서 원하는 세 문제를 풀고 올려주세요.',
      submissionMethod:
        'GitHub 저장소에 문제 번호로 폴더를 만들어 올린 뒤, 저장소나 PR 링크를 제출해주세요.',
      ...(assignmentId % 4 === 0 && { submissionId: assignmentId + 100 }),
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

  http.get(
    `${BASE_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`,
    ({ params }) => {
      return HttpResponse.json({
        id: Number(params.submissionId),
        name: '피즈',
        profileImage: 'http://localhost:8080',
        createdAt: '2025-04-16 16:44:10',
        content: '과제 제출합니다.',
        link: 'http://localhost:8080',
      });
    },
  ),

  http.post(`${BASE_URL}/studies/:studyId/assignments/:assignmentId/submissions`, () =>
    HttpResponse.json({ submissionId: 4 }, { status: 201 }),
  ),

  http.patch(
    `${BASE_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`,
    () => new HttpResponse(null, { status: 204 }),
  ),
];
