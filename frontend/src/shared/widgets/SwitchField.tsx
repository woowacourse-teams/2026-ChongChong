import type { ComponentProps } from 'react';
import Switch from '../ui/Switch';
import { Field } from '../ui/inputs/Field';
import { tokens } from '../../styles/global';

interface Props extends Omit<ComponentProps<typeof Switch>, 'id'> {
  id: string;
  label: string;
  helpText?: string;
  errorText?: string;
}

const switchContainerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[3],
};

export default function SwitchField({ id, label, helpText, errorText, ...props }: Props) {
  return (
    <Field>
      <div css={switchContainerStyle}>
        <Field.Label htmlFor={id}>{label}</Field.Label>
        <Switch {...props} id={id} aria-invalid={Boolean(errorText)} />
      </div>
      <Field.SubText errorText={errorText} helpText={helpText} />
    </Field>
  );
}
