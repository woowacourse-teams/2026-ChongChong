import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable } from '../../member/mocks/db';
import { userTable } from './db';
import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../shared/modules/files/mocks/db';
import { USER_URLS } from '../urls';

async function saveProfileImage(userId: number, profileImage: string | null) {
  await userTable.update((q) => q.where({ id: userId }), {
    data(user) {
      user.profileImage = profileImage;
    },
  });
  for (const member of memberTable.findMany((q) => q.where({ userId }))) {
    await memberTable.update(member, {
      data(current) {
        current.profileImage = profileImage;
      },
    });
  }
}

export const handlers = [
  http.patch(`${API_URL}${USER_URLS.profileImage}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const { fileId } = (await request.json()) as { fileId?: number };
    const file = fileTable.findFirst((q) => q.where({ id: fileId }));
    if (
      !file ||
      file.status !== 'COMPLETED' ||
      file.ownerUserId !== user.id ||
      !['image/jpeg', 'image/png', 'image/webp'].includes(file.contentType) ||
      file.size <= 0 ||
      file.size > 5_242_880
    ) {
      return HttpResponse.json(
        { code: 'INVALID_PROFILE_IMAGE', message: '업로드가 완료된 본인의 이미지를 선택해주세요.' },
        { status: 400 },
      );
    }
    const profileImageUrl = `${MOCK_UPLOAD_ORIGIN}/images/${file.id}`;
    await saveProfileImage(user.id, profileImageUrl);
    return HttpResponse.json({ name: user.name, profileImageUrl });
  }),
  http.delete(`${API_URL}${USER_URLS.profileImage}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    await saveProfileImage(user.id, null);
    return new HttpResponse(null, { status: 204 });
  }),
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

  http.patch(`${API_URL}${USER_URLS.me}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);

    if (!user) {
      return new HttpResponse(null, { status: 401 });
    }

    const body = (await request.json()) as { name?: unknown };
    if (typeof body.name !== 'string' || body.name.trim().length === 0 || body.name.length > 8) {
      return HttpResponse.json(
        { code: 'INVALID_USER_NAME', message: '사용자 이름이 올바르지 않습니다.' },
        { status: 400 },
      );
    }
    const name = body.name;

    await userTable.update((query) => query.where({ id: user.id }), {
      data(currentUser) {
        currentUser.name = name;
      },
    });

    return HttpResponse.json({
      name,
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
