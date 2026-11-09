import { screen } from '@testing-library/react';
import { Route } from 'react-router';
import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../../../config';
import { createWrapper, login, logout, setup } from '../../../../../test/render';
import { server } from '../../../../../mocks/msw-node';
import { userTable } from '../../../../user/mocks/db';
import { studyTable } from '../../../../study/mocks/db';
import { memberTable } from '../../../../member/mocks/db';
import CreateAssignmentPage from '../../../pages/CreateAssignmentPage';
import { completeContentStep, completeScheduleStep } from './fixtures/assignmentSteps';

async function setupSubmittersStep(browserHistory = false) {
  if (browserHistory) {
    window.history.replaceState({ idx: 0 }, '', '/studies/1/assignments/create');
  }
  const result = setup(<CreateAssignmentPage />, {
    wrapper: createWrapper({
      browserHistory,
      initialEntries: ['/studies/1/assignments/create'],
      routes: (element) => <Route path="/studies/:studyId/assignments/create" element={element} />,
    }),
  });
  await completeContentStep(result.user);
  await completeScheduleStep(result.user);
  return result;
}

describe('AssignmentSubmittersStep Tests', () => {
  beforeEach(async () => {
    await userTable.create({ id: 1, name: '안톨리니', profileImage: null });
    await studyTable.create({
      id: 1,
      name: '리액트 스터디',
      description: '리액트 상태 관리를 함께 공부합니다.',
      inviteLink: 'react-study',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
      userId: 1,
      name: '안톨리니',
      profileImage: null,
      role: 'LEADER',
    });
    await memberTable.create({
      id: 2,
      studyId: 1,
      userId: 2,
      name: '루멘',
      profileImage: null,
      role: 'MEMBER',
    });
    login('안톨리니');
  });

  afterEach(logout);

  test('제출 인원을 선택하지 않으면 다음 버튼이 비활성화되어 있다', async () => {
    const { user } = await setupSubmittersStep();

    await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));

    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled();
  });

  test('제출 인원을 한 명 이상 선택하면 다음 버튼이 활성화된다', async () => {
    const { user } = await setupSubmittersStep();
    await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));

    await user.click(screen.getByRole('checkbox', { name: '루멘' }));

    expect(screen.getByRole('button', { name: '다음' })).toBeEnabled();
  });

  test('제출 인원을 선택하고 다음 버튼을 클릭하면 다음 페이지로 이동한다', async () => {
    const { user } = await setupSubmittersStep();
    await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));
    await user.click(screen.getByRole('checkbox', { name: '루멘' }));

    await user.click(screen.getByRole('button', { name: '다음' }));

    expect(screen.getByRole('heading', { name: '제출물을 누구에게 보여줄까요?' })).toBeVisible();
  });

  describe('제출 인원 선택', () => {
    test('다음 단계에서 돌아오면 선택한 제출 인원이 유지된다', async () => {
      const { user } = await setupSubmittersStep(true);
      await user.click(await screen.findByRole('checkbox', { name: '루멘' }));
      await user.click(screen.getByRole('button', { name: '다음' }));

      await user.click(screen.getByRole('button', { name: '뒤로 가기' }));

      expect(await screen.findByRole('checkbox', { name: '안톨리니 (나)' })).toBeChecked();
      expect(screen.getByRole('checkbox', { name: '루멘' })).not.toBeChecked();
    });

    test('스터디원 목록에 각 스터디원의 이름이 표시된다', async () => {
      await setupSubmittersStep();

      expect(await screen.findByText('안톨리니 (나)')).toBeVisible();
      expect(screen.getByText('루멘')).toBeVisible();
    });

    test('스터디원을 선택하면 해당 스터디원의 체크박스가 선택된다', async () => {
      const { user } = await setupSubmittersStep();
      await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));

      await user.click(screen.getByRole('checkbox', { name: '루멘' }));

      expect(screen.getByRole('checkbox', { name: '루멘' })).toBeChecked();
    });

    test('선택된 스터디원을 다시 클릭하면 해당 스터디원의 선택이 해제된다', async () => {
      const { user } = await setupSubmittersStep();

      await user.click(await screen.findByRole('checkbox', { name: '루멘' }));

      expect(screen.getByRole('checkbox', { name: '루멘' })).not.toBeChecked();
    });

    test('스터디원을 선택하거나 해제하면 선택한 인원수가 변경된다', async () => {
      const { user } = await setupSubmittersStep();

      await user.click(await screen.findByRole('checkbox', { name: '루멘' }));
      expect(screen.getByText('1명 선택')).toBeVisible();

      await user.click(screen.getByRole('checkbox', { name: '루멘' }));
      expect(screen.getByText('2명 선택')).toBeVisible();
    });

    test('일부 스터디원만 선택한 상태에서 전체 선택을 클릭하면 모든 스터디원이 선택된다', async () => {
      const { user } = await setupSubmittersStep();
      await user.click(await screen.findByRole('checkbox', { name: '루멘' }));

      await user.click(screen.getByRole('checkbox', { name: '전체 선택' }));

      expect(screen.getByRole('checkbox', { name: '안톨리니 (나)' })).toBeChecked();
      expect(screen.getByRole('checkbox', { name: '루멘' })).toBeChecked();
    });

    test('전체 선택된 상태에서 전체 선택을 클릭하면 모든 스터디원의 선택이 해제된다', async () => {
      const { user } = await setupSubmittersStep();

      await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));

      expect(screen.getByRole('checkbox', { name: '안톨리니 (나)' })).not.toBeChecked();
      expect(screen.getByRole('checkbox', { name: '루멘' })).not.toBeChecked();
    });

    test('전체 선택된 상태에서 한 명의 선택을 해제하면 전체 선택 체크박스도 해제된다', async () => {
      const { user } = await setupSubmittersStep();

      await user.click(await screen.findByRole('checkbox', { name: '루멘' }));

      expect(screen.getByRole('checkbox', { name: '전체 선택' })).not.toBeChecked();
    });

    test('스터디원을 개별적으로 모두 선택하면 전체 선택 체크박스도 선택된다', async () => {
      const { user } = await setupSubmittersStep();
      await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));

      await user.click(screen.getByRole('checkbox', { name: '안톨리니 (나)' }));
      await user.click(screen.getByRole('checkbox', { name: '루멘' }));

      expect(screen.getByRole('checkbox', { name: '전체 선택' })).toBeChecked();
    });
  });

  describe('스터디원 목록 조회', () => {
    test('스터디원 목록을 불러오는 동안 로딩 화면이 표시된다', async () => {
      let finishLoading!: () => void;
      const pending = new Promise<void>((resolve) => {
        finishLoading = resolve;
      });
      server.use(
        http.get(`${API_URL}/studies/:studyId/members`, async () => {
          await pending;
        }),
      );

      await setupSubmittersStep();

      expect(await screen.findByRole('img', { name: '로딩 중' })).toBeVisible();
      finishLoading();
      await screen.findByRole('checkbox', { name: '전체 선택' });
    });

    test('스터디원 목록을 불러오지 못하면 오류 화면이 표시된다', async () => {
      server.use(
        http.get(`${API_URL}/studies/:studyId/members`, () =>
          HttpResponse.json(
            { code: 'STUDY_NOT_FOUND', message: '스터디를 찾을 수 없습니다.' },
            { status: 404 },
          ),
        ),
      );
      await setupSubmittersStep();

      expect(await screen.findByText('스터디를 찾을 수 없습니다.')).toBeVisible();
    });
  });
});
