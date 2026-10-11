import { HTTPError } from 'ky';

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
  private static readonly errorCode = 'INVALID_INPUT_VALUE';

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

  static tryFrom(error: unknown) {
    const errorResponse = ErrorResponse.from(error);

    if (!errorResponse || !this.is(errorResponse)) {
      return null;
    }

    return this.from(error, errorResponse!);
  }

  private static is(errorResponse: ErrorResponse): boolean {
    if (errorResponse?.code === this.errorCode) return true;
    return false;
  }

  private static from(error: unknown, errorResponse: ErrorResponse) {
    return new ValidationError({
      message: errorResponse.message,
      errors: errorResponse.errors,
      options: {
        cause: error,
      },
    });
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

  static tryFrom(error: unknown) {
    const errorResponse = ErrorResponse.from(error);

    if (!errorResponse || !this.is(error, errorResponse)) {
      return null;
    }

    return this.from(error, errorResponse!);
  }

  private static is(error: unknown, errorResponse: ErrorResponse): error is HTTPError {
    if (error instanceof HTTPError && errorResponse) return true;
    return false;
  }

  private static from(error: HTTPError, errorResponse: ErrorResponse) {
    return new ApiError({
      code: errorResponse.code,
      message: errorResponse.message,
      status: error.response.status,
      options: { cause: error },
    });
  }
}

interface Mapper {
  tryFrom(error: unknown): Error | null;
}

export function handleError(
  error: unknown,
  { mappers, fallback }: { mappers: Mapper[]; fallback: Error },
): Error {
  for (const mapper of mappers) {
    const mappedError = mapper.tryFrom(error);
    if (mappedError) {
      return mappedError;
    }
  }

  return fallback;
}
