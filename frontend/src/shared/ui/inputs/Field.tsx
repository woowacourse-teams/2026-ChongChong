import Label from './Label';
import HelpText from './HelpText';
import ErrorText from './ErrorText';
import type { CSSProperties } from 'react';
import { tokens } from '../../../styles/global';

interface InputSectionProps {
  id: string;
  label: string;
  isRequired?: boolean;
  helpText?: string;
  isError?: boolean;
  errorText?: string;
  children: React.ReactNode;
}

const inputSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

export default function InputSection({
  id,
  label,
  isRequired = false,
  helpText,
  isError = false,
  errorText,
  children,
}: InputSectionProps) {
  const messageId = `${id}-message`;
  let message: React.ReactNode = null;

  if (isError) {
    if (errorText) {
      message = (
        <ErrorText id={messageId} role="alert">
          {errorText}
        </ErrorText>
      );
    }
  } else if (helpText) {
    message = <HelpText id={messageId}>{helpText}</HelpText>;
  }

  return (
    <div css={inputSectionStyle}>
      <Label htmlFor={id} text={label} isRequired={isRequired} />
      {children}
      {message}
    </div>
  );
}
