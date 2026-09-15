import { router } from 'expo-router';
import { Image, Pressable, StyleSheet, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { noticeLayout as n } from './layout';
import type { Notice } from './model';
import { NoticeIcon } from './NoticeIcon';
import { previewNow } from './previewClock';
export function NoticeCard({
  notice,
  leader,
}: {
  readonly notice: Notice;
  readonly leader: boolean;
}) {
  const read = notice.recipients.filter((item) => item.readAt).length;
  const self = notice.recipients.find((item) => item.id === 'self');
  const filled = leader
    ? read === notice.recipients.length
    : !!self && !self.readAt;
  const label = leader
    ? filled
      ? '모두 읽음'
      : `${read}/${notice.recipients.length} 읽음`
    : !self
      ? '미대상'
      : self.readAt
        ? '읽음'
        : '읽지 않음';
  const nextReminder = notice.reminders
    .map((value) => new Date(value).getTime())
    .filter((value) => value > previewNow)
    .sort((a, b) => a - b)[0];
  const minutes = nextReminder
    ? Math.ceil((nextReminder - previewNow) / 60000)
    : 0;
  const reminderLabel =
    minutes < 60
      ? `${minutes}분 뒤 리마인드`
      : minutes < 1440
        ? `${Math.floor(minutes / 60)}시간 뒤 리마인드`
        : `${Math.floor(minutes / 1440)}일 뒤 리마인드`;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${notice.title}, ${label}`}
      onPress={() =>
        router.push({ pathname: '/notices/[id]', params: { id: notice.id } })
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
            {label}
          </AppText>
        </View>
        {leader && minutes > 0 && (
          <View style={[styles.badge, styles.reminder]}>
            <NoticeIcon name="clock" size={12} />
            <AppText variant="caption">{reminderLabel}</AppText>
          </View>
        )}
      </View>
      <View style={styles.content}>
        <View style={styles.copy}>
          <AppText variant="large" numberOfLines={1}>
            {notice.title}
          </AppText>
          <AppText tone="tertiary" numberOfLines={2}>
            {notice.body.replaceAll('\n', ' ')}
          </AppText>
          <AppText variant="caption" tone="tertiary">
            {notice.timeLabel}
          </AppText>
        </View>
        {notice.images[0] && (
          <Image
            source={
              notice.images[0].id === 'figma-dog'
                ? require('../../../assets/images/notices/thumbnail.png')
                : notice.images[0].source
            }
            style={styles.image}
            accessibilityLabel="공지 첨부 이미지"
          />
        )}
      </View>
    </Pressable>
  );
}
const styles = StyleSheet.create({
  card: {
    minHeight: n.cardHeight,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.lg,
    padding: t.space.gutter - t.size.line,
    gap: t.space.md,
  },
  badges: { flexDirection: 'row', gap: t.space.sm },
  badge: {
    paddingHorizontal: t.space.sm,
    paddingVertical: n.badgeVertical,
    borderRadius: t.radius.pill,
  },
  reminder: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.xs,
    backgroundColor: t.color.mutedBackground,
  },
  content: { flexDirection: 'row', gap: t.space.md },
  copy: { flex: 1, gap: t.space.xs },
  image: {
    width: n.thumbnailSize,
    height: n.thumbnailSize,
    borderRadius: t.radius.md,
  },
});
