<template>
  <el-card>
    <template #header>导入账单（微信：.xlsx；支付宝：.csv）</template>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
      请选择<strong>本账号已绑定</strong>的手机号；若列表为空请先到「绑定手机号」中添加。可一次选择<strong>多个文件</strong>，将依次导入。
    </el-alert>
    <el-form label-width="88px" style="max-width: 480px">
      <el-form-item label="手机号" required>
        <el-select
          v-model="mobileCn"
          placeholder="选择已绑定手机号"
          filterable
          clearable
          style="width: 100%"
        >
          <el-option v-for="m in boundPhones" :key="m" :label="m" :value="m" />
        </el-select>
      </el-form-item>
      <el-form-item label="渠道">
        <el-radio-group v-model="channel" @change="onChannelChange">
          <el-radio label="wechat">微信（.xlsx）</el-radio>
          <el-radio label="alipay">支付宝（.csv）</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <el-upload
      ref="uploadRef"
      :key="channel"
      drag
      multiple
      :auto-upload="false"
      :on-change="syncUploadFiles"
      :on-remove="syncUploadFiles"
      :accept="fileAccept"
    >
      <div class="el-upload__text">
        {{ channel === 'alipay' ? '拖拽或点击选择 .csv（可多选）' : '拖拽或点击选择 .xlsx（可多选）' }}
      </div>
    </el-upload>
    <el-button type="primary" :loading="loading" style="margin-top: 12px" @click="upload">上传导入</el-button>
    <el-alert v-if="resultSummary" type="success" :title="resultSummary" style="margin-top: 12px" />
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const mobileCn = ref('')
const channel = ref('wechat')
const files = ref([])
const uploadRef = ref(null)
const loading = ref(false)
const resultSummary = ref('')
const boundPhones = ref([])

const fileAccept = computed(() => (channel.value === 'alipay' ? '.csv' : '.xlsx'))

function syncUploadFiles(uploadFile, uploadFiles) {
  const list = uploadFiles || []
  files.value = list.map((u) => u.raw).filter((r) => r && fileMatchesChannel(r.name))
}

function onChannelChange() {
  files.value = []
  uploadRef.value?.clearFiles()
}

function fileMatchesChannel(name) {
  if (!name) return false
  const n = name.toLowerCase()
  if (channel.value === 'alipay') return n.endsWith('.csv')
  return n.endsWith('.xlsx')
}

async function loadPhones() {
  try {
    const { data } = await api.get('/me/phones')
    boundPhones.value = data || []
    localStorage.setItem('phones', JSON.stringify(boundPhones.value))
    if (boundPhones.value.length && !mobileCn.value) {
      mobileCn.value = boundPhones.value[0]
    }
  } catch {
    boundPhones.value = []
  }
}

onMounted(loadPhones)

async function upload() {
  const m = (mobileCn.value || '').trim()
  if (!m) {
    ElMessage.error('请选择手机号')
    return
  }
  if (!boundPhones.value.includes(m)) {
    ElMessage.error('请从已绑定号码中选择')
    return
  }
  if (!files.value.length) {
    ElMessage.warning('请选择文件')
    return
  }
  const bad = files.value.filter((f) => !fileMatchesChannel(f.name))
  if (bad.length) {
    ElMessage.error(channel.value === 'alipay' ? '所选文件中包含非 .csv' : '所选文件中包含非 .xlsx')
    return
  }
  loading.value = true
  resultSummary.value = ''
  const path = channel.value === 'alipay' ? '/import/alipay' : '/import/wechat'
  let lastId = null
  let ok = 0
  try {
    for (const file of files.value) {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('mobileCn', m)
      const { data } = await api.post(path, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      lastId = data?.id
      ok++
    }
    resultSummary.value =
      ok === 1 && lastId != null
        ? `导入成功 importId=${lastId}`
        : `共 ${ok} 个文件导入成功${lastId != null ? `，最后 importId=${lastId}` : ''}`
    ElMessage.success(`已导入 ${ok} 个文件`)
    uploadRef.value?.clearFiles()
    files.value = []
  } catch (e) {
    const d = e.response?.data
    const msg =
      (typeof d === 'string' && d) ||
      d?.message ||
      d?.error ||
      e.message ||
      '导入失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>
