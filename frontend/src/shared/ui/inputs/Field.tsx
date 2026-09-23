import type { CSSProperties, ComponentProps } from 'react';
import { tokens, typography } from '../../../styles/global';

interface SubTextProps {
  errorText?: string;
  helpText?: string;
}

interface LabelProps extends React.ComponentProps<'label'> {
  htmlFor: string;
  isRequired?: boolean;
}

interface CurrentLengthProps {
  currentLength: number;
  maxLength: number;
}

const inputSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

export function Field({ children, ...props }: ComponentProps<'div'>) {
  return (
    <div css={inputSectionStyle} {...props}>
      {children}
    </div>
  );
}

Field.Label = function Label({ htmlFor, children, isRequired = false, ...props }: LabelProps) {
  return (
    <label
      htmlFor={htmlFor}
      css={{
        ...typography.sectionLabel,
        color: tokens.text.primary,
        marginBottom: tokens.spacing[1],
      }}
      {...props}
    >
      {children}
      {isRequired && (
        <span aria-hidden="true" css={{ color: tokens.text.brand }}>
          {' '}
          *
        </span>
      )}
    </label>
  );
};

Field.SubText = function SubText({ errorText, helpText }: SubTextProps) {
  if (errorText) {
    return <Field.ErrorText role="alert">{errorText}</Field.ErrorText>;
  }

  if (helpText) {
    return <Field.HelpText>{helpText}</Field.HelpText>;
  }

  return null;
};

Field.ErrorText = function ErrorText({ children, ...props }: ComponentProps<'p'>) {
  return (
    <p css={{ ...typography.footnote, color: tokens.text.critical }} {...props}>
      {children}
    </p>
  );
};

Field.HelpText = function HelpText({ children, ...props }: ComponentProps<'p'>) {
  return (
    <p css={{ ...typography.footnote, color: tokens.text.muted }} {...props}>
      {children}
    </p>
  );
};

Field.CurrentLength = function CurrentLegth({ currentLength, maxLength }: CurrentLengthProps) {
  return (
    <span
      css={{ ...typography.footnote, color: tokens.text.muted, marginRight: tokens.spacing[1] }}
    >
      {currentLength} / {maxLength}
    </span>
  );
};
