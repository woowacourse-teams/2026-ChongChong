import { http, HttpResponse } from 'msw';
import { BASE_URL } from '../../../../config';
import { MEMBER_URLS } from '../urls';
import { memberTable } from './db';

export const handlers = [
  http.get(`${BASE_URL}${MEMBER_URLS.list}`, () => {
    return HttpResponse.json({
      members: memberTable.all(),
    });
  }),

  http.delete(`${BASE_URL}${MEMBER_URLS.kick}`, async ({ params }) => {
    const { memberId } = params;
    const member = await memberTable.findFirst((q) => q.where({ id: Number(memberId) }));
    if (!member) return new HttpResponse(null, { status: 404 });
    memberTable.delete(member);
    return new HttpResponse(null, { status: 204 });
  }),
];
