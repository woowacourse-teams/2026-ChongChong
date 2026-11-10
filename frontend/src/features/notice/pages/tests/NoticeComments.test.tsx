import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes, useLocation } from 'react-router';
import { API_URL } from '../../../../../config';
import { COMMENT_URLS } from '../../../../shared/modules/comments/urls';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { setAccessToken, clearAccessToken } from '../../../login/accessToken';
import { userTable } from '../../../user/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { noticeTable } from '../../mocks/db';
import { commentTable } from '../../mocks/commentDb';
import NoticeDetailPage from '../NoticeDetailPage';

const commentsUrl = `${API_URL}${COMMENT_URLS.notice.path(1, 1)}`;

beforeEach(async () => {
  await userTable.create({ id: 1, name: '바니', profileImage: null });
  await memberTable.create({
    id: 1,
    studyId: 1,
    userId: 1,
    name: '바니',
    profileImage: null,
    role: 'LEADER',
  });
  await noticeTable.create({
    id: 1,
    studyId: 1,
    title: '공지 원문',
    content: '본문은 유지됩니다.',
    createdAt: '2026-10-30T10:00:00',
  });
  setAccessToken('1');
  server.use(
    http.get(`${API_URL}/studies/1/info`, () =>
      HttpResponse.json({
        studyName: '스터디',
        isPublic: false,
        role: 'LEADER',
        userName: '바니',
        description: '스터디',
      }),
    ),
    http.get(`${API_URL}/studies/1/notices/1`, () =>
      HttpResponse.json({
        id: 1,
        title: '공지 원문',
        content: '본문은 유지됩니다.',
        createdAt: '2026-10-30T10:00:00',
        attachments: [],
      }),
    ),
    http.get(`${API_URL}/studies/1/notices/1/status`, () =>
      HttpResponse.json({
        id: 1,
        memberCount: 1,
        readCount: 1,
        unreadCount: 0,
        remindAt: null,
        readMembers: [],
        unreadMembers: [],
      }),
    ),
  );
});

afterEach(() => clearAccessToken());

function CurrentLocation() {
  const location = useLocation();
  return (
    <span hidden data-testid="current-location">
      {location.pathname + location.search + location.hash}
    </span>
  );
}

function renderPage(entry = '/studies/1/notices/1') {
  render(
    <>
      <Routes>
        <Route path="/studies/:studyId/notices/:noticeId" element={<NoticeDetailPage />} />
      </Routes>
      <CurrentLocation />
    </>,
    {
      wrapper: createWrapper({ initialEntries: [entry] }),
    },
  );
}

async function addComments(count: number) {
  for (let id = 1; id <= count; id++) {
    await commentTable.create({
      id,
      studyId: 1,
      noticeId: 1,
      authorUserId: id === 1 ? 1 : 2,
      author: {
        name: id === 1 ? '바니' : '준',
        role: 'LEADER',
        profileImageUrl: null,
        isDeparted: false,
      },
      content: `댓글 본문 ${id}`,
      createdAt: '2026-10-30T13:10:00',
      isEdited: id === 2,
    });
  }
}

describe('공지 댓글 테스트', () => {
  test('전체 개수를 표시하고 10개 더 보기로 다음 댓글을 추가한다', async () => {
    const user = userEvent.setup();
    await addComments(12);
    renderPage();
    await user.click(await screen.findByRole('button', { name: '공지 댓글 12 열기' }));
    const dialog = screen.getByRole('dialog', { name: '공지 댓글 12' });
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(10);
    expect(within(dialog).getByText('바니 · 나')).toBeVisible();
    expect(within(dialog).getAllByText('준 · 리더')).toHaveLength(9);
    expect(within(dialog).getByText('10월 30일 13:10 · 수정됨')).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '댓글 10개 더 보기' }));
    expect(await within(dialog).findByText('댓글 본문 12')).toBeVisible();
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(12);
    expect(
      within(dialog).queryByRole('button', { name: '댓글 10개 더 보기' }),
    ).not.toBeInTheDocument();
    expect(within(dialog).getByRole('heading', { name: '공지 댓글 12' })).toBeVisible();
  });

  test('패널을 닫으면 공지 원문과 진입 버튼 포커스로 돌아간다', async () => {
    const user = userEvent.setup();
    renderPage();
    const entry = await screen.findByRole('button', { name: '공지 댓글 0 열기' });
    await user.click(entry);
    expect(document.body.style.overflow).toBe('hidden');
    await user.click(screen.getByRole('button', { name: '댓글 닫기' }));
    expect(screen.queryByRole('dialog', { name: '공지 댓글 0' })).not.toBeInTheDocument();
    expect(entry).toHaveFocus();
    expect(document.body.style.overflow).not.toBe('hidden');
    expect(screen.getByText('본문은 유지됩니다.')).toBeVisible();
  });

  test('첫 조회 실패 후 다시 시도하면 목록을 표시한다', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    const user = userEvent.setup();
    await addComments(1);
    server.use(
      http.get(commentsUrl, () => new HttpResponse(null, { status: 500 }), { once: true }),
    );
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('댓글을 불러오지 못했어요.');
    await user.click(screen.getByRole('button', { name: '다시 시도' }));
    await user.click(await screen.findByRole('button', { name: '공지 댓글 1 열기' }));
    expect(within(screen.getByRole('dialog')).getByText('댓글 본문 1')).toBeVisible();
  });

  test('멤버 상세에서도 댓글을 열 수 있고 미배정 읽음 상태는 유지한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get(`${API_URL}/studies/1/info`, () =>
        HttpResponse.json({
          studyName: '스터디',
          isPublic: false,
          role: 'MEMBER',
          userName: '바니',
          description: '스터디',
        }),
      ),
      http.get(`${API_URL}/studies/1/notices/1/status/me`, () =>
        HttpResponse.json({ readStatus: 'NOT_ASSIGNED' }),
      ),
    );
    renderPage();
    await user.click(await screen.findByRole('button', { name: '공지 댓글 0 열기' }));
    expect(screen.getByRole('dialog', { name: '공지 댓글 0' })).toBeVisible();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByText('본문은 유지됩니다.')).toBeVisible();
  });

  test('탈퇴한 작성자는 서버 이름과 기본 프로필로 표시한다', async () => {
    const user = userEvent.setup();
    await commentTable.create({
      id: 1,
      studyId: 1,
      noticeId: 1,
      authorUserId: null,
      author: { name: '탈퇴한 스터디원', profileImageUrl: null, role: null, isDeparted: true },
      content: '보존된 댓글',
      createdAt: '2026-10-30T13:10:00',
      isEdited: false,
    });
    renderPage();
    await user.click(await screen.findByRole('button', { name: '공지 댓글 1 열기' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('탈퇴한 스터디원')).toBeVisible();
    expect(within(dialog).getByText('보존된 댓글')).toBeVisible();
    expect(within(dialog).queryByText(/· 리더|· 나/)).not.toBeInTheDocument();
  });

  async function openComments(user: ReturnType<typeof userEvent.setup>, count: number) {
    await user.click(await screen.findByRole('button', { name: `공지 댓글 ${count} 열기` }));
    return screen.getByRole('dialog', { name: `공지 댓글 ${count}` });
  }

  async function editFirstComment(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: '바니 댓글 더보기' }));
    await user.click(
      within(screen.getByRole('menu', { name: '댓글 관리' })).getByRole('menuitem', {
        name: '수정',
      }),
    );
    return screen.getByRole('form', { name: '댓글 수정' });
  }

  test('작성 후 개수와 입력을 갱신하고 나머지 댓글은 더 보기로 조회한다', async () => {
    const user = userEvent.setup();
    await addComments(12);
    renderPage();
    const dialog = await openComments(user, 12);
    const field = within(dialog).getByRole('textbox', { name: '댓글 내용' });
    await user.type(field, '  새 댓글\n두 번째 줄  ');
    await user.click(within(dialog).getByRole('button', { name: '등록' }));
    await waitFor(() =>
      expect(within(dialog).getByRole('textbox', { name: '댓글 내용' })).toHaveValue(''),
    );
    expect(await within(dialog).findByRole('heading', { name: '공지 댓글 13' })).toBeVisible();
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(10);
    await user.click(within(dialog).getByRole('button', { name: '댓글 10개 더 보기' }));
    expect(await within(dialog).findByText('새 댓글 두 번째 줄')).toBeVisible();
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(13);
    const saved = commentTable.findFirst((q) => q.where({ content: '새 댓글\n두 번째 줄' }));
    expect(saved?.content).toBe('새 댓글\n두 번째 줄');
  });

  test('등록 실패를 자동 재시도하지 않고 입력을 보존해 재시도한다', async () => {
    const user = userEvent.setup();
    let requests = 0;
    server.use(
      http.post(
        commentsUrl,
        () => {
          requests++;
          return new HttpResponse(null, { status: 500 });
        },
        { once: true },
      ),
    );
    renderPage();
    const dialog = await openComments(user, 0);
    const field = within(dialog).getByRole('textbox', { name: '댓글 내용' });
    await user.type(field, '보존할 입력');
    await user.click(within(dialog).getByRole('button', { name: '등록' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('댓글을 등록하지 못했어요.');
    expect(field).toHaveValue('보존할 입력');
    expect(requests).toBe(1);
    await user.click(within(dialog).getByRole('button', { name: '등록' }));
    expect(await within(dialog).findByText('보존할 입력')).toBeVisible();
    expect(within(dialog).getByRole('textbox', { name: '댓글 내용' })).toHaveValue('');
  });

  test('같은 원문에서 닫았다 열면 작성 초안을 유지한다', async () => {
    const user = userEvent.setup();
    renderPage();
    let dialog = await openComments(user, 0);
    await user.type(within(dialog).getByRole('textbox', { name: '댓글 내용' }), '닫아도 유지');
    await user.click(within(dialog).getByRole('button', { name: '댓글 닫기' }));
    dialog = await openComments(user, 0);
    expect(within(dialog).getByRole('textbox', { name: '댓글 내용' })).toHaveValue('닫아도 유지');
  });

  test('본인 댓글을 수정하면 위치와 개수를 유지하고 수정됨을 표시한다', async () => {
    const user = userEvent.setup();
    await addComments(2);
    renderPage();
    await openComments(user, 2);
    const form = await editFirstComment(user);
    const field = within(form).getByRole('textbox', { name: '댓글 수정 내용' });
    expect(field).toHaveValue('댓글 본문 1');
    await user.clear(field);
    await user.type(field, '수정 완료');
    await user.click(within(form).getByRole('button', { name: '저장' }));
    const list = await screen.findByRole('list', { name: '댓글 목록' });
    await waitFor(() =>
      expect(within(list).getAllByRole('listitem')[0]).toHaveTextContent('수정 완료'),
    );
    expect(within(list).getAllByRole('listitem')[1]).toHaveTextContent('댓글 본문 2');
    expect(within(list).getAllByRole('listitem')[0]).toHaveTextContent('수정됨');
    expect(screen.getByRole('heading', { name: '공지 댓글 2' })).toBeVisible();
  });

  test('수정 취소는 저장하지 않고 작성 초안을 유지한다', async () => {
    const user = userEvent.setup();
    await addComments(1);
    renderPage();
    const dialog = await openComments(user, 1);
    await user.type(within(dialog).getByRole('textbox', { name: '댓글 내용' }), '작성 초안');
    const menuButton = within(dialog).getByRole('button', { name: '바니 댓글 더보기' });
    const form = await editFirstComment(user);
    await user.clear(within(form).getByRole('textbox', { name: '댓글 수정 내용' }));
    await user.type(within(form).getByRole('textbox', { name: '댓글 수정 내용' }), '버릴 수정');
    await user.click(within(form).getByRole('button', { name: '취소' }));
    expect(screen.getByRole('textbox', { name: '댓글 내용' })).toHaveValue('작성 초안');
    expect(commentTable.findFirst((q) => q.where({ id: 1 }))?.content).toBe('댓글 본문 1');
    await waitFor(() => expect(menuButton).toHaveFocus());
  });

  test('수정 실패 시 편집 내용을 보존하고 원문을 바꾸지 않는다', async () => {
    const user = userEvent.setup();
    await addComments(1);
    server.use(
      http.patch(`${commentsUrl}/1`, () =>
        HttpResponse.json(
          {
            code: 'COMMENT_STATE_CHANGED',
            message: '댓글 대상이나 참여 상태가 변경되었습니다. 다시 시도해 주세요.',
          },
          { status: 409 },
        ),
      ),
    );
    renderPage();
    await openComments(user, 1);
    const form = await editFirstComment(user);
    const field = within(form).getByRole('textbox', { name: '댓글 수정 내용' });
    await user.clear(field);
    await user.type(field, '실패한 수정 초안');
    await user.click(within(form).getByRole('button', { name: '저장' }));
    expect(await within(form).findByRole('alert')).toHaveTextContent(
      '댓글 대상이나 참여 상태가 변경되었습니다.',
    );
    expect(field).toHaveValue('실패한 수정 초안');
    expect(commentTable.findFirst((q) => q.where({ id: 1 }))?.content).toBe('댓글 본문 1');
  });

  test('리더는 타인 댓글 메뉴에 수정 없이 삭제만 표시한다', async () => {
    const user = userEvent.setup();
    await addComments(2);
    renderPage();
    await openComments(user, 2);
    await user.click(screen.getByRole('button', { name: '준 댓글 더보기' }));
    const menu = screen.getByRole('menu', { name: '댓글 관리' });
    expect(within(menu).queryByRole('menuitem', { name: '수정' })).not.toBeInTheDocument();
    expect(within(menu).getByRole('menuitem', { name: '삭제' })).toBeVisible();
  });

  test('일반 멤버는 타인 댓글 더보기 버튼이 없다', async () => {
    const user = userEvent.setup();
    await addComments(2);
    const member = memberTable.findFirst((q) => q.where({ id: 1 }));
    if (!member) throw new Error('테스트 멤버 없음');
    memberTable.delete((q) => q.where({ id: 1 }));
    await memberTable.create({ ...member, role: 'MEMBER' });
    renderPage();
    await openComments(user, 2);
    expect(screen.getByRole('button', { name: '바니 댓글 더보기' })).toBeVisible();
    expect(screen.queryByRole('button', { name: '준 댓글 더보기' })).not.toBeInTheDocument();
  });

  test('등록 성공 뒤 조회만 실패하면 초안을 비우고 재진입 시 새 댓글을 표시한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(commentsUrl, async () => {
        await commentTable.create({
          id: 900,
          studyId: 1,
          noticeId: 1,
          authorUserId: 1,
          author: { name: '바니', profileImageUrl: null, role: 'LEADER', isDeparted: false },
          content: '등록 성공 조회 실패',
          createdAt: '2026-11-08T10:00:00',
          isEdited: false,
        });
        server.use(
          http.get(commentsUrl, () => new HttpResponse(null, { status: 500 }), { once: true }),
        );
        return HttpResponse.json({ commentId: 900 }, { status: 201 });
      }),
    );
    renderPage();
    const dialog = await openComments(user, 0);
    const field = within(dialog).getByRole('textbox', { name: '댓글 내용' });
    await user.type(field, '등록 성공 조회 실패');
    await user.click(within(dialog).getByRole('button', { name: '등록' }));
    await waitFor(() =>
      expect(within(dialog).getByRole('textbox', { name: '댓글 내용' })).toHaveValue(''),
    );
    expect(
      within(dialog).queryByText('댓글을 등록하지 못했어요. 다시 시도해주세요.'),
    ).not.toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: '댓글 닫기' }));
    await user.click(screen.getByRole('tab', { name: '현황' }));
    await user.click(screen.getByRole('tab', { name: '내용' }));
    await user.click(await screen.findByRole('button', { name: '공지 댓글 1 열기' }));
    expect(
      await within(screen.getByRole('dialog')).findByText('등록 성공 조회 실패'),
    ).toBeVisible();
  });
});

describe('알림 댓글 포커스', () => {
  test('처음부터 대상 댓글까지 조회하고 이후 더 보기는 이어서 조회한다', async () => {
    const user = userEvent.setup();
    await addComments(25);
    const requests: URLSearchParams[] = [];
    const listener = ({ request }: { request: Request }) => {
      if (request.url.startsWith(commentsUrl)) requests.push(new URL(request.url).searchParams);
    };
    server.events.on('request:start', listener);
    try {
      renderPage('/studies/1/notices/1?focusCommentId=12');
      const panel = within(await screen.findByRole('dialog', { name: '공지 댓글 25' }));
      expect(panel.getByText('댓글 본문 12')).toBeVisible();
      expect(panel.getByText('댓글 본문 1')).toBeInTheDocument();
      await user.click(panel.getByRole('button', { name: '댓글 10개 더 보기' }));
      expect(await panel.findByText('댓글 본문 25')).toBeVisible();
      expect(requests[0].has('focusCommentId')).toBe(false);
      expect(requests[0].has('cursor')).toBe(false);
      expect(requests[1].get('cursor')).toBe('10');
      expect(requests[2].get('cursor')).toBe('20');
      expect(requests[1].has('focusCommentId')).toBe(false);
    } finally {
      server.events.removeListener('request:start', listener);
    }
  });

  test.each(['0', '-1', 'abc', '1.5', '9007199254740992'])(
    '잘못된 focus %s는 일반 조회로 처리한다',
    async (focus) => {
      await addComments(1);
      renderPage(`/studies/1/notices/1?focusCommentId=${focus}`);
      expect(await screen.findByRole('button', { name: '공지 댓글 1 열기' })).toBeVisible();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    },
  );
});

describe('알림 댓글 복구', () => {
  test('삭제된 대상 댓글이면 목록을 표시하고 위치 이동만 생략한다', async () => {
    await addComments(1);
    renderPage('/studies/1/notices/1?focusCommentId=999&keep=value#content');
    const panel = within(await screen.findByRole('dialog', { name: '공지 댓글 1' }));
    expect(panel.getByText('댓글 본문 1')).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '전체 댓글 보기' })).not.toBeInTheDocument();
    expect(screen.getByTestId('current-location')).toHaveTextContent(
      '/studies/1/notices/1?focusCommentId=999&keep=value#content',
    );
  });

  test('조회 권한이 없으면 댓글을 숨기고 권한 안내를 표시한다', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(commentsUrl, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderPage('/studies/1/notices/1?focusCommentId=1');
    expect(await screen.findByRole('alert')).toHaveTextContent('접근 권한이 없습니다.');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '전체 댓글 보기' })).not.toBeInTheDocument();
  });
});
