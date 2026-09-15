import { router } from 'expo-router';
import { useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { entryAssets } from '../features/entry/assets';
import { EntryIcon } from '../features/entry/EntryIcon';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { ConfirmDialog } from '../ui/feedback';
import { chongchongV2 as v } from '../ui/figmaVariables';
import { AppText } from '../ui/primitives';
import { tokens as t } from '../ui/tokens';
export default function LoginScreen() {
  const [permission, setPermission] = useState(false);
  const { setNotifications } = useEntryScenario();
  const enter = (allowed: boolean) => {
    setNotifications(allowed);
    setPermission(false);
    router.replace('/studies');
  };
  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.content}>
          <View style={styles.hero}>
            <Image
              source={entryAssets.rabbit}
              style={styles.rabbit}
              resizeMode="contain"
              accessible={false}
            />
            <AppText variant="title" strong style={styles.center}>
              총총에 오신 걸 환영해요
            </AppText>
            <AppText variant="caption" tone="tertiary" style={styles.center}>
              번거로운 스터디 운영, 이제 총총에게 맡기세요
            </AppText>
          </View>
          <View style={styles.actions}>
            {(['kakao', 'google', 'apple'] as const).map((provider) => (
              <Pressable
                key={provider}
                accessibilityRole="button"
                accessibilityHint="UI 체험용 로그인입니다"
                onPress={() => setPermission(true)}
                style={({ pressed }) => [
                  styles.social,
                  styles[provider],
                  pressed && { opacity: t.pressedOpacity },
                ]}
              >
                <EntryIcon name={provider} size={20} />
                <AppText
                  variant="large"
                  style={{
                    color:
                      provider === 'apple'
                        ? t.color.background
                        : v.color.neutral.black,
                  }}
                >
                  {provider === 'kakao'
                    ? '카카오'
                    : provider === 'google'
                      ? 'Google'
                      : 'Apple'}
                  로 계속하기
                </AppText>
              </Pressable>
            ))}
          </View>
          <AppText variant="caption" tone="tertiary" style={styles.legal}>
            계속하면 서비스 이용약관과 개인정보 처리방침에 동의하게 됩니다.
          </AppText>
        </View>
      </ScrollView>
      <ConfirmDialog
        visible={permission}
        title="푸시 알림을 허용할까요?"
        description={'스터디 공지와 과제 소식을\n놓치지 않도록 알려드릴게요'}
        cancelLabel="나중에"
        confirmLabel="알림 허용"
        onCancel={() => enter(false)}
        onConfirm={() => enter(true)}
      />
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: t.color.background },
  scroll: { flexGrow: 1, alignItems: 'center', justifyContent: 'flex-end' },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    paddingHorizontal: 20,
    paddingBottom: 24,
    paddingTop: 48,
  },
  hero: { alignItems: 'center', gap: 8, marginBottom: 120 },
  rabbit: { width: 118, height: 118, marginBottom: 12 },
  center: { textAlign: 'center' },
  actions: { gap: 16 },
  social: {
    height: 56,
    borderRadius: 12,
    flexDirection: 'row',
    gap: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  kakao: { backgroundColor: v.color.social['kakao-background'] },
  google: {
    backgroundColor: t.color.background,
    borderWidth: 1,
    borderColor: v.color.social['google-border'],
  },
  apple: { backgroundColor: v.color.neutral.black },
  legal: { textAlign: 'center', marginTop: 16 },
});
