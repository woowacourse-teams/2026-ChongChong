import { tokens, typography } from '../../../styles/global';
import type { ComponentPropsWithoutRef } from 'react';

type ErrorTextProps = ComponentPropsWithoutRef<'p'>;

export default function ErrorText({ children, ...props }: ErrorTextProps) {
  return (
    <p css={{ ...typography.footnote, color: tokens.text.critical }} {...props}>
      {children}
    </p>
  );
}
