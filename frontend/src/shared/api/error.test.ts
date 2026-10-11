import { ApiError, ErrorResponse, handleError, ValidationError } from './error';
import { HTTPError, type NormalizedOptions } from 'ky';

function createHTTPError(data: unknown, status = 400) {
  const response = new Response(null, { status });
  const request = new Request('https://chongchong.test/studies', { method: 'POST' });
  const error = new HTTPError(response, request, {} as NormalizedOptions);

  error.data = data;

  return error;
}

describe('ErrorResponse.from 테스트', () => {
  test('HTTPError의 응답 본문이 에러 응답 형식이면 그대로 반환한다', () => {
    const data = { code: 'INTERNAL_SERVER_ERROR', message: '서버에서 문제가 발생했어요' };

    expect(ErrorResponse.from(createHTTPError(data))).toEqual(data);
  });

  test('필드 에러 목록이 있으면 함께 반환한다', () => {
    const data = {
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [{ code: 'REQUIRED', field: 'name', reason: '필수에요' }],
    };

    expect(ErrorResponse.from(createHTTPError(data))).toEqual(data);
  });

  test.each([{ errors: undefined }, { errors: [] }])(
    '필드 에러 목록이 $errors이면 응답을 반환한다',
    ({ errors }) => {
      const data = { code: 'INVALID_INPUT_VALUE', message: '입력값이 올바르지 않아요', errors };

      expect(ErrorResponse.from(createHTTPError(data))).toEqual(data);
    },
  );

  test.each([null, 123, 'invalid', {}])(
    '필드 에러 목록이 배열이 아니면 null을 반환한다: %p',
    (errors) => {
      const data = { code: 'INVALID_INPUT_VALUE', message: '입력값이 올바르지 않아요', errors };

      expect(ErrorResponse.from(createHTTPError(data))).toBeNull();
    },
  );

  test.each([
    null,
    undefined,
    123,
    'invalid',
    {},
    { field: 'name', reason: '필수에요' },
    { code: 'REQUIRED', reason: '필수에요' },
    { code: 'REQUIRED', field: 'name' },
    { code: 123, field: 'name', reason: '필수에요' },
    { code: 'REQUIRED', field: 123, reason: '필수에요' },
    { code: 'REQUIRED', field: 'name', reason: 123 },
  ])('필드 에러 항목의 형식이 잘못되면 null을 반환한다: %p', (fieldError) => {
    const data = {
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [fieldError],
    };

    expect(ErrorResponse.from(createHTTPError(data))).toBeNull();
  });

  test('정상 항목 뒤에 잘못된 필드 에러가 있어도 null을 반환한다', () => {
    const data = {
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [{ code: 'REQUIRED', field: 'name', reason: '필수에요' }, null],
    };

    expect(ErrorResponse.from(createHTTPError(data))).toBeNull();
  });

  test.each([
    { message: '입력값이 올바르지 않아요' },
    { code: 'INVALID_INPUT_VALUE' },
    { code: 400, message: '입력값이 올바르지 않아요' },
  ])('응답 본문이 에러 응답 형식이 아니면 null을 반환한다', (data) => {
    expect(ErrorResponse.from(createHTTPError(data))).toBeNull();
  });

  test.each([undefined, null, '서버에서 문제가 발생했어요'])(
    '응답 본문이 객체가 아니면 null을 반환한다',
    (data) => {
      expect(ErrorResponse.from(createHTTPError(data))).toBeNull();
    },
  );
});

describe('ValidationError 테스트', () => {
  test('필드 에러 목록을 필드명과 이유를 객체로 변환한다', () => {
    const error = new ValidationError({
      message: 'some error',
      errors: [
        { code: 'INVALID_SOME', field: 'name', reason: '유효하지 않아요' },
        { code: 'REQUIRED', field: 'age', reason: '필수에요' },
      ],
    });
    expect(error.fieldErrors).toEqual({ name: '유효하지 않아요', age: '필수에요' });
  });

  test('같은 필드의 에러가 여러 개면 첫 번째 에러만 적용한다', () => {
    const error = new ValidationError({
      message: 'some error',
      errors: [
        { code: 'INVALID_SOME', field: 'name', reason: '유효하지 않아요' },
        { code: 'REQUIRED', field: 'age', reason: '필수에요' },
        { code: 'INVALID_ZERO', field: 'age', reason: '0이면 안됩니다!' },
      ],
    });
    expect(error.fieldErrors).toEqual({ name: '유효하지 않아요', age: '필수에요' });
  });

  test('필드 에러 목록이 비어있으면 빈 객체를 반환한다', () => {
    const error = new ValidationError({
      message: 'some error',
      errors: [],
    });
    expect(error.fieldErrors).toEqual({});
  });
});

describe('ValidationError 테스트', () => {
  test('입력값 에러를 필드별 메시지와 원본 원인을 가진 ValidationError로 변환한다', () => {
    const cause = createHTTPError({
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [
        { code: 'REQUIRED', field: 'name', reason: '이름은 필수에요' },
        { code: 'SIZE', field: 'description', reason: '설명이 너무 길어요' },
      ],
    });

    const mapped = ValidationError.tryFrom(cause);

    expect(mapped).toBeInstanceOf(ValidationError);
    expect(mapped).toMatchObject({
      message: '입력값이 올바르지 않아요',
      fieldErrors: { name: '이름은 필수에요', description: '설명이 너무 길어요' },
    });
  });

  test('필드 에러 목록이 비어있으면 빈 fieldErrors로 변환한다', () => {
    const cause = createHTTPError({
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [],
    });
    const mapped = ValidationError.tryFrom(cause);

    expect(mapped).toBeInstanceOf(ValidationError);
    expect(mapped?.fieldErrors).toEqual({});
  });

  test('필드 에러 목록이 있어도 입력값 에러 코드가 아니면 null을 반환한다', () => {
    const cause = createHTTPError({
      code: 'INVALID_REQUEST_PARAMETER',
      message: '요청 파라미터가 올바르지 않아요',
      errors: [{ code: 'TYPE_MISMATCH', field: 'studyId', reason: '숫자여야 해요' }],
    });

    expect(ValidationError.tryFrom(cause)).toBeNull();
  });

  test('HTTPError 가 아닐 경우 null을 반환한다', () => {
    expect(ValidationError.tryFrom('알 수 없는 에러 형식')).toBeNull();
  });

  test('HTTPError지만 본문이 에러 응답 형식이 아니면 null을 반환한다', () => {
    const error = { message: '에러 코드가 없는 응답이에요' };
    expect(ValidationError.tryFrom(createHTTPError(error))).toBeNull();
  });
});

describe('ApiError 테스트', () => {
  test('에러의 코드, 메시지를 보존한 ApiError 클래스를 반환한다', () => {
    const cause = createHTTPError({
      code: 'FORBIDDEN',
      message: '접근 권한이 없어요',
    });
    const mapped = ApiError.tryFrom(cause);

    expect(mapped).toBeInstanceOf(ApiError);
    expect(mapped).toMatchObject({
      code: 'FORBIDDEN',
      message: '접근 권한이 없어요',
    });
  });

  test('HTTPError 가 아닐 경우 null을 반환한다', () => {
    expect(ApiError.tryFrom('알 수 없는 에러 형식')).toBeNull();
  });

  test('HTTPError지만 본문이 에러 응답 형식이 아니면 null을 반환한다', () => {
    const error = { message: '에러 코드가 없는 응답이에요' };
    expect(ApiError.tryFrom(createHTTPError(error))).toBeNull();
  });
});

describe('handleError 테스트', () => {
  test('여러 mapper에 일치하면 먼저 지정한 에러를 반환한다', () => {
    const cause = createHTTPError({
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [{ code: 'REQUIRED', field: 'name', reason: '이름은 필수에요' }],
    });

    const mapped = handleError(cause, {
      mappers: [ValidationError, ApiError],
      fallback: new Error('요청에 실패했어요', { cause }),
    });

    expect(mapped).toBeInstanceOf(ValidationError);
    expect(mapped).toMatchObject({ fieldErrors: { name: '이름은 필수에요' } });
    expect(mapped.cause).toBe(cause);
  });

  test('순차적으로 일치한 mapper가 먼저 반환된다', () => {
    const cause = createHTTPError({ code: 'FORBIDDEN', message: '접근 권한이 없어요' }, 403);

    const mapped = handleError(cause, {
      mappers: [ValidationError, ApiError],
      fallback: new Error('요청에 실패했어요', { cause }),
    });

    expect(mapped).toBeInstanceOf(ApiError);
    expect(mapped).toMatchObject({ code: 'FORBIDDEN', message: '접근 권한이 없어요', status: 403 });
    expect(mapped.cause).toBe(cause);
  });

  test('일치하는 mapper가 없으면 전달한 fallback 인스턴스를 반환한다', () => {
    const cause = new TypeError('Failed to fetch');
    const fallback = new Error('스터디 생성에 실패했어요', { cause });

    const mapped = handleError(cause, { mappers: [ValidationError, ApiError], fallback });

    expect(mapped).toBe(fallback);
  });

  test('mapper 목록이 비어 있으면 fallback이 반환된다', () => {
    const cause = createHTTPError({ code: 'FORBIDDEN', message: '접근 권한이 없어요' }, 403);
    const fallback = new ApiError({
      code: 'REQUEST_FAILED',
      message: '요청에 실패했어요',
      status: 403,
      options: { cause },
    });

    expect(handleError(cause, { mappers: [], fallback })).toBe(fallback);
  });
});
