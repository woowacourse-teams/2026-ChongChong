import { useSearchParams } from 'react-router';

export default function useFocusCommentId() {
  const [searchParams] = useSearchParams();
  const value = searchParams.get('focusCommentId');
  if (!value || !/^[1-9]\d*$/.test(value)) return null;

  const id = Number(value);
  return Number.isSafeInteger(id) ? id : null;
}
