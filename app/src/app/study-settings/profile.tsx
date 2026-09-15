import { router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';
import { useDraftGuard } from '../../features/assignments/useDraftGuard';
import { EntryIcon } from '../../features/entry/EntryIcon';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { memberStyles as s } from '../../features/members/styles';
import { useStudyManagement } from '../../features/members/useStudyManagement';
import { StudyField } from '../../features/studies/StudyField';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
export default function StudyProfileScreen() {
  const { name } = useEntryScenario();
  const { study, saveProfile } = useStudyManagement();
  const initial = study?.profileName ?? name;
  const [draft, setDraft] = useState(initial);
  const guard = useDraftGuard(draft !== initial);
  const error = [...draft].length > 8;
  return (
    <>
      <AppHeader
        title="스터디 프로필 수정"
        onBack={() =>
          router.canGoBack() ? router.back() : router.replace('/study-settings')
        }
      />
      <Screen>
        {study ? (
          <>
            <View style={s.profileIcon}>
              <EntryIcon name="user" size={70} />
            </View>
            <View style={s.group}>
              <StudyField
                label="이름"
                value={draft}
                onChangeText={setDraft}
                placeholder="이름을 입력해주세요"
                helper={
                  error
                    ? '이름은 8글자 이하로 입력할 수 있어요'
                    : '해당 스터디에서 사용하는 이름이에요'
                }
                error={error}
              />
              <Button
                label="프로필 수정하기"
                disabled={!draft.trim() || error}
                onPress={() => {
                  if (saveProfile(draft)) {
                    guard.allowLeave();
                    router.replace('/study-settings');
                  }
                }}
              />
            </View>
          </>
        ) : (
          <AppText>스터디를 선택해주세요</AppText>
        )}
      </Screen>
      <ConfirmDialog
        visible={guard.discard}
        title="수정을 그만할까요?"
        description="작성한 내용이 저장되지 않아요."
        onCancel={guard.cancel}
        onConfirm={guard.confirm}
        confirmLabel="나가기"
      />
    </>
  );
}
