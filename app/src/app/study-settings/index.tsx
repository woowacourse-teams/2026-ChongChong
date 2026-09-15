import { router } from 'expo-router';
import { useState } from 'react';
import { Image, Pressable, View } from 'react-native';
import { entryAssets } from '../../features/entry/assets';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { memberStyles as s } from '../../features/members/styles';
import { useStudyManagement } from '../../features/members/useStudyManagement';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
export default function StudySettingsScreen() {
  const { study, leader, deleteStudy } = useStudyManagement();
  const { removeStudy } = useEntryScenario();
  const [confirm, setConfirm] = useState(false);
  return (
    <>
      <AppHeader title="스터디 관리" onBack={() => router.replace('/study')} />
      <Screen>
        {study ? (
          <>
            <View style={s.card}>
              <Image
                source={entryAssets.studyCard}
                style={s.image}
                resizeMode="cover"
              />
              <AppText variant="large">{study.name}</AppText>
              <AppText variant="caption" tone="tertiary" style={s.centered}>
                {study.description.replace(/\n/g, ' ')}
              </AppText>
            </View>
            <View>
              <Pressable
                accessibilityRole="button"
                style={s.action}
                onPress={() => router.push('/study-settings/profile')}
              >
                <AppText variant="large">스터디 프로필 수정</AppText>
              </Pressable>
              {leader && (
                <Pressable
                  accessibilityRole="button"
                  style={s.action}
                  onPress={() => router.push('/study-settings/edit')}
                >
                  <AppText variant="large">스터디 정보 수정</AppText>
                </Pressable>
              )}
              <Pressable
                accessibilityRole="button"
                style={s.action}
                onPress={() => setConfirm(true)}
              >
                <AppText variant="large" style={s.danger}>
                  {leader ? '스터디 삭제' : '스터디 탈퇴'}
                </AppText>
              </Pressable>
            </View>
          </>
        ) : (
          <AppText>스터디를 선택해주세요</AppText>
        )}
      </Screen>
      <ConfirmDialog
        visible={confirm && !!study}
        title={leader ? '스터디를 삭제할까요?' : '스터디를 탈퇴할까요?'}
        description={
          leader
            ? '삭제한 스터디는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'
            : '이 스터디에서 나가게 돼요.\n정말 탈퇴하시겠어요?'
        }
        confirmLabel={leader ? '삭제' : '탈퇴'}
        destructive
        onCancel={() => setConfirm(false)}
        onConfirm={() => {
          if (!study) return;
          if (leader && !deleteStudy()) return;
          if (!leader) removeStudy(study.id);
          setConfirm(false);
          router.dismissAll();
          router.replace('/studies');
        }}
      />
    </>
  );
}
