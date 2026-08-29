import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { AUTH_URLS } from '../urls';

const mockLoginResponse = {
  tokenType: 'Bearer',
  accessToken: '1',
  accessTokenExpiresAt: new Date('9999-12-31T23:59:59Z').toISOString(),
};

export const handlers = [
  http.get(`${API_URL}${AUTH_URLS.csrf}`, async () => {
    return HttpResponse.json({
      headerName: 'X-XSRF-TOKEN',
      token: 'mock-token',
    });
  }),

  http.post(`${API_URL}${AUTH_URLS.login}`, async () => {
    return HttpResponse.json(mockLoginResponse);
  }),

  http.post(`${API_URL}${AUTH_URLS.refresh}`, async () => {
    return HttpResponse.json(mockLoginResponse);
  }),

  http.post(`${API_URL}${AUTH_URLS.logout}`, async () => {
    return new HttpResponse(null, { status: 204 });
  }),
];
