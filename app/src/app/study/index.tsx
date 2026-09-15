import { useRouter } from 'expo-router';
import {
  Image,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import { useActivity } from '../../features/activity/ActivityProvider';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { ActivityIcon } from '../../features/home/ActivityIcon';
import { ActivityRow } from '../../features/home/ActivityRow';
import { homeLayout as layout } from '../../features/home/layout';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';

export default function HomeScreen() {
  const router = useRouter();
  const { name, selectedStudy } = useEntryScenario();
  const { activities } = useActivity();
  const leader = selectedStudy?.role === 'leader';
  const unread = activities.filter(
    (item) => item.kind === 'notice' && !item.read,
  );
  const assignments = activities.filter(
    (item) =>
      item.kind === 'assignment' &&
      (leader || (!item.read && item.target !== false)),
  );
  if (!selectedStudy)
    return (
      <View style={styles.empty}>
        <AppText tone="tertiary">스터디를 선택해주세요</AppText>
      </View>
    );
  return (
    <ScrollView style={styles.page} contentContainerStyle={styles.content}>
      <View style={styles.banner}>
        <View style={styles.bannerCopy}>
          <AppText variant="subtitle" style={styles.white}>
            {leader
              ? `${selectedStudy?.profileName ?? name}님, 오늘도 화이팅!`
              : `${selectedStudy?.profileName ?? name}님, 할 일이 ${unread.length + assignments.length}건 있어요`}
          </AppText>
          <AppText style={styles.white}>
            {leader ? '리마인드는 총총이 대신 보낼게요' : '오늘 하루도 화이팅!'}
          </AppText>
        </View>
        <Image
          source={
            leader
              ? require('../../../assets/images/home/leader.png')
              : require('../../../assets/images/home/member.png')
          }
          style={styles.image}
          resizeMode="contain"
          accessible={false}
        />
      </View>
      {leader ? (
        <View style={styles.section}>
          <AppText variant="large">스터디 현황</AppText>
          <View style={styles.stats}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="안내 중인 공지"
              onPress={() => router.push('/study/notices')}
              style={styles.stat}
            >
              <ActivityIcon kind="notice" />
              <AppText variant="title" strong>
                {activities.filter((item) => item.kind === 'notice').length}
              </AppText>
              <AppText>안내 중인 공지</AppText>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="진행 중인 과제"
              onPress={() => router.push('/study/assignments')}
              style={styles.stat}
            >
              <ActivityIcon kind="assignment" />
              <AppText variant="title" strong>
                {assignments.length}
              </AppText>
              <AppText>진행 중인 과제</AppText>
            </Pressable>
          </View>
          <View style={styles.rows}>
            {activities.map((activity) => (
              <ActivityRow key={activity.id} activity={activity} leader />
            ))}
            {activities.length === 0 && (
              <AppText tone="tertiary">진행 중인 공지와 과제가 없어요</AppText>
            )}
          </View>
        </View>
      ) : (
        <>
          <View style={styles.section}>
            <AppText variant="large">읽지 않은 공지</AppText>
            {unread.map((activity) => (
              <ActivityRow
                key={activity.id}
                activity={activity}
                leader={false}
              />
            ))}
            {unread.length === 0 && (
              <AppText tone="tertiary">모든 공지를 확인했어요</AppText>
            )}
          </View>
          <View style={[styles.section, styles.memberAssignments]}>
            <AppText variant="large">제출하지 않은 과제</AppText>
            {assignments.map((activity) => (
              <ActivityRow
                key={activity.id}
                activity={activity}
                leader={false}
              />
            ))}
            {assignments.length === 0 && (
              <AppText tone="tertiary">제출할 과제가 없어요</AppText>
            )}
          </View>
        </>
      )}
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: t.color.background },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    padding: t.space.gutter,
    paddingBottom: t.space.xxl,
    gap: t.space.xxl,
  },
  banner: {
    minHeight: layout.bannerSize,
    borderRadius: t.radius.lg,
    backgroundColor: t.color.brand,
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: t.space.gutter,
    paddingRight: t.space.lg,
  },
  bannerCopy: { flex: 1, gap: t.space.xs },
  white: {
    color: t.color.onBrand,
    ...Platform.select({ web: { wordBreak: 'keep-all' }, default: {} }),
  },
  image: { width: layout.bannerSize, height: layout.bannerSize },
  section: { gap: layout.headingGap },
  memberAssignments: { marginTop: layout.memberSectionAdjustment },
  stats: { flexDirection: 'row', gap: t.space.md },
  stat: {
    flex: 1,
    minHeight: layout.statusHeight,
    boxShadow: layout.statusShadow,
    padding: t.space.lg,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
  },
  rows: { gap: t.space.md, marginTop: layout.statusListGap },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: t.color.background,
  },
});
