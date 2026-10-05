import type { GuidelineController } from './hooks/useGuidelineController';
import GuidelineModal from './components/GuidelineModal';

export default function Guideline({ controller }: { controller: GuidelineController }) {
  const { page, setPage, dismiss, isOpen, close } = controller;

  if (!isOpen) return null;

  return <GuidelineModal page={page} onPageChange={setPage} onClose={close} onDismiss={dismiss} />;
}
