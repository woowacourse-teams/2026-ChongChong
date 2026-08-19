import api from '../../client';
import { Study } from './types';

export async function fetchStudies() {
  try {
    const response = await api.get('/studies/me');
    return await response.json<{ studies: Study[] }>();
  } catch {
    throw new Error('스터디 목록을 불러오는데 실패했습니다.');
  }
}
