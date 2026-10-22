import { CSSProperties, useLayoutEffect, useRef } from 'react';
import type { SubmitEventHandler } from 'react';
import Button from '../../../shared/ui/Button';
import { tokens } from '../../../styles/global';
import { NOTICE_TITLE, NOTICE_CONTENT } from '../constants';
import type { NoticeFormValues } from '../types';
import { usePostHog } from '@posthog/react';
import InputField from '../../../shared/widgets/InputField';
import TextAreaField from '../../../shared/widgets/TextAreaField';
import AttachmentField from '../../../shared/widgets/AttachmentField';
import { useAttachments } from '../../../shared/files/useAttachments';
import { IMAGE_ACCEPT } from '../../../shared/files/validation';
import type { Attachment } from '../../../shared/files/types';
import { useInputState } from '../../../shared/hooks/useInputState';

const formStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

interface NoticeFormProps {
  initialValues?: NoticeFormValues;
  initialAttachments?: Attachment[];
  submitLabel: string;
  isSubmitting?: boolean;
  onSubmit: (values: NoticeFormValues) => void;
  fieldErrors?: Partial<Record<'title' | 'content' | 'attachmentFileIds', string>>;
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
  initialAttachments,
  submitLabel,
  isSubmitting = false,
  onSubmit,
  fieldErrors = {},
}: NoticeFormProps) {
  const contentRef = useRef<HTMLTextAreaElement>(null);
  const attachments = useAttachments({ initialAttachments, kind: 'image' });
  const [title, handleTitleChange] = useInputState(initialValues.title, (value, prevValue) => {
    if (value.length > NOTICE_TITLE.length) return prevValue;
    return value;
  });
  const [content, handleContentChange] = useInputState(
    initialValues.content,
    (value, prevValue) => {
      if (value.length > NOTICE_CONTENT.length) return prevValue;
      return value;
    },
  );
  const posthog = usePostHog();

  useLayoutEffect(() => {
    resizeTextArea(contentRef.current);
  }, [content]);

  const submitNotice: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    if (isSubmitting || !attachments.isReady) return;

    posthog?.capture('notice_form_submitted', {
      location: 'notice_create_page',
    });

    onSubmit({ title, content, attachmentFileIds: attachments.attachmentFileIds });
  };

  return (
    <form css={formStyle} onSubmit={submitNotice}>
      <InputField
        id="notice-title"
        label="제목"
        value={title}
        autoFocus
        onChange={handleTitleChange}
        placeholder="제목을 입력해 주세요"
        errorText={fieldErrors.title}
        maxLength={NOTICE_TITLE.length}
        isRequired
        testId="notice-title-field"
      />

      <TextAreaField
        ref={contentRef}
        id="notice-content"
        name="content"
        label="내용"
        value={content}
        onChange={handleContentChange}
        placeholder="내용을 입력해주세요"
        errorText={fieldErrors.content}
        maxLength={NOTICE_CONTENT.length}
        isRequired
        testId="notice-content-field"
        css={{ overflowY: 'hidden' }}
      />

      <AttachmentField
        id="notice-images"
        label="이미지 업로드"
        items={attachments.items}
        onSelect={attachments.addFiles}
        onRemove={attachments.removeFile}
        accept={IMAGE_ACCEPT}
        disabled={isSubmitting}
        errorText={attachments.error ?? fieldErrors.attachmentFileIds}
        helpText="본문을 설명할 이미지를 추가할 수 있어요"
        testId="notice-image-field"
      />

      <Button
        type="submit"
        variant="brandSolid"
        size="large"
        disabled={isSubmitting || !attachments.isReady}
        css={{ marginTop: tokens.spacing[8] }}
      >
        {submitLabel}
      </Button>
    </form>
  );
}
