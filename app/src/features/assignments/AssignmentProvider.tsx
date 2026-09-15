import {
  createContext,
  type PropsWithChildren,
  useContext,
  useState,
} from 'react';
import { useEntryScenario } from '../entry/EntryProvider';
import { isFutureReminder } from '../notices/previewClock';
import {
  type Assignment,
  type AssignmentDraft,
  createAssignmentFixtures,
  memberFixtures,
  type Submission,
  validSubmissionLink,
} from './model';

type AssignmentState = {
  readonly assignments: readonly Assignment[];
  readonly getAssignments: (studyId: string) => readonly Assignment[];
  readonly saveAssignment: (
    draft: AssignmentDraft,
    id?: string,
  ) => string | undefined;
  readonly deleteAssignment: (id: string) => void;
  readonly submit: (
    id: string,
    draft: Omit<Submission, 'submittedAt'>,
  ) => boolean;
  readonly remind: (id: string, memberId?: string) => void;
  readonly resetAssignments: () => void;
};
const Context = createContext<AssignmentState | null>(null);
export function AssignmentProvider({ children }: PropsWithChildren) {
  const { studies, selectedStudy } = useEntryScenario();
  const [byStudy, setByStudy] = useState<
    Readonly<Record<string, readonly Assignment[]>>
  >({});
  const getAssignments = (studyId: string): readonly Assignment[] => {
    const study = studies.find((item) => item.id === studyId);
    if (!study) return [];
    return (
      byStudy[studyId] ??
      (study.assignments
        ? createAssignmentFixtures(study.role === 'member')
        : [])
    );
  };
  const assignments = selectedStudy ? getAssignments(selectedStudy.id) : [];
  const update = (
    transform: (current: readonly Assignment[]) => readonly Assignment[],
  ) => {
    if (!selectedStudy) return;
    const study = selectedStudy;
    setByStudy((current) => ({
      ...current,
      [study.id]: transform(
        current[study.id] ??
          (study.assignments
            ? createAssignmentFixtures(study.role === 'member')
            : []),
      ),
    }));
  };
  const saveAssignment = (draft: AssignmentDraft, id?: string) => {
    if (
      selectedStudy?.role !== 'leader' ||
      !draft.title.trim() ||
      !draft.body.trim() ||
      !draft.method.trim() ||
      !isFutureReminder(draft.deadline) ||
      (id && !assignments.some((item) => item.id === id))
    )
      return;
    const savedId = id ?? `assignment-${Date.now()}`;
    const others = memberFixtures.filter((person) => person.id !== 'self');
    const people = Array.from(
      { length: Math.max(0, selectedStudy.members - 1) },
      (_, index) => ({
        id: `reader-${index + 1}`,
        name: others[index]?.name ?? `스터디원 ${index + 1}`,
        submission: null,
        remindedAt: null,
      }),
    );
    update((current) => {
      if (!id)
        return [
          {
            ...draft,
            id: savedId,
            members: draft.leaderParticipates
              ? [
                  ...people,
                  {
                    id: 'self',
                    name: '바니 (나)',
                    submission: null,
                    remindedAt: null,
                  },
                ]
              : people,
          },
          ...current,
        ];
      return current.map((item) => {
        if (item.id !== id) return item;
        const others = item.members.filter((person) => person.id !== 'self');
        const self = item.members.find((person) => person.id === 'self') ?? {
          id: 'self',
          name: '바니 (나)',
          submission: null,
          remindedAt: null,
        };
        return {
          ...item,
          ...draft,
          members: draft.leaderParticipates ? [...others, self] : others,
        };
      });
    });
    return savedId;
  };
  const submit = (id: string, draft: Omit<Submission, 'submittedAt'>) => {
    const item = assignments.find((assignment) => assignment.id === id);
    if (
      !item ||
      !item.members.some((person) => person.id === 'self') ||
      !draft.body.trim() ||
      !validSubmissionLink(draft.link)
    )
      return false;
    update((current) =>
      current.map((assignment) =>
        assignment.id === id
          ? {
              ...assignment,
              members: assignment.members.map((person) =>
                person.id === 'self'
                  ? { ...person, submission: { ...draft, submittedAt: '방금' } }
                  : person,
              ),
            }
          : assignment,
      ),
    );
    return true;
  };
  const remind = (id: string, memberId?: string) => {
    if (selectedStudy?.role !== 'leader') return;
    update((current) =>
      current.map((item) =>
        item.id === id
          ? {
              ...item,
              members: item.members.map((person) =>
                !person.submission && (!memberId || memberId === person.id)
                  ? { ...person, remindedAt: '방금' }
                  : person,
              ),
            }
          : item,
      ),
    );
  };
  return (
    <Context
      value={{
        assignments,
        getAssignments,
        saveAssignment,
        submit,
        remind,
        deleteAssignment: (id) => {
          if (selectedStudy?.role === 'leader')
            update((current) => current.filter((item) => item.id !== id));
        },
        resetAssignments: () => setByStudy({}),
      }}
    >
      {children}
    </Context>
  );
}
export function useAssignments() {
  const value = useContext(Context);
  if (!value) throw new MissingAssignmentProviderError();
  return value;
}
class MissingAssignmentProviderError extends Error {
  constructor() {
    super('AssignmentProvider 안에서 사용해야 합니다.');
    this.name = 'MissingAssignmentProviderError';
  }
}
