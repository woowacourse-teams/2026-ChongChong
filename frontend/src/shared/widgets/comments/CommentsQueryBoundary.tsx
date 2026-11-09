import { Suspense, type CSSProperties, type PropsWithChildren } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { tokens, typography } from '../../../styles/global';

const statusStyle = {
  ...typography.body,
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  margin: 0,
  marginTop: tokens.spacing[5],
  minHeight: '48px',
  color: tokens.text.secondary,
} satisfies CSSProperties;

export default function CommentsQueryBoundary({ children }: PropsWithChildren) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <ErrorBoundary
          onReset={reset}
          fallbackRender={({ resetErrorBoundary }) => (
            <div role="alert" css={statusStyle}>
              <span>댓글을 불러오지 못했어요.</span>
              <button type="button" onClick={resetErrorBoundary}>
                다시 시도
              </button>
            </div>
          )}
        >
          <Suspense
            fallback={
              <p role="status" css={statusStyle}>
                댓글을 불러오는 중이에요.
              </p>
            }
          >
            {children}
          </Suspense>
        </ErrorBoundary>
      )}
    </QueryErrorResetBoundary>
  );
}
