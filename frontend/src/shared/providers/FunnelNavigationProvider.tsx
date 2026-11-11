/* eslint-disable react-refresh/only-export-components */

import { createContext, useContext, useEffect, useRef, type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { getFunnelStartIndex, getHistoryIndex } from '../utils/funnelHistory';
import type { FunnelCompletionPolicy } from '../policy/urlPolicy';

interface PendingNavigation {
  policy: FunnelCompletionPolicy;
  targetIndex: number;
  locationKey: string;
  phase: 'rewind' | 'fallback' | 'detail';
  needsFallback: boolean;
}

const FunnelNavigationContext = createContext<((policy: FunnelCompletionPolicy) => void) | null>(
  null,
);

export function useCompleteFunnel() {
  const complete = useContext(FunnelNavigationContext);
  if (!complete) {
    throw new Error('useCompleteFunnel은 FunnelNavigationProvider 내부에서 사용해야 합니다.');
  }
  return complete;
}

// 페이지가 교체되어도 완료 이동을 이어갈 수 있도록 최상위 라우트에 배치한다.
export function FunnelNavigationProvider({ children }: { children: ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const navigateInfo = useRef<PendingNavigation | null>(null);

  function complete(policy: FunnelCompletionPolicy) {
    if (navigateInfo.current) return;

    const currentIndex = getHistoryIndex();
    const startIndex = getFunnelStartIndex(location.state?.funnel?.startIndex);
    // 링크를 통해 퍼널로 직접 접근한 상황은 Fallback을 사용한다. (히스토리가 없음)
    const needsFallback = startIndex === 0;
    const targetIndex = needsFallback ? startIndex : startIndex - 1;
    // 이동해야할 거리
    const delta = targetIndex - currentIndex;

    navigateInfo.current = {
      policy,
      targetIndex,
      locationKey: location.key,
      phase: delta === 0 ? 'fallback' : 'rewind',
      needsFallback,
    };

    if (delta === 0) {
      // 첫 단계에서 완료한 직접 진입도 목록을 남긴 뒤 상세를 추가한다.
      navigate(policy.fallback, { replace: true });
    } else {
      navigate(delta);
    }
  }

  useEffect(
    function continueFunnelCompletionAfterNavigation() {
      const transition = navigateInfo.current;
      if (!transition || transition.locationKey === location.key) return;

      if (transition.phase === 'detail') {
        navigateInfo.current = null;
        return;
      }

      // navigate(delta)의 호출 완료가 아니라 실제 라우트 전환을 기다린다.
      if (getHistoryIndex() !== transition.targetIndex) {
        navigateInfo.current = null;
        return;
      }

      transition.locationKey = location.key;
      if (transition.phase === 'rewind' && transition.needsFallback) {
        transition.phase = 'fallback';
        navigate(transition.policy.fallback, { replace: true });
        return;
      }

      transition.phase = 'detail';
      // push로 앞으로가기 기록에 남아 있는 퍼널 단계들을 제거한다.
      navigate(transition.policy.to);
    },
    [location.key, navigate],
  );

  return (
    <FunnelNavigationContext.Provider value={complete}>{children}</FunnelNavigationContext.Provider>
  );
}
