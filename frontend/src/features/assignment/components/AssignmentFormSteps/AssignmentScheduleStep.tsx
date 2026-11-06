import { stepStyle, questionStyle, footerStyle } from './assignmentFormSteps.styles';
import { useState } from 'react';
import CloseAtField from '../../../../shared/widgets/CloseAtField';
import ReminderField from '../../../../shared/widgets/ReminderField';
import { validateAssignmentSchedule } from '../../validation';
import { Heading, Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';
import Button from '../../../../shared/ui/Button';

interface AssignmentScheduleStepProps {
  closeAt: string;
  remindAts: string[];
  fieldErrors: Record<string, string>;
  setCloseAt: (value: string) => void;
  setRemindAts: (values: string[]) => void;
  onNext: () => void;
}

export default function AssignmentScheduleStep({
  closeAt,
  remindAts,
  fieldErrors,
  setCloseAt,
  setRemindAts,
  onNext,
}: AssignmentScheduleStepProps) {
  const [hasAttemptedNext, setHasAttemptedNext] = useState(false);
  const validationErrors = validateAssignmentSchedule(closeAt, remindAts);
  const canProceed = Object.keys(validationErrors).length === 0;
  return (
    <div css={[stepStyle, { gap: tokens.spacing[6] }]}>
      <div css={questionStyle}>
        <Paragraph variant="paragraph" css={{ color: tokens.text.brand }}>
          2 / 4
        </Paragraph>
        <Heading level={2} variant="title">
          과제 일정을 정해주세요
        </Heading>
        <Paragraph variant="paragraph" css={{ color: tokens.text.secondary }}>
          마감과 리마인드 시각을 설정해 주세요.
        </Paragraph>
      </div>
      <CloseAtField
        id="assignment-close-at"
        value={closeAt}
        onChange={setCloseAt}
        errorText={(hasAttemptedNext ? validationErrors.closeAt : undefined) ?? fieldErrors.closeAt}
        isRequired
      />
      <ReminderField
        id="assignment-reminders"
        label="리마인드 시각"
        value={remindAts}
        onChange={setRemindAts}
        helpText="리마인드가 필요하면 알림 시각을 추가해 주세요."
        errorText={
          (hasAttemptedNext ? validationErrors.remindAts : undefined) ?? fieldErrors.remindAts
        }
      />
      <div css={[footerStyle, { paddingTop: 0 }]}>
        <Button
          variant="brandSolid"
          size="large"
          disabled={!closeAt}
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
