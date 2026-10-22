import { getFileContentType, validateAttachments } from './validation';

test('객체의 상속 속성 이름을 이미지 확장자로 허용하지 않는다', () => {
  const file = new File(['data'], 'payload.constructor');
  expect(validateAttachments([file], 0, 'image')).toContain('JPEG, PNG, WebP');
  expect(getFileContentType(file)).toBe('application/octet-stream');
});
