import { HttpResponse } from 'msw';
import { type FieldError } from '../shared/api/error';

export function invalidInputResponse(errors: FieldError[]) {
  return HttpResponse.json(
    {
      code: 'INVALID_INPUT_VALUE',
      message: '입력값이 올바르지 않습니다.',
      errors,
    },
    { status: 400 },
  );
}
