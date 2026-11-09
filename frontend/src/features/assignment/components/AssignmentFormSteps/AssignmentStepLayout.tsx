import type { ReactNode } from 'react';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import type { CSSObject } from '@emotion/react';

interface AssignmentStepLayoutProps {
  step: number;
  totalSteps: number;
  title: string;
  subTitle: string;
  children: ReactNode;
  footer: ReactNode;
}

const stepStyle = {
  display: 'flex',
  flexDirection: 'column',
  flex: 1,
  gap: 16,
} satisfies CSSObject;

const questionStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: 8,
} satisfies CSSObject;

const footerStyle = {
  marginTop: 'auto',
  paddingTop: 8,
} satisfies CSSObject;

export default function AssignmentStepLayout({
  step,
  totalSteps,
  title,
  subTitle,
  children,
  footer,
}: AssignmentStepLayoutProps) {
  return (
    <div css={stepStyle}>
      <div css={questionStyle}>
        <Paragraph
          variant="paragraph"
          css={{ margin: tokens.spacing[0], color: tokens.text.brand }}
        >
          {step} / {totalSteps}
        </Paragraph>
        <Heading level={2} variant="title" css={{ margin: tokens.spacing[0] }}>
          {title}
        </Heading>
        <Paragraph
          variant="paragraph"
          css={{
            marginTop: tokens.spacing[0],
            marginBottom: tokens.spacing[3],
            color: tokens.text.secondary,
          }}
        >
          {subTitle}
        </Paragraph>
      </div>
      {children}
      <div css={footerStyle}>{footer}</div>
    </div>
  );
}
