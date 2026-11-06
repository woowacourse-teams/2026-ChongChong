import type { ReactNode } from 'react';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';

interface AssignmentStepLayoutProps {
  step: number;
  totalSteps: number;
  title: string;
  subTitle: string;
  children: ReactNode;
  footer: ReactNode;
}

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
        <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
          {step} / {totalSteps}
        </Paragraph>
        <Heading level={2} variant="title">
          {title}
        </Heading>
        <Paragraph variant="paragraph" css={{ color: tokens.text.secondary }}>
          {subTitle}
        </Paragraph>
      </div>
      {children}
      <div css={footerStyle}>{footer}</div>
    </div>
  );
}
