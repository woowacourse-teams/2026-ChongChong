import { useLocation, useNavigate } from 'react-router';
import usePageNavigation from './usePageNavigation';
import { getFunnelStartIndex } from '../utils/funnelHistory';

interface Params<Step> {
  steps: readonly Step[];
  initial: { step: Step };
}

export default function useFunnel<Step extends string>({ steps, initial }: Params<Step>) {
  const location = useLocation();
  const navigate = useNavigate();
  const { back } = usePageNavigation();
  const savedStep: unknown = location.state?.funnel?.step;
  const step = steps.find((step) => step === savedStep) ?? initial.step;
  const startIndex = getFunnelStartIndex(location.state?.funnel?.startIndex);

  function push(nextStep: Step) {
    navigate(
      { pathname: location.pathname, search: location.search, hash: location.hash },
      {
        state: {
          ...location.state,
          funnel: { step: nextStep, startIndex },
        },
      },
    );
  }

  function go(move: number) {
    navigate(move);
  }

  return { step, history: { push, back, go } };
}
