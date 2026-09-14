import { useRouter } from 'expo-router';
import { Platform, Pressable, StyleSheet } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import type { StudyActivity } from '../activity/fixtures';
import { ActivityIcon } from './ActivityIcon';
import { homeLayout as layout } from './layout';

export function ActivityRow({
  activity,
  leader,
}: {
  readonly activity: StudyActivity;
  readonly leader: boolean;
}) {
  const router = useRouter();
  const status = leader
    ? `${activity.readCount}/${activity.totalCount} ${activity.kind === 'notice' ? '읽음' : '제출'}`
    : activity.kind === 'notice'
      ? activity.read
        ? '읽음'
        : '읽지 않음'
      : '미제출';
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${activity.title}, ${status}`}
      onPress={() =>
        router.push({ pathname: '/activity/[id]', params: { id: activity.id } })
      }
      style={({ pressed }) => [
        styles.row,
        pressed && { opacity: t.pressedOpacity },
      ]}
    >
      <ActivityIcon kind={activity.kind} />
      <AppText style={styles.title}>{activity.title}</AppText>
      <AppText variant="caption" tone="tertiary">
        {status}
      </AppText>
    </Pressable>
  );
}
const styles = StyleSheet.create({
  row: {
    minHeight: layout.rowHeight,
    paddingLeft: layout.rowPaddingLeft,
    paddingRight: layout.rowPaddingRight,
    paddingVertical: t.space.lg,
    borderRadius: t.radius.md,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    flexDirection: 'row',
    alignItems: 'center',
    gap: layout.rowContentGap,
  },
  title: {
    flex: 1,
    ...Platform.select({ web: { wordBreak: 'keep-all' }, default: {} }),
  },
});
