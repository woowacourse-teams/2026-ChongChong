import { css } from '@emotion/react';
import { modernNormalize } from './normalize';

export const globalStyles = css`
  :root {
    /* Primitive Green */

    --green-50: #e5f9f0;
    --green-500: #00c472;
    --green-600: #00aa63;
    --green-800: #007544;

    /* Primitive Gray */

    --gray-50: #f9f9f9;
    --gray-100: #f4f4f4;
    --gray-200: #ededed;
    --gray-300: #d0d0d0;
    --gray-400: #adadad;
    --gray-500: #9fa2ab;
    --gray-600: #6b7079;
    --gray-700: #4a4f58;
    --gray-800: #2e323a;
    --gray-900: #1f2229;

    /* Primitive Red */

    --red-500: #de5e55;
    --red-600: #c74a42;

    /* Primitive Social */

    --social-kakao: #fee500;
    --social-apple: #000000;
    --social-google: #ffffff;

    /* Semantic Background */

    --bg-default: white;
    --bg-subtle: var(--gray-50);
    --bg-brand: var(--green-500);
    --bg-brand-subtle: var(--green-50);
    --bg-chip: var(--gray-100);
    --bg-disabled: var(--gray-500);
    --bg-dim: rgba(0, 0, 0, 0.4);

    /* Semantic Border */

    --border-default: var(--gray-300);
    --border-subtle: var(--gray-200);
    --border-brand: var(--green-500);
    --border-danger: var(--red-500);

    /* Semantic Text */

    --text-default: var(--gray-900);
    --text-secondary: var(--gray-600);
    --text-placeholder: var(--gray-400);
    --text-brand: var(--green-800);
    --text-danger: var(--red-500);
    --text-on-brand: white;
    --text-on-brand-strong: var(--gray-900);

    /* Layout */

    --layout-gutter: 16px;
    --layout-header-height: 48px;
    --layout-tab-bar-height: 56px;
    --safe-top: env(safe-area-inset-top, 0px);
    --safe-bottom: env(safe-area-inset-bottom, 0px);

    /* Spacing */

    --spacing-1: 4px;
    --spacing-2: 8px;
    --spacing-3: 12px;
    --spacing-4: 16px;
    --spacing-5: 20px;
    --spacing-6: 24px;
    --spacing-8: 32px;
    --spacing-10: 40px;
    --spacing-12: 48px;
    --spacing-16: 64px;

    /* Radius */

    --radius-sm: 4px;
    --radius-md: 8px;
    --radius-lg: 12px;
    --radius-xl: 16px;
    --radius-full: 9999px;

    /* Elevation */

    --shadow-1: 0 1px 2px rgba(0, 0, 0, 0.06);
    --shadow-2: 0 4px 12px rgba(0, 0, 0, 0.1);
    --shadow-3: 0 12px 32px rgba(0, 0, 0, 0.16);

    /* Typography Family */
    --font-family-base:
      'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui,
      'Apple SD Gothic Neo', sans-serif;

    /* Typography Weight */
    --font-weight-regular: 400;
    --font-weight-medium: 500;
    --font-weight-semibold: 600;
    --font-weight-bold: 700;

    /* Typography Size */
    --font-size-24: 24px;
    --font-size-20: 20px;
    --font-size-18: 18px;
    --font-size-16: 16px;
    --font-size-14: 14px;
    --font-size-13: 13px;
    --font-size-12: 12px;
    --font-size-11: 11px;

    /* Typography Line height */
    --line-height-tight: 1.3;
    --line-height-normal: 1.5;
    --line-height-relaxed: 1.7;

    /* Typography Letter spacing */
    --letter-spacing-tight: -0.02em;
    --letter-spacing-normal: -0.01em;
  }
  ${modernNormalize}
`;

export const typography = {
  headline: css`
    font-size: var(--font-size-24);
    font-weight: var(--font-weight-bold);
    line-height: var(--line-height-tight);
    letter-spacing: var(--letter-spacing-tight);
  `,

  title: css`
    font-size: var(--font-size-20);
    font-weight: var(--font-weight-bold);
    line-height: var(--line-height-tight);
    letter-spacing: var(--letter-spacing-tight);
  `,

  subtitle: css`
    font-size: var(--font-size-18);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-tight);
    letter-spacing: var(--letter-spacing-tight);
  `,

  sectionLabel: css`
    font-size: var(--font-size-16);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,

  button: css`
    font-size: var(--font-size-16);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,

  body: css`
    font-size: var(--font-size-14);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,

  bodyStrong: css`
    font-size: var(--font-size-14);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,
  paragraph: css`
    font-size: var(--font-size-14);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-relaxed);
    letter-spacing: var(--letter-spacing-normal);
  `,

  caption: css`
    font-size: var(--font-size-13);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,

  label: css`
    font-size: var(--font-size-12);
    font-weight: var(--font-weight-medium);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,

  footnote: css`
    font-size: var(--font-size-11);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-normal);
    letter-spacing: var(--letter-spacing-normal);
  `,
};
