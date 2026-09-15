import { ScrollView, StyleSheet, View } from 'react-native';
import { AppText, EmptyState } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { useActivity } from '../activity/ActivityProvider';
import type { StudyActivity } from '../activity/fixtures';
import { useEntryScenario } from '../entry/EntryProvider';
import { ActivityRow } from './ActivityRow';

export function ActivityList({
  kind,
}: {
  readonly kind: StudyActivity['kind'];
}) {
  const { activities } = useActivity();
  const { selectedStudy } = useEntryScenario();
  const title = kind === 'notice' ? '공지' : '과제';
  const items = activities.filter((activity) => activity.kind === kind);
  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <AppText variant="large">{title}</AppText>
      <View style={styles.list}>
        {items.length === 0 ? (
          <EmptyState title={`아직 등록된 ${title}가 없어요`} compact />
        ) : (
          items.map((activity) => (
            <ActivityRow
              key={activity.id}
              activity={activity}
              leader={selectedStudy?.role === 'leader'}
            />
          ))
        )}
      </View>
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: t.color.background },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    padding: t.space.gutter,
    paddingBottom: t.space.section,
    gap: t.space.lg,
  },
  list: { gap: t.space.md },
});
