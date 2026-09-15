import { router } from 'expo-router';
import { ScrollView, View } from 'react-native';
import { AssignmentCard } from '../../features/assignments/AssignmentCard';
import { useAssignments } from '../../features/assignments/AssignmentProvider';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { Button, EmptyState } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';

export default function AssignmentsScreen() {
  const { assignments } = useAssignments();
  const { selectedStudy } = useEntryScenario();
  const leader = selectedStudy?.role === 'leader';
  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: t.color.background }}
      contentContainerStyle={{
        width: '100%',
        maxWidth: t.size.content,
        alignSelf: 'center',
        padding: t.space.gutter,
        gap: t.space.md,
      }}
    >
      {assignments.length ? (
        assignments.map((assignment) => (
          <AssignmentCard
            key={assignment.id}
            assignment={assignment}
            leader={leader}
          />
        ))
      ) : (
        <View style={{ paddingTop: 140 }}>
          <EmptyState title="아직 과제가 없어요" compact />
        </View>
      )}
      {leader && (
        <Button
          label="과제 추가하기"
          onPress={() => router.push('/assignments/edit')}
        />
      )}
    </ScrollView>
  );
}
