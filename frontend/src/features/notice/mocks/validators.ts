import type { FieldError } from '../../../shared/api/error';

const NOTICE_TITLE_LIMIT_LENGTH = 100;
const NOTICE_CONTENT_LIMIT_LENGTH = 10000;

interface NoticeInput {
  title: string;
  content: string;
}

type FieldValidator = (value: string) => Omit<FieldError, 'field'> | null;

const noticeValidator = {
  title: (value) => {
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '공지 제목은 필수입니다.' };
    }

    if (value.length > NOTICE_TITLE_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `공지 제목은 ${NOTICE_TITLE_LIMIT_LENGTH}자 이하만 가능해요.`,
      };
    }

    return null;
  },
  content: (value) => {
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '공지 내용은 필수입니다.' };
    }

    if (value.length > NOTICE_CONTENT_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `공지 내용은 ${NOTICE_CONTENT_LIMIT_LENGTH.toLocaleString()}자 이하만 가능해요.`,
      };
    }

    return null;
  },
} satisfies Record<keyof NoticeInput, FieldValidator>;

export function validateNotice(input: Partial<NoticeInput>, partial = false): FieldError[] {
  return (Object.keys(noticeValidator) as (keyof NoticeInput)[]).flatMap((field) => {
    if (partial && input[field] === undefined) return [];

    const error = noticeValidator[field](input[field] ?? '');
    return error ? [{ field, ...error }] : [];
  });
}
