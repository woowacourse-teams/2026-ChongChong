import { useState } from 'react';
import {
  normalizeGuidelinePage,
  readGuidelineState,
  writeGuidelineState,
} from '../utils/guidelineStorage';

export function useGuidelineStorage(pageCount: number) {
  const [state, setState] = useState(() => readGuidelineState(pageCount));

  const setPage = (page: number) => {
    const nextState = { ...state, page: normalizeGuidelinePage(page, pageCount) };
    writeGuidelineState(nextState);
    setState(nextState);
  };

  const dismiss = () => {
    const nextState = { page: 1, dismissed: true };
    writeGuidelineState(nextState);
    setState(nextState);
  };

  return {
    ...state,
    setPage,
    dismiss,
  };
}
