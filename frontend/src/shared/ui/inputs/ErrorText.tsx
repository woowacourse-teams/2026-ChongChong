import { tokens, typography } from '../../../styles/global';

interface ErrorTextProps {
  children: React.ReactNode;
}

export default function ErrorText({ children }: ErrorTextProps) {
  return <p css={{ ...typography.footnote, color: tokens.text.critical }}>{children}</p>;
}
