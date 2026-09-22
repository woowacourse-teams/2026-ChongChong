import type { ComponentPropsWithoutRef, CSSProperties } from 'react';
import type { CSSObject } from '@emotion/react';
import { Field } from '../ui/inputs/Field';
import { tokens, typography } from '../../styles/global';

interface CheckFieldProps extends Omit<
  ComponentPropsWithoutRef<'input'>,
  'id' | 'required' | 'type'
> {
  id: string;
  label: string;
  checkLabel: string;
  isRequired?: boolean;
  helpText?: string;
  errorText?: string;
  testId?: string;
}

const checkLabelStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  width: 'fit-content',
  cursor: 'pointer',
  color: tokens.text.default,
  ...typography.body,
} satisfies CSSProperties;

const checkStyle = {
  appearance: 'none',
  width: 20,
  height: 20,
  margin: 0,
  cursor: 'pointer',
  border: tokens.border.default,
  borderRadius: tokens.spacing[1],
  '&:checked': {
    backgroundColor: tokens.bg.brand,
    borderColor: tokens.bg.brand,
  },
  '&:checked::after': {
    content: '""',
    display: 'block',
    width: 5,
    height: 9,
    margin: '4px 0 0 6px',
    border: `solid ${tokens.text.onBrand}`,
    borderWidth: '0 2px 2px 0',
    transform: 'rotate(45deg)',
  },
} satisfies CSSObject;

export default function CheckField({
  id,
  label,
  checkLabel,
  isRequired = false,
  helpText,
  errorText,
  testId,
  ...checkProps
}: CheckFieldProps) {
  const hasSubText = Boolean(errorText || helpText);

  return (
    <Field data-testid={testId}>
      <Field.Label htmlFor={id} isRequired={isRequired}>
        {label}
      </Field.Label>
      <label css={checkLabelStyle}>
        <input {...checkProps} id={id} type="checkbox" required={isRequired} css={checkStyle} />
        {checkLabel}
      </label>
      {hasSubText && <Field.SubText errorText={errorText} helpText={helpText} />}
    </Field>
  );
}
