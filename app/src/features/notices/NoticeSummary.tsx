import { Pressable, StyleSheet, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { noticeLayout as n } from './layout';
import type { Notice } from './model';
import { NoticeIcon } from './NoticeIcon';
import { useNotices } from './NoticeProvider';
export function NoticeSummary({ notice }: { readonly notice: Notice }) {
  const { remind } = useNotices();
  const read = notice.recipients.filter((item) => item.readAt);
  const unread = notice.recipients.filter((item) => !item.readAt);
  return (
    <View style={styles.summary}>
      <View style={styles.progressSection}>
        <AppText variant="large">확인 현황</AppText>
        <View style={styles.count}>
          <AppText strong style={styles.number}>
            {read.length}
          </AppText>
          <AppText tone="tertiary">/ {notice.recipients.length}명</AppText>
        </View>
        <View style={styles.track}>
          <View
            style={[
              styles.progress,
              {
                width: `${notice.recipients.length ? (read.length / notice.recipients.length) * 100 : 0}%`,
              },
            ]}
          />
        </View>
      </View>
      {[
        { title: '확인', items: read },
        { title: '미확인', items: unread },
      ].map(({ title, items }) => (
        <View key={title} style={styles.section}>
          <View style={styles.row}>
            <AppText variant="large">
              {title} {items.length}명
            </AppText>
            {title === '미확인' && items.length > 0 && (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="모두에게 알리기"
                onPress={() => remind(notice.id)}
              >
                <AppText style={styles.green}>모두에게 알리기</AppText>
              </Pressable>
            )}
          </View>
          {items.map((item) => (
            <View key={item.id} style={styles.card}>
              <NoticeIcon name="profile" size={n.avatarSize} />
              <View style={styles.person}>
                <AppText>{item.name}</AppText>
                {(item.readAt || item.remindedAt) && (
                  <AppText variant="compact" tone="tertiary">
                    {item.readAt
                      ? `${item.readAt} 확인`
                      : `${item.remindedAt} 보냄`}
                  </AppText>
                )}
              </View>
              {!item.readAt && (!item.remindedAt || item.reminderAvailable) && (
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`${item.name}에게 알리기`}
                  onPress={() => remind(notice.id, item.id)}
                  style={styles.send}
                >
                  <NoticeIcon name="send" size={16} />
                  <AppText style={styles.green}>알리기</AppText>
                </Pressable>
              )}
            </View>
          ))}
        </View>
      ))}
    </View>
  );
}
const styles = StyleSheet.create({
  summary: {
    gap: t.space.xxl,
    paddingTop: n.summaryTop,
    paddingBottom: t.space.xl,
  },
  progressSection: { gap: t.space.sm },
  count: { flexDirection: 'row', alignItems: 'baseline', gap: t.space.xs },
  number: {
    fontSize: n.countFontSize,
    lineHeight: n.countLineHeight,
    color: t.color.brand,
  },
  track: {
    height: t.space.sm,
    borderRadius: t.radius.pill,
    backgroundColor: t.color.mutedBackground,
    overflow: 'hidden',
  },
  progress: {
    height: t.space.sm,
    backgroundColor: t.color.brand,
    borderRadius: t.radius.pill,
  },
  section: { gap: t.space.sm },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  green: { color: t.color.brand },
  card: {
    minHeight: n.recipientHeight,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
    padding: t.space.gutter - t.size.line,
    paddingBottom: n.recipientBottomPadding,
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.md,
  },
  person: { flex: 1, gap: n.recipientTextGap },
  send: { flexDirection: 'row', gap: t.space.xs, alignItems: 'center' },
});
