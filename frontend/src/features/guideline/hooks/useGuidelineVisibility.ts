import { useState } from 'react';

interface Visibility {
  visitKey: string;
  isOpen: boolean;
  entrySource: 'auto' | 'help_link';
}

export function useGuidelineVisibility(dismissed: boolean, visitKey = '') {
  const [visibility, setVisibility] = useState<Visibility>({
    visitKey,
    isOpen: true,
    entrySource: 'auto',
  });

  // 같은 목록 경로의 새 방문에서도 이전 방문의 닫기 상태를 초기화한다.
  if (visibility.visitKey !== visitKey) {
    setVisibility({ visitKey, isOpen: true, entrySource: 'auto' });
  }

  return {
    isOpen: visibility.isOpen && (!dismissed || visibility.entrySource === 'help_link'),
    entrySource: visibility.entrySource,
    openHelp: () => setVisibility({ visitKey, isOpen: true, entrySource: 'help_link' }),
    close: () => setVisibility((current) => ({ ...current, isOpen: false })),
  };
}
