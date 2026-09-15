import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable } from '../../member/mocks/db';
import { userTable } from '../../user/mocks/db';
import { MYPAGE_URLS } from '../urls';

export const handlers = [
  http.get(`${API_URL}${MYPAGE_URLS.me}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);

    if (!user) {
      return new HttpResponse(null, { status: 401 });
    }

    return HttpResponse.json({
      name: user.name,
      profileImage: user.profileImage,
    });
  }),

  http.patch(`${API_URL}${MYPAGE_URLS.me}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);

    if (!user) {
      return new HttpResponse(null, { status: 401 });
    }

    const body = (await request.json()) as { name: string };

    await userTable.update((query) => query.where({ id: user.id }), {
      data(currentUser) {
        currentUser.name = body.name;
      },
    });

    return new HttpResponse(null, { status: 200 });
  }),

  http.delete(`${API_URL}${MYPAGE_URLS.me}`, ({ request }) => {
    const user = findUserFromHeader(request.headers);

    if (!user) {
      return new HttpResponse(null, { status: 401 });
    }

    memberTable.deleteMany((query) => query.where({ userId: user.id }));
    userTable.delete((query) => query.where({ id: user.id }));

    return new HttpResponse(null, { status: 204 });
  }),
];
