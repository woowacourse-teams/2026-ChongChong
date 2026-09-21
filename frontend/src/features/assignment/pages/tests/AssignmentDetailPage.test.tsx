import type { PropsWithChildren } from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../../../config';
import { invalidInputResponse } from '../../../../mocks/errors';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { memberTable } from '../../../member/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { assignmentTable, submissionTable } from '../../mocks/db';
import AssignmentDetailPage from '../AssignmentDetailPage';

jest.mock('@posthog/react', () => ({
  ...jest.requireActual('@posthog/react'),
  PostHogCaptureOnViewed: ({ children }: PropsWithChildren) => children,
}));

const ASSIGNMENT_DETAIL_URL = `${API_URL}/studies/:studyId/assignments/:assignmentId`;
const SUBMISSION_STATUS_URL = `${ASSIGNMENT_DETAIL_URL}/status`;
const SUBMISSIONS_URL = `${ASSIGNMENT_DETAIL_URL}/submissions`;
const MY_SUBMISSION_URL = `${SUBMISSIONS_URL}/my`;
const SUBMISSION_DETAIL_URL = `${SUBMISSIONS_URL}/:submissionId`;

function setupAssignmentDetailPage() {
  return setup(<AssignmentDetailPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments/1'],
      routes: (element) => (
        <Route path="/studies/:studyId/assignments/:assignmentId" element={element} />
      ),
    }),
  });
}

async function findSubmissionForm() {
  const submissionForm = within(await screen.findByRole('region', { name: '내 제출' }));
  const contentInput = await submissionForm.findByRole('textbox', { name: '내용' });
  const linkInput = submissionForm.getByRole('textbox', { name: '링크' });

  return { submissionForm, contentInput, linkInput };
}

describe('과제 상세 페이지 테스트', () => {
  const leaderUserName = '바니';
  const memberUserName = '이든';

  beforeEach(async () => {
    await userTable.create({
      id: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
    });
    await userTable.create({
      id: 2,
      name: memberUserName,
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: 1,
      name: '스프링 스터디',
      description: '스프링을 실제로 만들어보는 스터디',
      inviteLink: 'spring-winter-study',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
      userId: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
      role: 'LEADER',
    });
    await memberTable.create({
      id: 2,
      studyId: 1,
      userId: 2,
      name: memberUserName,
      profileImage: 'http://localhost:8000',
      role: 'MEMBER',
    });
    await assignmentTable.create({
      id: 1,
      studyId: 1,
      title: '스프링 설계 과제',
      content: '스프링에서 가장 중요한걸 정리해서 보내주세요',
      submissionMethod: '텍스트로 제출하세요',
      closeAt: '2999-12-31T23:59:59',
      submissionTarget: 'MEMBERS_AND_LEADER',
      completeUserIds: [],
    });
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    logout();
  });

  describe('스터디 리드', () => {
    beforeEach(() => {
      login(leaderUserName);
    });

    describe('내 과제 제출', () => {
      test('리더도 제출 대상이면 상세 하단에 제출 폼을 표시한다', async () => {
        const { user } = setupAssignmentDetailPage();

        const { submissionForm } = await findSubmissionForm();
        const moreButton = screen.getByRole('button', { name: '과제 더보기' });
        expect(submissionForm.getByRole('button', { name: '제출하기' })).toBeVisible();
        expect(moreButton.closest('header')).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: '과제 삭제' })).not.toBeInTheDocument();
        await user.click(moreButton);
        expect(screen.getByRole('menuitem', { name: '과제 수정' })).toBeVisible();
        expect(screen.getByRole('menuitem', { name: '과제 삭제' })).toBeVisible();
      });

      test('리더가 제출 대상이 아니면 내 제출 정보를 조회하지 않고 폼도 표시하지 않는다', async () => {
        server.use(
          http.get(ASSIGNMENT_DETAIL_URL, () =>
            HttpResponse.json({
              id: 1,
              title: '스프링 설계 과제',
              content: '스프링에서 가장 중요한걸 정리해서 보내주세요',
              submissionMethod: '텍스트로 제출하세요',
              closeAt: '2999-12-31T23:59:59',
              submissionTarget: 'MEMBERS_ONLY',
            }),
          ),
        );
        const getMySubmission = jest.fn(() => HttpResponse.json({ submitted: false }));
        server.use(http.get(MY_SUBMISSION_URL, getMySubmission));

        setupAssignmentDetailPage();

        expect(await screen.findByText('스프링 설계 과제')).toBeVisible();
        expect(screen.queryByRole('region', { name: '내 제출' })).not.toBeInTheDocument();
        expect(getMySubmission).not.toHaveBeenCalled();
      });

      test('이미 제출했다면 제출 폼 대신 내 제출 내용을 표시한다', async () => {
        await submissionTable.create({
          id: 10,
          assignmentId: 1,
          userId: 1,
          createdAt: '2026-09-01T09:00:00',
          content: '리더가 제출한 과제',
          link: null,
        });
        setupAssignmentDetailPage();

        const mySubmission = within(await screen.findByRole('region', { name: '내 제출' }));
        expect(mySubmission.getByText('리더가 제출한 과제')).toBeVisible();
        expect(mySubmission.getByRole('button', { name: '편집하기' })).toBeVisible();
        expect(mySubmission.queryByRole('button', { name: '제출하기' })).not.toBeInTheDocument();
      });

      test('제출 후 내 제출 내용과 현황을 갱신한다', async () => {
        const { user } = setupAssignmentDetailPage();
        const { submissionForm, contentInput } = await findSubmissionForm();

        await user.type(contentInput, '리더의 스프링 설계 과제');
        await user.click(submissionForm.getByRole('button', { name: '제출하기' }));

        const mySubmission = within(await screen.findByRole('region', { name: '내 제출' }));
        expect(await mySubmission.findByRole('button', { name: '편집하기' })).toBeVisible();
        expect(mySubmission.getByText('리더의 스프링 설계 과제')).toBeVisible();
        await waitFor(() =>
          expect(screen.getByRole('progressbar', { name: '과제 제출률' })).toHaveAttribute(
            'aria-valuenow',
            '1',
          ),
        );
      });
    });

    describe('과제 상세 조회', () => {
      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.get(ASSIGNMENT_DETAIL_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()),
          message: '과제 정보를 불러오는데 실패했습니다.',
        },
      ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
        server.use(handler);
        setupAssignmentDetailPage();

        expect(
          await within(screen.getByRole('main')).findByText(message, {}, { timeout: 3000 }),
        ).toBeVisible();
      });

      test('조회에 실패해도 헤더와 하단 탭을 유지한다', async () => {
        server.use(http.get(ASSIGNMENT_DETAIL_URL, () => new HttpResponse(null, { status: 403 })));
        setupAssignmentDetailPage();

        await within(screen.getByRole('main')).findByRole('img', { name: '오류' });

        const header = screen.getByRole('banner');
        expect(within(header).getByRole('heading', { name: '과제' })).toBeVisible();
        expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
        expect(screen.getByRole('navigation')).toBeVisible();
      });
    });

    describe('과제 제출 현황 조회', () => {
      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.get(SUBMISSION_STATUS_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(SUBMISSION_STATUS_URL, () => HttpResponse.error()),
          message: '과제 제출 현황을 불러오는데 실패했습니다.',
        },
      ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
        server.use(handler);
        setupAssignmentDetailPage();

        expect(
          await within(screen.getByRole('main')).findByText(message, {}, { timeout: 3000 }),
        ).toBeVisible();
      });
    });

    describe('과제 제출 내역 조회', () => {
      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.get(SUBMISSIONS_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(SUBMISSIONS_URL, () => HttpResponse.error()),
          message: '제출 내역을 불러오는데 실패했습니다.',
        },
      ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
        server.use(handler);
        setupAssignmentDetailPage();

        expect(
          await within(screen.getByRole('main')).findByText(message, {}, { timeout: 3000 }),
        ).toBeVisible();
      });
    });

    describe('과제 삭제', () => {
      test.each([
        {
          title: '과제 삭제 권한이 없으면',
          handler: http.delete(ASSIGNMENT_DETAIL_URL, () =>
            HttpResponse.json(
              { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '요청한 작업을 수행할 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.delete(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()),
          message: '과제 삭제에 실패했습니다.',
        },
      ])('$title 에러 메시지를 토스트로 표시하고 확인창을 닫는다', async ({ handler, message }) => {
        server.use(handler);
        const { user } = setupAssignmentDetailPage();

        await user.click(await screen.findByRole('button', { name: '과제 더보기' }));
        await user.click(screen.getByRole('menuitem', { name: '과제 삭제' }));
        const dialog = screen.getByRole('alertdialog', { name: '과제를 삭제할까요?' });
        expect(dialog).toBeVisible();
        await user.click(within(dialog).getByRole('button', { name: '삭제' }));

        const toast = await screen.findByRole('status');
        expect(toast).toHaveTextContent(message);
        expect(toast).toBeVisible();
        expect(dialog).not.toBeVisible();
      });
    });
  });

  describe('스터디원', () => {
    beforeEach(() => {
      login(memberUserName);
    });

    describe('과제 상세 조회', () => {
      test('스터디원에게는 과제 관리 메뉴를 표시하지 않는다', async () => {
        setupAssignmentDetailPage();

        expect(await screen.findByText('스프링 설계 과제')).toBeVisible();
        expect(screen.queryByRole('button', { name: '과제 더보기' })).not.toBeInTheDocument();
      });

      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.get(ASSIGNMENT_DETAIL_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()),
          message: '과제 정보를 불러오는데 실패했습니다.',
        },
      ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
        server.use(handler);
        setupAssignmentDetailPage();

        expect(
          await within(screen.getByRole('main')).findByText(message, {}, { timeout: 3000 }),
        ).toBeVisible();
      });
    });

    describe('본인 제출 정보 조회', () => {
      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.get(MY_SUBMISSION_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(MY_SUBMISSION_URL, () => HttpResponse.error()),
          message: '내 제출 정보를 불러오는데 실패했습니다.',
        },
      ])(
        '$title 본문에 에러 메시지를 표시하고 제출하기 버튼을 표시하지 않는다',
        async ({ handler, message }) => {
          server.use(handler);
          setupAssignmentDetailPage();

          expect(
            await within(screen.getByRole('main')).findByText(message, {}, { timeout: 3000 }),
          ).toBeVisible();
          expect(screen.queryByRole('button', { name: '제출하기' })).not.toBeInTheDocument();
        },
      );

      test('링크가 null인 제출 정보를 정상적으로 표시하고 편집 폼에는 빈 링크를 보여준다', async () => {
        await submissionTable.create({
          id: 1,
          assignmentId: 1,
          userId: 2,
          createdAt: '2026-09-01T09:00:00',
          content: '링크 없이 제출한 내용',
          link: null,
        });
        const { user } = setupAssignmentDetailPage();

        const mySubmission = within(await screen.findByRole('region', { name: '내 제출' }));
        expect(mySubmission.getByText('링크 없이 제출한 내용')).toBeVisible();
        expect(mySubmission.queryByRole('link')).not.toBeInTheDocument();

        await user.click(mySubmission.getByRole('button', { name: '편집하기' }));

        const { contentInput, linkInput } = await findSubmissionForm();
        expect(contentInput).toHaveValue('링크 없이 제출한 내용');
        expect(linkInput).toHaveValue('');
      });
    });

    describe('과제 제출', () => {
      test('제출 성공 시 내 제출 내용으로 전환한다', async () => {
        const { user } = setupAssignmentDetailPage();
        const { submissionForm, contentInput } = await findSubmissionForm();

        await user.type(contentInput, '스터디원의 스프링 설계 과제');
        await user.click(submissionForm.getByRole('button', { name: '제출하기' }));

        const mySubmission = within(await screen.findByRole('region', { name: '내 제출' }));
        expect(await mySubmission.findByRole('button', { name: '편집하기' })).toBeVisible();
        expect(mySubmission.getByText('스터디원의 스프링 설계 과제')).toBeVisible();
      });

      test('제출 내용과 링크가 유효하지 않으면 각 필드에 오류 메시지를 표시한다', async () => {
        server.use(
          http.post(SUBMISSIONS_URL, () =>
            invalidInputResponse([
              { field: 'content', code: 'INVALID', reason: '제출 내용을 확인해주세요.' },
              { field: 'link', code: 'INVALID', reason: '제출 링크를 확인해주세요.' },
            ]),
          ),
        );
        const { user } = setupAssignmentDetailPage();
        const { submissionForm, contentInput, linkInput } = await findSubmissionForm();

        await user.type(contentInput, '객체의 역할과 책임');
        await user.type(linkInput, 'https://example.com/submission');
        await user.click(submissionForm.getByRole('button', { name: '제출하기' }));

        expect(await submissionForm.findByText('제출 내용을 확인해주세요.')).toBeVisible();
        expect(await submissionForm.findByText('제출 링크를 확인해주세요.')).toBeVisible();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.post(SUBMISSIONS_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.post(SUBMISSIONS_URL, () => HttpResponse.error()),
          message: '과제 제출에 실패했습니다.',
        },
      ])('$title 에러 메시지를 토스트로 표시한다', async ({ handler, message }) => {
        server.use(handler);
        const { user } = setupAssignmentDetailPage();
        const { submissionForm, contentInput } = await findSubmissionForm();

        await user.type(contentInput, '객체의 역할과 책임');
        await user.click(submissionForm.getByRole('button', { name: '제출하기' }));

        const toast = await screen.findByRole('status');
        expect(toast).toHaveTextContent(message);
        expect(toast).toBeVisible();
      });
    });

    describe('과제 제출물 수정', () => {
      beforeEach(async () => {
        await submissionTable.create({
          id: 1,
          assignmentId: 1,
          userId: 2,
          createdAt: '2026-09-01T09:00:00',
          content: '기존 제출 내용',
          link: 'https://example.com/submission',
        });
      });

      test('수정한 내용과 링크가 유효하지 않으면 각 필드에 오류 메시지를 표시한다', async () => {
        server.use(
          http.patch(SUBMISSION_DETAIL_URL, () =>
            invalidInputResponse([
              { field: 'content', code: 'INVALID', reason: '제출 내용을 확인해주세요.' },
              { field: 'link', code: 'INVALID', reason: '제출 링크를 확인해주세요.' },
            ]),
          ),
        );
        const { user } = setupAssignmentDetailPage();

        await user.click(await screen.findByRole('button', { name: '편집하기' }));
        const { submissionForm, contentInput, linkInput } = await findSubmissionForm();
        await user.clear(contentInput);
        await user.type(contentInput, '수정한 제출 내용');
        await user.clear(linkInput);
        await user.type(linkInput, 'https://example.com/revised-submission');
        await user.click(submissionForm.getByRole('button', { name: '수정하기' }));

        expect(await submissionForm.findByText('제출 내용을 확인해주세요.')).toBeVisible();
        expect(await submissionForm.findByText('제출 링크를 확인해주세요.')).toBeVisible();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      test.each([
        {
          title: '스터디에 대한 접근 권한이 없으면',
          handler: http.patch(SUBMISSION_DETAIL_URL, () =>
            HttpResponse.json(
              { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
              { status: 403 },
            ),
          ),
          message: '해당 스터디에 대한 접근 권한이 없습니다.',
        },
        {
          title: '네트워크 오류가 발생하면',
          handler: http.patch(SUBMISSION_DETAIL_URL, () => HttpResponse.error()),
          message: '과제 제출물 수정에 실패했습니다.',
        },
      ])(
        '$title 에러 메시지를 토스트로 표시하고 입력값을 유지한다',
        async ({ handler, message }) => {
          server.use(handler);
          const { user } = setupAssignmentDetailPage();

          await user.click(await screen.findByRole('button', { name: '편집하기' }));
          const { submissionForm, contentInput } = await findSubmissionForm();
          await user.clear(contentInput);
          await user.type(contentInput, '수정한 제출 내용');
          await user.click(submissionForm.getByRole('button', { name: '수정하기' }));

          const toast = await screen.findByRole('status');
          expect(toast).toHaveTextContent(message);
          expect(toast).toBeVisible();
          expect(screen.getByRole('textbox', { name: '내용' })).toHaveValue('수정한 제출 내용');
        },
      );
    });
  });
});
