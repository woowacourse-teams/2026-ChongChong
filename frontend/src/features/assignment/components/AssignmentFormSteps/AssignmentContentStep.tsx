import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';
import { useState } from 'react';
import type { useInputState } from '../../../../shared/hooks/useInputState';
import type { useAttachments } from '../../../../shared/modules/files/useAttachments';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT } from '../../constants';
import InputField from '../../../../shared/widgets/InputField';
import AutoResizeTextAreaField from '../../../../shared/widgets/AutoResizeTextAreaField';
import AttachmentField from '../../../../shared/widgets/AttachmentField';
import { IMAGE_ACCEPT } from '../../../../shared/modules/files/validation';
import { validateAssignmentContent } from '../../validation';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import Button from '../../../../shared/ui/Button';

interface AssignmentContentStepProps {
  title: string;
  content: string;
  submissionMethod: string;
  handleTitleChange: ReturnType<typeof useInputState>[1];
  handleContentChange: ReturnType<typeof useInputState>[1];
  handleSubmissionMethodChange: ReturnType<typeof useInputState>[1];
  attachments: ReturnType<typeof useAttachments>;
  fieldErrors: Record<string, string>;
  onNext: () => void;
}

export default function AssignmentContentStep({
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
  const [hasAttemptedNext, setHasAttemptedNext] = useState(false);
  const validationErrors = validateAssignmentContent(title, content, submissionMethod);
  const canProceed = Object.keys(validationErrors).length === 0;
  return (
    <div
      css={[
        stepStyle,
        {
          '& label[for]': { marginBottom: 0 },
          '& input:not([type=file])': { padding: '14px 16px' },
          '& [aria-label="첨부 파일"]:empty': { display: 'none' },
          '& input[type="file"] ~ button': {
            height: 44,
            '& > img': { display: 'none' },
          },
        },
      ]}
    >
      <div css={questionStyle}>
        <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
          1 / 4
        </Paragraph>
        <Heading level={2} variant="title">
          어떤 과제를 함께 할까요?
        </Heading>
        <Paragraph variant="paragraph" css={{ color: tokens.text.secondary }}>
          제목과 내용, 제출 방법을 알려주세요.
        </Paragraph>
      </div>
      <InputField
        id="assignment-title"
        name="title"
        label="제목"
        value={title}
        autoFocus
        onChange={handleTitleChange}
        maxLength={ASSIGNMENT_TITLE.length}
        placeholder="제목을 입력해주세요"
        errorText={(hasAttemptedNext ? validationErrors.title : undefined) ?? fieldErrors.title}
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
        onChange={handleContentChange}
        errorText={(hasAttemptedNext ? validationErrors.content : undefined) ?? fieldErrors.content}
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
        name="method"
        label="제출 방법"
        value={submissionMethod}
        maxLength={ASSIGNMENT_CONTENT.length}
        onChange={handleSubmissionMethodChange}
        placeholder="제출 방법을 입력해주세요"
        errorText={
          (hasAttemptedNext ? validationErrors.submissionMethod : undefined) ??
          fieldErrors.submissionMethod
        }
        isRequired
      />
      <div css={footerStyle}>
        <Button
          variant="brandSolid"
          size="large"
          onClick={() => {
            setHasAttemptedNext(true);
            if (canProceed) onNext();
          }}
        >
          다음
        </Button>
      </div>
    </div>
  );
}
