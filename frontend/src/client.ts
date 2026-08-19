import ky from 'ky';
import { BASE_URL } from '../config';

const api = ky.create({
  baseUrl: BASE_URL,
  hooks: {},
});

export default api;
