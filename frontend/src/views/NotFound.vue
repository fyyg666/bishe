<template>
  <div class="not-found">
    <div class="error-content">
      <h1>404</h1>
      <h2>页面不存在</h2>
      <p>抱歉，您访问的页面不存在或已被移除</p>
      <el-button
        type="primary"
        @click="$router.push('/')"
      >
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.not-found {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-page;

  .error-content {
    text-align: center;

    h1 {
      font-size: 120px;
      font-weight: bold;
      color: $primary;
      margin-bottom: $space-5;
      line-height: 1;
    }

    h2 {
      font-size: 30px;
      color: $text-primary;
      margin-bottom: $space-3;
    }

    p {
      font-size: $font-size-lg;
      color: $text-secondary;
      margin-bottom: $space-8;
    }
  }
}
</style>
