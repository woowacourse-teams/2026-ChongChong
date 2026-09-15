import { router } from 'expo-router';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { NoticeIcon } from '../notices/NoticeIcon';
import { previewNow } from '../notices/previewClock';
import { LinkIcon } from './LinkIcon';
import type { Assignment } from './model';

export function AssignmentCard({
  assignment,
  leader,
}: {
  readonly assignment: Assignment;
  readonly leader: boolean;
}) {
  const self = assignment.members.find((person) => person.id === 'self');
  const count = assignment.members.filter((person) => person.submission).length;
  const status = leader
    ? `${count}/${assignment.members.length} 제출`
    : !self
      ? '제출 대상 아님'
      : self.submission
        ? '제출 완료'
        : '미제출';
  const filled = !leader && !!self && !self.submission;
  const next = assignment.reminders
    .map((value) => new Date(value).getTime())
    .filter((value) => value > previewNow)
    .sort((a, b) => a - b)[0];
  const minutes = next ? Math.ceil((next - previewNow) / 60000) : 0;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${assignment.title}, ${status}`}
      onPress={() =>
        router.push({
          pathname: '/assignments/[id]',
          params: { id: assignment.id },
        })
      }
      style={({ pressed }) => [
        styles.card,
        pressed && { opacity: t.pressedOpacity },
      ]}
    >
      <View style={styles.badges}>
        <View
          style={[
            styles.badge,
            { backgroundColor: filled ? t.color.brand : t.color.brandSubtle },
          ]}
        >
          <AppText
            variant="caption"
            style={{ color: filled ? t.color.onBrand : t.color.brand }}
          >
            {status}
          </AppText>
        </View>
        {leader && minutes > 0 && (
          <View style={[styles.badge, styles.reminder]}>
            <NoticeIcon name="clock" size={12} />
            <AppText variant="caption">
              {minutes < 60
                ? `${minutes}분`
                : minutes < 1440
                  ? `${Math.floor(minutes / 60)}시간`
                  : `${Math.floor(minutes / 1440)}일`}{' '}
              뒤 리마인드
            </AppText>
          </View>
        )}
      </View>
      <View style={styles.copy}>
        <AppText variant="large" numberOfLines={1}>
          {assignment.title}
        </AppText>
        <AppText tone="tertiary" numberOfLines={2} style={styles.description}>
          {assignment.body}
        </AppText>
        <View>
          <AppText variant="caption" style={{ color: t.color.brand }}>
            {Number(assignment.deadline.slice(0, 4))}년{' '}
            {Number(assignment.deadline.slice(5, 7))}월{' '}
            {Number(assignment.deadline.slice(8, 10))}일{' '}
            {assignment.deadline.slice(11)} 마감
          </AppText>
          <View style={styles.link}>
            <LinkIcon size={13} />
            <AppText variant="caption" tone="tertiary">
              정리 글 링크로 제출
            </AppText>
          </View>
        </View>
      </View>
    </Pressable>
  );
}
const styles = StyleSheet.create({
  card: {
    minHeight: 184,
    padding: t.space.gutter - t.size.line,
    gap: t.space.md,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.lg,
  },
  badges: { flexDirection: 'row', gap: t.space.sm },
  badge: {
    paddingHorizontal: t.space.sm,
    paddingVertical: 3,
    borderRadius: t.radius.pill,
  },
  reminder: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.xs,
    backgroundColor: t.color.mutedBackground,
  },
  copy: { gap: t.space.xs },
  description: {
    color: t.color.tertiary,
    ...Platform.select({
      web: { wordBreak: 'keep-all', overflowWrap: 'anywhere' },
      default: {},
    }),
  },
  link: { flexDirection: 'row', alignItems: 'center', gap: t.space.xs },
});
