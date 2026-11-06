import { useState, useCallback, useMemo } from 'react';

interface Params<Step> {
  initial: { step: Step };
}

export default function useFunnel<Step extends string>({ initial }: Params<Step>) {
  const [historySteps, setHistorySteps] = useState<{ historySteps: Step[]; index: number }>({
    historySteps: [initial.step],
    index: 0,
  });

  const push = useCallback((step: Step) => {
    setHistorySteps((current) => {
      const historySteps = [...current.historySteps.slice(0, current.index + 1), step];

      return { historySteps, index: historySteps.length - 1 };
    });
  }, []);

  const go = useCallback((move: number) => {
    setHistorySteps((current) => {
      const nextIndex = current.index + move;

      if (nextIndex < 0 || nextIndex >= current.historySteps.length) return current;

      return { ...current, index: nextIndex };
    });
  }, []);

  const back = useCallback(() => {
    go(-1);
  }, [go]);

  const history = useMemo(() => {
    return { push, back, go };
  }, [push, back, go]);

  return { step: historySteps.historySteps[historySteps.index], history };
}
