import { KAKAO_REDIRECT_URI, KAKAO_REST_API_KEY } from '../../../config';

const KAKAO_AUTHORIZE_URL = 'https://kauth.kakao.com/oauth/authorize';
const KAKAO_OAUTH_STATE_KEY = 'kakao_oauth_state';

export function startKakaoLogin() {
  if (!KAKAO_REST_API_KEY) {
    throw new Error('KAKAO_REST_API_KEY가 설정되지 않았습니다.');
  }

  const state = crypto.randomUUID();
  sessionStorage.setItem(KAKAO_OAUTH_STATE_KEY, state);

  const searchParams = new URLSearchParams({
    response_type: 'code',
    client_id: KAKAO_REST_API_KEY,
    redirect_uri: KAKAO_REDIRECT_URI,
    state,
  });

  window.location.assign(`${KAKAO_AUTHORIZE_URL}?${searchParams.toString()}`);
}

export function consumeKakaoCallback(search: string) {
  const searchParams = new URLSearchParams(search);
  const authorizationCode = searchParams.get('code');
  const returnedState = searchParams.get('state');
  const expectedState = sessionStorage.getItem(KAKAO_OAUTH_STATE_KEY);
  const error = searchParams.get('error');

  sessionStorage.removeItem(KAKAO_OAUTH_STATE_KEY);
  window.history.replaceState({}, document.title, '/auth/kakao/callback');

  if (error) {
    throw new Error('카카오 로그인이 취소되었습니다.');
  }

  if (!authorizationCode || !returnedState || returnedState !== expectedState) {
    throw new Error('잘못된 카카오 로그인 응답입니다. 다시 로그인해 주세요.');
  }

  return authorizationCode;
}
