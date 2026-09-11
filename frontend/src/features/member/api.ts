import api from '../../client';
import { isMemberResponse } from './responseSchemas';
import { ErrorResponse } from '../../shared/api/error';
import { ApiError } from '../../shared/api/error';
import { HTTPError } from 'ky';

export async function fetchMembers(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/members`);

    const data = await response.json();

    if (!isMemberResponse(data)) {
      throw new Error('멤버 목록 응답 형식이 올바르지 않습니다.');
    }

    return data;
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

    throw new Error('멤버 목록을 불러오는데 실패했습니다.', { cause: error });
  }
}

export async function kickMember({ studyId, memberId }: { studyId: number; memberId: number }) {
  try {
    await api.delete(`/studies/${studyId}/members/${memberId}`);
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

    throw new Error('멤버를 추방하는데 실패했습니다.', { cause: error });
  }
}

export async function leaveStudyMember({ studyId }: { studyId: number }) {
  try {
    await api.delete(`/studies/${studyId}/members/me`);
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

    throw new Error('스터디 탈퇴에 실패했습니다.', { cause: error });
  }
}
