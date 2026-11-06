import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';
import type { Member } from '../../../member/types';
import type { SubmissionVisibility } from '../../types';
import { Field } from '../../../../shared/ui/inputs/Field';
import selectedIcon from '../../assets/visibility-selected.svg';
import unselectedIcon from '../../assets/visibility-unselected.svg';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import Button from '../../../../shared/ui/Button';

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
  selectedMembers: Member[];
  submissionVisibility: SubmissionVisibility;
  setSubmissionVisibility: (visibility: SubmissionVisibility) => void;
  isSubmitting: boolean;
  attachmentsReady: boolean;
  onSubmit: () => void;
  errorText?: string;
}

export default function AssignmentVisibilityStep({
  selectedMembers,
  submissionVisibility,
  setSubmissionVisibility,
  isSubmitting,
  onSubmit,
  attachmentsReady,
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
      <div
        role="radiogroup"
        aria-label="제출물 공개 범위"
        css={{
          display: 'flex',
          flexDirection: 'column',
          gap: tokens.spacing[4],
        }}
      >
        {visibilityOptions.map((option) => (
          <label
            key={option.value}
            css={{
              display: 'flex',
              flexDirection: 'column',
              gap: tokens.spacing[2],
              padding: tokens.spacing[5],
              border:
                submissionVisibility === option.value
                  ? `1.5px solid ${tokens.text.brand}`
                  : tokens.border.default,
              borderRadius: tokens.radius.md,
              background:
                submissionVisibility === option.value ? tokens.bg.brandSubtle : tokens.bg.default,
              cursor: 'pointer',
            }}
          >
            <span
              css={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: tokens.spacing[3],
              }}
            >
              <Paragraph variant="title">{option.label}</Paragraph>
              <input
                type="radio"
                name="submissionVisibility"
                aria-label={option.label}
                value={option.value}
                checked={submissionVisibility === option.value}
                disabled={isSubmitting}
                onChange={() => setSubmissionVisibility(option.value)}
                css={{
                  appearance: 'none',
                  margin: 0,
                  width: 24,
                  height: 24,
                  flexShrink: 0,
                  cursor: 'pointer',
                  background: `url(${submissionVisibility === option.value ? selectedIcon : unselectedIcon}) center / 24px 24px no-repeat`,
                  '&:focus-visible': {
                    outline: `2px solid ${tokens.text.primary}`,
                    outlineOffset: 2,
                  },
                }}
              />
            </span>
            <Paragraph
              variant="paragraph"
              css={{ whiteSpace: 'pre-line', color: tokens.text.secondary }}
            >
              {option.description}
            </Paragraph>
          </label>
        ))}
      </div>
      <Field.SubText errorText={errorText} />
      <Paragraph variant="footnote" css={{ color: tokens.text.secondary }}>
        제출 인원 {selectedMembers.length}명 ·{' '}
        {selectedMembers
          .map((member) => `${member.name}${member.role === 'LEADER' ? '(나)' : ''}`)
          .join(', ')}
      </Paragraph>
      {!attachmentsReady && (
        <Field.ErrorText role="alert">
          이미지 업로드가 완료되지 않았어요. 내용 단계에서 업로드 상태를 확인해주세요.
        </Field.ErrorText>
      )}
      <div css={footerStyle}>
        <Button
          variant="brandSolid"
          size="large"
          onClick={onSubmit}
          disabled={isSubmitting || !attachmentsReady}
        >
          과제 만들기
        </Button>
      </div>
    </div>
  );
}
