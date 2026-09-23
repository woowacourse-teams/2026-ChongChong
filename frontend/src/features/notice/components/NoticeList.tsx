import rightArrow from '../../../shared/assets/right-arrow.svg';
import ContentCard from '../../../shared/ui/card/ContentCard';
import List from '../../../shared/ui/List';
import { Link } from 'react-router';
import type { Notice } from '../types';
import type { ReactNode } from 'react';
import { formatRelativeTime } from '../../../shared/utils/formatDate';

interface NoticeListProps<T extends Notice> {
  notices: T[];
  studyId: number;
  children: (notice: T) => ReactNode;
}

export default function NoticeList<T extends Notice>({
  notices,
  studyId,
  children,
}: NoticeListProps<T>) {
  return (
    <List>
      {notices.map((notice) => (
        <List.Item key={notice.id}>
          <Link
            to={{
              pathname: `/studies/${studyId}/notices/${notice.id}`,
            }}
          >
            <ContentCard>
              <ContentCard.Badges>{children(notice)}</ContentCard.Badges>
              <ContentCard.TitleRow>
                <ContentCard.Title>{notice.title}</ContentCard.Title>

                <ContentCard.Trailing>
                  <img src={rightArrow} alt="" width={20} height={20} />
                </ContentCard.Trailing>
              </ContentCard.TitleRow>

              <ContentCard.Description>{notice.content}</ContentCard.Description>

              <ContentCard.Footer direction="column">
                <ContentCard.Meta tone="brand">
                  {formatRelativeTime(notice.createdAt)}
                </ContentCard.Meta>
              </ContentCard.Footer>
            </ContentCard>
          </Link>
        </List.Item>
      ))}
    </List>
  );
}
