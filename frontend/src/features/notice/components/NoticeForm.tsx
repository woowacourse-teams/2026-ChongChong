import { CSSProperties, useLayoutEffect, useRef, useState } from 'react';
import type { SubmitEventHandler } from 'react';
import Button from '../../../shared/ui/Button';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import { tokens } from '../../../styles/global';
import { Field } from '../../../shared/ui/inputs/Field';
import { NOTICE_TITLE, NOTICE_CONTENT } from '../constants';
import type { NoticeFormValues } from '../types';
import { usePostHog } from '@posthog/react';

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
  fieldErrors?: Partial<Record<'title' | 'content', string>>;
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
  fieldErrors = {},
}: NoticeFormProps) {
  const contentRef = useRef<HTMLTextAreaElement>(null);
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const posthog = usePostHog();

  useLayoutEffect(() => {
    resizeTextArea(contentRef.current);
  }, [content]);

  const submitNotice: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();

    posthog?.capture('notice_form_submitted', {
      location: 'notice_create_page',
    });

    onSubmit({ title, content });
  };

  return (
    <form css={formStyle} onSubmit={submitNotice}>
      <Field data-testid="notice-title-field">
        <Field.Label htmlFor="notice-title" isRequired={true}>
          제목
        </Field.Label>
        <Input
          id="notice-title"
          value={title}
          maxLength={NOTICE_TITLE.length}
          autoFocus
          onChange={(event) => setTitle(event.target.value)}
          placeholder="제목을 입력해 주세요"
          required={true}
        />
        <div css={{ display: 'flex', justifyContent: 'space-between' }}>
          <Field.SubText errorText={fieldErrors.title} />
          <Field.CurrentLength currentLength={title.length} maxLength={NOTICE_TITLE.length} />
        </div>
      </Field>
      <Field data-testid="notice-content-field">
        <Field.Label htmlFor="notice-content" isRequired={true}>
          내용
        </Field.Label>
        <TextArea
          ref={contentRef}
          id="notice-content"
          name="content"
          value={content}
          maxLength={NOTICE_CONTENT.length}
          placeholder="내용을 입력해주세요"
          css={{ overflowY: 'hidden' }}
          onChange={(event) => setContent(event.currentTarget.value)}
          required={true}
        />
        <div css={{ display: 'flex', justifyContent: 'space-between' }}>
          <Field.SubText
            errorText={fieldErrors.content}
            helpText={'스터디원은 끝까지 읽어야 읽음 처리를 할 수 있어요'}
          />
          <Field.CurrentLength
            currentLength={content.length}
            maxLength={NOTICE_CONTENT.length}
          />
        </div>
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
