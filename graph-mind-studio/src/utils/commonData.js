// 路由数据
export const menuData = {
    data: [
        {
            id: 100,
            authName: "首页",
            icon: "Odometer",
            path: '/home',
            children: []
        },
        {
            id: 102,
            authName: "系统管理",
            icon: "HomeFilled",
            path: '/home/admin',
            children: [
                { id: 102001, authName: "用户管理", path: "/home/admin/user", icon: "User", children: [] },
                { id: 102004, authName: "角色管理", path: "/home/admin/role", icon: "Avatar", children: [] },
                { id: 102005, authName: "权限管理", path: "/home/admin/permission", icon: "Tools", children: [] }
            ]
        },
        {
            id: 200,
            authName: "图库管理",
            icon: "Share",
            path: '/home/graph',
            children: [
                { id: 200001, authName: "图连接管理", path: "/home/graph/connection", icon: "Connection", children: [] },
                { id: 200002, authName: "图管理", path: "/home/graph/list", icon: "Promotion", children: [] },
                { id: 200003, authName: "图可视化", path: "/home/graph/visual", icon: "DataAnalysis", children: [] },
                { id: 200004, authName: "图数据", path: "/home/graph/process", icon: "Upload", children: [] },
                { id: 200005, authName: "图分析", path: "/home/graph/analysis", icon: "DataAnalysis", children: [] }
            ]
        }
    ],
    meta: {
        msg: "获取菜单成功",
        status: 200
    }
}