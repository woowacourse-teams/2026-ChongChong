import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { consumeLoginReturnPath, saveLoginReturnPath } from '../loginReturnPath';

beforeEach(() => {
  sessionStorage.clear();
  window.history.replaceState({}, '', '/');
});

test.each([
  '/studies',
  '/studies/join?token=invite%2Btoken#join',
  '/studies/1/notices/10?view=status',
])('로그인 전 진입 주소 %s를 OAuth 왕복 후 복원한다', (path) => {
  window.history.replaceState({}, '', path);
  saveLoginReturnPath();
  window.history.replaceState({}, '', '/login');
  window.history.replaceState({}, '', '/auth/kakao/callback?code=code&state=state');

  expect(URL_POLICY.afterLogin(consumeLoginReturnPath()).to).toBe(path);
  expect(consumeLoginReturnPath()).toBeNull();
});

test('로그인과 콜백 화면은 기존 복귀 주소를 덮어쓰지 않는다', () => {
  window.history.replaceState({}, '', '/studies/join?token=invitation');
  saveLoginReturnPath();
  window.history.replaceState({}, '', '/login');
  saveLoginReturnPath();
  window.history.replaceState({}, '', '/auth/kakao/callback?error=access_denied');
  saveLoginReturnPath();

  expect(consumeLoginReturnPath()).toBe('/studies/join?token=invitation');
});

test.each([
  null,
  'https://example.com/studies',
  '//example.com/studies',
  '/login?next=/studies',
  '/LOGIN/',
  '/auth/kakao/callback?code=code',
  '/%6cogin',
])('복귀 주소 %s를 사용할 수 없으면 스터디 목록으로 이동한다', (path) => {
  expect(URL_POLICY.afterLogin(path).to).toBe('/studies');
  expect(URL_POLICY.afterLogin(path).replace).toBe(true);
});
