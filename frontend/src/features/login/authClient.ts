import ky from 'ky';
import { API_PREFIX, BASE_URL } from '../../../config';

const authApi = ky.create({
  baseUrl: BASE_URL,
  prefix: API_PREFIX,
  credentials: 'include',
  throwHttpErrors: false,
  retry: 0,
});

export default authApi;
