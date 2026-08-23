import { http, HttpResponse } from 'msw';
import { study } from './db';
import { BASE_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';

export const handlers = [
  http.get(`${BASE_URL}${STUDY_URLS.list}`, () => {
    return HttpResponse.json({
      studies: [
        {
          id: '1',
          role: 'STUDY_LEADER',
          title: '리액트 스터디',
          description: '매주 화요일 10시에 진행하는 리액트 스터디',
          memberCount: 3,
          noticeCount: 2,
          assignmentCount: 2,
        },
        {
          id: '2',
          role: 'SOME',
          title: '우테코 8기 FE 스터디',
          description: '매주 화요일 저녁 9시, 프론트엔드 CS와 코드 리뷰',
          memberCount: 5,
          noticeCount: 2,
          assignmentCount: 1,
        },
      ],
    });
  }),

  http.post(`${BASE_URL}${STUDY_URLS.create}`, async ({ request }) => {
    const body = (await request.json()) as { name: string; description: string };
    // msw 로직은 실제 backend API 로 대체될 예정입니다.
    // if (invalidInput) {
    //   return HttpResponse.json(invalidInput, { status: 400 });
    // }

    const studyId = Date.now();
    await study.create({ id: studyId, ...body });
    return HttpResponse.json({ studyId }, { status: 201 });
  }),

  http.get(`${BASE_URL}${STUDY_URLS.info}`, async ({ params }) => {
    const { studyId } = params;
    const found = await study.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!found) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      studyName: found.name,
      role: 'LEADER',
      memberName: '바니',
    });
  }),
];
