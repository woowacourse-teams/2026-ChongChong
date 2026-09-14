import { useRouter } from 'expo-router';
import { Image, Pressable, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { entryAssets } from '../features/entry/assets';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { AppText, Button } from '../ui/primitives';
import { Screen } from '../ui/Screen';
import { tokens as t } from '../ui/tokens';

export default function JoinStudyScreen() {
  const router = useRouter();
  const { joinStudy } = useEntryScenario();
  return (
    <>
      <SafeAreaView edges={['top', 'left', 'right']} style={styles.safe}>
        <View style={styles.header}>
          <Image
            source={entryAssets.rabbit}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="총총"
          />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="마이페이지"
            onPress={() => router.push('/account')}
            style={styles.my}
          >
            <AppText variant="large">My</AppText>
          </Pressable>
        </View>
      </SafeAreaView>
      <Screen>
        <View style={styles.content}>
          <View style={styles.title}>
            <AppText variant="subtitle" accessibilityRole="header">
              스터디 참여하기
            </AppText>
            <AppText variant="caption" tone="tertiary">
              해당 스터디에 참여하시겠습니까?
            </AppText>
          </View>
          <View style={styles.preview}>
            <Image
              source={entryAssets.study}
              style={styles.thumbnail}
              resizeMode="contain"
              accessible={false}
            />
            <AppText variant="subtitle">프론트엔드 CS 스터디</AppText>
            <AppText variant="caption" tone="tertiary">
              매주 화요일 저녁 9시, 프론트엔드 CS와 코드 리뷰
            </AppText>
            <View style={styles.border} />
          </View>
          <Button
            label="스터디 참여하기"
            onPress={() => {
              joinStudy();
              router.replace('/studies');
            }}
          />
        </View>
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
    alignItems: 'center',
    justifyContent: 'center',
  },
  logo: { width: 40, height: 40 },
  my: {
    position: 'absolute',
    right: t.space.gutter,
    width: t.size.touch,
    height: t.size.touch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: { gap: t.space.md },
  title: { gap: t.space.sm },
  preview: {
    minHeight: 168,
    marginBottom: t.space.md,
    borderRadius: t.radius.lg,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: t.space.gutter,
  },
  thumbnail: { width: 70, height: 70 },
  border: {
    ...StyleSheet.absoluteFill,
    pointerEvents: 'none',
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.lg,
  },
});
