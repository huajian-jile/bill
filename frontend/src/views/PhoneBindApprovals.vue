<template>
  <div>
    <div class="toolbar">
      <el-button @click="load">刷新</el-button>
    </div>
    <p class="hint">仅管理员可见：用户第二个及后续手机号绑定需在此通过后方可用于导入与筛选。</p>
    <el-table :data="rows" border v-loading="loading" empty-text="暂无待审核">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="mobileCn" label="申请手机号" width="140" />
      <el-table-column prop="createdAt" label="申请时间" min-width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :loading="actingId === row.id" @click="approve(row)">通过</el-button>
          <el-button type="danger" link :loading="actingId === row.id" @click="reject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const rows = ref([])
const loading = ref(false)
const actingId = ref(null)

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/admin/phone-bind-requests')
    rows.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  actingId.value = row.id
  try {
    await api.post(`/admin/phone-bind-requests/${row.id}/approve`)
    ElMessage.success('已通过')
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    actingId.value = null
  }
}

async function reject(row) {
  try {
    await ElMessageBox.confirm(`拒绝将 ${row.mobileCn} 绑定到账号 ${row.username}？`, '拒绝', { type: 'warning' })
  } catch {
    return
  }
  actingId.value = row.id
  try {
    await api.post(`/admin/phone-bind-requests/${row.id}/reject`)
    ElMessage.success('已拒绝')
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    actingId.value = null
  }
}

onMounted(load)
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
