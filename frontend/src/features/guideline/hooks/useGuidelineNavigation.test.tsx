import { act, renderHook } from '@testing-library/react';
import { useState } from 'react';
import { useGuidelineNavigation } from './useGuidelineNavigation';

function useNavigation(initialPage: number) {
  const [page, setPage] = useState(initialPage);
  return { page, ...useGuidelineNavigation(page, 5, setPage) };
}

test('첫 페이지에서 이전으로 이동하지 않는다', () => {
  const { result } = renderHook(() => useNavigation(1));

  expect(result.current.hasPrevious).toBe(false);
  expect(result.current.hasNext).toBe(true);
  act(() => result.current.goPrevious());
  expect(result.current.page).toBe(1);
});

test('마지막 페이지에서 다음으로 이동하지 않는다', () => {
  const { result } = renderHook(() => useNavigation(5));

  expect(result.current.hasPrevious).toBe(true);
  expect(result.current.hasNext).toBe(false);
  act(() => result.current.goNext());
  expect(result.current.page).toBe(5);
});

test('다음과 이전은 한 페이지씩 이동한다', () => {
  const { result } = renderHook(() => useNavigation(3));

  act(() => result.current.goNext());
  expect(result.current.page).toBe(4);

  act(() => result.current.goPrevious());
  expect(result.current.page).toBe(3);
});
