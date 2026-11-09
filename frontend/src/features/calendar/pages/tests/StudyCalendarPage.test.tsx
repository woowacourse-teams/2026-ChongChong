import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, login, logout } from '../../../../test/render';
import { assignmentTable, submissionTable } from '../../../assignment/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { noticeTable, noticeRecipientTable } from '../../../notice/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { CALENDAR_URLS } from '../../urls';
import StudyCalendarPage from '../StudyCalendarPage';

const calendarUrl = `${API_URL}${CALENDAR_URLS.monthly.path(42)}`;

function captureCalendarRequests() {
  const captureRequest = jest.fn();
  server.use(
    http.get(calendarUrl, ({ request }) => {
      captureRequest(new URL(request.url).search);
    }),
  );
  return captureRequest;
}

function setup(now = '2026-11-05T00:00:00Z') {
  jest.useFakeTimers({ now: new Date(now) });
  const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  render(
    <Routes>
      <Route path="/studies/:studyId/calendar" element={<StudyCalendarPage />} />
      <Route path="/studies/:studyId/notices/:noticeId" element={<p>공지 상세 화면</p>} />
      <Route path="/studies/:studyId/assignments/:assignmentId" element={<p>과제 상세 화면</p>} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/42/calendar'] }) },
  );
  return user;
}

beforeEach(async () => {
  await userTable.create({ id: 1, name: '스터디원', profileImage: null });
  await studyTable.create({ id: 42, name: '스터디', description: null, inviteLink: 'calendar' });
  await memberTable.create({
    id: 10,
    studyId: 42,
    userId: 1,
    name: '스터디원',
    profileImage: null,
    role: 'MEMBER',
  });
  await noticeTable.create({
    id: 12,
    studyId: 42,
    title: '11월 스터디 진행 안내',
    content: '이번 달 일정 안내',
    createdAt: '2026-11-05T00:15:00',
  });
  await noticeRecipientTable.create({
    id: 1,
    noticeId: 12,
    memberId: 10,
    readAt: '2026-11-05T01:00:00',
    lastRemindAt: null,
  });
  await assignmentTable.create({
    id: 31,
    studyId: 42,
    title: '이번 주 알고리즘 과제',
    content: '알고리즘 문제 풀이',
    submissionMethod: 'GitHub 링크',
    createdAt: '2026-11-05T18:00:00',
    closeAt: '2026-11-09T23:59:00',
    submissionTarget: 'MEMBERS_ONLY',
    completeUserIds: [],
  });
  await submissionTable.create({
    id: 1,
    assignmentId: 31,
    userId: 1,
    assigned: true,
    submitted: false,
  });
  login('스터디원');
});

afterEach(() => {
  logout();
  jest.useRealTimers();
});

test('한국 기준 오늘을 선택하고 해당 월의 일정을 표시한다', async () => {
  setup('2026-11-04T15:00:00Z');

  expect(
    await screen.findByRole('button', { name: '2026년 11월 5일 · 일정 있음' }),
  ).toHaveAttribute('aria-pressed', 'true');
  expect(
    within(screen.getByRole('navigation')).getByRole('link', { name: '일정' }),
  ).toHaveAttribute('aria-current', 'page');
  expect(screen.getByRole('link', { name: /11월 스터디 진행 안내/ })).toBeVisible();
});

test('같은 달에서 날짜를 선택하면 월간 데이터를 재사용한다', async () => {
  const requests = captureCalendarRequests();
  const user = setup();
  const list = await screen.findByRole('region', { name: '해당 날짜 일정' });
  expect(within(list).getAllByRole('link')).toHaveLength(2);

  await user.click(screen.getByRole('button', { name: '2026년 11월 9일 · 일정 있음' }));
  expect(within(list).getByRole('link')).toHaveTextContent('23:59');

  await user.click(screen.getByRole('button', { name: '2026년 11월 6일' }));
  expect(within(list).getByText('등록된 일정이 없어요')).toBeVisible();
  expect(requests).toHaveBeenCalledTimes(1);
});

test('연말에 다음 달로 이동하면 새해 1월을 조회하고 1일을 선택한다', async () => {
  const requests = captureCalendarRequests();
  const user = setup('2026-12-31T00:00:00Z');
  await screen.findByText('등록된 일정이 없어요');
  await act(async () => {
    await user.click(screen.getByRole('button', { name: '다음 달' }));
  });
  expect(await screen.findByRole('button', { name: '2027년 1월 1일' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await screen.findByText('등록된 일정이 없어요');
  expect(requests).toHaveBeenCalledWith('?year=2027&month=1');
});

test('다음 달을 조회하는 동안 페이지 로딩을 표시한다', async () => {
  server.use(
    http.get(calendarUrl, async ({ request }) => {
      const params = new URL(request.url).searchParams;
      if (params.get('month') === '12') await delay(1500);
    }),
  );

  const user = setup();
  await screen.findByRole('link', { name: /11월 스터디 진행 안내/ });
  await user.click(screen.getByRole('button', { name: '다음 달' }));
  expect(await screen.findByRole('img', { name: '로딩 중' })).toBeVisible();
  expect(screen.queryByRole('link', { name: /11월 스터디 진행 안내/ })).not.toBeInTheDocument();

  await act(async () => {
    await jest.advanceTimersByTimeAsync(1500);
  });
  expect(await screen.findByRole('button', { name: '2026년 12월 1일' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(screen.getByText('등록된 일정이 없어요')).toBeVisible();
});

test('공지 카드를 누르면 현재 스터디의 원본 상세로 이동한다', async () => {
  const user = setup();
  const card = await screen.findByRole('link', { name: /11월 스터디 진행 안내/ });
  expect(card).toHaveAttribute('href', '/studies/42/notices/12');
  await user.click(card);
  expect(screen.getByText('공지 상세 화면')).toBeVisible();
});

test('과제 등록과 마감 카드는 같은 과제 상세로 이동한다', async () => {
  const user = setup();
  expect(await screen.findByRole('link', { name: /이번 주 알고리즘 과제/ })).toHaveAttribute(
    'href',
    '/studies/42/assignments/31',
  );
  await user.click(screen.getByRole('button', { name: '2026년 11월 9일 · 일정 있음' }));
  const card = screen.getByRole('link', { name: /이번 주 알고리즘 과제 제출 마감/ });
  expect(card).toHaveAttribute('href', '/studies/42/assignments/31');
  await user.click(card);
  expect(screen.getByText('과제 상세 화면')).toBeVisible();
});

test('조회 실패 시 페이지에 오류를 표시하고 일정 탭을 유지한다', async () => {
  jest.spyOn(console, 'error').mockImplementation(() => {});
  server.use(http.get(calendarUrl, () => new HttpResponse(null, { status: 501 })));

  setup();
  expect(await screen.findByText('월간 일정을 불러오는 데 실패했습니다.')).toBeVisible();
  expect(screen.queryByText('등록된 일정이 없어요')).not.toBeInTheDocument();
  expect(
    within(screen.getByRole('navigation')).getByRole('link', { name: '일정' }),
  ).toHaveAttribute('aria-current', 'page');
});

test('스터디 접근이 거부되면 일정 대신 권한 오류를 표시한다', async () => {
  jest.spyOn(console, 'error').mockImplementation(() => {});
  await userTable.create({ id: 2, name: '비멤버', profileImage: null });
  login('비멤버');
  setup();

  expect(await screen.findByText('스터디에 접근할 수 없습니다.')).toBeVisible();
  expect(screen.queryByRole('region', { name: '해당 날짜 일정' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '다시 시도' })).not.toBeInTheDocument();
});
