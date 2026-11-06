import AssignmentStepLayout from './AssignmentStepLayout';
import { useState, type ChangeEvent, type ChangeEventHandler } from 'react';
import type { AttachmentItem } from '../../../../shared/modules/files/types';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT } from '../../constants';
import InputField from '../../../../shared/widgets/InputField';
import AutoResizeTextAreaField from '../../../../shared/widgets/AutoResizeTextAreaField';
import AttachmentField from '../../../../shared/widgets/AttachmentField';
import { IMAGE_ACCEPT } from '../../../../shared/modules/files/validation';
import { validateAssignmentContent } from '../../validation';
import Button from '../../../../shared/ui/Button';

interface AssignmentContentStepProps {
  step: number;
  totalSteps: number;
  title: string;
  content: string;
  submissionMethod: string;
  handleTitleChange: ChangeEventHandler<HTMLInputElement>;
  handleContentChange: ChangeEventHandler<HTMLTextAreaElement>;
  handleSubmissionMethodChange: ChangeEventHandler<HTMLInputElement>;
  attachments: {
    items: AttachmentItem[];
    error?: string;
    addFiles: (files: File[]) => void;
    removeFile: (id: string) => void;
  };
  fieldErrors: Record<string, string>;
  onNext: () => void;
}

export default function AssignmentContentStep({
  step,
  totalSteps,
  title,
  content,
  submissionMethod,
  handleTitleChange,
  handleContentChange,
  handleSubmissionMethodChange,
  attachments,
  fieldErrors,
  onNext,
}: AssignmentContentStepProps) {
  const [validationErrors, setValidationErrors] = useState<
    Partial<Record<'title' | 'content' | 'submissionMethod', string>>
  >({});
  const errors = { ...fieldErrors, ...validationErrors };

  function onTitleChange(event: ChangeEvent<HTMLInputElement>) {
    handleTitleChange(event);
    if (errors.title) {
      const { title: error } = validateAssignmentContent(
        event.target.value,
        content,
        submissionMethod,
      );
      setValidationErrors((current) => ({ ...current, title: error }));
    }
  }

  function onContentChange(event: ChangeEvent<HTMLTextAreaElement>) {
    handleContentChange(event);
    if (errors.content) {
      const { content: error } = validateAssignmentContent(
        title,
        event.target.value,
        submissionMethod,
      );
      setValidationErrors((current) => ({ ...current, content: error }));
    }
  }

  function onSubmissionMethodChange(event: ChangeEvent<HTMLInputElement>) {
    handleSubmissionMethodChange(event);
    if (errors.submissionMethod) {
      const { submissionMethod: error } = validateAssignmentContent(
        title,
        content,
        event.target.value,
      );
      setValidationErrors((current) => ({ ...current, submissionMethod: error }));
    }
  }

  function handleNext() {
    const nextErrors = validateAssignmentContent(title, content, submissionMethod);
    setValidationErrors({
      title: nextErrors.title,
      content: nextErrors.content,
      submissionMethod: nextErrors.submissionMethod,
    });
    if (Object.keys(nextErrors).length === 0) onNext();
  }
  return (
    <AssignmentStepLayout
      step={step}
      totalSteps={totalSteps}
      title="어떤 과제를 함께 할까요?"
      subTitle="제목과 내용, 제출 방법을 알려주세요."
      footer={
        <Button
          variant="brandSolid"
          size="large"
          disabled={!title.trim() || !content.trim() || !submissionMethod.trim()}
          onClick={handleNext}
        >
          다음
        </Button>
      }
    >
      <InputField
        id="assignment-title"
        name="title"
        label="제목"
        value={title}
        autoFocus
        onChange={onTitleChange}
        maxLength={ASSIGNMENT_TITLE.length}
        placeholder="제목을 입력해주세요"
        errorText={errors.title}
        isRequired
        testId="assignment-title-field"
      />
      <AutoResizeTextAreaField
        id="assignment-content"
        name="content"
        label="내용"
        value={content}
        placeholder="내용을 입력해주세요"
        maxLength={ASSIGNMENT_CONTENT.length}
        onChange={onContentChange}
        errorText={errors.content}
        isRequired
        testId="assignment-content-field"
      />
      <AttachmentField
        showImagePreviews
        id="assignment-images"
        label="이미지 업로드"
        items={attachments.items}
        onSelect={attachments.addFiles}
        onRemove={attachments.removeFile}
        accept={IMAGE_ACCEPT}
        errorText={attachments.error ?? fieldErrors.attachmentFileIds}
        helpText="본문을 설명할 이미지를 추가할 수 있어요"
      />
      <InputField
        id="submit-method"
        testId="assignment-submission-method-field"
        name="method"
        label="제출 방법"
        value={submissionMethod}
        maxLength={ASSIGNMENT_CONTENT.length}
        onChange={onSubmissionMethodChange}
        placeholder="제출 방법을 입력해주세요"
        errorText={errors.submissionMethod}
        isRequired
      />
    </AssignmentStepLayout>
  );
}
