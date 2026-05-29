<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
      <p class="page-description">管理个人资料和账户设置</p>
    </div>

    <div class="content-card">
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
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/views/auth/stores/useAuthStore'
import userApi from '@/api/user'

const authStore = useAuthStore()
const profileFormRef = ref()
const passwordFormRef = ref()

const activeTab = ref('info')
const saveLoading = ref(false)
const passwordLoading = ref(false)

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

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

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

function validateConfirmPassword(rule, value, callback) {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const getCurrentUserInfo = () => {
  const params = {
    username: authStore.userInfo?.username
  }
  userApi.getCurrentUser(params).then(response => {
    if (response.code === 200) {
      const userInfo = response.data
      Object.keys(userInfo).forEach(key => {
        if (key in profileForm) {
          profileForm[key] = userInfo[key]
        }
      })
      authStore.setUserInfo(userInfo)
    } else {
      ElMessage.error(response.message || '获取用户信息失败')
    }
  }).catch(error => {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  })
}

const submitProfile = async () => {
  if (!profileFormRef.value) return

  await profileFormRef.value.validate((valid) => {
    if (valid) {
      saveLoading.value = true
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
          const updatedInfo = { ...authStore.userInfo, ...submitData }
          authStore.setUserInfo(updatedInfo)
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

const resetProfileForm = () => {
  getCurrentUserInfo()
}

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

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

onMounted(() => {
  getCurrentUserInfo()
})
</script>

<style scoped>
.connection-container {
  padding: 5px;
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

.profile-tabs {
  border: none;
}

.form-actions {
  text-align: right;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .connection-container {
    padding: 10px;
  }

  .content-card {
    padding: 15px;
  }
}
</style>
