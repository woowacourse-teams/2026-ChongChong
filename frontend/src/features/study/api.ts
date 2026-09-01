import api from '../../client';
import { STUDY_URLS } from './urls';
import type { Role } from './types';
import {
  isCreateStudyResponse,
  isInviteLinkResponse,
  isStudyDetailResponse,
  isStudyInfoResponse,
  isStudyResponse,
} from './responseSchemas';

export async function fetchStudies() {
  try {
    const response = await api.get(STUDY_URLS.list);
    const data: unknown = await response.json();

    if (!isStudyResponse(data)) {
      throw new Error('스터디 목록 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('스터디 목록을 불러오는데 실패했습니다.');
  }
}

export async function fetchStudyDetail<R extends Role>(studyId: number, role: R) {
  try {
    const response = await api.get(`/studies/${studyId}`);
    const data: unknown = await response.json();

    if (!isStudyDetailResponse(data, role)) {
      throw new Error('스터디 상세 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('스터디 정보를 불러오는데 실패했습니다.');
  }
}

export async function createStudy(body: {
  name: string;
  description: string;
}): Promise<{ studyId: number }> {
  try {
    const response = await api.post(STUDY_URLS.create, { json: body });
    const data: unknown = await response.json();

    if (!isCreateStudyResponse(data)) {
      throw new Error('스터디 생성 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('아직 에러 처리 안함');
  }
}

export async function fetchStudyInfo(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/info`);
    const data: unknown = await response.json();

    if (!isStudyInfoResponse(data)) {
      throw new Error('스터디 기본 정보 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    // TODO: 에러코드별로 에러 분기가 필요합니다.
    throw new Error('스터디 정보를 불러오는데 실패했습니다.');
  }
}

export async function fetchStudyInviteLink(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/invite-link`);
    const data: unknown = await response.json();

    if (!isInviteLinkResponse(data)) {
      throw new Error('초대 링크 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('초대 링크를 가져오는데 실패했습니다.');
  }
}

export async function joinStudy(body: { token: string }) {
  try {
    const response = await api.post(STUDY_URLS.join, { json: body });
    return await response.json<{ studyId: number }>();
  } catch {
    throw new Error('스터디 참여에 실패했습니다.');
  }
}

export async function removeStudy(studyId: number) {
  try {
    await api.delete(`/studies/${studyId}`);
  } catch {
    throw new Error('스터디를 삭제하는데 실패했습니다.');
  }
}
