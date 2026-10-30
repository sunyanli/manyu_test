import { defineConfig } from 'bigfish';

/**
 * xchain 演示前端配置。
 */
export default defineConfig({
  antd: {},
  request: {},
  routes: [
    {
      path: '/demo/api-demo',
      name: '接口演示中心',
      component: './Demo/ApiDemo',
    },
    {
      path: '/',
      redirect: '/demo/api-demo',
    },
  ],
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:8080',
      changeOrigin: true,
    },
  },
  npmClient: 'tnpm',
});
