export const BASE_URL = process.env.API_BASE_URL ?? 'https://mock.chongchong.com';
export const API_PREFIX = '/api';
export const API_URL = `${BASE_URL}${API_PREFIX}`;

export const KAKAO_REST_API_KEY = process.env.KAKAO_REST_API_KEY ?? '';
export const KAKAO_REDIRECT_URI = new URL(
  '/auth/kakao/callback',
  window.location.origin,
).toString();
