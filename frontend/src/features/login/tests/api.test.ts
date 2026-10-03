import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { server } from '../../../mocks/msw-node';
import { PUSH_ENABLED_KEY, PUSH_SUBSCRIPTION_ID_KEY } from '../../notification/localPush';
import { clearAccessToken, getAccessToken, setAccessToken } from '../accessToken';
import { refreshAccessToken } from '../api';
import { AUTH_URLS } from '../urls';

const serviceWorkerDescriptor = Object.getOwnPropertyDescriptor(navigator, 'serviceWorker');
const unsubscribe = jest.fn().mockResolvedValue(true);
const unregister = jest.fn().mockResolvedValue(true);
const registration = {
  scope: new URL('/push/', window.location.origin).href,
  pushManager: { getSubscription: async () => ({ unsubscribe }) },
  unregister,
};

beforeEach(() => {
  unsubscribe.mockClear();
  unregister.mockClear();
  setAccessToken('expired-access-token');
  localStorage.setItem(PUSH_SUBSCRIPTION_ID_KEY, '42');
  localStorage.setItem(PUSH_ENABLED_KEY, 'true');
  localStorage.setItem('installationId', 'existing-installation');
  Object.defineProperty(navigator, 'serviceWorker', {
    configurable: true,
    value: {
      getRegistration: async () => registration,
      getRegistrations: async () => [registration],
    },
  });
});

afterEach(() => {
  clearAccessToken();
  localStorage.removeItem(PUSH_SUBSCRIPTION_ID_KEY);
  localStorage.removeItem(PUSH_ENABLED_KEY);
  localStorage.removeItem('installationId');
  if (serviceWorkerDescriptor) {
    Object.defineProperty(navigator, 'serviceWorker', serviceWorkerDescriptor);
  } else {
    Reflect.deleteProperty(navigator, 'serviceWorker');
  }
});

test.each([
  ['세션 만료', () => new HttpResponse(null, { status: 401 })],
  ['서버 오류', () => new HttpResponse(null, { status: 500 })],
  ['네트워크 오류', () => HttpResponse.error()],
] as const)(
  '%s로 세션 갱신에 실패해도 푸시 구독과 저장된 알림 설정을 유지한다',
  async (_reason, response) => {
    server.use(http.post(`${API_URL}${AUTH_URLS.refresh}`, response));

    await expect(refreshAccessToken()).rejects.toThrow();

    expect(getAccessToken()).toBeNull();
    expect(localStorage.getItem(PUSH_SUBSCRIPTION_ID_KEY)).toBe('42');
    expect(localStorage.getItem(PUSH_ENABLED_KEY)).toBe('true');
    expect(localStorage.getItem('installationId')).toBe('existing-installation');
    expect(unsubscribe).not.toHaveBeenCalled();
    expect(unregister).not.toHaveBeenCalled();
  },
);
