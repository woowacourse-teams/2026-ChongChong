import api from '../../client';
import { Member } from './types';

export async function fetchMembers(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/members`);
    return await response.json<{ members: Member[] }>();
  } catch {
    throw new Error('멤버 목록을 불러오는데 실패했습니다.');
  }
}

export async function kickMember({ studyId, memberId }: { studyId: number; memberId: number }) {
  try {
    await api.delete(`/studies/${studyId}/members/${memberId}`);
  } catch {
    throw new Error('멤버를 추방하는데 실패했습니다.');
  }
}

export async function leaveStudyMember({ studyId }: { studyId: number }) {
  try {
    await api.delete(`/studies/${studyId}/members/me`);
  } catch {
    throw new Error('스터디 탈퇴에 실패했습니다.');
  }
}
