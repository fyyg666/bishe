// ESLint 9 扁平配置格式
import { defineConfig } from 'eslint/config';
import vue from 'eslint-plugin-vue';
import vueTsConfig from '@vue/eslint-config-typescript';

export default defineConfig([
  {
    name: 'vue3-rules',
    plugins: {
      vue
    },
    rules: {
      // Vue 3 推荐规则
      ...vue.configs['vue3-recommended'].rules,
      
      // 自定义规则
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'warn',
      'vue/require-explicit-emits': 'warn',
      'vue/no-unused-components': 'warn',
      'vue/no-unused-vars': 'warn',
      
      // JavaScript 规则
      'no-console': ['warn', { allow: ['warn', 'error', 'info'] }],
      'no-debugger': 'error',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-var': 'error',
      'prefer-const': 'error'
    }
  },
  
  // TypeScript 配置（如果项目使用TS）
  // ...vueTsConfig,
  
  {
    name: 'global-ignores',
    ignores: [
      'dist/**',
      'node_modules/**',
      '*.config.js',
      '*.config.ts',
      'coverage/**'
    ]
  }
]);
