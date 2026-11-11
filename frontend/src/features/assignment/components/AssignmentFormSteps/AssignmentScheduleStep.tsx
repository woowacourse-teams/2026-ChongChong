import AssignmentStepLayout from './AssignmentStepLayout';
import { useState } from 'react';
import CloseAtField from '../../../../shared/widgets/CloseAtField';
import ReminderField from '../../../../shared/widgets/ReminderField';
import { validateAssignmentSchedule } from '../../validation';
import Button from '../../../../shared/ui/Button';

interface AssignmentScheduleStepProps {
  step: number;
  totalSteps: number;
  closeAt: string;
  remindAts: string[];
  fieldErrors: Record<string, string>;
  setCloseAt: (value: string) => void;
  setRemindAts: (values: string[]) => void;
  onNext: () => void;
}

export default function AssignmentScheduleStep({
  step,
  totalSteps,
  closeAt,
  remindAts,
  fieldErrors,
  setCloseAt,
  setRemindAts,
  onNext,
}: AssignmentScheduleStepProps) {
  const [validationErrors, setValidationErrors] = useState<
    Partial<Record<'closeAt' | 'remindAts', string>>
  >({});
  const errors = { ...fieldErrors, ...validationErrors };

  function handleCloseAtChange(value: string) {
    setCloseAt(value);
    if (errors.closeAt) {
      const { closeAt: error } = validateAssignmentSchedule(value, remindAts);
      setValidationErrors((current) => ({ ...current, closeAt: error }));
    }
  }

  function handleRemindAtsChange(values: string[]) {
    setRemindAts(values);
    if (errors.remindAts) {
      const { remindAts: error } = validateAssignmentSchedule(closeAt, values);
      setValidationErrors((current) => ({ ...current, remindAts: error }));
    }
  }

  function handleNext() {
    const nextErrors = validateAssignmentSchedule(closeAt, remindAts);
    setValidationErrors({ closeAt: nextErrors.closeAt, remindAts: nextErrors.remindAts });
    if (Object.keys(nextErrors).length === 0) onNext();
  }
  return (
    <AssignmentStepLayout
      step={step}
      totalSteps={totalSteps}
      title="과제 일정을 정해주세요"
      subTitle="마감과 리마인드 시각을 설정해 주세요."
      footer={
        <Button variant="brandSolid" size="large" disabled={!closeAt} onClick={handleNext}>
          다음
        </Button>
      }
    >
      <CloseAtField
        id="assignment-close-at"
        value={closeAt}
        onChange={handleCloseAtChange}
        errorText={errors.closeAt}
        isRequired
      />
      <ReminderField
        id="assignment-reminders"
        label="리마인드 시각"
        value={remindAts}
        onChange={handleRemindAtsChange}
        helpText="리마인드가 필요하면 알림 시각을 추가해 주세요."
        errorText={errors.remindAts}
      />
    </AssignmentStepLayout>
  );
}
