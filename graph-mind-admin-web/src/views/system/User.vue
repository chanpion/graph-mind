<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <p class="page-description">管理系统用户，包括添加、编辑、删除用户以及分配角色和岗位</p>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input
              v-model="searchKeyword"
              placeholder="用户名称"
              style="width: 200px"
              clearable
              @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
              v-model="queryParams.status"
              placeholder="用户状态"
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
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </div>
        <div class="action-area">
          <el-button type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
          <el-button type="danger" icon="Delete" :disabled="multipleSelection.length === 0" @click="handleDelete">删除</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="userList"
        @selection-change="handleSelectionChange"
        border
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="用户编号" align="center" prop="userId" width="100" />
        <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" />
        <el-table-column label="用户昵称" align="center" prop="nickName" :show-overflow-tooltip="true" />
        <el-table-column label="手机号码" align="center" prop="phonenumber" width="120" />
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
            <el-button type="text" size="small" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button type="text" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
            <el-button type="text" size="small" icon="Key" @click="handleResetPwd(scope.row)">重置密码</el-button>
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

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="userRef" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择">
                <el-option
                  v-for="dict in sys_user_sex"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
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
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择">
                <el-option
                  v-for="item in postOptions"
                  :key="item.postId"
                  :label="item.postName"
                  :value="item.postId"
                  :disabled="item.status == 1"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status == 1"
                ></el-option>
              </el-select>
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

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
      >
        <template #trigger>
          <el-button type="primary">选择文件</el-button>
        </template>
        <el-button type="success" @click="submitFileForm" :loading="upload.isUploading" style="margin-left: 10px;">
          {{ upload.isUploading ? '上传中...' : '开始上传' }}
        </el-button>
        <template #tip>
          <div class="el-upload__tip text-center">
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link type="primary" @click="importTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="User">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import Pagination from '@/components/Pagination.vue'

// 模拟字典数据
const sys_normal_disable = [
  { value: '0', label: '正常' },
  { value: '1', label: '停用' }
]

const sys_user_sex = [
  { value: '0', label: '男' },
  { value: '1', label: '女' },
  { value: '2', label: '未知' }
]

// 模拟岗位和角色数据
const postOptions = ref([
  { postId: 1, postName: '管理员', status: 0 },
  { postId: 2, postName: '运营专员', status: 0 },
  { postId: 3, postName: '财务专员', status: 1 }
])

const roleOptions = ref([
  { roleId: 1, roleName: '超级管理员', status: 0 },
  { roleId: 2, roleName: '普通用户', status: 0 },
  { roleId: 3, roleName: '访客', status: 1 }
])

// 页面状态
const loading = ref(true)
const open = ref(false)
const title = ref('')
const userList = ref([])
const total = ref(0)
const multipleSelection = ref([])
const searchKeyword = ref('')

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: undefined,
  phonenumber: undefined,
  status: undefined
})

// 表单参数
const form = reactive({
  userId: undefined,
  userName: undefined,
  nickName: undefined,
  phonenumber: undefined,
  email: undefined,
  sex: undefined,
  status: '0',
  remark: undefined,
  postIds: [],
  roleIds: []
})

// 表单校验规则
const rules = {
  userName: [
    { required: true, message: "用户名称不能为空", trigger: "blur" },
    { min: 2, max: 20, message: "用户名称长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  nickName: [
    { required: true, message: "用户昵称不能为空", trigger: "blur" }
  ],
  phonenumber: [
    { required: true, message: "手机号码不能为空", trigger: "blur" },
    {
      pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
      message: "请输入正确的手机号码",
      trigger: "blur"
    }
  ],
  email: [
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"]
    }
  ]
}

// 用户导入参数
const upload = reactive({
  // 是否显示弹出层
  open: false,
  // 弹出层标题
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + localStorage.getItem("token") },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/system/user/importData"
})

// 获取用户列表
const getList = () => {
  loading.value = true
  // 模拟API调用
  setTimeout(() => {
    userList.value = [
      {
        userId: 1,
        userName: 'admin',
        nickName: '管理员',
        phonenumber: '13800138000',
        email: 'admin@example.com',
        sex: '0',
        status: '0',
        createTime: '2023-01-01 12:00:00'
      },
      {
        userId: 2,
        userName: 'user',
        nickName: '普通用户',
        phonenumber: '13800138001',
        email: 'user@example.com',
        sex: '1',
        status: '0',
        createTime: '2023-01-02 12:00:00'
      }
    ]
    total.value = 2
    loading.value = false
  }, 500)
}

// 搜索
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 重置查询
const resetQuery = () => {
  queryParams.userName = undefined
  queryParams.phonenumber = undefined
  queryParams.status = undefined
  handleQuery()
}

// 多选框选中数据
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 新增按钮操作
const handleAdd = () => {
  reset()
  open.value = true
  title.value = "添加用户"
}

// 修改按钮操作
const handleUpdate = (row) => {
  reset()
  const userId = row.userId || multipleSelection.value[0].userId
  // 模拟获取用户详情
  getUser(userId).then(response => {
    const userData = response
    Object.assign(form, userData)
    form.postIds = [1] // 模拟岗位
    form.roleIds = [2] // 模拟角色
    open.value = true
    title.value = "修改用户"
  })
}

// 模拟获取用户详情
const getUser = (userId) => {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        userId: userId,
        userName: userId === 1 ? 'admin' : 'user',
        nickName: userId === 1 ? '管理员' : '普通用户',
        phonenumber: userId === 1 ? '13800138000' : '13800138001',
        email: userId === 1 ? 'admin@example.com' : 'user@example.com',
        sex: '0',
        status: '0',
        remark: ''
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
  form.userId = undefined
  form.userName = undefined
  form.nickName = undefined
  form.phonenumber = undefined
  form.email = undefined
  form.sex = undefined
  form.status = '0'
  form.remark = undefined
  form.postIds = []
  form.roleIds = []
}

// 删除按钮操作
const handleDelete = (row) => {
  const userIds = row.userId || multipleSelection.value.map(item => item.userId)
  ElMessageBox.confirm(
    '是否确认删除用户编号为"' + userIds + '"的数据项？',
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

// 重置密码按钮操作
const handleResetPwd = (row) => {
  ElMessageBox.prompt('请输入"' + row.userName + '"的新密码', "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    closeOnClickModal: false,
    inputPattern: /^.{5,20}$/,
    inputErrorMessage: "用户密码长度必须介于 5 和 20 之间"
  }).then(({ value }) => {
    // 模拟重置密码操作
    ElMessage.success("修改成功，新密码是：" + value)
  }).catch(() => {})
}

// 导入模板下载
const importTemplate = () => {
  ElMessage.info("正在下载模板")
}

// 文件上传中处理
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true
}

// 文件上传成功处理
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false
  upload.isUploading = false
  ElMessage.success("导入成功")
  getList()
}

// 提交上传文件
const submitFileForm = () => {
  ElMessage.info("正在上传文件")
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