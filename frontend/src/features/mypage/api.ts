import api from '../../client';
import { ApiError } from '../../shared/api/error';
import { MYPAGE_URLS } from './urls';
import { handleError } from '../../shared/api/error';
import z from 'zod';

const profileSchema = z.object({
  name: z.string(),
  profileImageUrl: z.string().nullable(),
});

export async function withdrawAccount() {
  try {
    await api.delete(MYPAGE_URLS.me);
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('회원 탈퇴에 실패했습니다.', { cause: error }),
    });
  }
}

export async function getProfile() {
  try {
    const response = await api.get(MYPAGE_URLS.me);
    const data: unknown = await response.json();
    const profile = profileSchema.safeParse(data);

    if (!profile.success) {
      throw new Error('회원 정보 응답 형식이 올바르지 않습니다.');
    }

    return profile.data;
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('회원 정보를 불러오는데 실패했습니다.', { cause: error }),
    });
  }
}
