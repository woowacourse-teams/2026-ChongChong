import Svg, { Path } from 'react-native-svg';
import { tokens as t } from '../../ui/tokens';

export function ActivityIcon({
  kind,
}: {
  readonly kind: 'notice' | 'assignment';
}) {
  return (
    <Svg width={18} height={18} viewBox="0 0 18 18" fill="none" aria-hidden>
      {kind === 'notice' ? (
        <>
          <Path
            d="M2.25 8.25L13.5 3V15L2.25 9.75V8.25Z"
            stroke={t.color.brand}
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <Path
            d="M2.25 8.25V9.75C2.25 10.3467 2.48705 10.919 2.90901 11.341C3.33097 11.7629 3.90326 12 4.5 12H5.25V8.25"
            stroke={t.color.brand}
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <Path
            d="M13.5 6C14.2956 6 15.0587 6.31607 15.6213 6.87868C16.1839 7.44129 16.5 8.20435 16.5 9C16.5 9.79565 16.1839 10.5587 15.6213 11.1213C15.0587 11.6839 14.2956 12 13.5 12"
            stroke={t.color.brand}
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      ) : (
        <>
          <Path
            d="M11.25 1.5H6.75C6.33579 1.5 6 1.83579 6 2.25V3.75C6 4.16421 6.33579 4.5 6.75 4.5H11.25C11.6642 4.5 12 4.16421 12 3.75V2.25C12 1.83579 11.6642 1.5 11.25 1.5Z"
            stroke={t.color.brand}
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <Path
            d="M12 3H13.5C13.8978 3 14.2794 3.15804 14.5607 3.43934C14.842 3.72064 15 4.10218 15 4.5V15C15 15.3978 14.842 15.7794 14.5607 16.0607C14.2794 16.342 13.8978 16.5 13.5 16.5H4.5C4.10218 16.5 3.72064 16.342 3.43934 16.0607C3.15804 15.7794 3 15.3978 3 15V4.5C3 4.10218 3.15804 3.72064 3.43934 3.43934C3.72064 3.15804 4.10218 3 4.5 3H6"
            stroke={t.color.brand}
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <Path
            d="M6.75 9.75H11.25M6.75 12.75H9.75"
            stroke={t.color.brand}
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}
    </Svg>
  );
}
