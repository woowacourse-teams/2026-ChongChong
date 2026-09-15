import { router, useLocalSearchParams } from 'expo-router';
import { View } from 'react-native';
import { SubmissionContent } from '../../features/assignments/AssignmentDetail';
import { useAssignments } from '../../features/assignments/AssignmentProvider';
import { assignmentStyles as s } from '../../features/assignments/styles';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { AppHeader } from '../../ui/AppHeader';
import { AppText } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
export default function SubmissionScreen() {
  const { id, memberId } = useLocalSearchParams<{
    id: string;
    memberId: string;
  }>();
  const { assignments } = useAssignments();
  const { selectedStudy } = useEntryScenario();
  const assignment = assignments.find((item) => item.id === id);
  const person = assignment?.members.find((item) => item.id === memberId);
  const allowed =
    selectedStudy?.role === 'leader' ||
    assignment?.visibility === 'public' ||
    memberId === 'self';
  return (
    <View style={s.page}>
      <AppHeader
        title="제출물"
        onBack={() =>
          router.canGoBack()
            ? router.back()
            : router.replace('/study/assignments')
        }
      />
      <Screen>
        {allowed && person?.submission ? (
          <>
            <AppText variant="title" strong>
              {person.name}의 제출물
            </AppText>
            <SubmissionContent submission={person.submission} />
          </>
        ) : (
          <AppText>제출물을 확인할 수 없어요</AppText>
        )}
      </Screen>
    </View>
  );
}
