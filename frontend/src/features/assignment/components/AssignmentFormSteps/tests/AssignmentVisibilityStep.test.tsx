import { screen, within } from '@testing-library/react';
import { Route } from 'react-router';
import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../../../config';
import { createWrapper, login, logout, setup } from '../../../../../test/render';
import { server } from '../../../../../mocks/msw-node';
import { userTable } from '../../../../user/mocks/db';
import { studyTable } from '../../../../study/mocks/db';
import { memberTable } from '../../../../member/mocks/db';
import CreateAssignmentPage from '../../../pages/CreateAssignmentPage';
import {
  completeContentStep,
  completeScheduleStep,
  completeSubmittersStep,
} from './fixtures/assignmentSteps';

async function setupVisibilityStep() {
  const result = setup(<CreateAssignmentPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments/create'],
      routes: (element) => <Route path="/studies/:studyId/assignments/create" element={element} />,
    }),
  });
  await completeContentStep(result.user);
  await completeScheduleStep(result.user);
  await completeSubmittersStep(result.user);
  return result;
}

describe('AssignmentVisibilityStep Tests', () => {
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
    login('안톨리니');
  });

  afterEach(logout);

  describe('공개 범위 선택', () => {
    test('각 공개 범위의 이름과 설명이 표시된다', async () => {
      await setupVisibilityStep();

      const visibility = within(screen.getByRole('radiogroup', { name: '제출물 공개 범위' }));
      expect(visibility.getByText('스터디원 모두')).toBeVisible();
      expect(
        visibility.getByText('제출 대상이 아닌 멤버도 서로의 제출물을 볼 수 있어요.'),
      ).toBeVisible();
      expect(visibility.getByText('제출한 사람과 리더만')).toBeVisible();
      expect(visibility.getByText('다른 스터디원에게는 제출물이 보이지 않아요.')).toBeVisible();
    });

    test('특정 항목을 선택하면 해당 항목이 선택되고 다른 항목은 선택 해제된다', async () => {
      const { user } = await setupVisibilityStep();
      const visibility = within(screen.getByRole('radiogroup', { name: '제출물 공개 범위' }));

      await user.click(visibility.getByRole('radio', { name: '제출한 사람과 리더만' }));
      expect(visibility.getByRole('radio', { name: '제출한 사람과 리더만' })).toBeChecked();
      expect(visibility.getByRole('radio', { name: '스터디원 모두' })).not.toBeChecked();

      await user.click(visibility.getByRole('radio', { name: '스터디원 모두' }));
      expect(visibility.getByRole('radio', { name: '스터디원 모두' })).toBeChecked();
      expect(visibility.getByRole('radio', { name: '제출한 사람과 리더만' })).not.toBeChecked();
    });

    test('이미 선택한 공개 범위를 다시 클릭해도 선택이 유지된다', async () => {
      const { user } = await setupVisibilityStep();
      const option = screen.getByRole('radio', { name: '제출한 사람과 리더만' });
      await user.click(option);

      await user.click(option);

      expect(option).toBeChecked();
    });
  });

  describe('과제 만들기', () => {
    test('과제를 생성하는 동안 과제 만들기 버튼이 비활성화되어 있다', async () => {
      let finishCreating!: () => void;
      const pending = new Promise<void>((resolve) => {
        finishCreating = resolve;
      });
      server.use(
        http.post(`${API_URL}/studies/:studyId/assignments`, async () => {
          await pending;
          return HttpResponse.json({ assignmentId: 1 }, { status: 201 });
        }),
      );
      const { user } = await setupVisibilityStep();

      await user.click(screen.getByRole('button', { name: '과제 만들기' }));

      expect(await screen.findByRole('button', { name: '과제 만들기' })).toBeDisabled();
      finishCreating();
    });
  });
});
