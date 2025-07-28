// 路由数据
export const menuData = {
    data: [
        {
            id: 102,
            authName: "系统管理",
            icon: "HomeFilled",
            path: '/home/admin',
            children: [
                { id: 102001, authName: "用户管理", path: "/home/admin/user", icon: "Menu", children: [] },
                { id: 102004, authName: "角色管理", path: "/home/admin/role", icon: "Food", children: [] },
                { id: 102005, authName: "权限管理", path: "/home/admin/permission", icon: "Opportunity", children: [] }
            ]
        },
        {
            id: 200,
            authName: "图库管理",
            icon: "Share",
            path: '/home/graph',
            children: [
                { id: 200001, authName: "图连接管理", path: "/home/graph/connection", icon: "Link", children: [] },
                { id: 200002, authName: "图管理", path: "/home/graph/list", icon: "Share", children: [] },
                { id: 200003, authName: "图谱可视化", path: "/home/graph/visual", icon: "Share", children: [] }
            ]
        }
    ],
    meta: {
        msg: "获取菜单成功",
        status: 200
    }
} 