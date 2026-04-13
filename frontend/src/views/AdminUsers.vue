<template>
  <div>
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
      密码以<strong>明文形式存档</strong>于库中仅供本页查看；自本功能上线后新建或注册的用户会显示。历史账号若显示「—」表示未存档，需由用户自行重置或通过「新建」流程覆盖。
    </el-alert>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建账号</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="users" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column label="密码（明文存档）" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="pwd-plain">{{ row.passwordPlain || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="r in row.roleCodes" :key="r" size="small" style="margin-right: 4px">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v) => setEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="已绑定手机" min-width="200">
        <template #default="{ row }">
          <span v-for="p in row.boundPhones" :key="p" class="phone">{{ p }} </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoles(row)">改角色</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogCreate" title="新建账号" width="420px" @closed="resetCreate">
      <el-form label-width="88px">
        <el-form-item label="账号">
          <el-input v-model="form.username" maxlength="10" placeholder="10 位数字" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCodes" multiple placeholder="必选" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogRoles" title="修改角色" width="420px">
      <el-select v-model="formRoles" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" />
      </el-select>
      <template #footer>
        <el-button @click="dialogRoles = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const users = ref([])
const roleOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogCreate = ref(false)
const dialogRoles = ref(false)
const form = ref({ username: '', password: '', roleCodes: [] })
const formRoles = ref([])
const editingId = ref(null)

async function loadRoles() {
  const { data } = await api.get('/admin/users/roles')
  roleOptions.value = data || []
}

async function load() {
  loading.value = true
  try {
    await loadRoles()
    const { data } = await api.get('/admin/users')
    users.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetCreate()
  dialogCreate.value = true
}

function resetCreate() {
  form.value = { username: '', password: '', roleCodes: [] }
}

async function submitCreate() {
  saving.value = true
  try {
    await api.post('/admin/users', form.value)
    ElMessage.success('已创建')
    dialogCreate.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

function openRoles(row) {
  editingId.value = row.id
  formRoles.value = [...(row.roleCodes || [])]
  dialogRoles.value = true
}

async function submitRoles() {
  saving.value = true
  try {
    await api.put(`/admin/users/${editingId.value}/roles`, { roleCodes: formRoles.value })
    ElMessage.success('已保存')
    dialogRoles.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function setEnabled(row, enabled) {
  try {
    await api.patch(`/admin/users/${row.id}/enabled`, { enabled })
    row.enabled = enabled
    ElMessage.success('已更新')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
    await load()
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.phone { margin-right: 8px; }
.pwd-plain {
  font-family: ui-monospace, monospace;
  font-size: 13px;
  word-break: break-all;
}
</style>
