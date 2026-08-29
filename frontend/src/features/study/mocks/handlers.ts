import { http, HttpResponse } from 'msw';
import { studyTable } from './db';
import { API_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';
import { CURRENT_USER } from '../../../mocks/currentUser';
import { memberTable } from '../../member/mocks/db';

export const handlers = [
  http.get(`${API_URL}${STUDY_URLS.list}`, async () => {
    const memberships = await memberTable.findMany((q) => q.where({ id: CURRENT_USER.id }));
    const studies = await Promise.all(
      memberships.map(async (membership) => {
        const study = await studyTable.findFirst((q) => q.where({ id: membership.studyId }));
        if (!study) return null;
        const members = await memberTable.findMany((q) => q.where({ studyId: study.id }));
        return {
          id: String(study.id),
          role: membership.role === 'LEADER' ? 'LEADER' : 'MEMBER',
          name: study.name,
          description: study.description,
          memberCount: members.length,
          // 공지/과제는 아직 mock table 이 없어 고정값을 사용합니다.
          noticeCount: 2,
          assignmentCount: 2,
        };
      }),
    );
    return HttpResponse.json({ studies: studies.filter((study) => study !== null) });
  }),

  http.post(`${API_URL}${STUDY_URLS.create}`, async ({ request }) => {
    const body = (await request.json()) as { name: string; description: string };
    // msw 로직은 실제 backend API 로 대체될 예정입니다.
    // if (invalidInput) {
    //   return HttpResponse.json(invalidInput, { status: 400 });
    // }

    const studyId = Date.now();
    await studyTable.create({ id: studyId, inviteLink: 'chongchong.app/join/new', ...body });
    await memberTable.create({
      id: CURRENT_USER.id,
      studyId,
      name: CURRENT_USER.name,
      profileImage: 'http://localhost:8000',
      role: 'LEADER',
    });
    return HttpResponse.json({ studyId }, { status: 201 });
  }),

  http.get(`${API_URL}${STUDY_URLS.info}`, async ({ params }) => {
    const { studyId } = params;
    const found = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!found) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      studyName: found.name,
      role: 'LEADER',
      userName: CURRENT_USER.name,
    });
  }),

  http.get(`${API_URL}${STUDY_URLS.inviteLink}`, async ({ params }) => {
    const { studyId } = params;
    const found = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!found) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      inviteLink: found.inviteLink,
    });
  }),

  http.delete(`${API_URL}${STUDY_URLS.remove}`, async ({ params }) => {
    const { studyId } = params;
    const study = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!study) return new HttpResponse(null, { status: 404 });
    studyTable.delete(study);
    return new HttpResponse(null, { status: 204 });
  }),
];
