import { tokens, typography } from '../../../styles/global';

interface HelpTextProps {
  children: React.ReactNode;
}

export default function HelpText({ children }: HelpTextProps) {
  return <p css={{ ...typography.footnote, color: tokens.text.muted }}>{children}</p>;
}
