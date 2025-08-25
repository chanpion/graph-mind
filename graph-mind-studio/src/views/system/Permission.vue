<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">权限管理</h2>
      <p class="page-description">管理系统权限菜单，包括菜单权限的增删改查和树形结构展示</p>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-area">
          <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
            <el-form-item label="权限名称" prop="menuName">
              <el-input
                v-model="queryParams.menuName"
                placeholder="请输入权限名称"
                clearable
                style="width: 200px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="queryParams.status"
                placeholder="权限状态"
                clearable
                style="width: 200px"
              >
                <el-option
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="action-area" style="margin-bottom: 20px;">
          <el-button type="primary" icon="Plus" @click="handleAdd">新增权限</el-button>
        </div>
      </div>


      <el-table
        v-loading="loading"
        :data="menuList"
        row-key="menuId"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        border
      >
        <el-table-column label="权限名称" prop="menuName" :show-overflow-tooltip="true" width="160" />
        <el-table-column label="图标" prop="icon" align="center" width="80">
          <template #default="scope">
            <el-icon v-if="scope.row.icon"><component :is="scope.row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" prop="perms" :show-overflow-tooltip="true" />
        <el-table-column label="组件路径" prop="component" :show-overflow-tooltip="true" />
        <el-table-column label="状态" prop="status" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" align="center" width="180">
          <template #default="scope">
            <span>{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button 
              type="primary" 
              circle 
              @click="handleUpdate(scope.row)"
              title="修改"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button 
              type="success" 
              circle 
              @click="handleAdd(scope.row)"
              title="新增"
            >
              <el-icon><Plus /></el-icon>
            </el-button>
            <el-button 
              type="danger" 
              circle 
              @click="handleDelete(scope.row)"
              title="删除"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加或修改菜单配置对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form :model="form" :rules="rules" ref="menuRef" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="menuOptions"
                :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
                node-key="menuId"
                placeholder="选择上级菜单"
                check-strictly
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType">
                <el-radio value="M">目录</el-radio>
                <el-radio value="C">菜单</el-radio>
                <el-radio value="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio
                  v-for="dict in sys_show_hide"
                  :key="dict.value"
                  :label="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="form.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.menuType == 'C'" label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="请输入组件路径" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.menuType == 'C'" label="是否缓存">
              <el-radio-group v-model="form.isCache">
                <el-radio value="0">缓存</el-radio>
                <el-radio value="1">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.menuType == 'C'" label="是否外链">
              <el-radio-group v-model="form.isFrame">
                <el-radio value="0">是</el-radio>
                <el-radio value="1">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.menuType != 'F'" label="菜单图标">
              <el-input v-model="form.icon" placeholder="请输入菜单图标" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限标识" prop="perms">
              <el-input v-model="form.perms" placeholder="请输入权限标识" maxlength="100" />
              <span class="text-tip">多个用逗号分隔，如：user:list,user:create</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Permission">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'

// 定义响应式数据
const loading = ref(false)
const open = ref(false)
const title = ref('')
const menuList = ref([])
const menuOptions = ref([])
const menuRef = ref(null)
const queryRef = ref(null)

// 状态枚举
const sys_normal_disable = ref([
  { value: '0', label: '正常' },
  { value: '1', label: '停用' }
])

const sys_show_hide = ref([
  { value: '0', label: '显示' },
  { value: '1', label: '隐藏' }
])

// 查询参数
const queryParams = reactive({
  menuName: undefined,
  perms: undefined,
  status: undefined
})

// 表单参数
const form = reactive({
  menuId: undefined,
  parentId: 0,
  menuName: undefined,
  icon: undefined,
  menuType: 'M',
  orderNum: undefined,
  isCache: '0',
  visible: '0',
  status: '0',
  path: undefined,
  component: undefined,
  perms: undefined,
  isFrame: '1'
})

// 表单校验规则
const rules = reactive({
  menuName: [
    { required: true, message: '菜单名称不能为空', trigger: 'blur' }
  ],
  orderNum: [
    { required: true, message: '菜单顺序不能为空', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '路由地址不能为空', trigger: 'blur' }
  ]
})

// 获取权限列表（模拟数据）
const getList = () => {
  loading.value = true
  // 模拟异步请求
  setTimeout(() => {
    menuList.value = [
      {
        menuId: 1,
        menuName: '系统管理',
        icon: 'system',
        menuType: 'M',
        orderNum: 1,
        isCache: '0',
        visible: '0',
        status: '0',
        path: '/system',
        component: '',
        perms: '',
        createTime: '2024-01-15 10:00:00',
        children: [
          {
            menuId: 100,
            menuName: '用户管理',
            icon: 'user',
            menuType: 'C',
            orderNum: 1,
            isCache: '0',
            visible: '0',
            status: '0',
            path: 'user',
            component: 'system/user/index',
            perms: 'system:user:list',
            createTime: '2024-01-15 10:00:00'
          }
        ]
      }
    ]
    loading.value = false
  }, 500)
}

// 获取菜单下拉树结构（模拟数据）
const getMenuTreeselect = () => {
  menuOptions.value = [
    {
      menuId: 0,
      menuName: '主类目',
      children: [
        {
          menuId: 1,
          menuName: '系统管理',
          children: [
            {
              menuId: 100,
              menuName: '用户管理'
            }
          ]
        }
      ]
    }
  ]
}

// 查询权限
const handleQuery = () => {
  getList()
}

// 重置查询
const resetQuery = () => {
  queryRef.value.resetFields()
  handleQuery()
}

// 新增按钮操作
const handleAdd = (row) => {
  getMenuTreeselect()
  open.value = true
  title.value = '添加权限'
  reset()
  if (row && row.menuId) {
    form.parentId = row.menuId
  }
}

// 修改按钮操作
const handleUpdate = (row) => {
  getMenuTreeselect()
  getMenu(row.menuId).then(response => {
    form.menuId = response.menuId
    form.parentId = response.parentId
    form.menuName = response.menuName
    form.icon = response.icon
    form.menuType = response.menuType
    form.orderNum = response.orderNum
    form.isCache = response.isCache
    form.visible = response.visible
    form.status = response.status
    form.path = response.path
    form.component = response.component
    form.perms = response.perms
    form.isFrame = response.isFrame
    open.value = true
    title.value = '修改权限'
  })
}

// 获取权限详细信息（模拟）
const getMenu = (menuId) => {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        menuId: menuId,
        parentId: menuId === 100 ? 1 : 0,
        menuName: menuId === 1 ? '系统管理' : menuId === 100 ? '用户管理' : '',
        icon: menuId === 1 ? 'system' : menuId === 100 ? 'user' : '',
        menuType: menuId === 1 ? 'M' : menuId === 100 ? 'C' : 'M',
        orderNum: menuId === 1 ? 1 : menuId === 100 ? 1 : 1,
        isCache: '0',
        visible: '0',
        status: '0',
        path: menuId === 1 ? '/system' : menuId === 100 ? 'user' : '',
        component: menuId === 100 ? 'system/user/index' : '',
        perms: menuId === 100 ? 'system:user:list' : menuId === 1001 ? 'system:user:query' : '',
        isFrame: '1'
      })
    }, 300)
  })
}

// 提交表单
const submitForm = () => {
  // 这里应该调用API保存数据
  ElMessage.success(`${title.value}成功`)
  open.value = false
  getList()
}

// 取消按钮
const cancel = () => {
  open.value = false
  reset()
}

// 表单重置
const reset = () => {
  form.menuId = undefined
  form.parentId = 0
  form.menuName = undefined
  form.icon = undefined
  form.menuType = 'M'
  form.orderNum = undefined
  form.isCache = '0'
  form.visible = '0'
  form.status = '0'
  form.path = undefined
  form.component = undefined
  form.perms = undefined
  form.isFrame = '1'
}

// 删除按钮操作
const handleDelete = (row) => {
  ElMessageBox.confirm(
    '是否确认删除名称为"' + row.menuName + '"的数据项？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 模拟删除操作
    ElMessage.success('删除成功')
    getList()
  })
}

// 初始化
onMounted(() => {
  getList()
})
</script>

<style scoped>
.connection-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.text-tip {
  color: #999;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .search-area {
    justify-content: center;
  }
  
  .action-area {
    justify-content: center;
  }
}
</style>