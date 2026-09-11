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

function isFieldError(data: unknown): data is FieldError {
  if (typeof data !== 'object' || data === null) return false;

  const { code, field, reason } = data as Record<string, unknown>;

  return typeof code === 'string' && typeof field === 'string' && typeof reason === 'string';
}

export const ErrorResponse = {
  is(data: unknown): data is ErrorResponse {
    if (typeof data !== 'object' || data === null) return false;

    const { code, message, errors } = data as Record<string, unknown>;

    return (
      typeof code === 'string' &&
      typeof message === 'string' &&
      (errors === undefined || (Array.isArray(errors) && errors.every(isFieldError)))
    );
  },

  from(error: unknown): ErrorResponse | null {
    if (!(error instanceof HTTPError)) return null;

    return this.is(error.data) ? error.data : null;
  },
};

export class ValidationError extends Error {
  public readonly fieldErrors: Record<string, string>;
  private readonly errors: FieldError[] | undefined;

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
    this.errors = errors;
    this.fieldErrors = this.toFieldErrors() ?? {};
  }

  private toFieldErrors() {
    return this.errors?.reduce<Record<string, string>>((fieldErrors, { field, reason }) => {
      if (!(field in fieldErrors)) fieldErrors[field] = reason;

      return fieldErrors;
    }, {});
  }
}

interface ApiErrorParams {
  code: string;
  message: string;
  status: number;
  options?: ErrorOptions;
}

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor({ code, message, status, options }: ApiErrorParams) {
    super(message, options);

    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}
