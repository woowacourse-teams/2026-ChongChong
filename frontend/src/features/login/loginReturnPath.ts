import { URL_POLICY } from '../../shared/constants/urlPolicy';

const LOGIN_RETURN_PATH_KEY = 'login_return_path';

export function saveLoginReturnPath() {
  const { pathname, search, hash } = window.location;
  const returnPath = URL_POLICY.loginReturnPath(`${pathname}${search}${hash}`);

  if (returnPath) sessionStorage.setItem(LOGIN_RETURN_PATH_KEY, returnPath);
}

// 로그인 실패/취소 시에는 호출하지 않아 재시도할 때도 복귀 주소를 유지한다.
export function consumeLoginReturnPath() {
  const returnPath = sessionStorage.getItem(LOGIN_RETURN_PATH_KEY);
  sessionStorage.removeItem(LOGIN_RETURN_PATH_KEY);
  return returnPath;
}
