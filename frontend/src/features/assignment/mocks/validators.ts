import type { FieldError } from '../../../shared/api/error';

const ASSIGNMENT_TITLE_LIMIT_LENGTH = 100;
const ASSIGNMENT_CONTENT_LIMIT_LENGTH = 10000;

interface AssignmentInput {
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
}

type FieldValidator = (value: string) => Omit<FieldError, 'field'> | null;

const assignmentValidator = {
  title: (value) => {
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '과제 제목은 필수입니다.' };
    }

    if (value.length > ASSIGNMENT_TITLE_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `과제 제목은 ${ASSIGNMENT_TITLE_LIMIT_LENGTH}자 이하만 가능해요.`,
      };
    }
    return null;
  },

  content: (value) => {
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '과제 내용은 필수입니다.' };
    }

    if (value.length > ASSIGNMENT_CONTENT_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `과제 내용은 ${ASSIGNMENT_CONTENT_LIMIT_LENGTH.toLocaleString()}자 이하만 가능해요.`,
      };
    }
    return null;
  },

  submissionMethod: (value) => {
    if (value.length > ASSIGNMENT_CONTENT_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `제출 방법은 ${ASSIGNMENT_CONTENT_LIMIT_LENGTH.toLocaleString()}자 이하만 가능해요.`,
      };
    }
    return null;
  },

  closeAt: (value) => {
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '마감 시각은 필수입니다.' };
    }

    const closeAt = new Date(value);
    if (Number.isNaN(closeAt.getTime())) {
      return { code: 'INVALID', reason: '올바른 시각 형식이 아니에요.' };
    }

    if (closeAt.getTime() <= Date.now()) {
      return { code: 'INVALID', reason: '마감 시각은 현재 이후만 가능해요.' };
    }
    return null;
  },
} satisfies Record<keyof AssignmentInput, FieldValidator>;

const assignmentFields = Object.keys(assignmentValidator) as (keyof AssignmentInput)[];

export interface AssignmentWriteInput extends Partial<AssignmentInput> {
  submitterIds?: number[] | null;
  submissionTarget?: 'MEMBERS_ONLY' | 'MEMBERS_AND_LEADER' | null;
  submissionVisibility?: 'LEADER_ONLY' | 'ALL_STUDY_MEMBERS' | null;
  remindAts?: string[] | null;
  attachmentFileIds?: number[] | null;
}

export function validateAssignment(input: AssignmentWriteInput, partial = false): FieldError[] {
  const errors: FieldError[] = assignmentFields.flatMap((field) => {
    if (partial && input[field] == null) return [];
    const value = input[field] ?? '';
    const error =
      typeof value === 'string'
        ? assignmentValidator[field](value)
        : { code: 'INVALID', reason: '문자열을 입력해주세요.' };
    return error ? [{ field, ...error }] : [];
  });
  if (
    (!partial || input.submissionVisibility != null) &&
    !['LEADER_ONLY', 'ALL_STUDY_MEMBERS'].includes(input.submissionVisibility ?? '')
  ) {
    errors.push({
      field: 'submissionVisibility',
      code: 'INVALID',
      reason: '공개 범위를 선택해주세요.',
    });
  }
  if (
    input.submitterIds == null &&
    (!partial || input.submissionTarget != null) &&
    !['MEMBERS_ONLY', 'MEMBERS_AND_LEADER'].includes(input.submissionTarget ?? '')
  ) {
    errors.push({ field: 'submitterIds', code: 'REQUIRED', reason: '제출할 멤버를 선택해주세요.' });
  }
  if (input.remindAts != null) {
    if (!Array.isArray(input.remindAts)) {
      errors.push({
        field: 'remindAts',
        code: 'INVALID',
        reason: '리마인드 시각 목록을 입력해주세요.',
      });
    } else {
      input.remindAts.forEach((value, index) => {
        if (
          typeof value !== 'string' ||
          !Number.isFinite(Date.parse(value)) ||
          Date.parse(value) <= Date.now()
        ) {
          errors.push({
            field: `remindAts[${index}]`,
            code: 'INVALID',
            reason: '리마인드 시각은 현재 이후만 가능해요.',
          });
        }
      });
    }
  }
  return errors;
}
