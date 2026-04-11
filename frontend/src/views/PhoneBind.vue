<template>
  <div>
    <h3>绑定手机号</h3>
    <p class="hint">登录账号即主手机号，登录后会自动出现在下列列表。可在此增加其它号码（与账单数据关联用）；短信验证将后续接入。</p>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const mobile = ref('')

const rows = computed(() => list.value.map((mobile, i) => ({ i: i + 1, mobile })))

async function load() {
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

async function add() {
  saving.value = true
  try {
    await api.post('/me/phones', { mobile: mobile.value })
    ElMessage.success('已绑定')
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
.hint { color: #666; font-size: 13px; margin-bottom: 16px; max-width: 640px; line-height: 1.5; }
.form { margin-top: 20px; }
</style>
