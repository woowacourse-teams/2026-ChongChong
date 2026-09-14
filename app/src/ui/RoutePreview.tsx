import { roleLabels, useScenario } from '../mocks/ScenarioProvider';
import { AppText, Badge, EmptyState } from './primitives';
import { Screen } from './Screen';
export function RoutePreview({ title }: { readonly title: string }) {
  const { role } = useScenario();
  return (
    <Screen>
      <AppText variant="title">{title}</AppText>
      <Badge>{roleLabels[role]}</Badge>
      <EmptyState
        title="화면 연결 준비 완료"
        description="이 화면의 기능 UI는 후속 이슈에서 구현합니다."
      />
    </Screen>
  );
}
