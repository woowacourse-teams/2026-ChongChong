import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';
import type { Member } from '../../../member/types';
import { SelectAllSubmittersField, SelectSubmitterField } from './SubmitterField';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import Button from '../../../../shared/ui/Button';
import type { CSSObject } from '@emotion/react';

interface AssignmentSubmittersStepProps {
  members: Member[];
  selectedIds: number[];
  isSubmitting: boolean;
  isMembersPending: boolean;
  membersError: Error | null;
  setSubmitterIds: (ids: number[]) => void;
  onNext: () => void;
}

const checkboxContainerStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[2],
} satisfies CSSObject;

export default function AssignmentSubmittersStep({
  members,
  selectedIds,
  isSubmitting,
  isMembersPending,
  membersError,
  setSubmitterIds,
  onNext,
}: AssignmentSubmittersStepProps) {
  const isAllSelected = members.length > 0 && selectedIds.length === members.length;

  function onChangeSubmitters(memberId: number) {
    const removeToSelected = selectedIds.filter((id) => id !== memberId);
    const addToSelected = [...selectedIds, memberId];
    setSubmitterIds(selectedIds.includes(memberId) ? removeToSelected : addToSelected);
  }

  function onToggleAllSelect() {
    const allSelect = members.map(({ id }) => id);
    setSubmitterIds(isAllSelected ? [] : allSelect);
  }

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
      <SelectAllSubmittersField
        checked={isAllSelected}
        disabled={isSubmitting}
        selectedCount={selectedIds.length}
        onChange={onToggleAllSelect}
      />
      <div css={checkboxContainerStyle}>
        {members.map((member) => (
          <SelectSubmitterField
            key={member.id}
            member={member}
            isChecked={selectedIds.includes(member.id)}
            onChangeSubmitters={onChangeSubmitters}
          />
        ))}
      </div>
      <div css={footerStyle}>
        <Button
          variant="brandSolid"
          size="large"
          disabled={
            isSubmitting || isMembersPending || Boolean(membersError) || selectedIds.length === 0
          }
          onClick={onNext}
        >
          다음
        </Button>
      </div>
    </div>
  );
}
