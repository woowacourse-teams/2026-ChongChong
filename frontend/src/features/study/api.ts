import api from '../../client';
import { STUDY_URLS } from './urls';
import { Role, Study, StudyDetail } from './types';

export async function fetchStudies() {
  try {
    const response = await api.get(STUDY_URLS.list);
    return await response.json<{ studies: Study[] }>();
  } catch {
    throw new Error('스터디 목록을 불러오는데 실패했습니다.');
  }
}

export async function fetchStudyDetail<R extends Role>(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}`);
    return await response.json<StudyDetail<R>>();
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
    return await response.json<{ studyId: number }>();
  } catch {
    throw new Error('아직 에러 처리 안함');
  }
}

export async function fetchStudyInfo(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/info`);
    return await response.json<{
      studyName: string;
      role: Role;
      userName: string;
    }>();
  } catch {
    // TODO: 에러코드별로 에러 분기가 필요합니다.
    throw new Error('스터디 정보를 불러오는데 실패했습니다.');
  }
}

export async function fetchStudyInviteLink(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/invite-link`);
    return await response.json<{
      inviteLink: string;
    }>();
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
