import { renderHook } from '@testing-library/react';
import { PropsWithChildren } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router';
import useIntegerParams from '../useIntegerParams';

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

function renderUseIntegerParams<K extends string>({
  params,
  path = '/studies/:studyId',
  entry,
}: {
  params: readonly K[];
  path?: string;
  entry: string;
}) {
  return renderHook(() => useIntegerParams(params), { wrapper: createWrapper({ path, entry }) });
}

describe('유효한 경로 파라미터', () => {
  test('요청한 경로 파라미터를 숫자로 변환해 반환한다', () => {
    const { result } = renderUseIntegerParams({ params: ['studyId'], entry: '/studies/1' });

    expect(result.current).toEqual({ studyId: 1 });
  });

  test('여러 경로 파라미터를 각각 숫자로 변환해 반환한다', () => {
    const { result } = renderUseIntegerParams({
      params: ['studyId', 'assignmentId'],
      path: '/studies/:studyId/assignments/:assignmentId',
      entry: '/studies/123/assignments/456',
    });

    expect(result.current).toEqual({ studyId: 123, assignmentId: 456 });
  });

  test('요청하지 않은 파라미터는 검증하거나 반환하지 않는다', () => {
    const { result } = renderUseIntegerParams({
      params: ['studyId'],
      path: '/studies/:studyId/assignments/:assignmentId',
      entry: '/studies/1/assignments/abc',
    });

    expect(result.current).toEqual({ studyId: 1 });
  });
});

describe('유효하지 않은 경로 파라미터', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('파라미터가 숫자가 아니면 에러가 발생한다', () => {
    expect(() => renderUseIntegerParams({ params: ['studyId'], entry: '/studies/abc' })).toThrow(
      "현재 'studyId'은 유효하지 않은 값을 가진 경로 파라미터 입니다",
    );
  });

  test('요청한 파라미터 중 정수가 아닌 값이 있으면 해당 파라미터의 에러가 발생한다', () => {
    expect(() =>
      renderUseIntegerParams({
        params: ['studyId', 'assignmentId'],
        path: '/studies/:studyId/assignments/:assignmentId',
        entry: '/studies/1/assignments/1.5',
      }),
    ).toThrow("현재 'assignmentId'은 유효하지 않은 값을 가진 경로 파라미터 입니다");
  });

  test('요청한 파라미터가 없으면 해당 파라미터의 에러가 발생한다', () => {
    expect(() =>
      renderUseIntegerParams({ params: ['studyId', 'assignmentId'], entry: '/studies/1' }),
    ).toThrow("현재 'assignmentId'은 유효하지 않은 값을 가진 경로 파라미터 입니다");
  });
});
