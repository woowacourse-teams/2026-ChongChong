import { useEntryScenario } from '../entry/EntryProvider';
import { studyPeople } from './model';
export function useStudyManagement() {
  const {
    name,
    selectedStudy: study,
    updateStudy,
    removeStudy,
  } = useEntryScenario();
  const people = study ? studyPeople(study, name) : [];
  const leader = study?.role === 'leader';
  return {
    study,
    people,
    leader,
    saveProfile: (value: string) => {
      if (!study || !value.trim() || [...value.trim()].length > 8) return false;
      updateStudy(study.id, (current) => ({
        ...current,
        profileName: value.trim(),
      }));
      return true;
    },
    saveInfo: (title: string, description: string) => {
      if (
        !study ||
        !leader ||
        !title.trim() ||
        [...title.trim()].length > 15 ||
        [...description.trim()].length > 30
      )
        return false;
      updateStudy(study.id, (current) => ({
        ...current,
        name: title.trim(),
        description: description.trim(),
      }));
      return true;
    },
    transfer: (id: string) => {
      if (
        !study ||
        !leader ||
        id === 'self' ||
        !people.some((person) => person.id === id)
      )
        return false;
      updateStudy(study.id, (current) => ({
        ...current,
        role: 'member',
        fixtureRole: current.fixtureRole ?? current.role,
        people: studyPeople(current, name).map((person) => ({
          ...person,
          leader: person.id === id,
        })),
      }));
      return true;
    },
    expel: (id: string) => {
      if (
        !study ||
        !leader ||
        id === 'self' ||
        !people.some((person) => person.id === id)
      )
        return false;
      updateStudy(study.id, (current) => {
        const next = studyPeople(current, name).filter(
          (person) => person.id !== id,
        );
        return { ...current, people: next, members: next.length };
      });
      return true;
    },
    deleteStudy: () => {
      if (!study || !leader) return false;
      removeStudy(study.id);
      return true;
    },
  };
}
