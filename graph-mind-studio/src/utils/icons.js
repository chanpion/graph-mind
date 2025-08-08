import {
    User, Avatar, Pointer, Menu,
    EditPen, Tools, HomeFilled, Food,
    Opportunity, Fold, Expand, Share, Link,
    Connection, Promotion, DataAnalysis, Upload,
    Odometer
} from '@element-plus/icons-vue'

// 图标映射对象
export const iconComponents = {
    User,
    Avatar,
    Pointer,
    Menu,
    EditPen,
    Tools,
    HomeFilled,
    Food,
    Opportunity,
    Fold,
    Expand,
    Share,
    Link,
    Connection,
    Promotion,
    DataAnalysis,
    Upload,
    Odometer
}

// 获取图标函数
export const getIcon = (iconName) => {
    return iconComponents[iconName] || User  // 默认返回User图标
}