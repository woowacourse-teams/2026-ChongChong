import { CSSProperties, useState } from 'react';
import type { SubmitEventHandler } from 'react';
import addIcon from '../../../shared/assets/add.svg';
import deleteIcon from '../../../shared/assets/delete-x.svg';
import Button from '../../../shared/ui/Button';
import DateTimePicker from '../../../shared/ui/date-time-picker/DateTimePicker';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import List from '../../../shared/ui/List';
import { tokens, typography } from '../../../styles/global';

const formStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

const reminderChipStyle = {
  ...typography.subtitle,
  display: 'flex',
  minHeight: '52px',
  padding: `${tokens.spacing[2]} ${tokens.spacing[3]} ${tokens.spacing[2]} ${tokens.spacing[4]}`,
  alignItems: 'center',
  justifyContent: 'space-between',
  border: tokens.border.default,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
  color: tokens.text.primary,
} satisfies CSSProperties;

const iconButtonStyle = {
  display: 'grid',
  width: '32px',
  height: '32px',
  padding: 0,
  flex: '0 0 32px',
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  cursor: 'pointer',
} satisfies CSSProperties;

export interface NoticeFormValues {
  title: string;
  content: string;
  reminders: Date[];
}

interface NoticeFormProps {
  initialValues?: NoticeFormValues;
  submitLabel: string;
  onSubmit?: (values: NoticeFormValues) => void;
}

const emptyValues: NoticeFormValues = {
  title: '',
  content: '',
  reminders: [],
};

function formatReminder(value: Date) {
  return `${value.getFullYear()}년 ${value.getMonth() + 1}월 ${value.getDate()}일 ${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
}

export default function NoticeForm({
  initialValues = emptyValues,
  submitLabel,
  onSubmit,
}: NoticeFormProps) {
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const [reminders, setReminders] = useState<Date[]>(initialValues.reminders);

  const removeReminder = (indexToRemove: number) => {
    setReminders((current) => current.filter((_, index) => index !== indexToRemove));
  };

  const submitNotice: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    onSubmit?.({ title, content, reminders });
  };

  return (
    <form css={formStyle} onSubmit={submitNotice}>
      <Field id="notice-title" label="제목" isRequired>
        <Input
          id="notice-title"
          name="title"
          value={title}
          autoFocus
          required
          onChange={(event) => setTitle(event.target.value)}
          placeholder="제목을 입력해주세요"
        />
      </Field>

      <Field
        id="notice-content"
        label="내용"
        isRequired
        helpText="스터디원은 끝까지 읽어야 읽음 처리를 할 수 있어요"
      >
        <TextArea
          id="notice-content"
          name="content"
          value={content}
          placeholder="내용을 입력해주세요"
          required
          onChange={(event) => setContent(event.target.value)}
        />
      </Field>

      <Field
        id="notice-reminder"
        label="리마인드 시각"
        helpText="설정한 시각마다 읽지 않은 스터디원에게 알림을 보내드릴게요"
      >
        <List>
          {reminders.map((reminder, index) => (
            <List.Item key={`${reminder.getTime()}-${index}`} css={reminderChipStyle}>
              <time dateTime={reminder.toISOString()}>{formatReminder(reminder)}</time>
              <button
                type="button"
                css={iconButtonStyle}
                aria-label={`${formatReminder(reminder)} 리마인드 삭제`}
                onClick={() => removeReminder(index)}
              >
                <img src={deleteIcon} alt="리마인드 삭제" width={20} height={20} />
              </button>
            </List.Item>
          ))}
        </List>

        <DateTimePicker
          triggerLabel={<img src={addIcon} alt="리마인드 시각 추가" width={24} height={24} />}
          onChange={(value) => setReminders((current) => [...current, value])}
        />
      </Field>

      <Button
        type="submit"
        variant="brandSolid"
        size="large"
        css={{ marginTop: tokens.spacing[8] }}
      >
        {submitLabel}
      </Button>
    </form>
  );
}
