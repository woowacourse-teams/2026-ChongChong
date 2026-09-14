import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { entryAssets } from '../features/entry/assets';
import { useEntryScenario } from '../features/entry/EntryProvider';
import { StudyField } from '../features/studies/StudyField';
import { AppHeader } from '../ui/AppHeader';
import { Button } from '../ui/primitives';
import { Screen } from '../ui/Screen';
import { tokens as t } from '../ui/tokens';

export default function CreateStudyScreen() {
  const router = useRouter();
  const { createStudy } = useEntryScenario();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const nameError = name.length > 15;
  const descriptionError = description.length > 30;
  return (
    <>
      <AppHeader title="스터디 만들기" onBack={() => router.back()} />
      <Screen>
        <Image
          source={entryAssets.rabbit}
          style={styles.rabbit}
          resizeMode="contain"
          accessible={false}
        />
        <View style={styles.form}>
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
            label="스터디 만들기"
            disabled={!name.trim() || nameError || descriptionError}
            onPress={() => {
              createStudy(name.trim(), description.trim());
              router.replace('/studies');
            }}
          />
        </View>
      </Screen>
    </>
  );
}
const styles = StyleSheet.create({
  rabbit: {
    width: 70,
    height: 70,
    alignSelf: 'center',
    marginTop: t.space.md,
    marginBottom: t.space.lg,
  },
  form: { gap: t.space.lg },
});
