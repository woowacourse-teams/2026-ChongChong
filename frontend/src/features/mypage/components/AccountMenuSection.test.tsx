import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../config';
import { server } from '../../../mocks/msw-node';
import { createWrapper } from '../../../test/render';
import { clearAccessToken, getAccessToken, setAccessToken } from '../../login/accessToken';
import { PUSH_ENABLED_KEY, PUSH_SUBSCRIPTION_ID_KEY } from '../../notification/localPush';
import AccountMenuSection from './AccountMenuSection';

test('직접 로그아웃하면 서버와 브라우저의 푸시 구독을 해제한다', async () => {
  const originalServiceWorker = Object.getOwnPropertyDescriptor(navigator, 'serviceWorker');
  const unsubscribe = jest.fn().mockResolvedValue(true);
  const getSubscription = jest.fn().mockResolvedValue({ unsubscribe });
  const scope = new URL('/push/', window.location.origin).href;
  const getRegistration = jest.fn().mockResolvedValue({ scope, pushManager: { getSubscription } });
  Object.defineProperty(navigator, 'serviceWorker', {
    configurable: true,
    value: { getRegistration },
  });

  const deactivatedIds: string[] = [];
  server.use(
    http.delete(`${API_URL}/web-push-subscriptions/:subscriptionId`, ({ params }) => {
      deactivatedIds.push(String(params.subscriptionId));
      return new HttpResponse(null, { status: 204 });
    }),
  );
  setAccessToken('access-token');
  localStorage.setItem(PUSH_SUBSCRIPTION_ID_KEY, '42');
  localStorage.setItem(PUSH_ENABLED_KEY, 'true');
  localStorage.setItem('installationId', 'saved-installation');

  try {
    render(
      <Routes>
        <Route path="/account" element={<AccountMenuSection />} />
        <Route path="/login" element={<h1>로그인 페이지</h1>} />
      </Routes>,
      { wrapper: createWrapper({ initialEntries: ['/account'] }) },
    );

    await userEvent.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('heading', { name: '로그인 페이지' })).toBeInTheDocument();
    expect(deactivatedIds).toEqual(['42']);
    expect(getRegistration).toHaveBeenCalledWith(scope);
    expect(unsubscribe).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem(PUSH_SUBSCRIPTION_ID_KEY)).toBeNull();
    expect(localStorage.getItem(PUSH_ENABLED_KEY)).toBe('false');
    expect(localStorage.getItem('installationId')).toBe('saved-installation');
    expect(getAccessToken()).toBeNull();
  } finally {
    clearAccessToken();
    localStorage.removeItem(PUSH_SUBSCRIPTION_ID_KEY);
    localStorage.removeItem(PUSH_ENABLED_KEY);
    localStorage.removeItem('installationId');
    if (originalServiceWorker) {
      Object.defineProperty(navigator, 'serviceWorker', originalServiceWorker);
    } else {
      Reflect.deleteProperty(navigator, 'serviceWorker');
    }
  }
});
