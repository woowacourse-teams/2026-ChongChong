import useBooleanState from '../../../shared/hooks/useBooleanState';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

const COPY_SUCCESS_DURATION_MS = 1000;

export default function useCopyLink() {
  const [isCopySuccess, showCopySuccess, hideCopySuccess] = useBooleanState();
  const toast = useToast();

  function copySuccessToHide() {
    setTimeout(hideCopySuccess, COPY_SUCCESS_DURATION_MS);
  }

  async function copyLink(value: string) {
    try {
      await navigator.clipboard.writeText(value);
      showCopySuccess();
      copySuccessToHide();
    } catch {
      toast.open(<StatusToast status="Error" message={'링크를 복사하지 못했어요'} />);
    }
  }

  return { isCopySuccess, copyLink };
}
