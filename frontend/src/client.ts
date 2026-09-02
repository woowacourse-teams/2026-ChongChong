import ky from 'ky';
import { API_PREFIX, BASE_URL } from '../config';
import { clearAccessToken, getAccessToken } from './features/login/accessToken';
import { refreshAccessToken } from './features/login/api';
import { alertErrorResponse } from './shared/api/error';

const api = ky.create({
  baseUrl: BASE_URL,
  prefix: API_PREFIX,
  credentials: 'include',
  hooks: {
    beforeRequest: [
      ({ request }) => {
        const accessToken = getAccessToken();
        if (accessToken) {
          request.headers.set('Authorization', `Bearer ${accessToken}`);
        }
      },
    ],
    afterResponse: [
      async ({ request, response, retryCount }) => {
        if (response.status !== 401 || retryCount > 0) return;

        try {
          const tokenResponse = await refreshAccessToken();
          const headers = new Headers(request.headers);
          headers.set('Authorization', `Bearer ${tokenResponse.accessToken}`);

          return ky.retry({
            request: new Request(request, { headers }),
            code: 'ACCESS_TOKEN_REFRESHED',
          });
        } catch {
          clearAccessToken();

          if (window.location.pathname !== '/login') {
            window.location.assign('/login');
          }

          return response;
        }
      },
    ],
    beforeError: [
      ({ request, error }) => {
        // 조회 실패는 각 페이지의 에러 UI가 표시하므로 Alert를 띄우지 않습니다.
        if (request.method !== 'GET') alertErrorResponse(error);

        return error;
      },
    ],
  },
});

export default api;
