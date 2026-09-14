import { Stack, useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AccountAlert } from '../features/account/AccountAlert';
import { EntryIcon } from '../features/entry/EntryIcon';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { ConfirmDialog, Toast } from '../ui/feedback';
import { AppText, Button, Field } from '../ui/primitives';
import { tokens as t } from '../ui/tokens';

export default function AccountScreen() {
  const router = useRouter();
  const { name, setName, notifications, setNotifications, studies, logout } =
    useEntryScenario();
  const [draft, setDraft] = useState(name);
  const [saved, setSaved] = useState(false);
  const [withdraw, setWithdraw] = useState(false);
  const [alert, setAlert] = useState<{
    readonly title: string;
    readonly description: string;
  } | null>(null);
  const tooLong = [...draft].length > 8;
  const exit = () => {
    logout();
    if (router.canDismiss()) router.dismissAll();
    router.replace('/login');
  };
  const requestWithdrawal = () => {
    if (studies.some((study) => study.role === 'leader')) {
      setAlert({
        title: '회원 탈퇴가 불가능해요',
        description:
          '스터디 리드는 다른 멤버에게\n리드 권한을 넘긴 후 탈퇴할 수 있어요',
      });
      return;
    }
    setWithdraw(true);
  };
  return (
    <SafeAreaView style={styles.screen} edges={['top', 'bottom']}>
      <Stack.Screen options={{ headerShown: false }} />
      <View style={styles.header}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="마이페이지 닫기"
          style={styles.close}
          onPress={() =>
            router.canGoBack() ? router.back() : router.replace('/studies')
          }
        >
          <EntryIcon name="close" />
        </Pressable>
      </View>
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.avatar}>
          <EntryIcon name="user" size={70} />
        </View>
        <View style={styles.profile}>
          <Field
            label="이름"
            value={draft}
            onChangeText={(value) => {
              setDraft(value);
              setSaved(false);
            }}
            autoCapitalize="none"
            autoCorrect={false}
            {...(tooLong
              ? { error: '이름은 8글자 이하로 입력할 수 있어요' }
              : {})}
          />
          {!tooLong && (
            <AppText variant="caption" tone="tertiary">
              스터디 참여 시 기본으로 설정되는 이름이에요.
            </AppText>
          )}
          <View style={styles.save}>
            <Button
              label="프로필 수정하기"
              disabled={tooLong || draft.trim().length === 0}
              onPress={() => {
                setName(draft.trim());
                setDraft(draft.trim());
                setSaved(true);
              }}
            />
          </View>
        </View>
        <View style={styles.menu}>
          <Pressable
            style={styles.row}
            accessibilityRole="switch"
            accessibilityLabel="푸시 알림"
            accessibilityState={{ checked: notifications }}
            aria-checked={notifications}
            onPress={() => setNotifications(!notifications)}
          >
            <AppText variant="large">푸시 알림</AppText>
            <View style={[styles.track, notifications && styles.trackOn]}>
              <View style={[styles.thumb, notifications && styles.thumbOn]} />
            </View>
          </Pressable>
          <Pressable
            style={styles.row}
            accessibilityRole="button"
            onPress={exit}
          >
            <AppText variant="large" style={styles.danger}>
              로그아웃
            </AppText>
          </Pressable>
          <Pressable
            style={styles.row}
            accessibilityRole="button"
            onPress={() =>
              setAlert({
                title: '개인정보처리방침',
                description: '연결 준비 중이에요.',
              })
            }
          >
            <AppText variant="large">개인정보처리방침</AppText>
          </Pressable>
          <Pressable
            style={styles.row}
            accessibilityRole="button"
            onPress={() =>
              setAlert({ title: '지원', description: '연결 준비 중이에요.' })
            }
          >
            <AppText variant="large">지원</AppText>
          </Pressable>
          <Pressable
            style={styles.row}
            accessibilityRole="button"
            onPress={requestWithdrawal}
          >
            <AppText variant="large" tone="tertiary">
              회원 탈퇴
            </AppText>
          </Pressable>
        </View>
        {saved && (
          <Toast
            message="프로필을 수정했어요."
            onDismiss={() => setSaved(false)}
          />
        )}
      </ScrollView>
      {alert && <AccountAlert {...alert} onClose={() => setAlert(null)} />}
      <ConfirmDialog
        visible={withdraw}
        title="회원 탈퇴하시겠어요?"
        description="현재 화면의 체험용 계정 정보가 초기화돼요."
        confirmLabel="탈퇴"
        destructive
        onCancel={() => setWithdraw(false)}
        onConfirm={() => {
          setWithdraw(false);
          exit();
        }}
      />
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: t.color.background },
  header: {
    height: t.size.header,
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    paddingHorizontal: t.space.md,
    alignItems: 'flex-end',
    justifyContent: 'center',
  },
  close: {
    width: t.size.touch,
    height: t.size.touch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    paddingHorizontal: t.space.gutter,
    paddingBottom: t.space.xxl,
  },
  avatar: {
    alignItems: 'center',
    marginTop: t.space.xxl,
    marginBottom: t.space.xxl,
  },
  profile: { gap: t.space.sm },
  save: { marginTop: t.space.sm },
  menu: { marginTop: t.size.header, marginBottom: t.space.xl },
  row: {
    minHeight: 44,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  danger: { color: t.color.danger },
  track: {
    width: 44,
    height: 24,
    borderRadius: t.radius.pill,
    backgroundColor: t.color.placeholder,
    padding: 2,
  },
  trackOn: { backgroundColor: t.color.brand },
  thumb: {
    width: 20,
    height: 20,
    borderRadius: t.radius.pill,
    backgroundColor: t.color.background,
  },
  thumbOn: { alignSelf: 'flex-end' },
});
