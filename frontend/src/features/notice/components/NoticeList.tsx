import clock from '../../../shared/assets/clock-black.svg';
import rightArrow from '../../../shared/assets/right-arrow.svg';
import Badge from '../../../shared/ui/Badge';
import ContentCard from '../../../shared/ui/card/ContentCard';
import List from '../../../shared/ui/List';

export default function NoticeList() {
  return (
    <List>
      <List.Item>
        <ContentCard>
          <ContentCard.Badges>
            <Badge variant="BrandOutline" size="Small">
              2/4 읽음
            </Badge>
            <Badge variant="NeutralSolid" size="Small">
              <img src={clock} alt="시간" width={12} height={12} />
              1분 뒤 리마인드
            </Badge>
          </ContentCard.Badges>

          <ContentCard.TitleRow>
            <ContentCard.Title>8월 스터디 운영 방식이 바뀝니다</ContentCard.Title>
            <ContentCard.Trailing>
              <img src={rightArrow} alt="" width={20} height={20} />
            </ContentCard.Trailing>
          </ContentCard.TitleRow>

          <ContentCard.Description>
            8월부터 스터디 운영 방식을 조금 바꾸려고 합니다.
          </ContentCard.Description>

          <ContentCard.Footer>
            <ContentCard.Meta>5시간 전</ContentCard.Meta>
          </ContentCard.Footer>
        </ContentCard>
      </List.Item>
    </List>
  );
}
