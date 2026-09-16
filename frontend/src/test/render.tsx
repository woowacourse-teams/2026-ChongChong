import userEvent from '@testing-library/user-event';
import { Routes } from 'react-router';
import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PropsWithChildren } from 'react';
import { MemoryRouter } from 'react-router';
import { http, HttpResponse } from 'msw';
import { ToastProvider } from '../shared/providers/ToastProvider';
import { server } from '../mocks/msw-node';
import { setAccessToken } from '../features/login/accessToken';
import { userTable } from '../features/user/mocks/db';

interface WrapperParams {
  initialEntries?: string[];
  routes?: (element: React.ReactNode) => React.ReactNode;
}

export function mockResponse<T>(url: string, studies: T[]) {
  server.use(http.get(url, () => HttpResponse.json({ studies })));
}

export function createWrapper({ initialEntries, routes }: WrapperParams = {}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <MemoryRouter initialEntries={initialEntries}>
            {routes ? <Routes>{routes(children)}</Routes> : children}
          </MemoryRouter>
        </ToastProvider>
      </QueryClientProvider>
    );
  };
}

export function setup(jsx: React.ReactNode, renderOptions?: RenderOptions) {
  return {
    user: userEvent.setup(),
    ...render(jsx, renderOptions),
  };
}

export function login(userName: string) {
  const user = userTable.findFirst((q) => q.where({ name: userName }));
  if (!user) {
    throw new Error('존재하는 테스트 환경 유저입니다');
  }
  setAccessToken(String(user.id));
}
