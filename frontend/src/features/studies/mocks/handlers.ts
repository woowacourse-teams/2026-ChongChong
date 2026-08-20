import { http, HttpResponse } from 'msw';
import { study } from './db';
import { validateStudy } from './validators';
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

    const invalidInput = validateStudy(body);
    if (invalidInput) {
      return HttpResponse.json(invalidInput, { status: 400 });
    }

    const studyId = Date.now();
    await study.create({ id: studyId, ...body });
    return HttpResponse.json({ studyId }, { status: 201 });
  }),
];
