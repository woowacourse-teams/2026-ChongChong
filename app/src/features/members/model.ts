import type { EntryStudy } from '../entry/EntryProvider';
export type StudyPerson = {
  readonly id: string;
  readonly name: string;
  readonly leader: boolean;
};
export function studyPeople(
  study: EntryStudy,
  defaultName: string,
): readonly StudyPerson[] {
  const selfName = study.profileName ?? defaultName;
  if (study.people)
    return study.people.map((person) =>
      person.id === 'self' ? { ...person, name: selfName } : person,
    );
  const names = ['피즈', '디움', '안톨리니', '이든'];
  return [
    { id: 'self', name: selfName, leader: study.role === 'leader' },
    ...Array.from({ length: Math.max(0, study.members - 1) }, (_, index) => ({
      id: `member-${index + 1}`,
      name: names[index] ?? `스터디원 ${index + 1}`,
      leader: study.role === 'member' && index === 0,
    })),
  ];
}
