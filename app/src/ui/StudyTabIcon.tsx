import Svg, { Path } from 'react-native-svg';
import { tokens as t } from './tokens';

const icons = {
  home: {
    inactive: {
      viewBox: '0 0 22 22',
      paths: [
        {
          d: 'M2.75 9.625L11 2.75L19.25 9.625',
          strokeWidth: 2.38333,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M4.58325 9.16699V18.3337H17.4166V9.16699',
          strokeWidth: 2.38333,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      ],
    },
    active: {
      viewBox: '0 0 22 22',
      paths: [
        {
          d: 'M2.75 9.625L11 2.75L19.25 9.625',
          strokeWidth: 2.38333,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M4.58325 9.16699V18.3337H17.4166V9.16699',
          strokeWidth: 2.38333,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      ],
    },
  },
  notices: {
    inactive: {
      viewBox: '0 0 12 12',
      paths: [
        {
          d: 'M1.5 5.5L9 2V10L1.5 6.5V5.5Z',
          strokeWidth: 1.3,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M1.5 5.5V6.5C1.5 6.89782 1.65804 7.27936 1.93934 7.56066C2.22064 7.84196 2.60218 8 3 8H3.5V5.5',
          strokeWidth: 1.3,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M9 4C9.53043 4 10.0391 4.21071 10.4142 4.58579C10.7893 4.96086 11 5.46957 11 6C11 6.53043 10.7893 7.03914 10.4142 7.41421C10.0391 7.78929 9.53043 8 9 8',
          strokeWidth: 1.3,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      ],
    },
    active: {
      viewBox: '0 0 18 18',
      paths: [
        {
          d: 'M2.25 8.25L13.5 3V15L2.25 9.75V8.25Z',
          strokeWidth: 1.8,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M2.25 8.25V9.75C2.25 10.3467 2.48705 10.919 2.90901 11.341C3.33097 11.7629 3.90326 12 4.5 12H5.25V8.25',
          strokeWidth: 1.8,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M13.5 6C14.2956 6 15.0587 6.31607 15.6213 6.87868C16.1839 7.44129 16.5 8.20435 16.5 9C16.5 9.79565 16.1839 10.5587 15.6213 11.1213C15.0587 11.6839 14.2956 12 13.5 12',
          strokeWidth: 1.8,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      ],
    },
  },
  assignments: {
    inactive: {
      viewBox: '0 0 12 12',
      paths: [
        {
          d: 'M7.5 1H4.5C4.22386 1 4 1.22386 4 1.5V2.5C4 2.77614 4.22386 3 4.5 3H7.5C7.77614 3 8 2.77614 8 2.5V1.5C8 1.22386 7.77614 1 7.5 1Z',
          strokeWidth: 1.3,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M8 2H9C9.26522 2 9.51957 2.10536 9.70711 2.29289C9.89464 2.48043 10 2.73478 10 3V10C10 10.2652 9.89464 10.5196 9.70711 10.7071C9.51957 10.8946 9.26522 11 9 11H3C2.73478 11 2.48043 10.8946 2.29289 10.7071C2.10536 10.5196 2 10.2652 2 10V3C2 2.73478 2.10536 2.48043 2.29289 2.29289C2.48043 2.10536 2.73478 2 3 2H4',
          strokeWidth: 1.3,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M4.5 6.5H7.5M4.5 8.5H6.5',
          strokeWidth: 1.3,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      ],
    },
    active: {
      viewBox: '0 0 18 18',
      paths: [
        {
          d: 'M11.25 1.5H6.75C6.33579 1.5 6 1.83579 6 2.25V3.75C6 4.16421 6.33579 4.5 6.75 4.5H11.25C11.6642 4.5 12 4.16421 12 3.75V2.25C12 1.83579 11.6642 1.5 11.25 1.5Z',
          strokeWidth: 1.8,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M12 3H13.5C13.8978 3 14.2794 3.15804 14.5607 3.43934C14.842 3.72064 15 4.10218 15 4.5V15C15 15.3978 14.842 15.7794 14.5607 16.0607C14.2794 16.342 13.8978 16.5 13.5 16.5H4.5C4.10218 16.5 3.72064 16.342 3.43934 16.0607C3.15804 15.7794 3 15.3978 3 15V4.5C3 4.10218 3.15804 3.72064 3.43934 3.43934C3.72064 3.15804 4.10218 3 4.5 3H6',
          strokeWidth: 1.8,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
        {
          d: 'M6.75 9.75H11.25M6.75 12.75H9.75',
          strokeWidth: 1.8,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      ],
    },
  },
  members: {
    inactive: {
      viewBox: '0 0 23 22',
      paths: [
        {
          d: 'M14.75 18.3333V17.4167C14.75 16.4442 14.3637 15.5116 13.6761 14.8239C12.9884 14.1363 12.0558 13.75 11.0833 13.75H4.66667C3.69421 13.75 2.76158 14.1363 2.07394 14.8239C1.38631 15.5116 1 16.4442 1 17.4167V18.3333M14.2917 10.0833C15.2641 10.0833 16.1968 9.69702 16.8844 9.00939C17.572 8.32176 17.9583 7.38913 17.9583 6.41667C17.9583 5.44421 17.572 4.51157 16.8844 3.82394C16.1968 3.13631 15.2641 2.75 14.2917 2.75M21.1667 18.3333V17.4167C21.1667 16.4442 20.7804 15.5116 20.0927 14.8239C19.4051 14.1363 18.4725 13.75 17.5 13.75M11.5417 6.41667C11.5417 7.38913 11.1554 8.32176 10.4677 9.00939C9.78009 9.69702 8.84746 10.0833 7.875 10.0833C6.90254 10.0833 5.96991 9.69702 5.28227 9.00939C4.59464 8.32176 4.20833 7.38913 4.20833 6.41667C4.20833 5.44421 4.59464 4.51157 5.28227 3.82394C5.96991 3.13631 6.90254 2.75 7.875 2.75C8.84746 2.75 9.78009 3.13631 10.4677 3.82394C11.1554 4.51157 11.5417 5.44421 11.5417 6.41667Z',
          strokeWidth: 2,
          strokeLinecap: 'square',
          strokeLinejoin: 'miter',
        },
      ],
    },
    active: {
      viewBox: '0 0 23 22',
      paths: [
        {
          d: 'M14.75 18.3333V17.4167C14.75 16.4442 14.3637 15.5116 13.6761 14.8239C12.9884 14.1363 12.0558 13.75 11.0833 13.75H4.66667C3.69421 13.75 2.76158 14.1363 2.07394 14.8239C1.38631 15.5116 1 16.4442 1 17.4167V18.3333M14.2917 10.0833C15.2641 10.0833 16.1968 9.69702 16.8844 9.00939C17.572 8.32176 17.9583 7.38913 17.9583 6.41667C17.9583 5.44421 17.572 4.51157 16.8844 3.82394C16.1968 3.13631 15.2641 2.75 14.2917 2.75M21.1667 18.3333V17.4167C21.1667 16.4442 20.7804 15.5116 20.0927 14.8239C19.4051 14.1363 18.4725 13.75 17.5 13.75M11.5417 6.41667C11.5417 7.38913 11.1554 8.32176 10.4677 9.00939C9.78009 9.69702 8.84746 10.0833 7.875 10.0833C6.90254 10.0833 5.96991 9.69702 5.28227 9.00939C4.59464 8.32176 4.20833 7.38913 4.20833 6.41667C4.20833 5.44421 4.59464 4.51157 5.28227 3.82394C5.96991 3.13631 6.90254 2.75 7.875 2.75C8.84746 2.75 9.78009 3.13631 10.4677 3.82394C11.1554 4.51157 11.5417 5.44421 11.5417 6.41667Z',
          strokeWidth: 2,
          strokeLinecap: 'square',
          strokeLinejoin: 'miter',
        },
      ],
    },
  },
} as const;

export function StudyTabIcon({
  name,
  focused,
}: {
  readonly name: keyof typeof icons;
  readonly focused: boolean;
}) {
  const icon = icons[name][focused ? 'active' : 'inactive'];
  return (
    <Svg
      width={t.size.tabIcon}
      height={t.size.tabIcon}
      viewBox={icon.viewBox}
      fill="none"
      aria-hidden={true}
    >
      {icon.paths.map((path) => (
        <Path
          key={path.d}
          {...path}
          stroke={focused ? t.color.brand : t.color.placeholder}
        />
      ))}
    </Svg>
  );
}
