import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';
import { useActivity } from '../features/activity/ActivityProvider';
import { EntryIcon } from '../features/entry/EntryIcon';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { ActivityIcon } from '../features/home/ActivityIcon';
import {
  homeLayout,
  notificationLayout as layout,
} from '../features/home/layout';
import { useScenario } from '../mocks/ScenarioProvider';
import { AppText } from '../ui/primitives';
import { Screen } from '../ui/Screen';
import { tokens as t } from '../ui/tokens';

export default function NotificationsScreen() {
  const router = useRouter();
  const { notifications, markNotificationRead } = useActivity();
  const { studies, selectStudy } = useEntryScenario();
  const { setRole } = useScenario();
  return (
    <Screen>
      <View style={styles.list}>
        {notifications.map((notification) => (
          <Pressable
            key={notification.id}
            accessibilityRole="button"
            accessibilityLabel={`${notification.title}, ${notification.description}, ${notification.read ? '읽음' : '읽지 않음'}`}
            onPress={() => {
              const study = studies.find(
                (item) => item.id === notification.studyId,
              );
              if (!study) return;
              selectStudy(study.id);
              setRole(study.role);
              markNotificationRead(notification.id);
              router.push({
                pathname: '/activity/[id]',
                params: { id: notification.activityId },
              });
            }}
            style={({ pressed }) => [
              styles.row,
              pressed && { opacity: t.pressedOpacity },
            ]}
          >
            <View
              style={[
                styles.icon,
                notification.kind === 'reminder' && styles.reminder,
              ]}
            >
              {notification.kind === 'reminder' ? (
                <EntryIcon
                  name="bell"
                  size={homeLayout.iconSize}
                  color={t.color.onBrand}
                />
              ) : (
                <ActivityIcon kind="notice" />
              )}
            </View>
            <View style={styles.copy}>
              <AppText variant="large">{notification.title}</AppText>
              <AppText variant="small" tone="tertiary">
                {notification.description}
              </AppText>
              <AppText variant="caption" tone="tertiary">
                {notification.time}
              </AppText>
            </View>
            {!notification.read && (
              <View accessibilityLabel="읽지 않은 알림" style={styles.dot} />
            )}
          </Pressable>
        ))}
        {notifications.length === 0 && (
          <AppText tone="tertiary" style={styles.empty}>
            아직 알림이 없어요
          </AppText>
        )}
      </View>
    </Screen>
  );
}
const styles = StyleSheet.create({
  list: { gap: t.space.sm },
  row: {
    minHeight: layout.rowHeight,
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingTop: t.space.lg,
    paddingBottom: layout.bottomPadding,
    paddingHorizontal: t.space.lg,
    gap: t.space.md,
  },
  icon: {
    width: layout.iconContainerSize,
    height: layout.iconContainerSize,
    borderRadius: t.radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  reminder: { backgroundColor: t.color.brand },
  copy: { flex: 1, gap: t.space.xs },
  dot: {
    width: layout.unreadDotSize,
    height: layout.unreadDotSize,
    borderRadius: t.radius.pill,
    backgroundColor: t.color.brand,
    marginTop: layout.unreadDotTop,
  },
  empty: { textAlign: 'center', marginTop: 100 },
});
