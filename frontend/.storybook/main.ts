import type { StorybookConfig } from '@storybook/react-webpack5';
import { fileURLToPath } from 'node:url';

const config: StorybookConfig = {
  stories: ['../src/**/*.stories.tsx'],
  addons: ['@storybook/addon-docs'],
  framework: '@storybook/react-webpack5',
  webpackFinal: async (config) => {
    config.module?.rules?.push({
      test: /\.[jt]sx?$/,
      exclude: /node_modules/,
      use: {
        loader: 'babel-loader',
        options: {
          configFile: fileURLToPath(new URL('../babel.config.mjs', import.meta.url)),
        },
      },
    });
    return config;
  },
};

export default config;
