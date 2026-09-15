import { type ReactNode, useState } from 'react';
import { Pressable, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { NoticeIcon } from '../notices/NoticeIcon';
import { ReminderSheet } from '../notices/ReminderSheet';
import { formatAssignmentDate } from './model';
import { assignmentStyles as s } from './styles';
export function AssignmentSchedule({
  deadline,
  reminders,
  onDeadline,
  onReminders,
  children,
}: {
  readonly children?: ReactNode;
  readonly deadline: string;
  readonly reminders: readonly string[];
  readonly onDeadline: (value: string) => void;
  readonly onReminders: (values: readonly string[]) => void;
}) {
  const [sheet, setSheet] = useState<'deadline' | 'reminder' | null>(null);
  return (
    <>
      <View style={s.group}>
        <AppText variant="large">
          마감 시각 <AppText style={s.green}>*</AppText>
        </AppText>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="마감 시각 선택"
          onPress={() => setSheet('deadline')}
          style={s.file}
        >
          <AppText>
            {deadline
              ? formatAssignmentDate(deadline)
              : '마감 시각을 선택해주세요'}
          </AppText>
        </Pressable>
      </View>
      {children}
      <View style={s.group}>
        <AppText variant="large">리마인드 시각</AppText>
        {reminders.map((value) => (
          <View key={value} style={s.file}>
            <AppText style={{ flex: 1 }}>{formatAssignmentDate(value)}</AppText>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${formatAssignmentDate(value)} 제거`}
              onPress={() =>
                onReminders(reminders.filter((item) => item !== value))
              }
            >
              <NoticeIcon name="close" />
            </Pressable>
          </View>
        ))}
        <Pressable
          accessibilityRole="button"
          onPress={() => setSheet('reminder')}
          style={s.add}
        >
          <NoticeIcon name="plus" />
          <AppText>리마인드 등록하기</AppText>
        </Pressable>
        <AppText variant="caption" tone="tertiary">
          설정한 시각마다 미제출 스터디원에게 알림을 보내드릴게요
        </AppText>
      </View>
      {sheet && (
        <ReminderSheet
          title={sheet === 'deadline' ? '마감 시각 설정' : '리마인드 시각 설정'}
          onClose={() => setSheet(null)}
          onSave={(value) => {
            if (sheet === 'deadline') onDeadline(value);
            else if (!reminders.includes(value))
              onReminders([...reminders, value]);
            setSheet(null);
          }}
        />
      )}
    </>
  );
}
