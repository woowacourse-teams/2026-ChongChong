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

export default function SwitchField({ id, label, helpText, errorText, ...props }: Props) {
  const descriptionId = `${id}-description`;
  return (
    <Field>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: tokens.spacing[3],
        }}
      >
        <Field.Label htmlFor={id}>{label}</Field.Label>
        <Switch
          {...props}
          id={id}
          aria-describedby={errorText || helpText ? descriptionId : undefined}
          aria-invalid={Boolean(errorText)}
        />
      </div>
      {(errorText || helpText) && (
        <div id={descriptionId}>
          <Field.SubText errorText={errorText} helpText={helpText} />
        </div>
      )}
    </Field>
  );
}
