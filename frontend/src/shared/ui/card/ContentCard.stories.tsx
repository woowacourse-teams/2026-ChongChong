import type { ComponentProps } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';
import Badge from '../Badge';
import clockIcon from '../../assets/clock.svg';
import rightArrow from '../../assets/right-arrow.svg';
import ContentCard from './ContentCard';

interface CardExampleProps {
  title: string;
  description: string;
  titleSize: ComponentProps<typeof ContentCard.Title>['size'];
  truncate: boolean;
  showBadges: boolean;
  showAccessory: boolean;
  showTrailing: boolean;
  showDescription: boolean;
  showFooter: boolean;
  footerDirection: ComponentProps<typeof ContentCard.Footer>['direction'];
  metaTone: ComponentProps<typeof ContentCard.Meta>['tone'];
  footerNote: string;
  onClick?: () => void;
}

function CardExample({
  title,
  description,
  titleSize,
  truncate,
  showBadges,
  showAccessory,
  showTrailing,
  showDescription,
  showFooter,
  footerDirection,
  metaTone,
  footerNote,
  onClick,
}: CardExampleProps) {
  return (
    <ContentCard onClick={onClick}>
      {showBadges && (
        <ContentCard.Badges>
          <Badge variant="brandOutline" size="small">
            공지
          </Badge>
          <Badge variant="neutralSolid" size="small">
            미확인
          </Badge>
        </ContentCard.Badges>
      )}
      <ContentCard.TitleRow>
        <ContentCard.Title size={titleSize} truncate={truncate}>
          {title}
        </ContentCard.Title>
        {showAccessory && <ContentCard.Accessory>5명</ContentCard.Accessory>}
        {showTrailing && (
          <ContentCard.Trailing>
            <img src={rightArrow} alt="" width={20} height={20} />
          </ContentCard.Trailing>
        )}
      </ContentCard.TitleRow>
      {showDescription && (
        <ContentCard.Description truncate={truncate}>{description}</ContentCard.Description>
      )}
      {showFooter && (
        <ContentCard.Footer direction={footerDirection}>
          <ContentCard.Meta
            tone={metaTone}
            icon={<img src={clockIcon} alt="" width={16} height={16} />}
          >
            오늘 작성
          </ContentCard.Meta>
          {footerNote && <ContentCard.Meta>{footerNote}</ContentCard.Meta>}
        </ContentCard.Footer>
      )}
    </ContentCard>
  );
}

const meta = {
  title: 'UI/Card/ContentCard',
  component: CardExample,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          'ContentCard와 하위 컴포넌트의 조합 예시입니다. onClick 외의 args는 예시 전용이며 원본 카드의 직접 props가 아닙니다. titleSize·truncate는 Title, truncate는 Description, footerDirection은 Footer, metaTone은 Meta에 전달합니다. 원본 카드의 높이는 180px입니다.',
      },
    },
  },
  args: {
    title: '1주 차 스터디 안내',
    description: '공식 문서를 읽고 궁금한 내용을 정리해주세요.',
    titleSize: 'default',
    truncate: true,
    showBadges: true,
    showAccessory: true,
    showTrailing: true,
    showDescription: true,
    showFooter: true,
    footerDirection: 'row',
    metaTone: 'muted',
    footerNote: '',
  },
  argTypes: {
    title: { description: 'Title의 내용입니다.', control: 'text' },
    description: { description: 'Description의 내용입니다.', control: 'text' },
    titleSize: {
      description: 'Title의 글자 크기입니다.',
      control: 'select',
      options: ['default', 'large'],
    },
    truncate: {
      description: 'Title·Description의 원본 말줄임 스타일 적용 여부입니다.',
      control: 'boolean',
    },
    showBadges: { description: '예시의 배지 영역을 표시합니다.', control: 'boolean' },
    showAccessory: {
      description: '제목 바로 옆의 보조 정보(5명)를 표시합니다.',
      control: 'boolean',
    },
    showTrailing: {
      description: '제목 행 오른쪽 끝의 화살표를 표시합니다.',
      control: 'boolean',
    },
    showDescription: { description: '예시의 설명 영역을 표시합니다.', control: 'boolean' },
    showFooter: { description: '예시의 하단 메타 영역을 표시합니다.', control: 'boolean' },
    footerDirection: {
      description: 'Footer 자식의 배치 방향입니다.',
      control: 'select',
      options: ['row', 'column'],
    },
    metaTone: {
      description: '첫 번째 Meta의 글자색입니다.',
      control: 'select',
      options: ['muted', 'brand'],
    },
    footerNote: {
      description: '예시의 두 번째 Meta 문구입니다. 비어 있으면 표시하지 않습니다.',
      control: 'text',
    },
    onClick: {
      description: '원본 카드의 클릭 콜백입니다. Clickable 예시에서 Actions로 기록합니다.',
      control: false,
    },
  },
} satisfies Meta<typeof CardExample>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
export const Minimal: Story = {
  args: {
    showBadges: false,
    showAccessory: false,
    showTrailing: false,
    showDescription: false,
    showFooter: false,
  },
};
export const LongText: Story = {
  args: {
    title: '여러 주제와 활동을 함께 다루는 아주 긴 스터디 공지 제목입니다.',
    description:
      '설명이 길어졌을 때 원본 카드의 말줄임 스타일과 주변 배치가 어떻게 표시되는지 확인하는 예시입니다.',
  },
};
export const Accessory: Story = {
  args: { showAccessory: true, showTrailing: false },
  parameters: {
    docs: {
      description: {
        story:
          'Accessory는 제목 바로 옆에 인원수 같은 보조 정보를 표시합니다. 오른쪽 끝의 Trailing과 별도로 사용합니다.',
      },
    },
  },
};
export const Trailing: Story = {
  args: { showAccessory: false, showTrailing: true },
  parameters: {
    docs: {
      description: { story: 'Trailing은 제목 행 오른쪽 끝에 화살표 같은 요소를 배치합니다.' },
    },
  },
};
export const LargeTitle: Story = { args: { titleSize: 'large' } };
export const BrandMeta: Story = { args: { metaTone: 'brand' } };
export const ColumnFooter: Story = {
  args: {
    title: '다음 모임 준비 안내',
    description: '자료를 읽고 모임에서 나눌 질문을 정리해주세요.',
    footerDirection: 'column',
    metaTone: 'brand',
    footerNote: '작성자: 총총',
  },
};
export const WithoutTruncation: Story = {
  args: {
    truncate: false,
    showBadges: false,
    showFooter: false,
    title: '말줄임 없이 스터디 제목을 표시하는 예시입니다.',
  },
};
export const Clickable: Story = {
  args: { onClick: fn() },
  parameters: {
    docs: {
      description: {
        story:
          '카드의 onClick 전달을 확인하는 예시입니다. 클릭은 Actions에만 기록합니다. 실제 서비스의 링크 이동은 카드 바깥의 Link에서 처리합니다.',
      },
    },
  },
};
