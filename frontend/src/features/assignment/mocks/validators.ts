import type { FieldError } from '../../../shared/api/error';

const ASSIGNMENT_TITLE_LIMIT_LENGTH = 20;
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
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '제출 방법은 필수입니다.' };
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

export function validateAssignment(input: Partial<AssignmentInput>): FieldError[] {
  return assignmentFields.flatMap((field) => {
    const error = assignmentValidator[field](input[field] ?? '');
    return error ? [{ field, ...error }] : [];
  });
}
