import { router } from 'expo-router';
import { useActivity } from '../features/activity/ActivityProvider';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { AppText, Button } from '../ui/primitives';
import { Screen } from '../ui/Screen';
export default function ScenariosScreen() {
  const { setScenario } = useEntryScenario();
  const { resetActivity } = useActivity();
  return (
    <Screen>
      <AppText>
        API 연결 전 UI 검증용 시나리오입니다. 새로고침하면 초기화됩니다.
      </AppText>
      {(['leader', 'member', 'empty', 'empty-notices'] as const).map((s) => (
        <Button
          key={s}
          label={
            s === 'leader'
              ? '리더 스터디 목록'
              : s === 'member'
                ? '스터디원 목록'
                : s === 'empty'
                  ? '빈 스터디 목록'
                  : '공지 없는 스터디'
          }
          onPress={() => {
            resetActivity();
            setScenario(s);
            router.replace('/studies');
          }}
        />
      ))}
      <Button label="로그인 화면" onPress={() => router.push('/login')} />
      <Button label="공통 UI" onPress={() => router.push('/showcase')} />
    </Screen>
  );
}
