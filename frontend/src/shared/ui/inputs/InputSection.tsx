import Label from './Label';
import HelpText from './HelpText';
import ErrorText from './ErrorText';
import type { CSSObject } from '@emotion/react';

interface InputSectionProps {
  label: string;
  isRequired: boolean;
  helpText: string;
  isError: boolean;
  errorText: string;
  children: React.ReactNode;
}

const inputSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '4px',
} satisfies CSSObject;

export default function InputSection({
  label,
  isRequired,
  helpText,
  isError,
  errorText,
  children,
}: InputSectionProps) {
  const bottomText = isError ? <ErrorText>{errorText}</ErrorText> : <HelpText>{helpText}</HelpText>;
  return (
    <div css={{ ...inputSectionStyle }}>
      <Label text={label} isRequired={isRequired} />
      {children}
      {bottomText}
    </div>
  );
}
