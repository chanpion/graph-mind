<template>
  <div class="profile-container">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
      <p class="page-description">管理个人资料和账户设置</p>
    </div>

    <div class="content-card">
      <el-card class="profile-card">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基本信息" name="info">
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
                <el-button type="primary" @click="submitProfile" :loading="saveLoading">保存信息</el-button>
                <el-button @click="resetProfileForm">重置</el-button>
              </div>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="修改密码" name="password">
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
                <el-button type="primary" @click="submitPassword" :loading="passwordLoading">修改密码</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </div>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import userApi from '@/api/user'

const userStore = useUserStore()
const profileFormRef = ref()
const passwordFormRef = ref()

// 激活的tab页
const activeTab = ref('info')

// 加载状态
const saveLoading = ref(false)
const passwordLoading = ref(false)

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

// 获取当前用户信息
const getCurrentUserInfo = () => {
  userApi.getCurrentUser(userStore.userInfo.username).then(response => {
    if (response.code === 200) {
      const userInfo = response.data
      // 更新表单数据
      Object.keys(userInfo).forEach(key => {
        if (key in profileForm) {
          profileForm[key] = userInfo[key]
        }
      })
      // 更新用户store中的信息
      userStore.setUserInfo(userInfo)
    } else {
      ElMessage.error(response.message || '获取用户信息失败')
    }
  }).catch(error => {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  })
}

// 提交个人信息
const submitProfile = async () => {
  if (!profileFormRef.value) return
  
  await profileFormRef.value.validate((valid) => {
    if (valid) {
      saveLoading.value = true
      // 准备提交的数据，排除只读字段
      const submitData = {
        nickname: profileForm.nickname,
        phonenumber: profileForm.phonenumber,
        email: profileForm.email,
        sex: profileForm.sex,
        remark: profileForm.remark
      }
      
      userApi.updateCurrentUser(submitData).then(response => {
        if (response.code === 200) {
          ElMessage.success('个人信息保存成功')
          // 更新用户store中的信息
          const updatedInfo = { ...userStore.userInfo, ...submitData }
          userStore.setUserInfo(updatedInfo)
        } else {
          ElMessage.error(response.message || '保存失败')
        }
      }).catch(error => {
        console.error('保存失败:', error)
        ElMessage.error('保存失败')
      }).finally(() => {
        saveLoading.value = false
      })
    }
  })
}

// 重置个人信息表单
const resetProfileForm = () => {
  getCurrentUserInfo()
}

// 提交密码修改
const submitPassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate((valid) => {
    if (valid) {
      passwordLoading.value = true
      const passwordData = {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      }
      
      userApi.changePassword(passwordData).then(response => {
        if (response.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          resetPasswordForm()
          // 可以在这里添加重新登录逻辑
        } else {
          ElMessage.error(response.message || '密码修改失败')
        }
      }).catch(error => {
        console.error('密码修改失败:', error)
        ElMessage.error('密码修改失败')
      }).finally(() => {
        passwordLoading.value = false
      })
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
  getCurrentUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
  text-align: center;
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

.profile-card {
  border: none;
  box-shadow: none;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}

.form-actions {
  text-align: right;
  margin-top: 20px;
}

.profile-tabs {
  border: none;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }
  
  .content-card {
    padding: 15px;
  }
  
  :deep(.el-form-item__label) {
    width: 100%;
    text-align: left;
    padding-bottom: 5px;
  }
  
  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
  
  :deep(.el-col) {
    width: 100%;
    margin-bottom: 10px;
  }
}
</style>