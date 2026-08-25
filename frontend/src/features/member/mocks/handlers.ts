import { http, HttpResponse } from 'msw';
import { BASE_URL } from '../../../../config';
import { MEMBER_URLS } from '../urls';
import { memberTable } from './db';
import { CURRENT_USER } from '../../../mocks/currentUser';

export const handlers = [
  http.get(`${BASE_URL}${MEMBER_URLS.list}`, async ({ params }) => {
    const { studyId } = params;
    const members = await memberTable.findMany((q) => q.where({ studyId: Number(studyId) }));
    return HttpResponse.json({ members });
  }),

  http.delete(`${BASE_URL}${MEMBER_URLS.leave}`, async ({ params }) => {
    const { studyId } = params;
    const member = await memberTable.findFirst((q) =>
      q.where({ studyId: Number(studyId), id: CURRENT_USER.id }),
    );
    if (!member) return new HttpResponse(null, { status: 404 });
    // 스터디 리드는 탈퇴할 수 없고 스터디를 삭제해야 합니다.
    // 리드 위임 기능이 추가되면 위임 후 탈퇴하는 흐름으로 변경됩니다.
    if (member.role === 'LEADER') return new HttpResponse(null, { status: 403 });
    memberTable.delete(member);
    return new HttpResponse(null, { status: 204 });
  }),

  http.delete(`${BASE_URL}${MEMBER_URLS.kick}`, async ({ params }) => {
    const { studyId, memberId } = params;
    const member = await memberTable.findFirst((q) =>
      q.where({ studyId: Number(studyId), id: Number(memberId) }),
    );
    if (!member) return new HttpResponse(null, { status: 404 });
    memberTable.delete(member);
    return new HttpResponse(null, { status: 204 });
  }),
];
