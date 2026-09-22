import type { ComponentPropsWithoutRef } from 'react';
import { Field } from '../ui/inputs/Field';
import Input from '../ui/inputs/Input';

interface InputFieldProps extends Omit<
  ComponentPropsWithoutRef<typeof Input>,
  'id' | 'required' | 'value' | 'maxLength'
> {
  id: string;
  label: string;
  value: string;
  isRequired?: boolean;
  helpText?: string;
  errorText?: string;
  testId?: string;
  maxLength: number;
}

export default function InputField({
  id,
  label,
  value,
  isRequired = false,
  helpText,
  errorText,
  testId,
  maxLength,
  ...inputProps
}: InputFieldProps) {
  const hasSubText = Boolean(errorText || helpText);

  return (
    <Field data-testid={testId}>
      <Field.Label htmlFor={id} isRequired={isRequired}>
        {label}
      </Field.Label>
      <Input {...inputProps} id={id} value={value} maxLength={maxLength} required={isRequired} />

      <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
        {hasSubText && <Field.SubText errorText={errorText} helpText={helpText} />}
        <Field.CurrentLength currentLength={value.length} maxLength={maxLength} />
      </div>
    </Field>
  );
}
