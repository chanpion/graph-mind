# 登录功能说明

## 功能特性

### 1. 登录页面
- 🎨 **美观的UI设计**：采用现代化的渐变背景和毛玻璃效果
- 📱 **响应式布局**：支持桌面端和移动端
- 🔐 **表单验证**：完整的输入验证和错误提示
- 🖼️ **验证码功能**：动态生成的图形验证码
- 💾 **记住我功能**：自动保存用户名

### 2. 安全特性
- 🔑 **Token认证**：基于JWT token的身份验证
- 🛡️ **路由守卫**：自动保护需要登录的页面
- 🔄 **自动跳转**：未登录用户自动跳转到登录页
- 🚪 **安全退出**：清除所有登录状态

### 3. 用户体验
- ⚡ **快速响应**：优化的加载状态和反馈
- 🎯 **智能提示**：友好的错误信息和成功提示
- 🔄 **状态持久化**：登录状态在页面刷新后保持
- 👤 **用户信息显示**：在系统头部显示当前用户信息

## 测试账号

### 管理员账号
- **用户名**: `admin`
- **密码**: `123456`

### 测试步骤
1. 访问 `/login` 页面
2. 输入用户名：`admin`
3. 输入密码：`123456`
4. 输入验证码（点击图片可刷新）
5. 点击"登录"按钮
6. 登录成功后自动跳转到系统首页

## 技术实现

### 1. 状态管理
使用 Pinia 进行用户状态管理：
```javascript
// 用户store
const userStore = useUserStore()

// 登录
userStore.login(token, userInfo)

// 退出登录
userStore.logout()

// 检查登录状态
userStore.isLoggedIn
```

### 2. 路由守卫
```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path === '/login') {
    if (token) {
      next('/home')
    } else {
      next()
    }
    return
  }
  
  if (!token) {
    next('/login')
    return
  }
  
  next()
})
```

### 3. 验证码生成
使用 Canvas API 动态生成验证码：
- 随机字符生成
- 旋转和变形效果
- 干扰线和干扰点
- 点击刷新功能

### 4. 表单验证
使用 Element Plus 的表单验证：
- 必填项验证
- 长度限制验证
- 实时验证反馈

## 文件结构

```
src/
├── views/
│   └── Login.vue              # 登录页面组件
├── stores/
│   └── user.js               # 用户状态管理
├── router/
│   └── index.js              # 路由配置和守卫
└── components/
    └── layout/
        └── ...               # 布局组件
```

## 自定义配置

### 1. 修改登录API
在 `Login.vue` 中修改 `loginAPI` 函数：
```javascript
const loginAPI = (data) => {
  return new Promise((resolve, reject) => {
    // 替换为实际的API调用
    fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(resolve)
    .catch(reject)
  })
}
```

### 2. 修改验证规则
在 `Login.vue` 中修改 `loginRules`：
```javascript
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  // 添加更多验证规则...
}
```

### 3. 自定义样式
在 `Login.vue` 的 `<style>` 部分修改样式：
```css
.login-container {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  /* 修改背景色 */
}
```

## 注意事项

1. **安全性**：当前使用模拟API，生产环境需要替换为真实的后端接口
2. **Token存储**：Token存储在localStorage中，可根据需要改为sessionStorage或cookie
3. **验证码**：当前验证码为前端生成，生产环境建议使用后端生成
4. **密码加密**：生产环境需要对密码进行加密传输
5. **错误处理**：可以根据实际需求添加更详细的错误处理逻辑

## 扩展功能

### 1. 添加注册功能
- 创建注册页面
- 添加注册表单验证
- 实现注册API调用

### 2. 添加忘记密码功能
- 创建忘记密码页面
- 实现邮箱验证
- 添加密码重置功能

### 3. 添加第三方登录
- 微信登录
- QQ登录
- 钉钉登录

### 4. 添加多因素认证
- 短信验证码
- 邮箱验证码
- 谷歌验证器 