import { router } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { noticeLayout as n } from '../../features/notices/layout';
import { NoticeCard } from '../../features/notices/NoticeCard';
import { useNotices } from '../../features/notices/NoticeProvider';
import { Button, EmptyState } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
export default function NoticesScreen() {
  const { notices } = useNotices();
  const { selectedStudy } = useEntryScenario();
  const leader = selectedStudy?.role === 'leader';
  return (
    <View style={styles.page}>
      <ScrollView contentContainerStyle={styles.content}>
        {notices.length ? (
          notices.map((notice) => (
            <NoticeCard key={notice.id} notice={notice} leader={leader} />
          ))
        ) : (
          <View style={styles.empty}>
            <EmptyState title="아직 공지가 없어요" compact />
          </View>
        )}
        {leader && (
          <Button
            label="공지 작성하기"
            onPress={() => router.push('/notices/edit')}
          />
        )}
      </ScrollView>
    </View>
  );
}
const styles = StyleSheet.create({
  empty: { paddingTop: n.emptyTop },
  page: { flex: 1, backgroundColor: t.color.background },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    padding: t.space.gutter,
    gap: t.space.md,
  },
});
