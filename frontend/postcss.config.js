// PostCSS 配置文件
// 用于自动添加CSS前缀，确保跨浏览器兼容性

export default {
  plugins: {
    autoprefixer: {
      // 从 .browserslistrc 读取浏览器兼容性配置
      overrideBrowserslist: undefined, // 使用默认的 .browserslistrc
      grid: true, // 启用 CSS Grid 的 IE 前缀（如果支持 IE）
      flexbox: 'no-2009' // 使用现代 flexbox 语法
    }
  }
};
