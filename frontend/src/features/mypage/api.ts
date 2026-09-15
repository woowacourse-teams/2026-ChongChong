import { HTTPError } from 'ky';
import api from '../../client';
import { ApiError, ErrorResponse } from '../../shared/api/error';
import { MYPAGE_URLS } from './urls';

export async function withdrawAccount() {
  try {
    await api.delete(MYPAGE_URLS.me);
  } catch (error) {
    const errorResponse = ErrorResponse.from(error);

    if (error instanceof HTTPError && errorResponse) {
      throw new ApiError({
        code: errorResponse.code,
        message: errorResponse.message,
        status: error.response.status,
        options: { cause: error },
      });
    }

    throw new Error('회원 탈퇴에 실패했습니다.', { cause: error });
  }
}
