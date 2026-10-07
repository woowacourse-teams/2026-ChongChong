import api from '../../client';
import { ApiError } from '../../shared/api/error';
import { USER_URLS } from './urls';
import { handleError } from '../../shared/api/error';
import z from 'zod';

const profileSchema = z.object({
  name: z.string(),
  profileImageUrl: z.string().nullable(),
});

function parseProfile(data: unknown) {
  const profile = profileSchema.safeParse(data);

  if (!profile.success) {
    throw new Error('회원 정보 응답 형식이 올바르지 않습니다.');
  }

  return profile.data;
}

export async function withdrawAccount() {
  try {
    await api.delete(USER_URLS.me);
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('회원 탈퇴에 실패했습니다.', { cause: error }),
    });
  }
}

export async function getProfile() {
  try {
    const response = await api.get(USER_URLS.me);
    const data: unknown = await response.json();
    return parseProfile(data);
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('회원 정보를 불러오는데 실패했습니다.', { cause: error }),
    });
  }
}

export async function updateProfileName(name: string) {
  try {
    const response = await api.patch(USER_URLS.me, { json: { name } });
    const data: unknown = await response.json();
    return parseProfile(data);
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('프로필 이름을 수정하는데 실패했습니다.', { cause: error }),
    });
  }
}
