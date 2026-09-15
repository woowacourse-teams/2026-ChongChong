import type { PropsWithChildren } from 'react';
import { createContext, useContext, useState } from 'react';
import type { StudyPerson } from '../members/model';

export type EntryStudy = {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly role: 'leader' | 'member';
  readonly notices: number;
  readonly assignments: number;
  readonly members: number;
  readonly profileName?: string;
  readonly fixtureRole?: 'leader' | 'member';
  readonly people?: readonly StudyPerson[];
};
type EntryScenario = {
  readonly name: string;
  readonly setName: (name: string) => void;
  readonly notifications: boolean;
  readonly setNotifications: (enabled: boolean) => void;
  readonly studies: readonly EntryStudy[];
  readonly selectedStudyId: string | undefined;
  readonly selectedStudy: EntryStudy | undefined;
  readonly selectStudy: (id: string) => void;
  readonly createStudy: (name: string, description: string) => void;
  readonly joinStudy: () => void;
  readonly updateStudy: (
    id: string,
    update: (study: EntryStudy) => EntryStudy,
  ) => void;
  readonly removeStudy: (id: string) => void;
  readonly logout: () => void;
  readonly setScenario: (
    scenario:
      | 'leader'
      | 'member'
      | 'empty'
      | 'empty-notices'
      | 'empty-assignments',
  ) => void;
};
const initialStudies: readonly EntryStudy[] = [
  {
    id: 'frontend-cs',
    name: '프론트엔드 CS 스터디',
    description: '매주 화요일 저녁 9시,\n프론트엔드 CS와 코드 리뷰',
    role: 'leader',
    notices: 2,
    assignments: 1,
    members: 5,
  },
  {
    id: 'algorithm',
    name: '알고리즘 스터디',
    description: '매주 수요일 저녁 6시,\n알고리즘 풀이 공유',
    role: 'leader',
    notices: 2,
    assignments: 1,
    members: 5,
  },
];
const EntryContext = createContext<EntryScenario | null>(null);

export function EntryProvider({ children }: PropsWithChildren) {
  const [name, setName] = useState('바니');
  const [notifications, setNotifications] = useState(true);
  const [studies, setStudies] = useState<readonly EntryStudy[]>(initialStudies);
  const [selectedStudyId, setSelectedStudyId] = useState<string | undefined>(
    'frontend-cs',
  );
  const selectedStudy = studies.find((study) => study.id === selectedStudyId);
  const selectStudy = (id: string) => {
    if (studies.some((study) => study.id === id)) setSelectedStudyId(id);
  };
  const createStudy = (studyName: string, description: string) => {
    setStudies((current) => [
      ...current,
      {
        id: `created-${Date.now()}-${current.length}`,
        name: studyName,
        description,
        role: 'leader',
        notices: 0,
        assignments: 0,
        members: 1,
      },
    ]);
  };
  const joinStudy = () => {
    setStudies((current) =>
      current.some((study) => study.id === 'invited-study')
        ? current
        : [
            ...current,
            {
              id: 'invited-study',
              name: '프론트엔드 CS 스터디',
              description: '매주 화요일 저녁 9시,\n프론트엔드 CS와 코드 리뷰',
              role: 'member',
              notices: 2,
              assignments: 1,
              members: 6,
            },
          ],
    );
  };
  const logout = () => {
    setName('바니');
    setNotifications(true);
    setStudies(initialStudies);
    setSelectedStudyId('frontend-cs');
  };
  const setScenario = (
    scenario:
      | 'leader'
      | 'member'
      | 'empty'
      | 'empty-notices'
      | 'empty-assignments',
  ) => {
    setSelectedStudyId(scenario === 'empty' ? undefined : 'frontend-cs');
    switch (scenario) {
      case 'leader':
        setStudies(initialStudies);
        break;
      case 'member':
        setStudies(
          initialStudies.map((study) => ({ ...study, role: 'member' })),
        );
        break;
      case 'empty-assignments':
        setStudies(
          initialStudies.map((study) => ({
            ...study,
            role: 'member',
            assignments: 0,
          })),
        );
        break;
      case 'empty-notices':
        setStudies(
          initialStudies.map((study) => ({
            ...study,
            role: 'member',
            notices: 0,
          })),
        );
        break;
      case 'empty':
        setStudies([]);
        break;
    }
  };
  return (
    <EntryContext
      value={{
        name,
        setName,
        notifications,
        setNotifications,
        studies,
        selectedStudyId,
        selectedStudy,
        selectStudy,
        createStudy,
        joinStudy,
        updateStudy: (id, update) =>
          setStudies((current) =>
            current.map((study) => (study.id === id ? update(study) : study)),
          ),
        removeStudy: (id) => {
          setStudies((current) => current.filter((study) => study.id !== id));
          if (selectedStudyId === id) setSelectedStudyId(undefined);
        },
        logout,
        setScenario,
      }}
    >
      {children}
    </EntryContext>
  );
}
export function useEntryScenario() {
  const value = useContext(EntryContext);
  if (!value) throw new MissingEntryProviderError();
  return value;
}
class MissingEntryProviderError extends Error {
  constructor() {
    super('EntryProvider 안에서 사용해야 합니다.');
    this.name = 'MissingEntryProviderError';
  }
}
