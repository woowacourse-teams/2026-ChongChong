import { useState, useCallback, useMemo } from 'react';

export default function useBooleanState(initialState: boolean | (() => boolean) = false) {
  const [booleanState, setBooleanState] = useState(initialState);

  const setTrue = useCallback(() => {
    setBooleanState(true);
  }, []);

  const setFalse = useCallback(() => {
    setBooleanState(false);
  }, []);

  const toggle = useCallback(() => {
    setBooleanState((prev) => !prev);
  }, []);

  const actions = useMemo(() => ({ setTrue, setFalse, toggle }), [setTrue, setFalse, toggle]);

  return [booleanState, actions] as const;
}
