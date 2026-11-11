import { Field } from '../ui/inputs/Field';
import DateTimePicker from '../ui/date-time-picker/DateTimePicker';
import { formatDateToString, toLocalDateTime } from '../utils/formatDate';

interface CloseAtFieldProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  errorText?: string;
  isRequired?: boolean;
  disabled?: boolean;
}

export default function CloseAtField({
  id,
  value,
  onChange,
  errorText,
  isRequired = false,
  disabled = false,
}: CloseAtFieldProps) {
  return (
    <Field>
      <Field.Label htmlFor={id} isRequired={isRequired}>
        마감 시각
      </Field.Label>
      <DateTimePicker
        id={id}
        title="마감 시각 설정"
        value={value ? new Date(value) : undefined}
        triggerLabel={value ? formatDateToString(value) : '마감 시각 설정'}
        triggerVariant="neutralOutline"
        disabled={disabled}
        onChange={(date) => onChange(toLocalDateTime(date))}
      />
      <Field.SubText errorText={errorText} />
    </Field>
  );
}
