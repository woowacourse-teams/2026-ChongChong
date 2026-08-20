const STUDY_NAME_LIMIT_LENGTH = 15;
const STUDY_DESCRIPTION_LIMIT_LENGTH = 30;

const ERROR_CODES = {
  required: 'REQUIRED_FIELD',
  length: 'INVALID_LENGTH',
} as const;

type ErrorCode = (typeof ERROR_CODES)[keyof typeof ERROR_CODES];

export interface FieldError {
  code: ErrorCode;
  field: string;
  reason: string;
}

export interface InvalidInputResponse {
  code: 'INVALID_INPUT_VALUE';
  message: string;
  errors: FieldError[];
}

interface StudyInput {
  name: string;
  description: string;
}

type FieldValidator = (value: string) => FieldError | null;

const studyValidator = {
  name: (value) => {
    if (value.trim().length === 0) {
      return { code: ERROR_CODES.required, field: 'name', reason: '스터디 이름은 필수입니다.' };
    }

    if (value.length > STUDY_NAME_LIMIT_LENGTH) {
      return {
        code: ERROR_CODES.length,
        field: 'name',
        reason: `스터디 이름은 ${STUDY_NAME_LIMIT_LENGTH}자 이하로 입력할 수 있어요.`,
      };
    }
    return null;
  },

  description: (value) => {
    if (value.length > STUDY_DESCRIPTION_LIMIT_LENGTH) {
      return {
        code: ERROR_CODES.length,
        field: 'description',
        reason: `설명은 ${STUDY_DESCRIPTION_LIMIT_LENGTH}자 이하로 입력할 수 있어요.`,
      };
    }
    return null;
  },
} satisfies Record<keyof StudyInput, FieldValidator>;

export function validateStudy(input: Partial<StudyInput>): InvalidInputResponse | null {
  const errors = Object.entries(studyValidator)
    .map(([field, validateFn]) => validateFn(input[field as keyof StudyInput] ?? ''))
    .filter((error) => error !== null);

  if (errors.length === 0) return null;

  return {
    code: 'INVALID_INPUT_VALUE',
    message: '입력값이 올바르지 않습니다.',
    errors,
  };
}
