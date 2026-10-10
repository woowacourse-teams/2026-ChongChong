import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';
import { merge } from 'webpack-merge';
import { createCommonConfig, createSentryPlugin } from './webpack.common.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const commonConfig = createCommonConfig({
  envFile: '.env.local',
  environment: 'development',
  assetFilename: 'assets/[name][ext]',
});
const isDeploymentBuild = process.env.DEPLOY_ENV === 'dev';
const sentryAuthToken = process.env.SENTRY_AUTH_TOKEN;

if (isDeploymentBuild && !sentryAuthToken) {
  throw new Error('SENTRY_AUTH_TOKEN is required for a development deployment build.');
}

export default merge(commonConfig, {
  mode: 'development',
  devtool: 'source-map',

  output: {
    filename: 'bundle.js',
    chunkFilename: '[name].js',
  },

  plugins: isDeploymentBuild ? [createSentryPlugin(sentryAuthToken)] : [],

  devServer: {
    static: [
      {
        directory: path.join(__dirname, 'dist'),
      },
      {
        directory: path.join(__dirname, 'public'),
      },
      {
        directory: path.join(__dirname, 'src/pwa'),
        publicPath: '/pwa',
      },
    ],
    port: 3005,
    open: true,
    hot: true,
    historyApiFallback: true,
    client: {
      overlay: true,
    },
    proxy: process.env.DEV_API_PROXY_TARGET
      ? [
          {
            context: ['/api'],
            target: process.env.DEV_API_PROXY_TARGET,
            changeOrigin: true,
            secure: true,
          },
        ]
      : [],
  },
});
