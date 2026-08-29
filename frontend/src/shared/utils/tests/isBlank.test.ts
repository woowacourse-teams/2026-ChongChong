import isBlank from '../isBlank';

describe('isBlank 테스트', () => {
  test('빈 문자열이면 true를 반환한다', () => {
    expect(isBlank('')).toBe(true);
  });

  test.each([' ', '   ', '\t', '\n'])('공백 문자로만 이루어져 있으면 true를 반환한다', (value) => {
    expect(isBlank(value)).toBe(true);
  });

  test('공백이 아닌 문자가 있으면 false를 반환한다', () => {
    expect(isBlank('치킨')).toBe(false);
  });

  test.each(['  치킨  ', '  치킨', '치킨  '])(
    '앞뒤에 공백이 있어도 내용이 있으면 false를 반환한다',
    (value) => {
      expect(isBlank(value)).toBe(false);
    },
  );
});
