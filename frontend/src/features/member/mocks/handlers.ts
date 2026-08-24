import { http, HttpResponse } from 'msw';
import { BASE_URL } from '../../../../config';
import { MEMBER_URLS } from '../urls';
import { member } from './db';

export const handlers = [
  http.get(`${BASE_URL}${MEMBER_URLS.list}`, () => {
    return HttpResponse.json({
      members: member.all(),
    });
  }),
];
