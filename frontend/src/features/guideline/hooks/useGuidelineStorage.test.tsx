import { act, renderHook } from '@testing-library/react';
import { GUIDELINE_STORAGE_KEY } from '../utils/guidelineStorage';
import { useGuidelineStorage } from './useGuidelineStorage';

beforeEach(() => localStorage.clear());

test('이전 방문의 페이지를 복원한다', () => {
  localStorage.setItem(GUIDELINE_STORAGE_KEY, JSON.stringify({ page: 3, dismissed: false }));

  const { result } = renderHook(() => useGuidelineStorage(5));

  expect(result.current.page).toBe(3);
  expect(result.current.dismissed).toBe(false);
});

test('페이지 이동을 즉시 저장하여 화면을 바로 떠나도 진행 상황을 유지한다', () => {
  const firstVisit = renderHook(() => useGuidelineStorage(5));

  act(() => {
    firstVisit.result.current.setPage(4);
    expect(JSON.parse(localStorage.getItem(GUIDELINE_STORAGE_KEY)!)).toEqual({
      page: 4,
      dismissed: false,
    });
  });
  firstVisit.unmount();

  const nextVisit = renderHook(() => useGuidelineStorage(5));
  expect(nextVisit.result.current.page).toBe(4);
});

test('다시 보지 않기를 선택하면 즉시 저장하고 페이지를 처음으로 되돌린다', () => {
  localStorage.setItem(GUIDELINE_STORAGE_KEY, JSON.stringify({ page: 4, dismissed: false }));
  const { result } = renderHook(() => useGuidelineStorage(5));

  act(() => {
    result.current.dismiss();
    expect(JSON.parse(localStorage.getItem(GUIDELINE_STORAGE_KEY)!)).toEqual({
      page: 1,
      dismissed: true,
    });
  });

  expect(result.current.page).toBe(1);
  expect(result.current.dismissed).toBe(true);
});

test.each([
  [-2, 1],
  [8, 5],
  [2.5, 1],
])('요청 페이지 %s를 %s로 제한하여 저장한다', (requestedPage, expectedPage) => {
  const { result } = renderHook(() => useGuidelineStorage(5));

  act(() => result.current.setPage(requestedPage));

  expect(result.current.page).toBe(expectedPage);
  expect(JSON.parse(localStorage.getItem(GUIDELINE_STORAGE_KEY)!).page).toBe(expectedPage);
});

test('저장소를 사용할 수 없어도 현재 방문 중 이동과 다시 보지 않기가 동작한다', () => {
  jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
    throw new DOMException('Storage is blocked', 'SecurityError');
  });
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new DOMException('Storage is blocked', 'SecurityError');
  });
  const { result } = renderHook(() => useGuidelineStorage(5));

  act(() => result.current.setPage(3));
  expect(result.current.page).toBe(3);

  act(() => result.current.dismiss());
  expect(result.current.page).toBe(1);
  expect(result.current.dismissed).toBe(true);
});
