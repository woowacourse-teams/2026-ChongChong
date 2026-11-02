import type { CSSProperties } from 'react';
import { Field } from '../ui/inputs/Field';
import DateTimePicker from '../ui/date-time-picker/DateTimePicker';
import { formatDateToString, toLocalDateTime } from '../utils/formatDate';
import { tokens, typography } from '../../styles/global';
import addIcon from '../assets/add.svg';
import deleteIcon from '../assets/image-upload-delete.svg';

interface ReminderFieldProps {
  id: string;
  value?: string[] | null;
  onChange: (value: string[]) => void;
  helpText: string;
  errorText?: string;
  disabled?: boolean;
}

const reminderStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[2],
  minHeight: 52,
  padding: `0 ${tokens.spacing[3]}`,
  border: tokens.border.default,
  borderRadius: tokens.radius.md,
  color: tokens.text.primary,
  ...typography.subtitle,
} satisfies CSSProperties;

const addButtonStyle = {
  gap: tokens.spacing[2],
  height: 48,
  border: 0,
  background: tokens.bg.subtle,
  color: tokens.text.primary,
  ...typography.body,
} satisfies CSSProperties;

export default function ReminderField({
  id,
  value,
  onChange,
  helpText,
  errorText,
  disabled = false,
}: ReminderFieldProps) {
  const reminders = value ?? [];

  return (
    <Field>
      <Field.Label htmlFor={id}>리마인드 시각</Field.Label>
      {reminders.length > 0 && (
        <div
          role="list"
          aria-label="등록한 리마인드 시각"
          css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[1] }}
        >
          {reminders.map((remindAt, index) => (
            <div role="listitem" key={`${remindAt}-${index}`} css={reminderStyle}>
              <time dateTime={remindAt}>{formatDateToString(remindAt)}</time>
              <button
                type="button"
                aria-label={`${formatDateToString(remindAt)} 리마인드 삭제`}
                disabled={disabled}
                onClick={() => onChange(reminders.filter((_, i) => i !== index))}
                css={{
                  display: 'grid',
                  placeItems: 'center',
                  padding: 0,
                  border: 0,
                  background: 'none',
                  cursor: 'pointer',
                }}
              >
                <img src={deleteIcon} alt="" width={20} height={20} />
              </button>
            </div>
          ))}
        </div>
      )}
      <DateTimePicker
        id={id}
        disabled={disabled}
        triggerStyle={addButtonStyle}
        triggerLabel={
          <>
            <img src={addIcon} alt="" width={24} height={24} />
            리마인드 등록하기
          </>
        }
        onChange={(date) => {
          const remindAt = toLocalDateTime(date);
          if (!reminders.includes(remindAt)) onChange([...reminders, remindAt]);
        }}
      />
      <Field.SubText helpText={helpText} errorText={errorText} />
    </Field>
  );
}
