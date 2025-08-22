# 菜单结构说明

## 一级菜单

### 首页
- 路由: `/`
- 图标: HomeFilled

### 图管理
- 路由: `/graph`
- 图标: Promotion

### 系统管理
- 路由: `/system`
- 图标: Setting

## 二级菜单

### 图管理
- 图列表: `/graph/list`
- 图连接: `/graph/connection`
- 数据加工: `/graph/process`
- 图数据管理: `/graph/data`
- 图分析: `/graph/analysis`

### 系统管理
- 用户管理: `/system/user`
- 角色管理: `/system/role`
- 权限管理: `/system/permission`
- 个人中心: `/system/profile`
- 系统设置: `/system/settings`

## 当前菜单结构

### 1. 图库管理 (ID: 200)
- **图标**: Share
- **路径**: `/home/graph`
- **子菜单**:
  - 图连接管理 (ID: 200001)
    - **路径**: `/home/graph/connection`
    - **图标**: Link
    - **功能**: 管理图数据库连接配置

### 2. 系统管理 (ID: 102)
- **图标**: HomeFilled
- **路径**: `/home/admin`
- **子菜单**:
  - 用户管理 (ID: 102001)
    - **路径**: `/home/admin/user`
    - **图标**: Menu
    - **功能**: 管理系统用户
  - 岗位管理 (ID: 102002)
    - **路径**: `/home/admin/station`
    - **图标**: Pointer
    - **功能**: 管理岗位信息
  - 部门管理 (ID: 102003)
    - **路径**: `/home/admin/dept`
    - **图标**: EditPen
    - **功能**: 管理部门结构
  - 角色管理 (ID: 102004)
    - **路径**: `/home/admin/role`
    - **图标**: Food
    - **功能**: 管理系统角色
  - 权限管理 (ID: 102005)
    - **路径**: `/home/admin/permission`
    - **图标**: Opportunity
    - **功能**: 管理系统权限

## 菜单配置

### 配置文件位置
- **菜单数据**: `src/utils/commonData.js`
- **路由配置**: `src/router/index.js`
- **图标配置**: `src/utils/icons.js`

### 菜单数据结构
``javascript
{
    id: 200,                    // 菜单ID
    authName: "图库管理",        // 菜单名称
    icon: "Share",             // 菜单图标
    path: '/home/graph',       // 菜单路径
    children: [                // 子菜单数组
        {
            id: 200001,
            authName: "图连接管理",
            path: "/home/graph/connection",
            icon: "Link",
            children: []
        }
    ]
}
```

## 路由配置

### 路由结构
``javascript
{
    path: 'graph',                    // 路由路径
    name: 'Graph',                    // 路由名称
    component: MainLayout,            // 布局组件
    meta: { 
        title: '图库管理',            // 页面标题
        closable: true               // 是否可关闭
    },
    children: [                       // 子路由
        {
            path: 'connection',
            name: 'Connection',
            component: Connection,
            meta: { 
                title: '图连接管理',
                closable: true 
            }
        }
    ]
}
```

## 图标配置

### 支持的图标
- **Share**: 图库管理主菜单
- **Link**: 图连接管理
- **Menu**: 用户管理
- **Pointer**: 岗位管理
- **EditPen**: 部门管理
- **Food**: 角色管理
- **Opportunity**: 权限管理
- **HomeFilled**: 系统管理主菜单

### 图标来源
所有图标均来自 Element Plus Icons Vue 库。

## 菜单功能

### 1. 多页签支持
- 每个菜单项都可以在独立页签中打开
- 支持页签的关闭和切换
- 页签状态持久化存储

### 2. 面包屑导航
- 自动生成面包屑导航
- 显示当前页面路径
- 支持点击跳转

### 3. 权限控制
- 基于角色的菜单权限控制
- 支持菜单项的显示/隐藏
- 支持按钮级别的权限控制

### 4. 响应式设计
- 支持菜单折叠/展开
- 移动端适配
- 自适应布局

## 开发指南

### 添加新菜单

#### 1. 更新菜单数据
在 `src/utils/commonData.js` 中添加菜单配置：
``javascript
{
    id: 300,
    authName: "新功能模块",
    icon: "NewIcon",
    path: '/home/newmodule',
    children: [
        {
            id: 300001,
            authName: "子功能",
            path: "/home/newmodule/subfunction",
            icon: "SubIcon",
            children: []
        }
    ]
}
```

#### 2. 添加图标
在 `src/utils/icons.js` 中导入新图标：
``javascript
import { NewIcon, SubIcon } from '@element-plus/icons-vue'

export const iconComponents = {
    // ... 其他图标
    NewIcon,
    SubIcon
}
```

#### 3. 创建页面组件
在 `src/views/` 下创建对应的页面组件。

#### 4. 配置路由
在 `src/router/index.js` 中添加路由配置：
``javascript
import NewComponent from '@/views/newmodule/NewComponent.vue'

// 在路由配置中添加
{
    path: 'newmodule',
    name: 'NewModule',
    component: MainLayout,
    meta: { title: '新功能模块', closable: true },
    children: [
        {
            path: 'subfunction',
            name: 'SubFunction',
            component: NewComponent,
            meta: { title: '子功能', closable: true }
        }
    ]
}
```

### 删除菜单

#### 1. 删除菜单数据
从 `src/utils/commonData.js` 中移除对应的菜单配置。

#### 2. 删除路由配置
从 `src/router/index.js` 中移除对应的路由配置。

#### 3. 删除页面组件
删除对应的页面组件文件。

#### 4. 清理图标
如果图标不再使用，可以从 `src/utils/icons.js` 中移除。

## 注意事项

### 1. ID 唯一性
- 菜单ID必须唯一
- 建议使用有规律的ID分配策略
- 避免ID冲突

### 2. 路径规范
- 路径使用小写字母
- 使用连字符分隔单词
- 保持路径的语义化

### 3. 图标选择
- 选择语义化的图标
- 保持图标风格统一
- 考虑图标的可识别性

### 4. 权限控制
- 在菜单配置中添加权限字段
- 实现基于权限的菜单过滤
- 考虑权限的继承关系

## 最佳实践

### 1. 菜单层级
- 建议不超过3级菜单
- 保持菜单结构清晰
- 避免菜单项过多

### 2. 命名规范
- 使用中文菜单名称
- 保持命名的一致性
- 避免使用缩写

### 3. 图标使用
- 主菜单使用大图标
- 子菜单使用小图标
- 保持图标的语义化

### 4. 路由设计
- 使用RESTful风格的路由
- 保持路由的层次结构
- 考虑路由的可扩展性 