<template>
  <div class="settings-container">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
      <p class="page-description">管理系统配置和个性化设置</p>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- 基础设置 -->
        <el-tab-pane label="基础设置" name="basic">
          <el-card class="settings-card">
            <template #header>
              <div class="card-header">
                <span>基础设置</span>
              </div>
            </template>
            
            <el-form
              ref="basicFormRef"
              :model="basicForm"
              label-width="120px"
            >
              <el-form-item label="系统名称">
                <el-input v-model="basicForm.systemName" placeholder="请输入系统名称" />
              </el-form-item>
              
              <el-form-item label="系统标题">
                <el-input v-model="basicForm.systemTitle" placeholder="请输入系统标题" />
              </el-form-item>
              
              <el-form-item label="系统Logo">
                <el-input v-model="basicForm.systemLogo" placeholder="请输入系统Logo URL">
                  <template #append>
                    <el-button @click="handleLogoUpload">上传</el-button>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item label="系统描述">
                <el-input
                  v-model="basicForm.systemDescription"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入系统描述"
                />
              </el-form-item>
              
              <el-form-item label="默认语言">
                <el-select v-model="basicForm.defaultLanguage" placeholder="请选择默认语言">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="时区设置">
                <el-select v-model="basicForm.timezone" placeholder="请选择时区">
                  <el-option label="北京时间 (UTC+8)" value="Asia/Shanghai" />
                  <el-option label="东京时间 (UTC+9)" value="Asia/Tokyo" />
                  <el-option label="纽约时间 (UTC-5)" value="America/New_York" />
                </el-select>
              </el-form-item>
              
              <div class="form-actions">
                <el-button type="primary" @click="saveBasicSettings">保存设置</el-button>
              </div>
            </el-form>
          </el-card>
        </el-tab-pane>
        
        <!-- 界面设置 -->
        <el-tab-pane label="界面设置" name="interface">
          <el-card class="settings-card">
            <template #header>
              <div class="card-header">
                <span>界面设置</span>
              </div>
            </template>
            
            <el-form
              ref="interfaceFormRef"
              :model="interfaceForm"
              label-width="120px"
            >
              <el-form-item label="主题颜色">
                <el-color-picker v-model="interfaceForm.themeColor" />
                <span style="margin-left: 10px">{{ interfaceForm.themeColor }}</span>
              </el-form-item>
              
              <el-form-item label="导航模式">
                <el-radio-group v-model="interfaceForm.navMode">
                  <el-radio value="side">侧边栏导航</el-radio>
                  <el-radio value="top">顶部导航</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item label="页面动画">
                <el-switch v-model="interfaceForm.pageAnimation" />
                <span style="margin-left: 10px">
                  {{ interfaceForm.pageAnimation ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <el-form-item label="标签页显示">
                <el-switch v-model="interfaceForm.showTabs" />
                <span style="margin-left: 10px">
                  {{ interfaceForm.showTabs ? '显示' : '隐藏' }}
                </span>
              </el-form-item>
              
              <el-form-item label="页脚显示">
                <el-switch v-model="interfaceForm.showFooter" />
                <span style="margin-left: 10px">
                  {{ interfaceForm.showFooter ? '显示' : '隐藏' }}
                </span>
              </el-form-item>
              
              <div class="form-actions">
                <el-button type="primary" @click="saveInterfaceSettings">保存设置</el-button>
              </div>
            </el-form>
          </el-card>
        </el-tab-pane>
        
        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-card class="settings-card">
            <template #header>
              <div class="card-header">
                <span>安全设置</span>
              </div>
            </template>
            
            <el-form
              ref="securityFormRef"
              :model="securityForm"
              label-width="150px"
            >
              <el-form-item label="密码最小长度">
                <el-input-number
                  v-model="securityForm.passwordMinLength"
                  :min="6"
                  :max="20"
                  controls-position="right"
                />
                <span style="margin-left: 10px">位</span>
              </el-form-item>
              
              <el-form-item label="密码复杂度">
                <el-checkbox-group v-model="securityForm.passwordComplexity">
                  <el-checkbox label="uppercase">大写字母</el-checkbox>
                  <el-checkbox label="lowercase">小写字母</el-checkbox>
                  <el-checkbox label="number">数字</el-checkbox>
                  <el-checkbox label="special">特殊字符</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              
              <el-form-item label="登录失败次数">
                <el-input-number
                  v-model="securityForm.loginFailureLimit"
                  :min="1"
                  :max="10"
                  controls-position="right"
                />
                <span style="margin-left: 10px">次后锁定账户</span>
              </el-form-item>
              
              <el-form-item label="会话超时时间">
                <el-input-number
                  v-model="securityForm.sessionTimeout"
                  :min="10"
                  :max="1440"
                  controls-position="right"
                />
                <span style="margin-left: 10px">分钟后自动退出</span>
              </el-form-item>
              
              <el-form-item label="验证码开关">
                <el-switch v-model="securityForm.captchaEnabled" />
                <span style="margin-left: 10px">
                  {{ securityForm.captchaEnabled ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <div class="form-actions">
                <el-button type="primary" @click="saveSecuritySettings">保存设置</el-button>
              </div>
            </el-form>
          </el-card>
        </el-tab-pane>
        
        <!-- 通知设置 -->
        <el-tab-pane label="通知设置" name="notification">
          <el-card class="settings-card">
            <template #header>
              <div class="card-header">
                <span>通知设置</span>
              </div>
            </template>
            
            <el-form
              ref="notificationFormRef"
              :model="notificationForm"
              label-width="150px"
            >
              <el-form-item label="邮件通知">
                <el-switch v-model="notificationForm.emailEnabled" />
                <span style="margin-left: 10px">
                  {{ notificationForm.emailEnabled ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <el-form-item label="短信通知">
                <el-switch v-model="notificationForm.smsEnabled" />
                <span style="margin-left: 10px">
                  {{ notificationForm.smsEnabled ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <el-form-item label="站内信通知">
                <el-switch v-model="notificationForm.inboxEnabled" />
                <span style="margin-left: 10px">
                  {{ notificationForm.inboxEnabled ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <el-form-item label="操作日志记录">
                <el-switch v-model="notificationForm.logEnabled" />
                <span style="margin-left: 10px">
                  {{ notificationForm.logEnabled ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <el-form-item label="重要操作提醒">
                <el-switch v-model="notificationForm.importantOperationAlert" />
                <span style="margin-left: 10px">
                  {{ notificationForm.importantOperationAlert ? '启用' : '禁用' }}
                </span>
              </el-form-item>
              
              <div class="form-actions">
                <el-button type="primary" @click="saveNotificationSettings">保存设置</el-button>
              </div>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('basic')
const basicFormRef = ref()
const interfaceFormRef = ref()
const securityFormRef = ref()
const notificationFormRef = ref()

// 基础设置表单
const basicForm = reactive({
  systemName: '图数据库管理系统',
  systemTitle: 'Graph Mind Platform',
  systemLogo: '',
  systemDescription: '一款面向企业级应用的智能知识图谱管理系统',
  defaultLanguage: 'zh-CN',
  timezone: 'Asia/Shanghai'
})

// 界面设置表单
const interfaceForm = reactive({
  themeColor: '#409EFF',
  navMode: 'side',
  pageAnimation: true,
  showTabs: true,
  showFooter: true
})

// 安全设置表单
const securityForm = reactive({
  passwordMinLength: 8,
  passwordComplexity: ['uppercase', 'lowercase', 'number'],
  loginFailureLimit: 5,
  sessionTimeout: 30,
  captchaEnabled: true
})

// 通知设置表单
const notificationForm = reactive({
  emailEnabled: true,
  smsEnabled: false,
  inboxEnabled: true,
  logEnabled: true,
  importantOperationAlert: true
})

// 保存基础设置
const saveBasicSettings = () => {
  ElMessage.success('基础设置保存成功')
  // 这里应该调用API保存设置
}

// 保存界面设置
const saveInterfaceSettings = () => {
  ElMessage.success('界面设置保存成功')
  // 这里应该调用API保存设置
}

// 保存安全设置
const saveSecuritySettings = () => {
  ElMessage.success('安全设置保存成功')
  // 这里应该调用API保存设置
}

// 保存通知设置
const saveNotificationSettings = () => {
  ElMessage.success('通知设置保存成功')
  // 这里应该调用API保存设置
}

// 处理Logo上传
const handleLogoUpload = () => {
  ElMessage.info('请在弹出的文件选择框中选择图片')
  // 模拟文件上传
  setTimeout(() => {
    basicForm.systemLogo = 'https://via.placeholder.com/100x100'
    ElMessage.success('Logo上传成功')
  }, 1000)
}

// 组件挂载时加载设置
onMounted(() => {
  // 这里应该从API或本地存储加载设置
  ElMessage.info('已加载系统设置')
})
</script>

<style scoped>
.settings-container {
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

.settings-tabs {
  border: none;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.form-actions {
  text-align: right;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
</style>