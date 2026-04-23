<template>
  <div class="not-found">
    <div class="error-content">
      <h1>404</h1>
      <h2>页面不存在</h2>
      <p>抱歉，您访问的页面不存在或已被移除</p>
      <el-button type="primary" @click="$router.push('/')">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.not-found {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;

  .error-content {
    text-align: center;

    h1 {
      font-size: 120px;
      font-weight: bold;
      color: #409eff;
      margin-bottom: 20px;
      line-height: 1;
    }

    h2 {
      font-size: 30px;
      color: #303133;
      margin-bottom: 15px;
    }

    p {
      font-size: 16px;
      color: #909399;
      margin-bottom: 30px;
    }
  }
}
</style>
