import { useLocation } from 'react-router';
import { guidelinePages } from '../guidelinePages';
import { useGuidelineStorage } from './useGuidelineStorage';
import { useGuidelineVisibility } from './useGuidelineVisibility';

export function useGuidelineController() {
  const location = useLocation();
  const storage = useGuidelineStorage(guidelinePages.length);
  const visibility = useGuidelineVisibility(storage.dismissed, location.key);

  function dismiss() {
    storage.dismiss();
    visibility.close();
  }

  return { ...storage, ...visibility, dismiss };
}

export type GuidelineController = ReturnType<typeof useGuidelineController>;
