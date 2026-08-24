import api from '../../client';
import { Assignment } from './types';

export async function fetchAssignments(studyId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments`);
    return await response.json<{ assignments: Assignment[] }>();
  } catch {
    throw new Error('과제 목록을 불러오는데 실패했습니다.');
  }
}
