import { screen, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

export async function completeContentStep(user: UserEvent) {
  await user.type(screen.getByRole('textbox', { name: '제목' }), '리액트 상태 관리');
  await user.type(
    screen.getByRole('textbox', { name: '내용' }),
    'Context 사용 경험을 정리해주세요.',
  );
  await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '블로그 링크 제출');
  await user.click(screen.getByRole('button', { name: '다음' }));
}

export async function completeScheduleStep(user: UserEvent) {
  await user.click(screen.getByRole('button', { name: '마감 시각' }));
  await selectDateTime(user, 10);
  await user.click(screen.getByRole('button', { name: '다음' }));
}

export async function completeSubmittersStep(user: UserEvent) {
  await screen.findByRole('checkbox', { name: '전체 선택' });
  await user.click(screen.getByRole('button', { name: '다음' }));
}

export function nextMonthDate(day: number) {
  const now = new Date();
  const date = new Date(now.getFullYear(), now.getMonth() + 1, day);
  return {
    month: `${date.getFullYear()}년 ${date.getMonth() + 1}월`,
    label: `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${day}일`,
  };
}

export async function selectDateTime(user: UserEvent, day: number) {
  const dialog = within(screen.getByRole('dialog'));
  const date = nextMonthDate(day);
  if (!dialog.queryByText(date.month)) {
    await user.click(dialog.getByRole('button', { name: '다음 달' }));
  }
  await user.click(dialog.getByRole('button', { name: date.label }));
  await user.click(dialog.getByRole('button', { name: '다음' }));
  await user.click(dialog.getByRole('button', { name: '오후' }));
  await user.click(dialog.getByRole('button', { name: '3시' }));
  await user.click(dialog.getByRole('button', { name: '30분' }));
  await user.click(dialog.getByRole('button', { name: '완료' }));
}
