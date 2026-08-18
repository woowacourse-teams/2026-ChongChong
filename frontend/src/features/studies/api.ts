import { isNetworkError } from 'ky';
import api from '../../client';
import { Study } from './types';

export async function fetchStudies() {
  try {
    const response = await api.get('/studies/me');
    return response.json<{ studies: Study[] }>();
  } catch (err) {
    if (isNetworkError(err)) {
      console.log(123);
    }
    throw err;
  }
}
