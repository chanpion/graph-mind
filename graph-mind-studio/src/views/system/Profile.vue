<template>
  <div class="profile-container">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
      <p class="page-description">管理个人资料和账户设置</p>
    </div>

    <div class="content-card">
      <el-row :gutter="20">
        <!-- 个人信息卡片 -->
        <el-col :span="16">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <span>基本信息</span>
              </div>
            </template>
            
            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="100px"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="用户名" prop="username">
                    <el-input v-model="profileForm.username" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="profileForm.nickname" />
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="手机号" prop="phonenumber">
                    <el-input v-model="profileForm.phonenumber" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                    <el-input v-model="profileForm.email" />
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="性别" prop="sex">
                    <el-select v-model="profileForm.sex" placeholder="请选择性别" style="width: 100%">
                      <el-option label="男" value="0" />
                      <el-option label="女" value="1" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="创建时间">
                    <el-input v-model="profileForm.createTime" disabled />
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-form-item label="个人简介">
                <el-input
                  v-model="profileForm.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入个人简介"
                />
              </el-form-item>
              
              <div class="form-actions">
                <el-button type="primary" @click="submitProfile">保存信息</el-button>
                <el-button @click="resetProfileForm">重置</el-button>
              </div>
            </el-form>
          </el-card>
          
          <!-- 修改密码卡片 -->
          <el-card class="password-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <span>修改密码</span>
              </div>
            </template>
            
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
            >
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入旧密码"
                  show-password
                />
              </el-form-item>
              
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                />
              </el-form-item>
              
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请确认新密码"
                  show-password
                />
              </el-form-item>
              
              <div class="form-actions">
                <el-button type="primary" @click="submitPassword">修改密码</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </div>
            </el-form>
          </el-card>
        </el-col>
        
        <!-- 头像卡片 -->
        <el-col :span="8">
          <el-card class="avatar-card">
            <template #header>
              <div class="card-header">
                <span>头像</span>
              </div>
            </template>
            
            <div class="avatar-container">
              <el-avatar
                :size="100"
                :src="profileForm.avatar"
                class="user-avatar"
              >
                {{ profileForm.nickname?.charAt(0) || profileForm.username?.charAt(0) || 'U' }}
              </el-avatar>
              
              <div class="avatar-actions">
                <el-button type="primary" @click="handleAvatarUpload">
                  上传头像
                </el-button>
                <input
                  ref="avatarInputRef"
                  type="file"
                  accept="image/*"
                  style="display: none"
                  @change="handleAvatarChange"
                />
              </div>
              
              <div class="avatar-info">
                <p>支持 JPG、PNG 格式，文件小于 2MB</p>
              </div>
            </div>
          </el-card>
          
          <!-- 账户安全卡片 -->
          <el-card class="security-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <span>账户安全</span>
              </div>
            </template>
            
            <div class="security-info">
              <div class="security-item">
                <div class="security-label">登录状态</div>
                <div class="security-value">
                  <el-tag type="success">在线</el-tag>
                </div>
              </div>
              
              <div class="security-item">
                <div class="security-label">最近登录</div>
                <div class="security-value">{{ profileForm.loginTime }}</div>
              </div>
              
              <div class="security-item">
                <div class="security-label">登录IP</div>
                <div class="security-value">{{ profileForm.loginIp }}</div>
              </div>
              
              <div class="security-item">
                <div class="security-label">角色</div>
                <div class="security-value">{{ profileForm.roles?.join(', ') }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const profileFormRef = ref()
const passwordFormRef = ref()
const avatarInputRef = ref()

// 个人信息表单
const profileForm = reactive({
  userId: '',
  username: '',
  nickname: '',
  phonenumber: '',
  email: '',
  sex: '',
  avatar: '',
  remark: '',
  createTime: '',
  loginTime: '',
  loginIp: '',
  roles: []
})

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 个人信息验证规则
const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2 到 20 个字符', trigger: 'blur' }
  ],
  phonenumber: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { pattern: /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/, message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 密码验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6 到 20 个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 确认密码验证器
function validateConfirmPassword(rule, value, callback) {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 获取用户信息
const getUserInfo = () => {
  // 从用户store获取信息
  profileForm.userId = userStore.userInfo.userId || ''
  profileForm.username = userStore.userInfo.username || ''
  profileForm.nickname = userStore.userInfo.nickname || ''
  profileForm.phonenumber = userStore.userInfo.phonenumber || ''
  profileForm.email = userStore.userInfo.email || ''
  profileForm.sex = userStore.userInfo.sex || ''
  profileForm.avatar = userStore.userInfo.avatar || ''
  profileForm.remark = userStore.userInfo.remark || ''
  profileForm.createTime = userStore.userInfo.createTime || ''
  profileForm.loginTime = userStore.userInfo.loginTime || ''
  profileForm.loginIp = userStore.userInfo.loginIp || ''
  profileForm.roles = userStore.userInfo.roles || []
}

// 提交个人信息
const submitProfile = async () => {
  if (!profileFormRef.value) return
  
  await profileFormRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('个人信息保存成功')
      // 这里应该调用API保存用户信息
      // 更新用户store中的信息
      const updatedInfo = { ...userStore.userInfo, ...profileForm }
      userStore.setUserInfo(updatedInfo)
    }
  })
}

// 重置个人信息表单
const resetProfileForm = () => {
  getUserInfo()
}

// 提交密码修改
const submitPassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('密码修改成功，请重新登录')
      // 这里应该调用API修改密码
      resetPasswordForm()
    }
  })
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

// 处理头像上传点击
const handleAvatarUpload = () => {
  avatarInputRef.value?.click()
}

// 处理头像变更
const handleAvatarChange = (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  // 验证文件类型和大小
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }
  
  // 模拟上传头像
  const reader = new FileReader()
  reader.onload = (e) => {
    profileForm.avatar = e.target.result
    ElMessage.success('头像上传成功')
  }
  reader.readAsDataURL(file)
}

// 组件挂载时获取用户信息
onMounted(() => {
  getUserInfo()
})
</script>

<style scoped>
.profile-container {
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

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.form-actions {
  text-align: right;
  margin-top: 20px;
}

.avatar-container {
  text-align: center;
  padding: 20px 0;
}

.user-avatar {
  margin-bottom: 20px;
}

.avatar-actions {
  margin-bottom: 15px;
}

.avatar-info {
  font-size: 12px;
  color: #909399;
}

.security-info {
  padding: 10px 0;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.security-item:last-child {
  border-bottom: none;
}

.security-label {
  font-size: 14px;
  color: #606266;
}

.security-value {
  font-size: 14px;
  color: #303133;
}
</style>