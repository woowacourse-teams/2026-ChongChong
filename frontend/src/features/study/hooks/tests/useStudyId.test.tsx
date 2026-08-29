import { renderHook } from '@testing-library/react';
import { PropsWithChildren } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router';
import useStudyId from '../useStudyId';

function createWrapper({ path, entry }: { path: string; entry: string }) {
  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path={path} element={children} />
        </Routes>
      </MemoryRouter>
    );
  };
}

function renderUseStudyId({ path = '/studies/:studyId', entry }: { path?: string; entry: string }) {
  return renderHook(() => useStudyId(), { wrapper: createWrapper({ path, entry }) });
}

describe('유효한 studyId', () => {
  test('경로의 studyId를 숫자로 변환해 반환한다', () => {
    const { result } = renderUseStudyId({ entry: '/studies/1' });

    expect(result.current).toEqual({ studyId: 1 });
  });

  test('여러 자리 수의 studyId도 숫자로 변환해 반환한다', () => {
    const { result } = renderUseStudyId({ entry: '/studies/123' });

    expect(result.current).toEqual({ studyId: 123 });
  });
});

describe('유효하지 않은 studyId', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('studyId가 숫자가 아니면 에러가 발생한다', () => {
    expect(() => renderUseStudyId({ entry: '/studies/abc' })).toThrow(
      "'abc'는 유효하지 않은 studyId 입니다.",
    );
  });

  test('studyId가 정수가 아니면 에러가 발생한다', () => {
    expect(() => renderUseStudyId({ entry: '/studies/1.5' })).toThrow(
      "'1.5'는 유효하지 않은 studyId 입니다.",
    );
  });
});
