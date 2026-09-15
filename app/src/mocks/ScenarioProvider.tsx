import type { PropsWithChildren } from 'react';
import { createContext, useContext, useState } from 'react';

export type Role = 'leader' | 'member';
export const roleLabels = { leader: '리더', member: '스터디원' } as const;
export const studyFixture = {
  id: 'study-demo',
  name: '프론트엔드 CS 스터디',
  description: '매주 함께 배우고 기록하는 스터디',
} as const;
type Scenario = { readonly role: Role; readonly setRole: (role: Role) => void };
const ScenarioContext = createContext<Scenario | null>(null);

export function ScenarioProvider({ children }: PropsWithChildren) {
  const [role, setRole] = useState<Role>('leader');
  return (
    <ScenarioContext value={{ role, setRole }}>{children}</ScenarioContext>
  );
}
export function useScenario() {
  const value = useContext(ScenarioContext);
  if (!value) throw new MissingScenarioProviderError();
  return value;
}
class MissingScenarioProviderError extends Error {
  constructor() {
    super('ScenarioProvider 안에서 사용해야 합니다.');
    this.name = 'MissingScenarioProviderError';
  }
}
