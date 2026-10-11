import { useState, useCallback } from 'react';

export default function useBooleanState(initialState: boolean | (() => boolean) = false) {
  const [booleanState, setBooleanState] = useState(initialState);

  const setTrue = useCallback(() => {
    setBooleanState(true);
  }, []);

  const setFalse = useCallback(() => {
    setBooleanState(false);
  }, []);

  return [booleanState, setTrue, setFalse] as const;
}
