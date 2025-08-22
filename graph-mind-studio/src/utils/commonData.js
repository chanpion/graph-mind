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
            id: 101,
            authName: "系统管理",
            icon: "Tools",  // 将HomeFilled改为Tools，更符合系统管理的语义
            path: '/home/admin',
            children: [
                { id: 101001, authName: "用户管理", path: "/home/admin/user", icon: "User", children: [] },
                { id: 102002, authName: "角色管理", path: "/home/admin/role", icon: "Avatar", children: [] },  // 将Food改为Avatar，更适合角色管理
                { id: 101003, authName: "权限管理", path: "/home/admin/permission", icon: "Opportunity", children: [] },
                { id: 101004, authName: "系统配置", path: "/home/admin/config", icon: "Setting", children: [] }
            ]
        },
        {
            id: 200,
            authName: "图库管理",
            icon: "Share",
            path: '/home/graph',
            children: [
                { id: 200001, authName: "图连接管理", path: "/home/graph/connection", icon: "Connection", children: [] },
                { id: 200002, authName: "图管理", path: "/home/graph/list", icon: "DataAnalysis", children: [] },  // 将Promotion改为DataAnalysis，更适合图管理
                { id: 200003, authName: "图设计", path: "/home/graph/detail/:id", icon: "EditPen", children: [] },   // 将Promotion改为EditPen，更适合图设计
                { id: 200004, authName: "图可视化", path: "/home/graph/visual", icon: "DataAnalysis", children: [] },
                { id: 200005, authName: "图加工", path: "/home/graph/process", icon: "Upload", children: [] },
                { id: 200006, authName: "图数据", path: "/home/graph/data", icon: "DataAnalysis", children: [] }
            ]
        }
    ],
    meta: {
        msg: "获取菜单成功",
        status: 200
    }
}