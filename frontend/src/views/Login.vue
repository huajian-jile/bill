<template>
  <div class="login-page">
    <!-- 左侧灯箱面板 -->
    <aside class="lamp-panel">
      <div class="lamp-inner">
        <!-- 台灯 SVG -->
        <div class="lamp-wrap">
          <svg class="lamp-svg" viewBox="0 0 120 200" xmlns="http://www.w3.org/2000/svg">
            <!-- 灯罩 -->
            <path class="lamp-shade" d="M20,60 L100,60 L90,110 L30,110 Z" />
            <!-- 灯罩纹理 -->
            <line class="lamp-line" x1="35" y1="65" x2="50" y2="105" />
            <line class="lamp-line" x1="60" y1="60" x2="60" y2="110" />
            <line class="lamp-line" x1="85" y1="65" x2="70" y2="105" />
            <!-- 灯泡发光效果 -->
            <ellipse class="lamp-glow" cx="60" cy="115" rx="30" ry="10" />
            <!-- 灯颈 -->
            <rect class="lamp-neck" x="56" y="110" width="8" height="50" />
            <!-- 灯座 -->
            <rect class="lamp-base" x="40" y="160" width="40" height="8" rx="4" />
            <rect class="lamp-base" x="35" y="168" width="50" height="6" rx="3" />
            <!-- 开关线 -->
            <line class="lamp-cord" x1="35" y1="60" x2="10" y2="80" />
            <circle class="lamp-switch" cx="10" cy="82" r="5" />
          </svg>
        </div>

        <!-- 时间问候语 -->
        <div class="greeting-section">
          <p class="greeting-time">{{ timeHint }}</p>
          <p class="greeting-main">{{ greeting }}</p>
          <p class="greeting-sub">{{ subGreeting }}</p>
        </div>
      </div>

      <!-- 底部装饰 -->
      <div class="lamp-ground" />
    </aside>

    <!-- 右侧表单面板 -->
    <main class="form-panel">
      <div class="form-inner">
        <div class="form-header">
          <h1 class="form-title">账单分析系统</h1>
          <p class="form-subtitle">记录每一笔，掌控每一分</p>
        </div>

        <el-tabs v-model="tab" class="auth-tabs" animated>
          <!-- 登录 -->
          <el-tab-pane label="登录" name="login">
            <el-form class="auth-form" ref="loginFormRef">
              <el-form-item>
                <el-input
                  v-model="loginForm.mobile"
                  clearable
                  placeholder="请输入手机号"
                  prefix-icon="Phone"
                  size="large"
                  @keyup.enter="doLogin"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  show-password
                  autocomplete="current-password"
                  placeholder="请输入密码"
                  prefix-icon="Lock"
                  size="large"
                  @keyup.enter="doLogin"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  class="submit-btn"
                  size="large"
                  @click="doLogin"
                >
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 注册 -->
          <el-tab-pane label="注册" name="reg">
            <el-form class="auth-form" ref="regFormRef">
              <el-form-item>
                <el-input
                  v-model="regForm.mobile"
                  clearable
                  placeholder="请输入手机号"
                  prefix-icon="Phone"
                  size="large"
                  @keyup.enter="doRegister"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="regForm.password"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请输入密码"
                  prefix-icon="Lock"
                  size="large"
                  @keyup.enter="doRegister"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="regForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请再次输入密码"
                  prefix-icon="Lock"
                  size="large"
                  @keyup.enter="doRegister"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  class="submit-btn"
                  size="large"
                  @click="doRegister"
                >
                  注 册
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({ mobile: '', password: '' })
const regForm = reactive({ mobile: '', password: '', confirmPassword: '' })

// 实时时间
const now = ref(new Date())
let timer = null
onMounted(() => { timer = setInterval(() => { now.value = new Date() }, 30000) })
onUnmounted(() => clearInterval(timer))

const hour = computed(() => now.value.getHours())

// 台灯亮度（小时 6-22 亮，其余暗）
const lampOn = computed(() => hour.value >= 6 && hour.value <= 22)

const timeHint = computed(() => {
  const h = hour.value
  if (h >= 5 && h < 7) return '🌅 清晨'
  if (h >= 7 && h < 12) return '☀️ 上午'
  if (h >= 12 && h < 14) return '🍜 中午'
  if (h >= 14 && h < 18) return '☀️ 下午'
  if (h >= 18 && h < 22) return '🌙 傍晚'
  return '🌛 夜深了'
})

const greeting = computed(() => {
  const h = hour.value
  if (h >= 5 && h < 7) return '早起的你，最棒'
  if (h >= 7 && h < 9) return '美好的一天，开始啦'
  if (h >= 9 && h < 12) return '加油，今天也要元气满满'
  if (h >= 12 && h < 14) return '午休片刻，下午更高效'
  if (h >= 14 && h < 18) return '下午茶时间到~'
  if (h >= 18 && h < 20) return '晚餐要吃好，账单要记好'
  if (h >= 20 && h < 22) return '晚上好，记账时间'
  return '注意休息，明天见'
})

const subGreeting = computed(() => {
  const h = hour.value
  if (h >= 22 || h < 5) return '放下手机，早点休息'
  if (h >= 20) return '夜深了，别熬太晚'
  if (h >= 18) return '记账有助于理财哦'
  if (h >= 12) return '记得吃午饭'
  if (h >= 9) return '每一笔支出都值得被记录'
  return '美好的一天从记录开始'
})


function saveSession(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('username', data.username || '')
  localStorage.setItem('authorities', JSON.stringify(data.authorities || []))
  if (data.phones) {
    localStorage.setItem('phones', JSON.stringify(data.phones))
  } else {
    localStorage.removeItem('phones')
  }
}

async function redirectAfterLogin() {
  try {
    const { data: hasImport } = await api.get('/me/has-import')
    if (!hasImport) {
      router.push('/import')
    } else {
      router.push('/analytics')
    }
  } catch {
    router.push('/import')
  }
}

async function doLogin() {
  const mobile = loginForm.mobile.trim()
  const pwd = loginForm.password
  loading.value = true
  try {
    const { data } = await api.post('/auth/login', { mobile, password: pwd })
    saveSession(data)
    await redirectAfterLogin()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  const mobile = regForm.mobile.trim()
  const pwd = regForm.password
  const confirm = regForm.confirmPassword
  if (pwd !== confirm) { ElMessage.error('两次输入的密码不一致'); return }
  loading.value = true
  try {
    const { data } = await api.post('/auth/register', {
      mobile,
      password: pwd,
      confirmPassword: confirm
    })
    saveSession(data)
    await redirectAfterLogin()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 全页布局 */
.login-page {
  display: flex;
  min-height: 100vh;
  background: #0f0f1a;
}

/* ========== 左侧台灯面板 ========== */
.lamp-panel {
  flex: 0 0 42%;
  position: relative;
  background: linear-gradient(160deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.lamp-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  z-index: 1;
}

/* 台灯 */
.lamp-wrap {
  width: 140px;
  height: 220px;
}

.lamp-svg {
  width: 100%;
  height: 100%;
}

.lamp-shade {
  fill: #2c3e50;
  stroke: #34495e;
  stroke-width: 1;
}

.lamp-line {
  stroke: #34495e;
  stroke-width: 0.8;
  opacity: 0.6;
}

.lamp-neck {
  fill: #7f8c8d;
}

.lamp-base {
  fill: #95a5a6;
}

/* 灯泡发光 */
.lamp-glow {
  fill: #f9ca24;
  opacity: 0.9;
  animation: glow-pulse 3s ease-in-out infinite;
}

.lamp-cord {
  stroke: #555;
  stroke-width: 1.5;
  fill: none;
}

.lamp-switch {
  fill: #e74c3c;
  stroke: #c0392b;
  stroke-width: 0.5;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.85; }
  50% { opacity: 1; }
}

/* 灯暗的时候 */
.lamp-panel:not(.lamp-on) .lamp-glow {
  opacity: 0.3;
  animation: none;
}
.lamp-panel:not(.lamp-on) .lamp-shade {
  fill: #1a252f;
}

/* 地面光晕 */
.lamp-ground {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: radial-gradient(ellipse at 50% 100%, rgba(243, 156, 18, 0.15) 0%, transparent 70%);
}

/* 时间提示 */
.greeting-section {
  text-align: center;
  color: #ecf0f1;
}

.greeting-time {
  font-size: 13px;
  color: #7f8c8d;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.greeting-main {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #f5f5f5;
}

.greeting-sub {
  font-size: 13px;
  color: #95a5a6;
  margin: 0;
}

/* ========== 右侧表单面板 ========== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d0d1a;
  padding: 40px 20px;
}

.form-inner {
  width: 100%;
  max-width: 380px;
}

.form-header {
  text-align: center;
  margin-bottom: 36px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #f5f5f5;
  margin: 0 0 8px;
  letter-spacing: 4px;
}

.form-subtitle {
  font-size: 13px;
  color: #606266;
  margin: 0;
  letter-spacing: 1px;
}

/* Tabs */
.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 28px;
}

.auth-tabs :deep(.el-tabs__item) {
  color: #7f8c8d;
  font-size: 16px;
  height: 44px;
  line-height: 44px;
}

.auth-tabs :deep(.el-tabs__item.is-active) {
  color: #f9ca24;
}

.auth-tabs :deep(.el-tabs__active-bar) {
  background-color: #f9ca24;
}

.auth-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: #2c2c3e;
}

/* 表单项 */
.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-input__wrapper) {
  background: #1a1a2e;
  border: 1px solid #2c2c3e;
  box-shadow: none;
  border-radius: 8px;
  height: 44px;
}

.auth-form :deep(.el-input__wrapper:hover) {
  border-color: #3a3a5c;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: #f9ca24;
  box-shadow: 0 0 0 2px rgba(249, 202, 36, 0.15);
}

.auth-form :deep(.el-input__inner) {
  color: #ecf0f1;
  font-size: 14px;
}

.auth-form :deep(.el-input__inner::placeholder) {
  color: #4a4a6a;
}

.auth-form :deep(.el-input__prefix-icon) {
  color: #606266;
  font-size: 16px;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f9ca24, #f0932b);
  border: none;
  color: #1a1a2e;
  font-weight: 700;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #fbd335, #f5a623);
}

.submit-btn:active {
  background: linear-gradient(135deg, #e8b820, #e0951f);
}

/* 表单错误提示 */
.auth-form :deep(.el-form-item__error) {
  font-size: 12px;
}
</style>
