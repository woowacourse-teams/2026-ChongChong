import { CSSProperties, useLayoutEffect, useRef, useState } from 'react';
import type { SubmitEventHandler } from 'react';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import { tokens } from '../../../styles/global';
import type { NoticeFormValues } from '../types';

const formStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

interface NoticeFormProps {
  initialValues?: NoticeFormValues;
  submitLabel: string;
  isSubmitting?: boolean;
  onSubmit: (values: NoticeFormValues) => void;
}

const emptyValues: NoticeFormValues = {
  title: '',
  content: '',
};

function resizeTextArea(textArea: HTMLTextAreaElement | null) {
  if (!textArea) return;

  textArea.style.height = 'auto';
  textArea.style.height = `${textArea.scrollHeight}px`;
}

export default function NoticeForm({
  initialValues = emptyValues,
  submitLabel,
  isSubmitting = false,
  onSubmit,
}: NoticeFormProps) {
  const contentRef = useRef<HTMLTextAreaElement>(null);
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);

  useLayoutEffect(() => {
    resizeTextArea(contentRef.current);
  }, [content]);

  const submitNotice: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    onSubmit({ title, content });
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
          ref={contentRef}
          id="notice-content"
          name="content"
          value={content}
          placeholder="내용을 입력해주세요"
          required
          css={{ overflowY: 'hidden' }}
          onChange={(event) => setContent(event.currentTarget.value)}
        />
      </Field>

      <Button
        type="submit"
        variant="brandSolid"
        size="large"
        disabled={isSubmitting}
        css={{ marginTop: tokens.spacing[8] }}
      >
        {submitLabel}
      </Button>
    </form>
  );
}
