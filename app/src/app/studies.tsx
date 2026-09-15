import { useRouter } from 'expo-router';
import { Image, Pressable, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useActivity } from '../features/activity/ActivityProvider';
import { entryAssets } from '../features/entry/assets';
import { EntryIcon } from '../features/entry/EntryIcon';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { useNotices } from '../features/notices/NoticeProvider';
import { StudyCard } from '../features/studies/StudyCard';
import { useScenario } from '../mocks/ScenarioProvider';
import { AppText, Button, EmptyState } from '../ui/primitives';
import { Screen } from '../ui/Screen';
import { tokens as t } from '../ui/tokens';

export default function StudiesScreen() {
  const { getNotices } = useNotices();
  const router = useRouter();
  const { studies, selectStudy } = useEntryScenario();
  const { unreadCount } = useActivity();
  const { setRole } = useScenario();
  return (
    <>
      <SafeAreaView edges={['top', 'left', 'right']} style={styles.safe}>
        <View style={styles.header}>
          <Image
            source={entryAssets.wordmark}
            style={styles.wordmark}
            resizeMode="contain"
            accessibilityLabel="총총"
          />
          <View style={styles.actions}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="알림"
              onPress={() => router.push('/notifications')}
              style={styles.touch}
            >
              <EntryIcon name="bell" />
              {unreadCount > 0 && <View style={styles.notificationDot} />}
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="마이페이지"
              onPress={() => router.push('/account')}
              style={styles.touch}
            >
              <EntryIcon name="menu" size={32} />
            </Pressable>
          </View>
        </View>
      </SafeAreaView>
      <Screen>
        <View style={styles.section}>
          <AppText variant="large">내 스터디</AppText>
          {studies.length === 0 ? (
            <EmptyState title="아직 스터디가 없어요" compact />
          ) : (
            studies.map((study) => (
              <StudyCard
                key={study.id}
                study={{ ...study, notices: getNotices(study.id).length }}
                onPress={() => {
                  selectStudy(study.id);
                  setRole(study.role);
                  router.push('/study');
                }}
              />
            ))
          )}
          <Button
            label="스터디 만들기"
            onPress={() => router.push('/create-study')}
          />
        </View>
        {studies.length > 0 && (
          <View style={styles.reminder}>
            <Image
              source={entryAssets.reminder}
              style={styles.reminderImage}
              resizeMode="contain"
              accessible={false}
            />
            <View style={styles.reminderCopy}>
              <AppText variant="small">리마인드는 총총이 보낼게요</AppText>
              <AppText variant="caption" tone="tertiary">
                정해둔 시간에 리마인더, 미제출자에게 알림을 보내요
              </AppText>
            </View>
          </View>
        )}
      </Screen>
    </>
  );
}
const styles = StyleSheet.create({
  safe: { backgroundColor: t.color.background },
  header: {
    height: t.size.header,
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    paddingLeft: t.space.gutter,
    paddingRight: t.space.md,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  wordmark: { width: 72, height: 36 },
  notificationDot: {
    position: 'absolute',
    right: 12,
    top: 10,
    width: 8,
    height: 8,
    borderRadius: t.radius.pill,
    backgroundColor: t.color.brand,
    borderWidth: 2,
    borderColor: t.color.background,
  },
  actions: { flexDirection: 'row' },
  touch: {
    width: t.size.touch,
    height: t.size.touch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  section: { gap: t.space.md, marginTop: 20 },
  reminder: {
    paddingHorizontal: t.space.lg,
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.lg,
  },
  reminderImage: { width: 52, height: 49 },
  reminderCopy: { flex: 1, gap: t.space.xs },
});
