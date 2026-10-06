import { act, renderHook } from '@testing-library/react';
import { useNavigate } from 'react-router';
import { createWrapper } from '../../../test/render';
import { GUIDELINE_STORAGE_KEY } from '../utils/guidelineStorage';
import { useGuidelineController } from './useGuidelineController';

beforeEach(() => localStorage.clear());
afterEach(() => localStorage.clear());

test('수동으로 연 숨김 가이드도 다시 보지 않기를 누르면 닫고 페이지를 초기화한다', () => {
  localStorage.setItem(GUIDELINE_STORAGE_KEY, JSON.stringify({ page: 3, dismissed: true }));
  const { result } = renderHook(useGuidelineController, { wrapper: createWrapper() });

  expect(result.current.isOpen).toBe(false);
  act(() => result.current.openHelp());
  expect(result.current.isOpen).toBe(true);
  expect(result.current.entrySource).toBe('help_link');
  expect(result.current.page).toBe(3);
  expect(result.current.dismissed).toBe(true);

  act(() => result.current.dismiss());
  expect(result.current.isOpen).toBe(false);
  expect(result.current.page).toBe(1);
  expect(JSON.parse(localStorage.getItem(GUIDELINE_STORAGE_KEY)!)).toEqual({
    page: 1,
    dismissed: true,
  });
});

test.each([false, true])(
  '같은 목록 경로에 새로 진입하면 숨김 설정(%s)에 따라 열림을 초기화한다',
  (dismissed) => {
    localStorage.setItem(GUIDELINE_STORAGE_KEY, JSON.stringify({ page: 3, dismissed }));
    const { result } = renderHook(
      () => ({ controller: useGuidelineController(), navigate: useNavigate() }),
      { wrapper: createWrapper({ initialEntries: ['/studies'] }) },
    );

    act(() => result.current.controller.openHelp());
    act(() => result.current.controller.close());
    expect(result.current.controller.isOpen).toBe(false);

    act(() => result.current.navigate('/studies'));
    expect(result.current.controller.isOpen).toBe(!dismissed);
    expect(result.current.controller.entrySource).toBe('auto');
    expect(result.current.controller.page).toBe(3);

    // 이전 location.key로 돌아가도 과거 방문에서 닫은 상태를 재사용하지 않는다.
    act(() => result.current.controller.openHelp());
    act(() => result.current.controller.close());
    act(() => result.current.navigate(-1));
    expect(result.current.controller.isOpen).toBe(!dismissed);
    expect(result.current.controller.entrySource).toBe('auto');
  },
);
