import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import StudyCalendarPage from '../StudyCalendarPage';
import StudyCalendar, { type CalendarEvent } from '../../components/StudyCalendar';

const eventsByDate: Record<string, CalendarEvent[]> = {
  '2026.11.05': [
    { id: 'notice', resourceId: 1, type: 'notice', label: '공지', title: '11월 스터디 진행 안내' },
    {
      id: 'created',
      resourceId: 1,
      type: 'assignment',
      label: '과제 등록',
      title: '이번 주 알고리즘 과제',
    },
  ],
  '2026.11.09': [
    {
      id: 'due',
      resourceId: 1,
      type: 'due',
      label: '제출 마감',
      title: '이번 주 알고리즘 과제 제출 마감',
      time: '23:59',
    },
  ],
};

function setup(now = '2026-11-05T00:00:00Z', withEvents = true) {
  jest.useFakeTimers({ now: new Date(now) });
  const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  render(
    <MemoryRouter initialEntries={['/studies/42/calendar']}>
      <Routes>
        <Route
          path="/studies/:studyId/calendar"
          element={
            withEvents ? (
              <StudyCalendar studyId={42} eventsByDate={eventsByDate} />
            ) : (
              <StudyCalendarPage />
            )
          }
        />
        <Route path="/studies/:studyId/notices/:noticeId" element={<p>공지 상세 화면</p>} />
        <Route path="/studies/:studyId/assignments/:assignmentId" element={<p>과제 상세 화면</p>} />
      </Routes>
    </MemoryRouter>,
  );
  return user;
}

afterEach(() => jest.useRealTimers());

describe('일정 탭 캘린더 테스트', () => {
  test('한국 기준 오늘을 선택하고 일정 탭을 활성화한다', () => {
    setup('2026-11-04T15:00:00Z', false);

    expect(screen.getByRole('button', { name: '2026년 11월 5일' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(
      within(screen.getByRole('navigation')).getByRole('link', { name: '일정' }),
    ).toHaveAttribute('aria-current', 'page');
  });

  test('날짜를 선택하면 그 날짜의 목록과 빈 상태가 표시된다', async () => {
    const user = setup();
    const list = screen.getByRole('region', { name: '해당 날짜 일정' });
    expect(within(list).getAllByRole('link')).toHaveLength(2);

    await user.click(screen.getByRole('button', { name: '2026년 11월 9일 · 일정 있음' }));
    expect(within(list).getByRole('link')).toHaveTextContent('23:59');

    await user.click(screen.getByRole('button', { name: '2026년 11월 6일' }));
    expect(within(list).getByText('등록된 일정이 없어요')).toBeVisible();
    expect(within(list).queryByRole('link')).not.toBeInTheDocument();
  });

  test('연말에 다음 달로 이동하면 새해 1일이 선택된다', async () => {
    const user = setup('2026-12-31T00:00:00Z');
    await user.click(screen.getByRole('button', { name: '다음 달' }));
    expect(screen.getByRole('button', { name: '2027년 1월 1일' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await user.click(screen.getByRole('button', { name: '이전 달' }));
    expect(screen.getByRole('button', { name: '2026년 12월 1일' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  test('공지 카드를 누르면 현재 스터디의 원본 상세로 이동한다', async () => {
    const user = setup();
    const card = screen.getByRole('link', { name: /11월 스터디 진행 안내/ });
    expect(card).toHaveAttribute('href', '/studies/42/notices/1');
    await user.click(card);
    expect(screen.getByText('공지 상세 화면')).toBeVisible();
  });

  test('과제 등록과 마감 카드는 같은 과제 상세로 이동한다', async () => {
    const user = setup();
    expect(screen.getByRole('link', { name: /이번 주 알고리즘 과제/ })).toHaveAttribute(
      'href',
      '/studies/42/assignments/1',
    );
    await user.click(screen.getByRole('button', { name: '2026년 11월 9일 · 일정 있음' }));
    const card = screen.getByRole('link', { name: /이번 주 알고리즘 과제 제출 마감/ });
    expect(card).toHaveAttribute('href', '/studies/42/assignments/1');
    await user.click(card);
    expect(screen.getByText('과제 상세 화면')).toBeVisible();
  });
});
