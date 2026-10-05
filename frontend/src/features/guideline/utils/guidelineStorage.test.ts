import { GUIDELINE_STORAGE_KEY, readGuidelineState, writeGuidelineState } from './guidelineStorage';

beforeEach(() => localStorage.clear());

test('저장된 안내가 없으면 첫 페이지부터 표시한다', () => {
  expect(readGuidelineState(5)).toEqual({ page: 1, dismissed: false });
});

test('저장한 페이지와 다시 보지 않기 설정을 다시 읽는다', () => {
  writeGuidelineState({ page: 3, dismissed: false });

  expect(readGuidelineState(5)).toEqual({ page: 3, dismissed: false });
});

test.each(['{broken', 'null', '[]', 'true', '"text"'])(
  '손상된 저장값 %s는 처음 표시하는 상태로 복구한다',
  (value) => {
    localStorage.setItem(GUIDELINE_STORAGE_KEY, value);

    expect(readGuidelineState(5)).toEqual({ page: 1, dismissed: false });
  },
);

test.each([
  [
    { page: 9, dismissed: false },
    { page: 5, dismissed: false },
  ],
  [
    { page: -2, dismissed: false },
    { page: 1, dismissed: false },
  ],
  [
    { page: 2.5, dismissed: false },
    { page: 1, dismissed: false },
  ],
  [
    { page: '3', dismissed: 'true' },
    { page: 1, dismissed: false },
  ],
  [
    { page: 4, dismissed: true },
    { page: 4, dismissed: true },
  ],
])('저장값 %j를 유효한 안내 상태로 제한한다', (stored, expected) => {
  localStorage.setItem(GUIDELINE_STORAGE_KEY, JSON.stringify(stored));

  expect(readGuidelineState(5)).toEqual(expected);
});

test('브라우저가 저장소 읽기를 차단해도 안내를 표시할 수 있다', () => {
  jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
    throw new DOMException('Storage is blocked', 'SecurityError');
  });

  expect(readGuidelineState(5)).toEqual({ page: 1, dismissed: false });
});

test('브라우저가 저장소 쓰기를 차단해도 예외를 전파하지 않는다', () => {
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new DOMException('Storage is full', 'QuotaExceededError');
  });

  expect(() => writeGuidelineState({ page: 2, dismissed: false })).not.toThrow();
});
