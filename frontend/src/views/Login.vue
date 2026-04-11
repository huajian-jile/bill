<template>
  <div class="wrap">
    <el-card class="card">
      <h2>微信账单分析系统</h2>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form @submit.prevent="doLogin">
            <el-form-item label="手机号">
              <el-input v-model="mobile" maxlength="11" autocomplete="username" placeholder="11 位 1 开头" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="password" type="password" autocomplete="current-password" />
            </el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="reg">
          <el-form @submit.prevent="doRegister">
            <el-form-item label="手机号">
              <el-input v-model="regMobile" maxlength="11" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regPassword" type="password" autocomplete="new-password" />
            </el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <p class="hint">账号为 11 位中国大陆手机号；密码非空且不超过 128 字符。管理员账号见后端 application.yaml（app.bootstrap）。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const tab = ref('login')
const mobile = ref('13800138000')
const password = ref('admin123')
const regMobile = ref('')
const regPassword = ref('')
const loading = ref(false)

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

async function doLogin() {
  loading.value = true
  try {
    const { data } = await api.post('/auth/login', {
      username: mobile.value.trim(),
      password: password.value
    })
    saveSession(data)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  loading.value = true
  try {
    const { data } = await api.post('/auth/register', {
      mobile: regMobile.value.trim(),
      password: regPassword.value
    })
    saveSession(data)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap { min-height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#1a1a2e,#16213e); }
.card { width: 400px; padding: 8px 0; }
.hint { font-size: 12px; color: #888; margin-top: 12px; line-height: 1.5; }
</style>
