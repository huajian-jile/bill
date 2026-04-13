<template>
  <div>
    <div class="toolbar">
      <el-button @click="refreshAll">刷新</el-button>
    </div>
    <p class="hint">仅管理员可见：用户第二个及后续手机号绑定需在此通过后方可用于导入与筛选。通过或拒绝后记录均保留在「审核记录」中。</p>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="待审核" name="pending">
        <el-table :data="pendingRows" border v-loading="pendingLoading" empty-text="暂无待审核">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="mobileCn" label="申请手机号" width="140" />
          <el-table-column prop="createdAt" label="申请时间" min-width="180">
            <template #default="{ row }">{{ formatInstant(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :loading="actingId === row.id" @click="approve(row)">通过</el-button>
              <el-button type="danger" link :loading="actingId === row.id" @click="reject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="审核记录" name="history">
        <el-table :data="historyRows" border v-loading="historyLoading" empty-text="暂无审核记录">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="mobileCn" label="申请手机号" width="140" />
          <el-table-column prop="createdAt" label="申请时间" min-width="160">
            <template #default="{ row }">{{ formatInstant(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'APPROVED'" type="success" size="small">已通过</el-tag>
              <el-tag v-else-if="row.status === 'REJECTED'" type="danger" size="small">已拒绝</el-tag>
              <span v-else>{{ row.status || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewedAt" label="审核时间" min-width="160">
            <template #default="{ row }">{{ formatInstant(row.reviewedAt) }}</template>
          </el-table-column>
          <el-table-column prop="reviewedByUsername" label="审核人" width="120" />
          <el-table-column label="拒绝理由" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.status === 'REJECTED' ? (row.rejectReason || '—') : '' }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const activeTab = ref('pending')
const pendingRows = ref([])
const historyRows = ref([])
const pendingLoading = ref(false)
const historyLoading = ref(false)
const actingId = ref(null)

function formatInstant(v) {
  if (v == null || v === '') return '—'
  try {
    return new Date(v).toLocaleString()
  } catch {
    return String(v)
  }
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const { data } = await api.get('/admin/phone-bind-requests')
    pendingRows.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '待审核列表加载失败')
  } finally {
    pendingLoading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const { data } = await api.get('/admin/phone-bind-requests/history')
    historyRows.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '审核记录加载失败')
  } finally {
    historyLoading.value = false
  }
}

function onTabChange(name) {
  if (name === 'history') loadHistory()
}

function refreshAll() {
  loadPending()
  loadHistory()
}

async function approve(row) {
  actingId.value = row.id
  try {
    await api.post(`/admin/phone-bind-requests/${row.id}/approve`)
    ElMessage.success('已通过')
    await loadPending()
    await loadHistory()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    actingId.value = null
  }
}

async function reject(row) {
  let reason
  try {
    const ret = await ElMessageBox.prompt(
      '拒绝理由将展示给申请人，请简要说明原因。',
      `拒绝绑定：${row.mobileCn} → ${row.username}`,
      {
        confirmButtonText: '提交拒绝',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：与账号实名信息不一致',
        inputValidator: (v) => {
          const s = v != null ? String(v).trim() : ''
          if (!s) return '请填写拒绝理由'
          if (s.length > 500) return '理由不超过 500 字'
          return true
        },
      }
    )
    reason = String(ret.value).trim()
  } catch {
    return
  }
  actingId.value = row.id
  try {
    await api.post(`/admin/phone-bind-requests/${row.id}/reject`, { reason })
    ElMessage.success('已拒绝')
    await loadPending()
    await loadHistory()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    actingId.value = null
  }
}

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.hint {
  color: #606266;
  font-size: 13px;
  margin-bottom: 12px;
  line-height: 1.5;
}
</style>
