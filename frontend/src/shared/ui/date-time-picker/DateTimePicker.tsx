import { useCallback, useId, useState } from 'react';
import type { ReactNode } from 'react';
import type { CSSObject } from '@emotion/react';
import Button from '../Button';
import BottomSheet from '../BottomSheet';
import DatePicker from './DatePicker';
import TimePicker from './TimePicker';
import { actionStyle, headerStyle } from './dateTimePicker.styles';
import { clampToMinimumTime, getMinimumDateTime } from './dateTimePicker.utils';
import { Heading } from '../Typography';

type PickerStep = 'date' | 'time';

export interface DateTimePickerProps {
  id?: string;
  value?: Date;
  minDate?: Date;
  title?: string;
  triggerLabel?: ReactNode;
  triggerVariant?: 'brandOutline' | 'neutralOutline';
  triggerStyle?: CSSObject;
  disabled?: boolean;
  onChange: (value: Date) => void;
}

export default function DateTimePicker({
  id,
  value,
  minDate,
  title = '리마인드 시각 설정',
  triggerLabel = '리마인드 시각 설정',
  triggerVariant = 'neutralOutline',
  triggerStyle,
  disabled = false,
  onChange,
}: DateTimePickerProps) {
  const titleId = useId();
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
        css={triggerStyle}
        disabled={disabled}
        variant={triggerVariant}
        size="large"
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        onClick={openPicker}
      >
        {triggerLabel}
      </Button>

      <BottomSheet open={isOpen} aria-labelledby={titleId} onClose={closePicker}>
        <header css={headerStyle}>
          <Heading level={2} id={titleId} variant="title">
            {title}
          </Heading>
          <button
            type="button"
            css={actionStyle}
            onClick={step === 'date' ? () => setStep('time') : complete}
          >
            {step === 'date' ? '다음' : '완료'}
          </button>
        </header>
        {step === 'date' ? (
          <DatePicker value={draftValue} minDate={minimumDateTime} onChange={setDraftValue} />
        ) : (
          <TimePicker value={draftValue} minDate={minimumDateTime} onChange={setDraftValue} />
        )}
      </BottomSheet>
    </>
  );
}
