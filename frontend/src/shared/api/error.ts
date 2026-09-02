import { HTTPError } from 'ky';

export const FIELD_ERROR_CODE = 'INVALID_INPUT_VALUE';

export interface FieldError {
  code: string;
  field: string;
  reason: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
  errors?: FieldError[];
}

function isErrorResponse(data: unknown): data is ErrorResponse {
  return (
    typeof data === 'object' &&
    data !== null &&
    typeof (data as ErrorResponse).code === 'string' &&
    typeof (data as ErrorResponse).message === 'string'
  );
}

export function getErrorResponse(error: unknown): ErrorResponse | null {
  if (!(error instanceof HTTPError)) return null;

  return isErrorResponse(error.data) ? error.data : null;
}

export class ValidationError extends Error {
  readonly fieldErrors: Record<string, string>;

  constructor({
    message,
    errors,
    options,
  }: {
    message: string;
    errors?: FieldError[];
    options?: ErrorOptions;
  }) {
    super(message, options);
    this.name = 'ValidationError';
    this.fieldErrors = toFieldErrors(errors);
  }
}

function toFieldErrors(errors: FieldError[] = []) {
  return errors.reduce<Record<string, string>>((fieldErrors, { field, reason }) => {
    if (!(field in fieldErrors)) fieldErrors[field] = reason;

    return fieldErrors;
  }, {});
}

const FALLBACK_MESSAGE: Record<number, string> = {
  400: '요청이 올바르지 않습니다.',
  403: '권한이 없습니다.',
  404: '대상을 찾을 수 없습니다.',
  409: '이미 처리된 요청입니다.',
};

// TODO: Toast UI로 교체합니다.
export function alertErrorResponse(error: unknown) {
  if (!(error instanceof HTTPError)) return;

  // 401은 무시합니다, client의 afterResponse에서 토큰 갱신/로그인 이동으로 처리합니다.
  if (error.response.status === 401) return;

  const errorResponse = getErrorResponse(error);

  if (errorResponse?.code === FIELD_ERROR_CODE) return;

  const message = errorResponse?.message ?? FALLBACK_MESSAGE[error.response.status];

  if (message) window.alert(message);
}
