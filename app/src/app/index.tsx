import { router } from 'expo-router';
import {
  roleLabels,
  studyFixture,
  useScenario,
} from '../mocks/ScenarioProvider';
import { AppText, Badge, Button, Card } from '../ui/primitives';
import { Screen } from '../ui/Screen';

export default function FoundationScreen() {
  const { role, setRole } = useScenario();
  return (
    <Screen>
      <AppText variant="title">함께하는 스터디, 총총</AppText>
      <AppText muted>
        앱 기반 확인 화면입니다. 실제 서비스 화면은 다음 작업에서 연결합니다.
      </AppText>
      <Card>
        <Badge>{roleLabels[role]}</Badge>
        <AppText variant="subtitle">{studyFixture.name}</AppText>
        <AppText muted>{studyFixture.description}</AppText>
      </Card>
      <Button
        label={`역할 전환: ${role === 'leader' ? '스터디원' : '리더'}`}
        variant="secondary"
        onPress={() => setRole(role === 'leader' ? 'member' : 'leader')}
      />
      <Button label="스터디 탭 확인" onPress={() => router.push('/study')} />
      <Button
        label="공통 UI 확인"
        variant="secondary"
        onPress={() => router.push('/showcase')}
      />
    </Screen>
  );
}
