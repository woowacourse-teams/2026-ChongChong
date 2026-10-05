import type { CSSProperties } from 'react';
import type { CSSObject } from '@emotion/react';
import { Field } from '../ui/inputs/Field';
import { tokens, typography } from '../../styles/global';

interface RadioOption<T extends string> {
  label: string;
  value: T;
}

interface RadioFieldProps<T extends string> {
  id: string;
  name: string;
  label: string;
  options: readonly [RadioOption<T>, ...RadioOption<T>[]];
  value: T;
  onChange: (value: T) => void;
  isRequired?: boolean;
  helpText?: string;
  errorText?: string;
}

const optionsStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[6],
} satisfies CSSProperties;

const optionStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  width: 'fit-content',
  cursor: 'pointer',
  color: tokens.text.default,
  ...typography.body,
} satisfies CSSProperties;

const radioStyle = {
  appearance: 'none',
  width: 20,
  height: 20,
  margin: 0,
  border: `2px solid ${tokens.text.placeholder}`,
  borderRadius: tokens.radius.full,
  cursor: 'pointer',

  '&:checked': {
    borderColor: tokens.bg.brand,
  },

  '&:checked::after': {
    content: '""',
    display: 'block',
    width: 10,
    height: 10,
    margin: 3,
    borderRadius: tokens.radius.full,
    backgroundColor: tokens.bg.brand,
  },

  '&:focus-visible': {
    outline: `2px solid ${tokens.text.primary}`,
    outlineOffset: 2,
  },
} satisfies CSSObject;

export default function RadioField<T extends string>({
  id,
  name,
  label,
  options,
  value,
  onChange,
  isRequired = false,
  helpText,
  errorText,
}: RadioFieldProps<T>) {
  return (
    <Field>
      <Field.Label htmlFor={id} isRequired={isRequired}>
        {label}
      </Field.Label>
      <div role="radiogroup" aria-labelledby={id} css={optionsStyle}>
        {options.map((option) => (
          <label key={option.value} css={optionStyle}>
            <input
              id={`${id}-${option.value}`}
              type="radio"
              name={name}
              value={option.value}
              checked={value === option.value}
              onChange={() => onChange(option.value)}
              css={radioStyle}
            />
            {option.label}
          </label>
        ))}
      </div>
      <Field.SubText errorText={errorText} helpText={helpText} />
    </Field>
  );
}
