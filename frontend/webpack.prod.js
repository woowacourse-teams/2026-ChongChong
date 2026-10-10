import process from 'node:process';
import { sentryWebpackPlugin } from '@sentry/webpack-plugin';
import { merge } from 'webpack-merge';
import { createCommonConfig } from './webpack.common.js';

const commonConfig = createCommonConfig({
  envFile: '.env.production',
  environment: 'production',
  assetFilename: 'assets/[name].[contenthash:16][ext]',
});
const sentryAuthToken = process.env.SENTRY_AUTH_TOKEN;

if (!sentryAuthToken) {
  throw new Error('SENTRY_AUTH_TOKEN is required for a production build.');
}

export default merge(commonConfig, {
  mode: 'production',
  devtool: 'hidden-source-map',

  output: {
    filename: '[name].[contenthash:16].js',
    chunkFilename: '[name].[contenthash:16].js',
  },

  plugins: [
    sentryWebpackPlugin({
      org: 'woowacourse-31',
      project: 'chongchong',
      authToken: sentryAuthToken,
      errorHandler(error) {
        throw error;
      },
      sourcemaps: {
        assets: './dist/**/*.{js,map}',
      },
    }),
  ],
});
