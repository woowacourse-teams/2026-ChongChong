import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';
import type { SubmissionVisibility } from '../../types';
import { Field } from '../../../../shared/ui/inputs/Field';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import Button from '../../../../shared/ui/Button';
import VisibilityOptionField from './VisibilityOptionField';
import { CSSObject } from '@emotion/react';

const visibilityOptions = [
  {
    value: 'ALL_STUDY_MEMBERS',
    label: '스터디원 모두',
    description: '제출 대상이 아닌 멤버도\n서로의 제출물을 볼 수 있어요.',
  },
  {
    value: 'LEADER_ONLY',
    label: '제출한 사람과 리더만',
    description: '다른 스터디원에게는\n제출물이 보이지 않아요.',
  },
] as const;

interface AssignmentVisibilityStepProps {
  submissionVisibility: SubmissionVisibility;
  changeSubmissionVisibility: (visibility: SubmissionVisibility) => void;
  isSubmitting: boolean;
  onSubmit: () => void;
  errorText?: string;
}

const optionGroupStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSObject;

export default function AssignmentVisibilityStep({
  submissionVisibility,
  changeSubmissionVisibility,
  isSubmitting,
  onSubmit,
  errorText,
}: AssignmentVisibilityStepProps) {
  return (
    <div css={stepStyle}>
      <div css={questionStyle}>
        <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
          4 / 4
        </Paragraph>
        <Heading level={2} variant="title">
          제출물을 누구에게 보여줄까요?
        </Heading>
        <Paragraph variant="paragraph" css={{ color: tokens.text.secondary }}>
          제출 인원과 별개로 볼 수 있는 범위를 정해요.
        </Paragraph>
      </div>
      <div role="radiogroup" aria-label="제출물 공개 범위" css={optionGroupStyle}>
        {visibilityOptions.map((option) => (
          <VisibilityOptionField
            label={option.label}
            description={option.description}
            value={option.value}
            onChange={changeSubmissionVisibility}
            isActive={submissionVisibility === option.value}
          />
        ))}
      </div>
      <Field.SubText errorText={errorText} />
      <div css={footerStyle}>
        <Button variant="brandSolid" size="large" onClick={onSubmit} disabled={isSubmitting}>
          과제 만들기
        </Button>
      </div>
    </div>
  );
}
