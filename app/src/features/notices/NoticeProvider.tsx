import {
  createContext,
  type PropsWithChildren,
  useContext,
  useState,
} from 'react';
import { useEntryScenario } from '../entry/EntryProvider';
import {
  createNoticeFixtures,
  type Notice,
  type NoticeDraft,
  recipients,
} from './model';

type NoticeState = {
  readonly notices: readonly Notice[];
  readonly getNotices: (studyId: string) => readonly Notice[];
  readonly saveNotice: (draft: NoticeDraft, id?: string) => string | undefined;
  readonly deleteNotice: (id: string) => void;
  readonly markRead: (id: string) => void;
  readonly remind: (id: string, recipientId?: string) => void;
  readonly resetNotices: () => void;
};
const NoticeContext = createContext<NoticeState | null>(null);
export function NoticeProvider({ children }: PropsWithChildren) {
  const { studies, selectedStudy } = useEntryScenario();
  const [byStudy, setByStudy] = useState<
    Readonly<Record<string, readonly Notice[]>>
  >({});
  const getNotices = (studyId: string): readonly Notice[] => {
    const study = studies.find((item) => item.id === studyId);
    if (!study) return [];
    return (
      byStudy[studyId] ??
      (study.notices > 0
        ? createNoticeFixtures((study.fixtureRole ?? study.role) === 'member')
        : [])
    );
  };
  const notices = selectedStudy ? getNotices(selectedStudy.id) : [];
  const update = (
    transform: (current: readonly Notice[]) => readonly Notice[],
  ) => {
    if (!selectedStudy) return;
    const study = selectedStudy;
    setByStudy((current) => ({
      ...current,
      [study.id]: transform(
        current[study.id] ??
          (study.notices > 0
            ? createNoticeFixtures(
                (study.fixtureRole ?? study.role) === 'member',
              )
            : []),
      ),
    }));
  };
  const saveNotice = (draft: NoticeDraft, id?: string) => {
    if (
      selectedStudy?.role !== 'leader' ||
      !draft.title.trim() ||
      !draft.body.trim()
    )
      return;
    const savedId = id ?? `notice-${Date.now()}`;
    update((current) =>
      id
        ? current.map((notice) =>
            notice.id === id ? { ...notice, ...draft } : notice,
          )
        : [
            {
              ...draft,
              id: savedId,
              createdLabel: '방금 작성',
              timeLabel: '방금 전',
              recipients: recipients
                .slice(0, Math.max(0, selectedStudy.members - 1))
                .map((item) => ({ ...item, readAt: null, remindedAt: null })),
            },
            ...current,
          ],
    );
    return savedId;
  };
  const deleteNotice = (id: string) => {
    if (selectedStudy?.role === 'leader')
      update((current) => current.filter((notice) => notice.id !== id));
  };
  const markRead = (id: string) => {
    if (selectedStudy?.role !== 'member') return;
    update((current) =>
      current.map((notice) =>
        notice.id === id
          ? {
              ...notice,
              recipients: notice.recipients.map((item) =>
                item.id === 'self' && !item.readAt
                  ? { ...item, readAt: '8월 3일 21:14' }
                  : item,
              ),
            }
          : notice,
      ),
    );
  };
  const remind = (id: string, recipientId?: string) => {
    if (selectedStudy?.role !== 'leader') return;
    update((current) =>
      current.map((notice) =>
        notice.id === id
          ? {
              ...notice,
              recipients: notice.recipients.map((item) =>
                !item.readAt && (!recipientId || recipientId === item.id)
                  ? { ...item, remindedAt: '방금', reminderAvailable: false }
                  : item,
              ),
            }
          : notice,
      ),
    );
  };
  return (
    <NoticeContext
      value={{
        notices,
        getNotices,
        saveNotice,
        deleteNotice,
        markRead,
        remind,
        resetNotices: () => setByStudy({}),
      }}
    >
      {children}
    </NoticeContext>
  );
}
export function useNotices() {
  const context = useContext(NoticeContext);
  if (!context) throw new MissingNoticeProviderError();
  return context;
}
class MissingNoticeProviderError extends Error {
  constructor() {
    super('NoticeProvider 안에서 사용해야 합니다.');
    this.name = 'MissingNoticeProviderError';
  }
}
