<template>
  <div class="wrap">
    <el-card class="card">
      <h2>微信账单分析系统</h2>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form @submit.prevent="doLogin">
            <el-form-item label="账号">
              <el-input
                v-model="username"
                maxlength="10"
                autocomplete="username"
                placeholder="10 位数字"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="password" type="password" autocomplete="current-password" />
            </el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="reg">
          <el-form @submit.prevent="doRegister">
            <el-form-item label="账号">
              <el-input v-model="regUsername" maxlength="10" autocomplete="username" placeholder="10 位数字" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regPassword" type="password" autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="手机号（可选）">
              <el-input v-model="regMobile" maxlength="11" clearable placeholder="不填则仅账号登录" />
            </el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <p class="hint">
        登录与注册账号均为 <strong>10 位数字</strong>；密码非空且不超过 128 字符。注册时可选择填写大陆手机号用于绑定账单。首次启动管理员见后端
        application.yaml（app.bootstrap.admin-username / admin-password）。
      </p>
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
const username = ref('1000000001')
const password = ref('admin123')
const regUsername = ref('')
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

/** 登录成功后：根据手机绑定和导入状态决定跳转 */
async function redirectAfterLogin() {
  // 检查是否有已绑定的手机号
  const phones = JSON.parse(localStorage.getItem('phones') || '[]')
  if (!phones || phones.length === 0) {
    // 没有绑定手机号，跳转到绑定页面
    router.push('/phones')
    return
  }
  // 有绑定手机号，检查是否有导入记录
  try {
    const { data } = await api.get('/me/phones')
    const phoneList = data || []
    if (phoneList.length === 0) {
      router.push('/phones')
      return
    }
    // 调用API检查是否有导入
    const { data: hasImport } = await api.get('/me/has-import')
    if (!hasImport) {
      router.push('/import')
    } else {
      router.push('/analytics')
    }
  } catch {
    // 出错默认跳转到导入页面
    router.push('/import')
  }
}

async function doLogin() {
  loading.value = true
  try {
    const { data } = await api.post('/auth/login', {
      username: username.value.trim(),
      password: password.value
    })
    saveSession(data)
    await redirectAfterLogin()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  loading.value = true
  try {
    const payload = {
      username: regUsername.value.trim(),
      password: regPassword.value
    }
    const m = regMobile.value.trim()
    if (m) payload.mobile = m
    const { data } = await api.post('/auth/register', payload)
    saveSession(data)
    // 注册时如果没填手机号，跳转到绑定
    if (!m) {
      router.push('/phones')
    } else {
      // 填写了手机号，登录后检查导入状态
      await redirectAfterLogin()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
}
.card {
  width: 400px;
  padding: 8px 0;
}
.hint {
  font-size: 12px;
  color: #888;
  margin-top: 12px;
  line-height: 1.5;
}
</style>
