import type { ComponentProps, ComponentPropsWithoutRef } from 'react';
import { typography } from '../../styles/global';

type Variant = keyof typeof typography;

interface HeadingProps extends ComponentPropsWithoutRef<'h1'> {
  level: 1 | 2 | 3 | 4 | 5 | 6;
  variant: Variant;
}

interface ParagraphProps extends ComponentPropsWithoutRef<'p'> {
  variant: Variant;
}

interface TextProps extends ComponentProps<'span'> {
  variant: Variant;
}

export function Heading({ level, variant, ...props }: HeadingProps) {
  const Tag = `h${level}` as const;
  return <Tag css={typography[variant]} {...props} />;
}

export function Paragraph({ variant, ...props }: ParagraphProps) {
  return <p css={typography[variant]} {...props} />;
}

export function Text({ variant, ...props }: TextProps) {
  return <span css={typography[variant]} {...props} />;
}
