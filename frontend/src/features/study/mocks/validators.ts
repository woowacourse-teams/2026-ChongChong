import type { FieldError } from '../../../shared/api/error';

const STUDY_NAME_LIMIT_LENGTH = 15;
const STUDY_DESCRIPTION_LIMIT_LENGTH = 30;

interface StudyInput {
  name: string;
  description: string;
}

type FieldValidator = (value: string) => Omit<FieldError, 'field'> | null;

const studyValidator = {
  name: (value) => {
    if (value.trim().length === 0) {
      return { code: 'REQUIRED', reason: '스터디 이름은 필수입니다.' };
    }

    if (value.length > STUDY_NAME_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `스터디 이름은 ${STUDY_NAME_LIMIT_LENGTH}자 이하만 가능해요.`,
      };
    }
    return null;
  },

  description: (value) => {
    if (value.length > STUDY_DESCRIPTION_LIMIT_LENGTH) {
      return {
        code: 'MAX_LENGTH',
        reason: `설명은 ${STUDY_DESCRIPTION_LIMIT_LENGTH}자 이하만 가능해요.`,
      };
    }
    return null;
  },
} satisfies Record<keyof StudyInput, FieldValidator>;

const studyFields = Object.keys(studyValidator) as (keyof StudyInput)[];

export function validateStudy(input: Partial<StudyInput>): FieldError[] {
  return studyFields.flatMap((field) => {
    const error = studyValidator[field](input[field] ?? '');
    return error ? [{ field, ...error }] : [];
  });
}
