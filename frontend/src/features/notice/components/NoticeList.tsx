import type { ReactNode } from 'react';
import rightArrow from '../../../shared/assets/right-arrow.svg';
import ContentCard from '../../../shared/ui/card/ContentCard';
import List from '../../../shared/ui/List';
import { Link } from 'react-router';
import { Notice } from '../types';

interface NoticeListProps {
  notices: Notice[];
  studyId: string;
  role: 'leader' | 'member';
  children: (notice: Notice) => ReactNode;
}

export default function NoticeList({ notices, studyId, role, children }: NoticeListProps) {
  return (
    <List>
      {notices.map((notice) => (
        <List.Item key={notice.id}>
          <Link
            to={{
              pathname: `/studies/${studyId}/notices/${notice.id}`,
            }}
            state={{ role: role }}
          >
            <ContentCard>
              <ContentCard.Badges>{children(notice)}</ContentCard.Badges>

              <ContentCard.TitleRow>
                <ContentCard.Title>{notice.title}</ContentCard.Title>

                <ContentCard.Trailing>
                  <img src={rightArrow} alt="공지 상세 보기" width={20} height={20} />
                </ContentCard.Trailing>
              </ContentCard.TitleRow>

              <ContentCard.Description>{notice.description}</ContentCard.Description>

              <ContentCard.Footer>
                <ContentCard.Meta>{notice.createdAt}</ContentCard.Meta>
              </ContentCard.Footer>
            </ContentCard>
          </Link>
        </List.Item>
      ))}
    </List>
  );
}
