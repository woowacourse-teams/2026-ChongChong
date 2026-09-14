import { Stack, useLocalSearchParams } from 'expo-router';
import { useEffect } from 'react';
import { useActivity } from '../../features/activity/ActivityProvider';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { AppText } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';

export default function ActivityDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { activities, markActivityRead } = useActivity();
  const { selectedStudy } = useEntryScenario();
  const activity = activities.find((item) => item.id === id);
  useEffect(() => {
    if (selectedStudy && activity?.kind === 'notice' && !activity.read)
      markActivityRead(selectedStudy.id, activity.id);
  }, [selectedStudy, activity, markActivityRead]);
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
