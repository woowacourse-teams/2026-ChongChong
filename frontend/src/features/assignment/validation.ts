import { ASSIGNMENT_CONTENT, ASSIGNMENT_TITLE } from './constants';

export function validateAssignmentContent(
  title: string,
  content: string,
  submissionMethod: string,
) {
  const errors: Record<string, string> = {};
  const fields = [
    { key: 'title', value: title, label: '제목', limit: ASSIGNMENT_TITLE.length },
    { key: 'content', value: content, label: '내용', limit: ASSIGNMENT_CONTENT.length },
    {
      key: 'submissionMethod',
      value: submissionMethod,
      label: '제출 방법',
      limit: ASSIGNMENT_CONTENT.length,
    },
  ];

  for (const { key, value, label, limit } of fields) {
    if (!value.trim()) errors[key] = `${label}을 입력해주세요.`;
    else if (value.length > limit) errors[key] = `${label}은 ${limit}자 이하로 입력해주세요.`;
  }
  return errors;
}

export function validateAssignmentSchedule(closeAt: string, remindAts: string[]) {
  const errors: Record<string, string> = {};
  if (!closeAt) errors.closeAt = '마감 시각을 선택해주세요.';
  else if (!Number.isFinite(Date.parse(closeAt)))
    errors.closeAt = '올바른 마감 시각을 선택해주세요.';
  if (remindAts.some((value) => !Number.isFinite(Date.parse(value)))) {
    errors.remindAts = '올바른 리마인드 시각을 선택해주세요.';
  }
  return errors;
}
