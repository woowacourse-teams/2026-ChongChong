import { useCallback, useState } from 'react';
import type { ReactNode } from 'react';
import Button from '../Button';
import BottomSheet from './BottomSheet';
import DatePicker from './DatePicker';
import TimePicker from './TimePicker';
import { clampToMinimumTime, getMinimumDateTime } from './dateTimePicker.utils';

type PickerStep = 'date' | 'time';

export interface DateTimePickerProps {
  id?: string;
  value?: Date;
  minDate?: Date;
  title?: string;
  triggerLabel?: ReactNode;
  triggerVariant?: 'brandOutline' | 'neutralOutline';
  onChange: (value: Date) => void;
}

export default function DateTimePicker({
  id,
  value,
  minDate,
  title = '리마인드 시각 설정',
  triggerLabel = '리마인드 시각 설정',
  triggerVariant = 'neutralOutline',
  onChange,
}: DateTimePickerProps) {
  const [minimumDateTime, setMinimumDateTime] = useState(() => getMinimumDateTime(minDate));
  const [draftValue, setDraftValue] = useState(() =>
    clampToMinimumTime(value ?? minimumDateTime, minimumDateTime),
  );
  const [step, setStep] = useState<PickerStep>('date');
  const [isOpen, setIsOpen] = useState(false);

  const closePicker = useCallback(() => setIsOpen(false), []);

  const openPicker = () => {
    const nextMinimumDateTime = getMinimumDateTime(minDate);

    setMinimumDateTime(nextMinimumDateTime);
    setDraftValue(clampToMinimumTime(value ?? nextMinimumDateTime, nextMinimumDateTime));
    setStep('date');
    setIsOpen(true);
  };

  const complete = () => {
    const completedValue = clampToMinimumTime(draftValue, getMinimumDateTime(minDate));

    onChange(completedValue);
    closePicker();
  };

  return (
    <>
      <Button
        id={id}
        variant={triggerVariant}
        size="large"
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        onClick={openPicker}
      >
        {triggerLabel}
      </Button>

      <BottomSheet
        open={isOpen}
        title={title}
        actionLabel={step === 'date' ? '다음' : '완료'}
        onAction={step === 'date' ? () => setStep('time') : complete}
        onClose={closePicker}
      >
        {step === 'date' ? (
          <DatePicker value={draftValue} minDate={minimumDateTime} onChange={setDraftValue} />
        ) : (
          <TimePicker value={draftValue} minDate={minimumDateTime} onChange={setDraftValue} />
        )}
      </BottomSheet>
    </>
  );
}
