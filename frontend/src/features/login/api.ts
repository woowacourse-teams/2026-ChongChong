import type { Options } from 'ky';
import authApi from './authClient';
import { clearAccessToken, setAccessToken } from './accessToken';
import { AUTH_URLS } from './urls';
import type { CsrfResponse, LoginResponse } from './types';

let refreshRequest: Promise<LoginResponse> | null = null;

async function getCsrfToken(): Promise<CsrfResponse> {
  const response = await authApi.get<CsrfResponse>(AUTH_URLS.csrf, {
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error('CSRF Token을 가져오지 못했습니다.');
  }

  return response.json();
}

async function postWithCsrf(path: string, options?: Options) {
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const csrf = await getCsrfToken();
    const headers = new Headers(options?.headers as HeadersInit);
    headers.set(csrf.headerName, csrf.token);

    const response = await authApi.post<LoginResponse>(path, {
      ...options,
      headers,
    });

    if (response.status !== 403 || attempt === 1) {
      return response;
    }
  }

  throw new Error('인증 요청에 실패했습니다.');
}

export async function loginWithKakaoCode(authorizationCode: string) {
  const response = await postWithCsrf(AUTH_URLS.login, {
    json: {
      provider: 'KAKAO',
      authorizationCode,
    },
  });

  if (!response.ok) {
    throw new Error('카카오 로그인에 실패했습니다. 다시 로그인해 주세요.');
  }

  const tokenResponse = await response.json();
  setAccessToken(tokenResponse.accessToken);
  return tokenResponse;
}

async function rotateAccessToken() {
  const response = await postWithCsrf(AUTH_URLS.refresh);

  if (!response.ok) {
    clearAccessToken();
    throw new Error('로그인 세션이 만료되었습니다.');
  }

  const tokenResponse = await response.json();
  setAccessToken(tokenResponse.accessToken);
  return tokenResponse;
}

export function refreshAccessToken() {
  if (!refreshRequest) {
    refreshRequest = rotateAccessToken().finally(() => {
      refreshRequest = null;
    });
  }

  return refreshRequest;
}

export async function logout() {
  try {
    await postWithCsrf(AUTH_URLS.logout);
  } finally {
    clearAccessToken();
  }
}
