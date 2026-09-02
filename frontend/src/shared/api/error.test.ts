import { getErrorResponse, ValidationError } from './error';
import { HTTPError, type NormalizedOptions } from 'ky';

function createHTTPError(data: unknown) {
  const response = new Response(null, { status: 400, statusText: 'Bad Request' });
  const request = new Request('https://chongchong.test/studies', { method: 'POST' });
  const error = new HTTPError(response, request, {} as NormalizedOptions);

  error.data = data;

  return error;
}

describe('getErrorResponse 테스트', () => {
  test('HTTPError의 응답 본문이 에러 응답 형식이면 그대로 반환한다', () => {
    const data = { code: 'INTERNAL_SERVER_ERROR', message: '서버에서 문제가 발생했어요' };

    expect(getErrorResponse(createHTTPError(data))).toEqual(data);
  });

  test('필드 에러 목록이 있으면 함께 반환한다', () => {
    const data = {
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않아요',
      errors: [{ code: 'REQUIRED', field: 'name', reason: '필수에요' }],
    };

    expect(getErrorResponse(createHTTPError(data))).toEqual(data);
  });

  test.each([
    { message: '입력값이 올바르지 않아요' },
    { code: 'INVALID_INPUT_VALUE' },
    { code: 400, message: '입력값이 올바르지 않아요' },
  ])('응답 본문이 에러 응답 형식이 아니면 null을 반환한다', (data) => {
    expect(getErrorResponse(createHTTPError(data))).toBeNull();
  });

  test.each([undefined, null, '서버에서 문제가 발생했어요'])(
    '응답 본문이 객체가 아니면 null을 반환한다',
    (data) => {
      expect(getErrorResponse(createHTTPError(data))).toBeNull();
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
