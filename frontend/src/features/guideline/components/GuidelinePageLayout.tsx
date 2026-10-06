import type { ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';

interface Props {
  artwork: ReactNode;
  title: ReactNode;
  description: ReactNode;
  children?: ReactNode;
  action: ReactNode;
}

export default function GuidelinePageLayout({
  artwork,
  title,
  description,
  children,
  action,
}: Props) {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        flex: 1,
        minHeight: 0,
        minWidth: 0,
        width: '100%',
        color: tokens.text.default,
        overflowWrap: 'anywhere',
        wordBreak: 'keep-all',
      }}
    >
      <div
        data-guideline-scroll
        css={{
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          minHeight: 0,
          gap: tokens.spacing[4],
          overflowY: 'auto',
          overscrollBehavior: 'contain',
          '& > *': { flexShrink: 0 },
        }}
      >
        {artwork}
        <div css={{ textAlign: 'center' }}>
          <h2
            id="guideline-page-title"
            css={{
              ...typography.title,
              fontWeight: tokens.fontWeight.semibold,
              margin: 0,
            }}
          >
            {title}
          </h2>
          <p
            css={{
              ...typography.body,
              color: tokens.text.secondary,
              margin: `${tokens.spacing[2]} 0 0`,
            }}
          >
            {description}
          </p>
        </div>
        {children}
      </div>
      <div
        css={{
          display: 'grid',
          gap: tokens.spacing[2],
          flexShrink: 0,
          paddingTop: tokens.spacing[3],
        }}
      >
        {action}
      </div>
    </div>
  );
}
