import * as Clipboard from 'expo-clipboard';
import { useState } from 'react';
import { Pressable, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { MemberIcon } from '../../features/members/MemberIcon';
import { memberStyles as s } from '../../features/members/styles';
import { useStudyManagement } from '../../features/members/useStudyManagement';
import { ConfirmDialog, Toast } from '../../ui/feedback';
import { AppText } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
import { tokens as t } from '../../ui/tokens';
export default function MembersScreen() {
  const { study, people, leader, transfer, expel } = useStudyManagement();
  const [menu, setMenu] = useState<string | null>(null);
  const [action, setAction] = useState<{
    readonly id: string;
    readonly kind: 'transfer' | 'expel';
  } | null>(null);
  const [toast, setToast] = useState('');
  if (!study)
    return (
      <Screen>
        <AppText>스터디를 선택해주세요</AppText>
      </Screen>
    );
  const invite = `https://chongchong.example/join/${study.id}`;
  const copy = async () => {
    try {
      const copied = await Clipboard.setStringAsync(invite);
      setToast(
        copied
          ? '시연용 초대 링크를 복사했어요'
          : '링크를 선택해서 복사해주세요',
      );
    } catch (error) {
      if (!(error instanceof Error)) throw error;
      setToast('링크를 선택해서 복사해주세요');
    }
  };
  return (
    <>
      <Screen>
        <View style={s.group}>
          <AppText variant="large">스터디 멤버</AppText>
          <View style={s.list}>
            {people.map((person) => (
              <View
                key={person.id}
                style={{ zIndex: menu === person.id ? 2 : 0 }}
              >
                <View style={s.row}>
                  <View style={s.avatar}>
                    <AppText variant="caption" tone="tertiary">
                      {[...person.name][0]}
                    </AppText>
                  </View>
                  <View style={s.name}>
                    <AppText
                      variant="large"
                      numberOfLines={1}
                      style={{ flexShrink: 1 }}
                    >
                      {person.name}
                    </AppText>
                    {person.leader && (
                      <View accessibilityLabel="리더">
                        <MemberIcon name="crown" />
                      </View>
                    )}
                  </View>
                  {leader && person.id !== 'self' && (
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel={`${person.name} 관리`}
                      onPress={() =>
                        setMenu(menu === person.id ? null : person.id)
                      }
                      style={s.touch}
                    >
                      <Svg width={16} height={24} viewBox="0 0 16 24">
                        {[6, 12, 18].map((cy) => (
                          <Circle
                            key={cy}
                            cx={8}
                            cy={cy}
                            r={1}
                            fill={t.color.tertiary}
                          />
                        ))}
                      </Svg>
                    </Pressable>
                  )}
                </View>
                {menu === person.id && leader && (
                  <View style={s.menu}>
                    <Pressable
                      accessibilityRole="button"
                      style={s.menuItem}
                      onPress={() => {
                        setMenu(null);
                        setAction({ id: person.id, kind: 'transfer' });
                      }}
                    >
                      <AppText>리더 양도</AppText>
                    </Pressable>
                    <Pressable
                      accessibilityRole="button"
                      style={s.menuItem}
                      onPress={() => {
                        setMenu(null);
                        setAction({ id: person.id, kind: 'expel' });
                      }}
                    >
                      <AppText style={s.danger}>방출</AppText>
                    </Pressable>
                  </View>
                )}
              </View>
            ))}
          </View>
        </View>
        <View style={s.invitation}>
          <AppText variant="caption" tone="tertiary">
            링크를 통해 새로운 스터디원을 초대해요
          </AppText>
          <View style={s.inviteBox}>
            <AppText
              selectable
              variant="caption"
              numberOfLines={1}
              style={{ flex: 1 }}
            >
              {invite}
            </AppText>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="초대 링크 복사"
              onPress={copy}
              style={s.touch}
            >
              <MemberIcon name="copy" />
            </Pressable>
          </View>
        </View>
      </Screen>
      <ConfirmDialog
        visible={action !== null}
        title={
          action?.kind === 'transfer'
            ? '리드를 양도할까요?'
            : '팀원을 방출할까요?'
        }
        description={
          action?.kind === 'transfer'
            ? '스터디 리드를 양도하면\n더이상 리드의 권한을 가질 수 없어요'
            : '방출된 팀원은 이 스터디에\n더 이상 참여할 수 없어요.'
        }
        confirmLabel={action?.kind === 'transfer' ? '양도' : '방출'}
        destructive
        onCancel={() => setAction(null)}
        onConfirm={() => {
          if (action) {
            const success =
              action.kind === 'transfer'
                ? transfer(action.id)
                : expel(action.id);
            if (success)
              setToast(
                action.kind === 'transfer'
                  ? '리더를 양도했어요'
                  : '스터디원을 방출했어요',
              );
          }
          setAction(null);
        }}
      />
      {!!toast && <Toast message={toast} onDismiss={() => setToast('')} />}
    </>
  );
}
