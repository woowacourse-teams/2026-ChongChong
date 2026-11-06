import { completeContentStep, nextMonthDate, selectDateTime } from './fixtures/assignmentSteps';
import { screen, within } from '@testing-library/react';
import { Route } from 'react-router';
import { createWrapper, login, logout, setup } from '../../../../../test/render';
import { userTable } from '../../../../user/mocks/db';
import { studyTable } from '../../../../study/mocks/db';
import { memberTable } from '../../../../member/mocks/db';
import CreateAssignmentPage from '../../../pages/CreateAssignmentPage';

async function setupScheduleStep() {
  const result = setup(<CreateAssignmentPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments/create'],
      routes: (element) => <Route path="/studies/:studyId/assignments/create" element={element} />,
    }),
  });
  await completeContentStep(result.user);
  return result;
}

describe('AssignmentScheduleStep Tests', () => {
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

  test('필수 선택 필드가 모두 선택되지 않으면 다음 버튼이 비활성화되어 있다', async () => {
    await setupScheduleStep();

    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled();
  });

  test('필수 선택 필드를 전부 선택하면 다음 버튼이 활성화된다', async () => {
    const { user } = await setupScheduleStep();

    await user.click(screen.getByRole('button', { name: '마감 시각' }));
    await selectDateTime(user, 10);

    expect(screen.getByRole('button', { name: '다음' })).toBeEnabled();
  });

  test('유효한 일정으로 다음 버튼을 클릭하면 다음 페이지로 이동한다', async () => {
    const { user } = await setupScheduleStep();
    await user.click(screen.getByRole('button', { name: '마감 시각' }));
    await selectDateTime(user, 10);

    await user.click(screen.getByRole('button', { name: '다음' }));

    expect(await screen.findByRole('heading', { name: '누가 과제를 제출하나요?' })).toBeVisible();
  });

  describe('마감 시각 설정', () => {
    test('마감 시각을 선택하면 선택한 날짜가 표시된다', async () => {
      const { user } = await setupScheduleStep();

      await user.click(screen.getByRole('button', { name: '마감 시각' }));
      await selectDateTime(user, 10);

      expect(screen.getByRole('button', { name: '마감 시각' })).toHaveTextContent(
        `${nextMonthDate(10).label} 15:30`,
      );
    });

    test('마감 시각을 선택한 상태에서 해당 버튼을 클릭하면 다시 마감 시각을 설정할 수 있다', async () => {
      const { user } = await setupScheduleStep();
      await user.click(screen.getByRole('button', { name: '마감 시각' }));
      await selectDateTime(user, 10);

      await user.click(screen.getByRole('button', { name: '마감 시각' }));
      await selectDateTime(user, 11);

      expect(screen.getByRole('button', { name: '마감 시각' })).toHaveTextContent(
        `${nextMonthDate(11).label} 15:30`,
      );
    });
  });

  describe('리마인드 설정', () => {
    test('리마인드 등록하기 버튼을 클릭하면 날짜 선택 화면이 열린다', async () => {
      const { user } = await setupScheduleStep();

      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));

      expect(screen.getByRole('dialog', { name: '리마인드 시각 설정' })).toBeVisible();
    });

    test('날짜와 시간을 선택하고 완료하면 리마인드 시각이 목록에 표시된다', async () => {
      const { user } = await setupScheduleStep();

      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 10);

      const reminders = within(screen.getByRole('list', { name: '등록한 리마인드 시각' }));
      expect(reminders.getByText(`${nextMonthDate(10).label} 15:30`)).toBeVisible();
    });

    test('서로 다른 시각의 리마인드를 여러 개 등록하면 등록한 시각이 모두 표시된다', async () => {
      const { user } = await setupScheduleStep();

      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 10);
      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 11);

      const reminders = within(screen.getByRole('list', { name: '등록한 리마인드 시각' }));
      expect(reminders.getByText(`${nextMonthDate(10).label} 15:30`)).toBeVisible();
      expect(reminders.getByText(`${nextMonthDate(11).label} 15:30`)).toBeVisible();
    });

    test('이미 등록한 시각을 다시 선택해도 리마인드가 중복으로 추가되지 않는다', async () => {
      const { user } = await setupScheduleStep();
      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 10);

      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 10);

      const reminders = within(screen.getByRole('list', { name: '등록한 리마인드 시각' }));
      expect(reminders.getAllByRole('listitem')).toHaveLength(1);
    });

    test('삭제 버튼을 클릭하면 해당 리마인드만 목록에서 제외된다', async () => {
      const { user } = await setupScheduleStep();
      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 10);
      await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
      await selectDateTime(user, 11);
      const reminders = within(screen.getByRole('list', { name: '등록한 리마인드 시각' }));

      await user.click(
        reminders.getByRole('button', { name: `${nextMonthDate(10).label} 15:30 리마인드 삭제` }),
      );

      expect(reminders.queryByText(`${nextMonthDate(10).label} 15:30`)).not.toBeInTheDocument();
      expect(reminders.getByText(`${nextMonthDate(11).label} 15:30`)).toBeVisible();
    });
  });
});
