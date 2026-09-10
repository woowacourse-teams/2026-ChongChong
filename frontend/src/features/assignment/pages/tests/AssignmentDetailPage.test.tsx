import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { invalidInputResponse } from '../../../../mocks/errors';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import AssignmentDetailPage from '../AssignmentDetailPage';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const ASSIGNMENT_DETAIL_URL = `${API_URL}/studies/:studyId/assignments/:assignmentId`;

function renderAssignmentDetailPage() {
  render(
    <Routes>
      <Route
        path="/studies/:studyId/assignments/:assignmentId"
        element={<AssignmentDetailPage />}
      />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/assignments/1'] }) },
  );
}

describe('리드 과제 상세 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'LEADER',
          userName: '안톨리니',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/status`, () =>
        HttpResponse.json({
          id: 1,
          memberCount: 1,
          completeCount: 0,
          incompleteCount: 1,
          completeMembers: [],
          incompleteMembers: [{ id: 1, name: '안톨리니', profileImage: null }],
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        HttpResponse.json({ submissions: [] }),
      ),
    );
  });

  test('접근 권한 관련 에러가 발생하면 에러 메시지를 본문에 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '과제 정보를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });

  test('조회에 실패해도 헤더와 하단 탭을 유지한다', async () => {
    server.use(http.get(ASSIGNMENT_DETAIL_URL, () => new HttpResponse(null, { status: 403 })));
    renderAssignmentDetailPage();

    await within(screen.getByRole('main')).findByRole('img', { name: '오류' });

    const header = screen.getByRole('banner');
    expect(within(header).getByRole('heading', { name: '과제' })).toBeVisible();
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('리드 과제 제출 현황 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'LEADER',
          userName: '안톨리니',
        }),
      ),
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '객체지향 설계 과제',
          content: '객체의 역할과 책임을 정리해주세요.',
          submissionMethod: '텍스트로 제출하세요',
          closeAt: '2999-12-31T23:59:59',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        HttpResponse.json({ submissions: [] }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(`${ASSIGNMENT_DETAIL_URL}/status`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(`${ASSIGNMENT_DETAIL_URL}/status`, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '과제 제출 현황을 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});

describe('리드 과제 제출 내역 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'LEADER',
          userName: '안톨리니',
        }),
      ),
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '객체지향 설계 과제',
          content: '객체의 역할과 책임을 정리해주세요.',
          submissionMethod: '텍스트로 제출하세요',
          closeAt: '2999-12-31T23:59:59',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/status`, () =>
        HttpResponse.json({
          id: 1,
          memberCount: 1,
          completeCount: 0,
          incompleteCount: 1,
          completeMembers: [],
          incompleteMembers: [{ id: 1, name: '안톨리니', profileImage: null }],
        }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(`${ASSIGNMENT_DETAIL_URL}/submissions`, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '제출 내역을 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});

describe('스터디원 과제 상세 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions/my`, () =>
        HttpResponse.json({ submitted: false }),
      ),
    );
  });

  test('접근 권한 관련 에러가 발생하면 에러 메시지를 본문에 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '과제 정보를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});

describe('스터디원 본인 제출 정보 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        }),
      ),
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '객체지향 설계 과제',
          content: '객체의 역할과 책임을 정리해주세요.',
          submissionMethod: '텍스트로 제출하세요',
          closeAt: '2999-12-31T23:59:59',
        }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions/my`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 기본 에러 메시지를 표시하고 제출하기 버튼을 표시하지 않는다', async () => {
    server.use(http.get(`${ASSIGNMENT_DETAIL_URL}/submissions/my`, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '내 제출 정보를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: '제출하기' })).not.toBeInTheDocument();
  });
});

describe('스터디원 과제 제출 실패', () => {
  beforeEach(() => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        }),
      ),
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '객체지향 설계 과제',
          content: '객체의 역할과 책임을 정리해주세요.',
          submissionMethod: '텍스트로 제출하세요',
          closeAt: '2999-12-31T23:59:59',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions/my`, () =>
        HttpResponse.json({ submitted: false }),
      ),
    );
  });

  test('제출 내용과 링크가 유효하지 않으면 각 필드에 오류 메시지를 표시한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        invalidInputResponse([
          { field: 'content', code: 'INVALID', reason: '제출 내용을 확인해주세요.' },
          { field: 'link', code: 'INVALID', reason: '제출 링크를 확인해주세요.' },
        ]),
      ),
    );
    renderAssignmentDetailPage();

    await user.type(await screen.findByRole('textbox', { name: '내용' }), '객체의 역할과 책임');
    await user.type(
      screen.getByRole('textbox', { name: '링크' }),
      'https://example.com/submission',
    );
    await user.click(screen.getByRole('button', { name: '제출하기' }));

    const submissionForm = within(screen.getByRole('region', { name: '내 제출' }));
    expect(await submissionForm.findByText('제출 내용을 확인해주세요.')).toBeVisible();
    expect(await submissionForm.findByText('제출 링크를 확인해주세요.')).toBeVisible();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('스터디에 대한 접근 권한이 없으면 접근 권한 안내를 토스트로 표시한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    await user.type(await screen.findByRole('textbox', { name: '내용' }), '객체의 역할과 책임');
    await user.click(screen.getByRole('button', { name: '제출하기' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('해당 스터디에 대한 접근 권한이 없습니다.');
    expect(toast).toBeVisible();
  });

  test('네트워크 에러가 발생하면 과제 제출 실패 안내를 토스트로 표시한다', async () => {
    const user = userEvent.setup();
    server.use(http.post(`${ASSIGNMENT_DETAIL_URL}/submissions`, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    await user.type(await screen.findByRole('textbox', { name: '내용' }), '객체의 역할과 책임');
    await user.click(screen.getByRole('button', { name: '제출하기' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('과제 제출에 실패했습니다.');
    expect(toast).toBeVisible();
  });
});

describe('스터디원 과제 제출물 수정 실패', () => {
  beforeEach(() => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        }),
      ),
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '객체지향 설계 과제',
          content: '객체의 역할과 책임을 정리해주세요.',
          submissionMethod: '텍스트로 제출하세요',
          closeAt: '2999-12-31T23:59:59',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions/my`, () =>
        HttpResponse.json({
          submitted: true,
          submissionId: 1,
          createdAt: '2026-09-01T09:00:00',
          content: '기존 제출 내용',
          link: 'https://example.com/submission',
        }),
      ),
    );
  });

  test('수정한 내용과 링크가 유효하지 않으면 각 필드에 오류 메시지를 표시한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.patch(`${ASSIGNMENT_DETAIL_URL}/submissions/:submissionId`, () =>
        invalidInputResponse([
          { field: 'content', code: 'INVALID', reason: '제출 내용을 확인해주세요.' },
          { field: 'link', code: 'INVALID', reason: '제출 링크를 확인해주세요.' },
        ]),
      ),
    );
    renderAssignmentDetailPage();

    await user.click(await screen.findByRole('button', { name: '편집하기' }));
    const contentInput = screen.getByRole('textbox', { name: '내용' });
    const linkInput = screen.getByRole('textbox', { name: '링크' });
    await user.clear(contentInput);
    await user.type(contentInput, '수정한 제출 내용');
    await user.clear(linkInput);
    await user.type(linkInput, 'https://example.com/revised-submission');
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    const submissionForm = within(screen.getByRole('region', { name: '내 제출' }));
    expect(await submissionForm.findByText('제출 내용을 확인해주세요.')).toBeVisible();
    expect(await submissionForm.findByText('제출 링크를 확인해주세요.')).toBeVisible();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('스터디에 대한 접근 권한이 없으면 접근 권한 안내를 토스트로 표시한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.patch(`${ASSIGNMENT_DETAIL_URL}/submissions/:submissionId`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    await user.click(await screen.findByRole('button', { name: '편집하기' }));
    const contentInput = screen.getByRole('textbox', { name: '내용' });
    await user.clear(contentInput);
    await user.type(contentInput, '수정한 제출 내용');
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('해당 스터디에 대한 접근 권한이 없습니다.');
    expect(toast).toBeVisible();
  });

  test('네트워크 에러가 발생하면 제출물 수정 실패 안내를 토스트로 표시한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.patch(`${ASSIGNMENT_DETAIL_URL}/submissions/:submissionId`, () => HttpResponse.error()),
    );
    renderAssignmentDetailPage();

    await user.click(await screen.findByRole('button', { name: '편집하기' }));
    const contentInput = screen.getByRole('textbox', { name: '내용' });
    await user.clear(contentInput);
    await user.type(contentInput, '수정한 제출 내용');
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('과제 제출물 수정에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(screen.getByRole('textbox', { name: '내용' })).toHaveValue('수정한 제출 내용');
  });
});
