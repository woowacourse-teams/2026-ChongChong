import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import NoticeDetailPage from '../NoticeDetailPage';

const NOTICE_DETAIL_URL = `${API_URL}/studies/:studyId/notices/:noticeId`;
const resizeObserverDescriptor = Object.getOwnPropertyDescriptor(window, 'ResizeObserver');

function renderNoticeDetailPage() {
  render(
    <Routes>
      <Route path="/studies/:studyId/notices/:noticeId" element={<NoticeDetailPage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/notices/1'] }) },
  );
}

describe('공지 읽음 처리', () => {
  beforeEach(() => {
    // 한 화면에 들어오는 공지는 본문 표시 후 자동으로 읽음 처리를 요청한다.
    jest.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(200);
    jest.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400);
    // jsdom에 없는 ResizeObserver만 대체하고 읽음 처리 로직은 그대로 실행한다.
    Object.defineProperty(window, 'ResizeObserver', {
      configurable: true,
      value: jest.fn(() => ({
        observe: jest.fn(),
        unobserve: jest.fn(),
        disconnect: jest.fn(),
      })),
    });
    server.use(
      http.get(`${API_URL}${STUDY_URLS.info}`, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        }),
      ),
      http.get(NOTICE_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '스터디 일정 안내',
          content: '이번 주 스터디는 토요일에 진행합니다.',
          createdAt: '2026-09-01T09:00:00',
        }),
      ),
      http.get(`${NOTICE_DETAIL_URL}/status/me`, () => HttpResponse.json({ readStatus: 'UNREAD' })),
    );
  });

  afterEach(() => {
    if (resizeObserverDescriptor) {
      Object.defineProperty(window, 'ResizeObserver', resizeObserverDescriptor);
    } else {
      Reflect.deleteProperty(window, 'ResizeObserver');
    }
  });

  describe('실패', () => {
    test('스터디에 대한 접근 권한이 없으면 접근 권한 안내를 토스트로 표시한다', async () => {
      server.use(
        http.patch(`${NOTICE_DETAIL_URL}/read`, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
      );
      renderNoticeDetailPage();

      const toast = await screen.findByRole('status', {}, { timeout: 4000 });
      expect(toast).toHaveTextContent('해당 스터디에 대한 접근 권한이 없습니다.');
      expect(toast).toBeVisible();
    });

    test('네트워크 에러가 발생하면 읽음 처리 실패 안내를 토스트로 표시한다', async () => {
      server.use(http.patch(`${NOTICE_DETAIL_URL}/read`, () => HttpResponse.error()));
      renderNoticeDetailPage();

      const toast = await screen.findByRole('status', {}, { timeout: 4000 });
      expect(toast).toHaveTextContent('공지 읽음 처리에 실패했습니다.');
      expect(toast).toBeVisible();
      expect(screen.getByRole('heading', { name: '스터디 일정 안내' })).toBeVisible();
      expect(screen.queryByRole('img', { name: '읽음 완료' })).not.toBeInTheDocument();
    });
  });

  describe('확인 대상이 아닌 경우', () => {
    test('읽음 상태 영역을 표시하지 않고 읽음 처리 요청을 보내지 않는다', async () => {
      const readRequest = jest.fn();
      server.use(
        http.get(`${NOTICE_DETAIL_URL}/status/me`, () =>
          HttpResponse.json({ readStatus: 'NOT_ASSIGNED' }),
        ),
        http.patch(`${NOTICE_DETAIL_URL}/read`, () => {
          readRequest();

          return HttpResponse.json({ readAt: '2026-09-01T10:00:00' });
        }),
      );
      renderNoticeDetailPage();

      expect(await screen.findByRole('heading', { name: '스터디 일정 안내' })).toBeVisible();
      expect(screen.queryByText('끝까지 읽으면 읽음으로 표시돼요')).not.toBeInTheDocument();
      expect(screen.queryByText(/지금 \d+% 읽었어요/)).not.toBeInTheDocument();
      expect(readRequest).not.toHaveBeenCalled();
    });
  });
});
