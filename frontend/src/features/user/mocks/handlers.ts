import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable } from '../../member/mocks/db';
import { userTable } from './db';
import { USER_URLS } from '../urls';

export const handlers = [
  http.get(`${API_URL}${USER_URLS.me}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);

    if (!user) {
      return new HttpResponse(null, { status: 401 });
    }

    return HttpResponse.json({
      name: user.name,
      profileImageUrl: user.profileImage,
    });
  }),

  http.delete(`${API_URL}${USER_URLS.me}`, ({ request }) => {
    const user = findUserFromHeader(request.headers);

    if (!user) {
      return new HttpResponse(null, { status: 401 });
    }

    memberTable.deleteMany((query) => query.where({ userId: user.id }));
    userTable.delete((query) => query.where({ id: user.id }));

    return new HttpResponse(null, { status: 204 });
  }),
];
