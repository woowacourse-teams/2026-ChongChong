import { useRef, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { noticeLayout as n } from './layout';
import { isFutureReminder, previewDate } from './previewClock';
import { styles } from './ReminderSheet.styles';
export function ReminderSheet({
  onClose,
  onSave,
}: {
  readonly onClose: () => void;
  readonly onSave: (value: string) => void;
}) {
  const timeScroll = useRef<ScrollView>(null);
  const [step, setStep] = useState<'date' | 'time'>('date');
  const [month, setMonth] = useState(new Date(2026, 7, 1));
  const [date, setDate] = useState<string>(previewDate);
  const [time, setTime] = useState('18:00');
  const validTime = isFutureReminder(`${date}T${time}`);
  const days = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const offset = month.getDay();
  const timeLabel = (value: string) => {
    const hour = Number(value.slice(0, 2));
    return `${hour < 12 ? '오전' : '오후'} ${hour % 12 || 12}:${value.slice(3)}`;
  };
  const times = Array.from(
    { length: 48 },
    (_, index) =>
      `${String(Math.floor(index / 2)).padStart(2, '0')}:${index % 2 ? '30' : '00'}`,
  );
  return (
    <Modal transparent onRequestClose={onClose} animationType="slide">
      <View style={styles.overlay}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="리마인드 설정 닫기"
          onPress={onClose}
          style={StyleSheet.absoluteFill}
        />
        <View style={styles.sheet} accessibilityViewIsModal>
          <View style={styles.handle} />
          <View style={styles.header}>
            <AppText variant="subtitle">리마인드 시각 설정</AppText>
            <Pressable
              accessibilityRole="button"
              disabled={step === 'time' && !validTime}
              onPress={() => {
                if (step === 'date') setStep('time');
                else if (validTime) onSave(`${date}T${time}`);
              }}
            >
              <AppText
                style={
                  step === 'time' && !validTime
                    ? { color: t.color.placeholder }
                    : styles.green
                }
              >
                {step === 'date' ? '다음' : '완료'}
              </AppText>
            </Pressable>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="날짜 다시 선택"
            onPress={() => setStep('date')}
            style={styles.selection}
          >
            <AppText>
              {step === 'date'
                ? `날짜 · ${date.replaceAll('-', '.')}`
                : `시간 · ${timeLabel(time)}`}
            </AppText>
          </Pressable>
          {step === 'date' ? (
            <View>
              <View style={styles.header}>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="이전 달"
                  disabled={month <= new Date(2026, 7, 1)}
                  onPress={() =>
                    setMonth(
                      new Date(month.getFullYear(), month.getMonth() - 1, 1),
                    )
                  }
                  style={styles.arrow}
                >
                  <AppText>‹</AppText>
                </Pressable>
                <AppText>
                  {month.getFullYear()}년 {month.getMonth() + 1}월
                </AppText>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="다음 달"
                  onPress={() =>
                    setMonth(
                      new Date(month.getFullYear(), month.getMonth() + 1, 1),
                    )
                  }
                  style={styles.arrow}
                >
                  <AppText>›</AppText>
                </Pressable>
              </View>
              <View style={styles.grid}>
                {['일', '월', '화', '수', '목', '금', '토'].map((label) => (
                  <View key={label} style={styles.day}>
                    <AppText variant="caption" tone="tertiary">
                      {label}
                    </AppText>
                  </View>
                ))}
                {Array.from({ length: offset + days }, (_, index) => {
                  const day = index - offset + 1;
                  const value = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                  return day < 1 ? (
                    <View key={`blank-${index}`} style={styles.day} />
                  ) : (
                    <Pressable
                      key={value}
                      accessibilityRole="button"
                      accessibilityLabel={value}
                      disabled={value < previewDate}
                      accessibilityState={{
                        selected: date === value,
                        disabled: value < previewDate,
                      }}
                      onPress={() => {
                        setDate(value);
                        if (!isFutureReminder(`${value}T${time}`))
                          setTime('18:00');
                      }}
                      style={styles.day}
                    >
                      <View
                        style={[
                          styles.dayCircle,
                          date === value && { backgroundColor: t.color.brand },
                        ]}
                      >
                        <AppText
                          variant="small"
                          style={{
                            color:
                              value < previewDate
                                ? t.color.placeholder
                                : date === value
                                  ? t.color.onBrand
                                  : index % 7 === 0
                                    ? t.color.danger
                                    : t.color.text,
                          }}
                        >
                          {day}
                        </AppText>
                      </View>
                    </Pressable>
                  );
                })}
              </View>
            </View>
          ) : (
            <ScrollView
              style={styles.times}
              ref={timeScroll}
              onContentSizeChange={() =>
                timeScroll.current?.scrollTo({
                  y: n.initialTimeRow * n.timeRowHeight + t.space.sm,
                  animated: false,
                })
              }
              contentContainerStyle={styles.timeContent}
            >
              {times.map((value) => (
                <Pressable
                  key={value}
                  accessibilityRole="button"
                  accessibilityLabel={timeLabel(value)}
                  disabled={!isFutureReminder(`${date}T${value}`)}
                  accessibilityState={{
                    selected: time === value,
                    disabled: !isFutureReminder(`${date}T${value}`),
                  }}
                  onPress={() => setTime(value)}
                  style={[time === value && styles.selection, styles.time]}
                >
                  <AppText
                    tone={time === value ? 'primary' : 'tertiary'}
                    style={
                      !isFutureReminder(`${date}T${value}`) && {
                        color: t.color.placeholder,
                      }
                    }
                  >
                    {timeLabel(value)}
                  </AppText>
                </Pressable>
              ))}
            </ScrollView>
          )}
        </View>
      </View>
    </Modal>
  );
}
