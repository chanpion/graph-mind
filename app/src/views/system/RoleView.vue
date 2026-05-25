<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <p class="page-description">管理系统角色，包括添加、编辑、删除角色以及分配菜单权限和数据权限</p>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-area">
          <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
            <el-form-item label="名称" prop="roleName">
              <el-input
                v-model="queryParams.roleName"
                placeholder="请输入角色名称"
                clearable
                style="width: 200px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="queryParams.status"
                placeholder="角色状态"
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
              <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
              <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="action-area" style="margin-bottom: 20px;">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增角色</el-button>
          <el-button type="danger" :icon="Delete" :disabled="multipleSelection.length === 0" @click="handleDelete">删除</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="roleList"
        @selection-change="handleSelectionChange"
        border
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="角色编号" align="center" prop="roleId" width="100" />
        <el-table-column label="角色名称" align="center" prop="roleName" :show-overflow-tooltip="true" />
        <el-table-column label="权限字符" align="center" prop="roleKey" :show-overflow-tooltip="true" />
        <el-table-column label="显示顺序" align="center" prop="roleSort" width="100" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
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
              type="danger"
              circle
              @click="handleDelete(scope.row)"
              title="删除"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button
              type="warning"
              circle
              @click="handleDataScope(scope.row)"
              title="数据权限"
            >
              <el-icon><Lock /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <!-- 添加或修改角色配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="roleRef" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="请输入角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限字符" prop="roleKey">
              <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="roleSort">
              <el-input-number v-model="form.roleSort" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
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
        <el-row>
          <el-col :span="12">
            <el-form-item label="菜单权限">
              <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">展开/折叠</el-checkbox>
              <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">全选/全不选</el-checkbox>
              <el-checkbox v-model="form.menuCheckStrictly" @change="handleCheckedTreeConnect($event, 'menu')">父子联动</el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item>
                <el-tree
                  class="tree-border"
                  ref="menuRef"
                  :data="menuOptions"
                  show-checkbox
                  node-key="id"
                  :check-strictly="!form.menuCheckStrictly"
                  empty-text="加载中，请稍候"
                  :props="{ label: 'label', children: 'children' }"
                ></el-tree>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
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

      <!-- 分配角色数据权限对话框 -->
      <el-dialog :title="title" v-model="openDataScope" width="500px" append-to-body>
        <el-form :model="form" label-width="80px">
          <el-form-item label="角色名称">
            <el-input v-model="form.roleName" :disabled="true" />
          </el-form-item>
          <el-form-item label="权限字符">
            <el-input v-model="form.roleKey" :disabled="true" />
          </el-form-item>
          <el-form-item label="数据权限">
            <el-select v-model="form.dataScope" style="width: 100%">
              <el-option
                v-for="item in dataScopeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button type="primary" @click="submitDataScope">确 定</el-button>
            <el-button @click="cancelDataScope">取 消</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </template>

  <script setup name="Role">
  import { ref, reactive, onMounted } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {Plus, Search, Refresh, Edit, Delete, Lock} from '@element-plus/icons-vue'
  import Pagination from '@/components/Pagination.vue'
  import { roleApi } from '@/api/role'
  import { permissionApi } from '@/api/permission'

  // 定义响应式数据
  const loading = ref(false)
  const open = ref(false)
  const openDataScope = ref(false)
  const title = ref('')
  const menuExpand = ref(false)
  const menuNodeAll = ref(false)
  const multipleSelection = ref([])
  const roleList = ref([])
  const menuOptions = ref([])
  const menuRef = ref(null)
  const roleRef = ref(null)
  const queryRef = ref(null)
  const total = ref(0)

  // 数据范围选项
  const dataScopeOptions = ref([
    { value: '1', label: '全部数据权限' },
    { value: '2', label: '自定义数据权限' },
    { value: '3', label: '本部门数据权限' },
    { value: '4', label: '本部门及以下数据权限' },
    { value: '5', label: '仅本人数据权限' }
  ])

  // 状态枚举
  const sys_normal_disable = ref([
    { value: '0', label: '正常' },
    { value: '1', label: '停用' }
  ])

  // 查询参数
  const queryParams = reactive({
    pageNum: 1,
    pageSize: 10,
    roleName: undefined,
    roleKey: undefined,
    status: undefined
  })

  // 表单参数
  const form = reactive({
    roleId: undefined,
    roleName: undefined,
    roleKey: undefined,
    roleSort: 0,
    status: '0',
    menuIds: [],
    menuCheckStrictly: true,
    remark: undefined
  })

  // 表单校验规则
  const rules = reactive({
    roleName: [
      { required: true, message: '角色名称不能为空', trigger: 'blur' }
    ],
    roleKey: [
      { required: true, message: '权限字符不能为空', trigger: 'blur' }
    ],
    roleSort: [
      { required: true, message: '角色顺序不能为空', trigger: 'blur' }
    ]
  })

  // 获取角色列表
  const getList = () => {
    loading.value = true
    const params = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      roleName: queryParams.roleName,
      roleKey: queryParams.roleKey,
      status: queryParams.status
    }

    roleApi.list(params).then(response => {
      if (response.code === 200) {
        roleList.value = response.data.records || response.data
        total.value = response.data.total || 0
      } else {
        ElMessage.error(response.message || '获取角色列表失败')
      }
      loading.value = false
    }).catch(error => {
      console.error('获取角色列表失败:', error)
      ElMessage.error('获取角色列表失败')
      loading.value = false
    })
  }

  // 获取菜单树结构
  const getMenuTreeselect = () => {
    permissionApi.list().then(response => {
      if (response.code === 200) {
        menuOptions.value = response.data || []
      }
    }).catch(error => {
      console.error('获取菜单树失败:', error)
    })
  }

  // 查询角色
  const handleQuery = () => {
    queryParams.pageNum = 1
    getList()
  }

  // 重置查询
  const resetQuery = () => {
    queryRef.value.resetFields()
    handleQuery()
  }

  // 多选框选中数据
  const handleSelectionChange = (selection) => {
    multipleSelection.value = selection
  }

  // 角色状态修改
  const handleStatusChange = (row) => {
    const text = row.status === '0' ? '启用' : '停用'
    ElMessageBox.confirm(
      '确认要' + text + ' "' + row.roleName + '" 角色吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      roleApi.updateStatus(row.roleId, row.status).then(response => {
        if (response.code === 200) {
          ElMessage.success(text + '成功')
          getList()
        }
      })
    }).catch(() => {})
  }

  // 新增按钮操作
  const handleAdd = () => {
    getMenuTreeselect()
    open.value = true
    title.value = '添加角色'
    reset()
  }

  // 修改按钮操作
  const handleUpdate = (row) => {
    getMenuTreeselect()
    const roleId = row.roleId || multipleSelection.value[0].roleId
    getRole(roleId).then(response => {
      form.roleId = response.roleId
      form.roleName = response.roleName
      form.roleKey = response.roleKey
      form.roleSort = response.roleSort
      form.status = response.status
      form.menuCheckStrictly = response.menuCheckStrictly
      form.remark = response.remark
      open.value = true
      title.value = '修改角色'
    })
  }

  // 获取角色详细信息
  const getRole = (roleId) => {
    return roleApi.get(roleId).then(response => {
      if (response.code === 200) {
        return response.data
      }
      return Promise.reject(new Error(response.message || '获取角色信息失败'))
    })
  }

  // 提交表单
  const submitForm = () => {
    roleRef.value.validate(valid => {
      if (!valid) return

      const menuIds = menuRef.value ? menuRef.value.getCheckedKeys() : []
      const data = {
        roleName: form.roleName,
        roleKey: form.roleKey,
        roleSort: form.roleSort,
        status: form.status,
        menuIds: menuIds,
        menuCheckStrictly: form.menuCheckStrictly,
        remark: form.remark
      }

      if (form.roleId) {
        roleApi.update(form.roleId, data).then(response => {
          if (response.code === 200) {
            ElMessage.success('修改成功')
            open.value = false
            getList()
          }
        }).catch(error => {
          console.error('更新角色失败:', error)
        })
      } else {
        roleApi.create(data).then(response => {
          if (response.code === 200) {
            ElMessage.success('新增成功')
            open.value = false
            getList()
          }
        }).catch(error => {
          console.error('新增角色失败:', error)
        })
      }
    })
  }

  // 取消按钮
  const cancel = () => {
    open.value = false
    reset()
  }

  // 表单重置
  const reset = () => {
    form.roleId = undefined
    form.roleName = undefined
    form.roleKey = undefined
    form.roleSort = 0
    form.status = '0'
    form.menuIds = []
    form.menuCheckStrictly = true
    form.remark = undefined
    menuRef.value?.setCheckedKeys([])
  }

  // 删除按钮操作
  const handleDelete = (row) => {
    const roleIds = row.roleId ? [row.roleId] : multipleSelection.value.map(item => item.roleId)
    if (!roleIds || roleIds.length === 0) return

    ElMessageBox.confirm(
      '是否确认删除角色编号为"' + roleIds.join(',') + '"的数据项？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      roleApi.delete(roleIds).then(response => {
        if (response.code === 200) {
          ElMessage.success('删除成功')
          getList()
        }
      }).catch(error => {
        console.error('删除角色失败:', error)
      })
    }).catch(() => {})
  }

  // 树权限（展开/折叠）
  const handleCheckedTreeExpand = (value, type) => {
    if (type == 'menu') {
      let treeList = menuOptions.value
      for (let i = 0; i < treeList.length; i++) {
        menuRef.value.setExpanded(treeList[i].id, value)
      }
    }
  }

  // 树权限（全选/全不选）
  const handleCheckedTreeNodeAll = (value, type) => {
    if (type == 'menu') {
      menuRef.value.setCheckedNodes(value ? menuOptions.value : [])
    }
  }

  // 树权限（父子联动）
  const handleCheckedTreeConnect = (value, type) => {
    if (type == 'menu') {
      form.menuCheckStrictly = value ? true : false
    }
  }

  // 分配数据权限
  const handleDataScope = (row) => {
    getRole(row.roleId).then(response => {
      form.roleId = response.roleId
      form.roleName = response.roleName
      form.roleKey = response.roleKey
      form.dataScope = response.dataScope
      openDataScope.value = true
      title.value = "分配数据权限"
    })
  }

  // 提交数据权限
  const submitDataScope = () => {
    if (!form.roleId) return
    roleApi.updateDataScope(form.roleId, form.dataScope).then(response => {
      if (response.code === 200) {
        ElMessage.success("数据权限分配成功")
        openDataScope.value = false
        getList()
      }
    }).catch(error => {
      console.error('分配数据权限失败:', error)
    })
  }

  // 取消数据权限
  const cancelDataScope = () => {
    openDataScope.value = false
    reset()
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
    color: var(--el-text-color-primary);
    margin: 0 0 8px 0;
  }

  .page-description {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin: 0;
  }

  .content-card {
    background: var(--el-bg-color);
    border-radius: 8px;
    padding: 20px;
    box-shadow: var(--el-box-shadow-light);
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

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }

  .tree-border {
    margin-top: 5px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    padding: 10px;
    max-height: 300px;
    overflow-y: auto;
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
