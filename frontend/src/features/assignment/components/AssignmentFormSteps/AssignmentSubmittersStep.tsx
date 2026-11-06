import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';
import { useState } from 'react';
import type { Member } from '../../../member/types';
import { Field } from '../../../../shared/ui/inputs/Field';
import Loading from '../../../../shared/ui/Loading';
import checkedIcon from '../../assets/submitter-checked.svg';
import { validateAssignmentSubmitters } from '../../validation';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import Button from '../../../../shared/ui/Button';

const checkboxStyle = {
  appearance: 'none',
  width: 20,
  height: 20,
  flexShrink: 0,
  margin: 0,
  border: `2px solid ${tokens.color.optionGrayBackgroundIcon}`,
  borderRadius: 4,
  cursor: 'pointer',
  '&:checked': { background: `url(${checkedIcon}) center / 20px 20px no-repeat`, border: 0 },
  '&:focus-visible': { outline: `2px solid ${tokens.text.primary}`, outlineOffset: 2 },
} as const;

interface AssignmentSubmittersStepProps {
  members: Member[];
  selectedIds: number[];
  isSubmitting: boolean;
  isMembersPending: boolean;
  membersError: Error | null;
  errorText?: string;
  onRetry: () => void;
  setSubmitterIds: (ids: number[]) => void;
  onNext: () => void;
}

export default function AssignmentSubmittersStep({
  members,
  selectedIds,
  isSubmitting,
  isMembersPending,
  membersError,
  errorText,
  onRetry,
  setSubmitterIds,
  onNext,
}: AssignmentSubmittersStepProps) {
  const [hasAttemptedNext, setHasAttemptedNext] = useState(false);
  const validationErrors = validateAssignmentSubmitters(selectedIds);
  const canProceed = Object.keys(validationErrors).length === 0;
  const allSelected = members.length > 0 && selectedIds.length === members.length;

  return (
    <div css={stepStyle}>
      <div css={questionStyle}>
        <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
          3 / 4
        </Paragraph>
        <Heading level={2} variant="title">
          누가 과제를 제출하나요?
        </Heading>
        <Paragraph variant="paragraph" css={{ color: tokens.text.secondary }}>
          본인을 포함해 제출할 멤버를 선택해 주세요.
        </Paragraph>
      </div>
      {isMembersPending ? (
        <Loading />
      ) : membersError ? (
        <>
          <Field.ErrorText role="alert">{membersError.message}</Field.ErrorText>
          <Button variant="neutralOutline" size="large" onClick={onRetry}>
            다시 불러오기
          </Button>
        </>
      ) : (
        <>
          <label
            css={{
              display: 'flex',
              alignItems: 'center',
              gap: tokens.spacing[3],
              cursor: 'pointer',
            }}
          >
            <input
              type="checkbox"
              aria-label="전체 선택"
              checked={allSelected}
              disabled={isSubmitting}
              onChange={() => setSubmitterIds(allSelected ? [] : members.map(({ id }) => id))}
              css={checkboxStyle}
            />
            <Paragraph variant="paragraph" css={{ flex: 1 }}>
              전체 선택
            </Paragraph>
            <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
              {selectedIds.length}명 선택
            </Paragraph>
          </label>
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              gap: tokens.spacing[2],
            }}
          >
            {members.map((member) => (
              <label
                key={member.id}
                css={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: tokens.spacing[3],
                  minHeight: 68,
                  padding: `0 ${tokens.spacing[4]}`,
                  border: tokens.border.default,
                  borderRadius: tokens.radius.md,
                  cursor: 'pointer',
                }}
              >
                <span
                  aria-hidden="true"
                  css={{
                    display: 'grid',
                    placeItems: 'center',
                    width: 28,
                    height: 28,
                    flexShrink: 0,
                    borderRadius: tokens.radius.full,
                    background: tokens.color.optionSub6,
                    fontSize: tokens.fontSize[12],
                  }}
                >
                  {member.name.slice(0, 1)}
                </span>
                <Paragraph variant="subtitle" css={{ flex: 1 }}>
                  {member.name}
                  {member.role === 'LEADER' ? ' (나)' : ''}
                </Paragraph>
                <input
                  type="checkbox"
                  aria-label={`${member.name}${member.role === 'LEADER' ? ' (나)' : ''}`}
                  checked={selectedIds.includes(member.id)}
                  disabled={isSubmitting}
                  css={checkboxStyle}
                  onChange={() =>
                    setSubmitterIds(
                      selectedIds.includes(member.id)
                        ? selectedIds.filter((id) => id !== member.id)
                        : [...selectedIds, member.id],
                    )
                  }
                />
              </label>
            ))}
          </div>
          <Field.SubText
            errorText={(hasAttemptedNext ? validationErrors.submitterIds : undefined) ?? errorText}
            helpText={members.length === 0 ? '제출할 스터디원이 없습니다.' : undefined}
          />
        </>
      )}
      <div css={footerStyle}>
        <Button
          variant="brandSolid"
          size="large"
          disabled={
            isSubmitting || isMembersPending || Boolean(membersError) || selectedIds.length === 0
          }
          onClick={() => {
            setHasAttemptedNext(true);
            if (!canProceed) return;
            setSubmitterIds(selectedIds);
            onNext();
          }}
        >
          다음
        </Button>
      </div>
    </div>
  );
}
