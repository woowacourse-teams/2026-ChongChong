import { router } from 'expo-router';
import { useState } from 'react';
import { Image, View } from 'react-native';
import { useDraftGuard } from '../../features/assignments/useDraftGuard';
import { entryAssets } from '../../features/entry/assets';
import { memberStyles as s } from '../../features/members/styles';
import { useStudyManagement } from '../../features/members/useStudyManagement';
import { StudyField } from '../../features/studies/StudyField';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
export default function EditStudyScreen() {
  const { study, leader, saveInfo } = useStudyManagement();
  const [name, setName] = useState(study?.name ?? '');
  const [description, setDescription] = useState(study?.description ?? '');
  const guard = useDraftGuard(
    name !== study?.name || description !== study?.description,
  );
  const nameError = [...name].length > 15;
  const descriptionError = [...description].length > 30;
  return (
    <>
      <AppHeader
        title="스터디 정보 수정"
        onBack={() =>
          router.canGoBack() ? router.back() : router.replace('/study-settings')
        }
      />
      <Screen>
        {study && leader ? (
          <>
            <View style={s.profileIcon}>
              <Image
                source={entryAssets.studyCard}
                style={s.image}
                resizeMode="cover"
              />
            </View>
            <View style={s.form}>
              <StudyField
                label="스터디 이름"
                required
                value={name}
                onChangeText={setName}
                placeholder="스터디 이름을 입력해주세요"
                helper={
                  nameError
                    ? '스터디 이름은 15자 이하로 입력할 수 있어요'
                    : '스터디원에게 그대로 보여요'
                }
                error={nameError}
              />
              <StudyField
                label="어떤 스터디인가요?"
                value={description}
                onChangeText={setDescription}
                placeholder="스터디에 대해 설명해주세요"
                multiline
                helper={
                  descriptionError
                    ? '스터디 설명은 30자 이하로 입력할 수 있어요'
                    : '모이는 요일과 시간을 적어두면 초대할 때 설명이 줄어들어요'
                }
                error={descriptionError}
              />
              <Button
                label="스터디 수정하기"
                disabled={!name.trim() || nameError || descriptionError}
                onPress={() => {
                  if (saveInfo(name, description)) {
                    guard.allowLeave();
                    router.replace('/study-settings');
                  }
                }}
              />
            </View>
          </>
        ) : (
          <AppText>리더만 스터디 정보를 수정할 수 있어요</AppText>
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
