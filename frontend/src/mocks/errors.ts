import { HttpResponse } from 'msw';
import { FIELD_ERROR_CODE, type FieldError } from '../shared/api/error';

export function invalidInputResponse(errors: FieldError[]) {
  return HttpResponse.json(
    {
      code: FIELD_ERROR_CODE,
      message: '입력값이 올바르지 않습니다.',
      errors,
    },
    { status: 400 },
  );
}
