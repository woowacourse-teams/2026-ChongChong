import { act, renderHook } from '@testing-library/react';
import { GUIDELINE_STORAGE_KEY } from '../utils/guidelineStorage';
import { useGuidelineStorage } from './useGuidelineStorage';
import { useGuidelineVisibility } from './useGuidelineVisibility';

beforeEach(() => localStorage.clear());

function useGuidelineVisit() {
  const storage = useGuidelineStorage(5);
  return { ...storage, ...useGuidelineVisibility(storage.dismissed) };
}

test('일반 닫기는 현재 페이지만 유지하고 다음 방문에는 다시 연다', () => {
  const firstVisit = renderHook(useGuidelineVisit);
  expect(firstVisit.result.current.isOpen).toBe(true);

  act(() => firstVisit.result.current.setPage(3));
  act(() => firstVisit.result.current.close());
  expect(firstVisit.result.current.isOpen).toBe(false);
  expect(JSON.parse(localStorage.getItem(GUIDELINE_STORAGE_KEY)!)).toEqual({
    page: 3,
    dismissed: false,
  });
  firstVisit.unmount();

  const nextVisit = renderHook(useGuidelineVisit);
  expect(nextVisit.result.current.isOpen).toBe(true);
  expect(nextVisit.result.current.page).toBe(3);
});

test('다시 보지 않기는 즉시 닫고 다음 방문에도 열지 않는다', () => {
  const firstVisit = renderHook(useGuidelineVisit);

  act(() => firstVisit.result.current.dismiss());
  expect(firstVisit.result.current.isOpen).toBe(false);
  firstVisit.unmount();

  const nextVisit = renderHook(useGuidelineVisit);
  expect(nextVisit.result.current.isOpen).toBe(false);
  expect(nextVisit.result.current.page).toBe(1);
});
