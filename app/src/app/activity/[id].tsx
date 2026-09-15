import { Redirect, Stack, useLocalSearchParams } from 'expo-router';
import { useActivity } from '../../features/activity/ActivityProvider';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { AppText } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';

export default function ActivityDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { activities } = useActivity();
  const { selectedStudy } = useEntryScenario();
  const activity = activities.find((item) => item.id === id);
  if (activity?.kind === 'notice')
    return <Redirect href={{ pathname: '/notices/[id]', params: { id } }} />;
  return (
    <Screen>
      <Stack.Screen
        options={{ title: activity?.kind === 'assignment' ? '과제' : '공지' }}
      />
      <AppText variant="caption" tone="tertiary">
        {selectedStudy?.name}
      </AppText>
      <AppText variant="subtitle">
        {activity?.title ?? '내용을 찾을 수 없어요'}
      </AppText>
      {activity && <AppText>{activity.body}</AppText>}
    </Screen>
  );
}
