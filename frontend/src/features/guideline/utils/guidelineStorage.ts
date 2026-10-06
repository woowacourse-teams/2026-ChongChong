export const GUIDELINE_STORAGE_KEY = 'chongchong:guideline';

export interface GuidelineState {
  page: number;
  dismissed: boolean;
}

export function normalizeGuidelinePage(page: unknown, pageCount: number): number {
  if (typeof page !== 'number' || !Number.isInteger(page)) return 1;
  return Math.min(Math.max(page, 1), Math.max(pageCount, 1));
}

export function readGuidelineState(pageCount: number): GuidelineState {
  const initialState = { page: 1, dismissed: false };

  try {
    const stored: unknown = JSON.parse(localStorage.getItem(GUIDELINE_STORAGE_KEY) ?? 'null');
    if (stored === null || typeof stored !== 'object' || Array.isArray(stored)) return initialState;

    const dismissed = 'dismissed' in stored && stored.dismissed === true;
    const page = 'page' in stored ? normalizeGuidelinePage(stored.page, pageCount) : 1;
    return { page, dismissed };
  } catch {
    return initialState;
  }
}

export function writeGuidelineState(state: GuidelineState) {
  try {
    localStorage.setItem(GUIDELINE_STORAGE_KEY, JSON.stringify(state));
  } catch {
    // 저장소가 차단된 브라우저에서도 현재 방문의 안내는 계속 사용할 수 있습니다.
  }
}
