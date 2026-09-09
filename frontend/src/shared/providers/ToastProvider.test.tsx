import { act, cleanup, fireEvent, renderHook, screen } from '@testing-library/react';
import { ToastProvider, useToast } from './ToastProvider';

function advanceTime(milliseconds: number) {
  act(() => jest.advanceTimersByTime(milliseconds));
}

function getToastWrapper() {
  return screen.getByRole('toast').parentElement!;
}

function finishTransition(element: HTMLElement, propertyName = 'opacity') {
  const event = new Event('transitionend', { bubbles: true });
  Object.assign(event, { propertyName });
  fireEvent(element, event);
}

describe('ToastProvider', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    cleanup();
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  test('open을 호출하면 전달한 컴포넌트가 렌더링 된다', () => {
    const { result } = renderHook(() => useToast(), { wrapper: ToastProvider });

    act(() => {
      result.current.open(
        <div role="toast">
          <p>이삭 토스트</p>
        </div>,
      );
    });

    expect(screen.getByRole('toast')).toBeVisible();
    expect(screen.getByRole('toast')).toHaveTextContent('이삭 토스트');
  });

  test.each([
    { options: undefined, elapsed: 3000 },
    { options: { duration: 1500 }, elapsed: 1500 },
  ])(
    '전달한 지속시간 이후 퇴장이 시작하고 스타일 전환이 끝나면 UI를 제거한다',
    ({ options, elapsed }) => {
      const { result } = renderHook(() => useToast(), { wrapper: ToastProvider });

      act(() => {
        result.current.open(<div role="toast">저장했습니다</div>, options);
      });
      const toast = screen.getByRole('toast');
      const wrapper = getToastWrapper();

      advanceTime(elapsed - 1);
      expect(toast).toBeVisible();

      advanceTime(1);
      expect(toast).not.toBeVisible();
      // 퇴장 애니메이션이 끝나기 전까지는 DOM에는 존재합니다.
      expect(toast).toBeInTheDocument();

      finishTransition(wrapper);
      expect(screen.queryByRole('toast')).not.toBeInTheDocument();
    },
  );

  test('등장 애니메이션이 끝나도 토스트는 제거하지 않는다', () => {
    const { result } = renderHook(() => useToast(), { wrapper: ToastProvider });

    act(() => {
      result.current.open(<div role="toast">저장했습니다</div>);
    });

    finishTransition(getToastWrapper());

    expect(screen.getByRole('toast')).toBeVisible();
  });

  test('퇴장 중 opacity속성 외 전환으로는 토스트를 제거하지 않는다', () => {
    const { result } = renderHook(() => useToast(), { wrapper: ToastProvider });

    act(() => {
      result.current.open(<div role="toast">저장했습니다</div>);
    });
    const wrapper = getToastWrapper();
    advanceTime(3000);

    finishTransition(wrapper, 'transform');
    expect(screen.getByRole('toast')).toBeInTheDocument();

    finishTransition(wrapper);
    expect(screen.queryByRole('toast')).not.toBeInTheDocument();
  });

  test('새 토스트로 교체하면 이전 타이머를 취소하고 새 지연시간을 적용한다', () => {
    const { result } = renderHook(() => useToast(), { wrapper: ToastProvider });

    act(() => {
      result.current.open(<div role="toast">첫 번째 토스트</div>);
    });
    advanceTime(2000);

    act(() => {
      result.current.open(<div role="toast">두 번째 토스트</div>, { duration: 5000 });
    });
    expect(screen.queryByText('첫 번째 토스트')).not.toBeInTheDocument();
    expect(screen.getByRole('toast')).toHaveTextContent('두 번째 토스트');

    // 첫 번째 토스트의 만료 시각과 상관없이 새 토스트는 표시되어야 합니다.
    advanceTime(1000);
    expect(screen.getByRole('toast')).toBeVisible();

    advanceTime(3999);
    expect(screen.getByRole('toast')).toBeVisible();

    advanceTime(1);
    expect(screen.getByRole('toast')).not.toBeVisible();
    finishTransition(getToastWrapper());
    expect(screen.queryByRole('toast')).not.toBeInTheDocument();
  });

  test('퇴장 중에도 새 토스트를 열면 다시 표시한다', () => {
    const { result } = renderHook(() => useToast(), { wrapper: ToastProvider });

    act(() => {
      result.current.open(<div role="toast">첫 번째 토스트</div>);
    });
    advanceTime(3000);
    expect(screen.getByRole('toast')).not.toBeVisible();

    act(() => {
      result.current.open(<div role="toast">두 번째 토스트</div>);
    });
    expect(screen.queryByText('첫 번째 토스트')).not.toBeInTheDocument();
    expect(screen.getByRole('toast')).toBeVisible();

    finishTransition(getToastWrapper());
    expect(screen.getByRole('toast')).toHaveTextContent('두 번째 토스트');

    advanceTime(3000);
    finishTransition(getToastWrapper());
    expect(screen.queryByRole('toast')).not.toBeInTheDocument();
  });
});

describe('useToast', () => {
  test('ToastProvider 밖에서 호출하면 사용 범위를 안내하는 오류를 던진다', () => {
    expect(() => renderHook(() => useToast())).toThrow(
      'useToast는 ToastProvider 내부에서만 사용할 수 있습니다.',
    );
  });
});
