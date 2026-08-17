import { tokens, typography } from '../../../styles/global';
import type { ComponentPropsWithoutRef } from 'react';

type HelpTextProps = ComponentPropsWithoutRef<'p'>;

export default function HelpText({ children, ...props }: HelpTextProps) {
  return (
    <p css={{ ...typography.footnote, color: tokens.text.muted }} {...props}>
      {children}
    </p>
  );
}
