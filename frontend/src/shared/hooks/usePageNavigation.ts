import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { URL_POLICY, type NavigationPolicy } from '../policy/urlPolicy';

// 정책을 실행할 뿐, 목적지와 이력 방식을 여기서 결정하지 않는다.
export default function usePageNavigation() {
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const move = useCallback(
    (policy: NavigationPolicy) => {
      return typeof policy.to === 'number'
        ? navigate(policy.to)
        : navigate(policy.to, { replace: policy.replace });
    },
    [navigate],
  );
  const back = useCallback(() => move(URL_POLICY.back(pathname)), [move, pathname]);

  return { move, back };
}
