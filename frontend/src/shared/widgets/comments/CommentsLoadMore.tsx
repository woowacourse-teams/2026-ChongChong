import { useState, type CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';

interface Props {
  onLoad: () => Promise<unknown>;
}

const buttonStyle = {
  ...typography.body,
  width: '100%',
  minHeight: '48px',
  color: tokens.text.secondary,
} satisfies CSSProperties;

export default function CommentsLoadMore({ onLoad }: Props) {
  const [status, setStatus] = useState<'idle' | 'pending' | 'error'>('idle');

  async function load() {
    if (status === 'pending') return;
    setStatus('pending');
    try {
      await onLoad();
      setStatus('idle');
    } catch {
      setStatus('error');
    }
  }

  return (
    <div>
      {status === 'error' && (
        <p role="alert" css={{ ...typography.body, color: tokens.text.secondary }}>
          댓글을 더 불러오지 못했어요. 다시 시도해주세요.
        </p>
      )}
      <button
        type="button"
        css={buttonStyle}
        disabled={status === 'pending'}
        onClick={() => void load()}
      >
        {status === 'pending'
          ? '댓글을 불러오는 중이에요.'
          : status === 'error'
            ? '댓글 더 보기 다시 시도'
            : '댓글 10개 더 보기'}
      </button>
    </div>
  );
}
