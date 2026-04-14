<template>
  <div>
    <h3>绑定手机号</h3>
    <p class="hint">
      登录<strong>账号</strong>（多为 10 位数字）与<strong>绑定手机号</strong>不同：下列为与账单关联的号码。<strong>首个</strong>号码可直接绑定；从<strong>第二个</strong>起需管理员在「手机号审核」中通过后方可生效。短信验证将后续接入。
    </p>
    <h4 class="sub">已绑定号码</h4>
    <el-table :data="rows" border style="max-width: 480px" v-loading="loading">
      <el-table-column prop="i" label="#" width="56" />
      <el-table-column prop="mobile" label="手机号" />
    </el-table>
    <el-form @submit.prevent="add" class="form" inline>
      <el-form-item label="新增绑定">
        <el-input v-model="mobile" placeholder="11 位中国大陆手机号" maxlength="11" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving">绑定</el-button>
      </el-form-item>
    </el-form>

    <h4 class="sub">申请与审核记录</h4>
    <p class="hint hint-tight">含待审核、已通过、已拒绝；拒绝时管理员填写的理由会显示在「拒绝理由」列。</p>
    <el-table :data="requestRows" border style="max-width: 720px" v-loading="reqLoading" empty-text="暂无申请记录">
      <el-table-column prop="mobileCn" label="申请手机号" width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'PENDING'" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status === 'APPROVED'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="row.status === 'REJECTED'" type="danger" size="small">已拒绝</el-tag>
          <span v-else>{{ row.status || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" min-width="160">
        <template #default="{ row }">{{ formatInstant(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" min-width="160">
        <template #default="{ row }">{{ formatInstant(row.reviewedAt) }}</template>
      </el-table-column>
      <el-table-column label="拒绝理由" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.rejectReason || (row.status === 'REJECTED' ? '—' : '') }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()

const list = ref([])
const requests = ref([])
const loading = ref(false)
const reqLoading = ref(false)
const saving = ref(false)
const mobile = ref('')

const rows = computed(() => list.value.map((mobile, i) => ({ i: i + 1, mobile })))
const requestRows = computed(() => requests.value)

function formatInstant(v) {
  if (v == null || v === '') return '—'
  try {
    return new Date(v).toLocaleString()
  } catch {
    return String(v)
  }
}

async function loadPhones() {
  loading.value = true
  try {
    const { data } = await api.get('/me/phones')
    list.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadRequests() {
  reqLoading.value = true
  try {
    const { data } = await api.get('/me/phone-bind-requests')
    requests.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '申请记录加载失败')
  } finally {
    reqLoading.value = false
  }
}

async function load() {
  await Promise.all([loadPhones(), loadRequests()])
}

async function add() {
  saving.value = true
  try {
    const { data } = await api.post('/me/phones', { mobile: mobile.value })
    if (data?.status === 'pending_review') {
      ElMessage.success('已提交审核，管理员通过后即可绑定')
    } else {
      ElMessage.success('已绑定，即将跳转到导入页面')
      // 更新本地存储的手机号列表
      const phones = JSON.parse(localStorage.getItem('phones') || '[]')
      if (!phones.includes(mobile.value)) {
        phones.push(mobile.value)
        localStorage.setItem('phones', JSON.stringify(phones))
      }
      // 跳转到导入页面
      router.push('/import')
    }
    mobile.value = ''
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '绑定失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hint {
  color: #666;
  font-size: 13px;
  margin-bottom: 16px;
  max-width: 640px;
  line-height: 1.5;
}
.hint-tight {
  margin-bottom: 8px;
}
.sub {
  margin: 20px 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.form {
  margin-top: 20px;
}
</style>
