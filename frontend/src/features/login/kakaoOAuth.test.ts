import { consumeKakaoCallback } from './kakaoOAuth';

const OAUTH_STATE_KEY = 'kakao_oauth_state';

describe('consumeKakaoCallback', () => {
  beforeEach(() => {
    sessionStorage.clear();
    window.history.replaceState({}, '', '/auth/kakao/callback');
  });

  it('state가 일치하면 인가 코드를 반환하고 저장된 state를 제거한다', () => {
    sessionStorage.setItem(OAUTH_STATE_KEY, 'expected-state');

    const authorizationCode = consumeKakaoCallback('?code=authorization-code&state=expected-state');

    expect(authorizationCode).toBe('authorization-code');
    expect(sessionStorage.getItem(OAUTH_STATE_KEY)).toBeNull();
    expect(window.location.search).toBe('');
  });

  it('state가 일치하지 않으면 인가 코드를 사용하지 않는다', () => {
    sessionStorage.setItem(OAUTH_STATE_KEY, 'expected-state');

    expect(() => consumeKakaoCallback('?code=authorization-code&state=unexpected-state')).toThrow(
      '잘못된 카카오 로그인 응답입니다.',
    );
    expect(sessionStorage.getItem(OAUTH_STATE_KEY)).toBeNull();
  });

  it('카카오가 오류를 반환하면 로그인을 중단한다', () => {
    sessionStorage.setItem(OAUTH_STATE_KEY, 'expected-state');

    expect(() => consumeKakaoCallback('?error=access_denied&state=expected-state')).toThrow(
      '카카오 로그인이 취소되었습니다.',
    );
  });
});
