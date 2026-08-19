import type { CSSProperties } from 'react';
import clock from '../../../shared/assets/clock-black.svg';
import rightArrow from '../../../shared/assets/right-arrow.svg';
import Badge from '../../../shared/ui/Badge';
import ContentCard from '../../../shared/ui/card/ContentCard';
import List from '../../../shared/ui/List';
import { Link } from 'react-router';

export interface NoticeListItem {
  id: number;
  title: string;
  description: string;
  createdAt: string;
  isRead?: boolean;
  readCount?: number;
  totalCount?: number;
  reminderText?: string;
}

interface NoticeListProps {
  notices: NoticeListItem[];
  isLeader: boolean;
  studyId: string;
}

const listStyle = {
  flex: 'initial',
} satisfies CSSProperties;

export default function NoticeList({ notices, isLeader, studyId }: NoticeListProps) {
  return (
    <List css={listStyle}>
      {notices.map((notice) => (
        <List.Item key={notice.id}>
          <Link
            to={{
              pathname: `/studies/${studyId}/notices/${notice.id}`,
              search: isLeader ? '?role=leader' : '',
            }}
            css={{
              display: 'block',
              color: 'inherit',
              textDecoration: 'none',
            }}
          >
            <ContentCard>
              <ContentCard.Badges>
                {isLeader ? (
                  <>
                    <Badge variant="BrandOutline" size="Small">
                      {notice.readCount}/{notice.totalCount} 읽음
                    </Badge>
                    {notice.reminderText && (
                      <Badge variant="NeutralSolid" size="Small">
                        <img src={clock} alt="리마인드 시각" width={12} height={12} />
                        {notice.reminderText}
                      </Badge>
                    )}
                  </>
                ) : (
                  <Badge variant={notice.isRead ? 'BrandOutline' : 'BrandSolid'} size="Small">
                    {notice.isRead ? '읽음' : '읽지 않음'}
                  </Badge>
                )}
              </ContentCard.Badges>

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
