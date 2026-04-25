<template>
  <div>
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
      密码以<strong>明文形式存档</strong>于库中仅供本页查看，便于 master 找回丢失账号；微信登录用户可能显示「—」。
    </el-alert>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建账号</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="users" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="手机号" width="140" />
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
      <el-table-column label="已绑定手机" min-width="240">
        <template #default="{ row }">
          <template v-if="row.boundPhones?.length">
            <span v-for="p in row.boundPhones" :key="p" class="phone-wrap">
              <span class="phone">{{ p }}</span>
              <el-button link type="danger" size="small" @click="unbindPhone(row, p)">解绑</el-button>
            </span>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoles(row)">改角色</el-button>
          <el-button link type="primary" @click="openPassword(row)">改密</el-button>
          <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogCreate" title="新建账号" width="420px" @closed="resetCreate">
      <el-form label-width="88px">
        <el-form-item label="手机号">
          <el-input v-model="form.username" maxlength="11" placeholder="11 位手机号" />
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

    <el-dialog v-model="dialogPassword" title="修改密码" width="420px" @closed="resetPasswordForm">
      <el-form label-width="88px">
        <el-form-item label="新密码">
          <el-input v-model="formPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogPassword = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const users = ref([])
const roleOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogCreate = ref(false)
const dialogRoles = ref(false)
const dialogPassword = ref(false)
const form = ref({ username: '', password: '', roleCodes: [] })
const formRoles = ref([])
const formPassword = ref('')
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

function openPassword(row) {
  editingId.value = row.id
  formPassword.value = ''
  dialogPassword.value = true
}

function resetPasswordForm() {
  formPassword.value = ''
}

async function submitPassword() {
  saving.value = true
  try {
    await api.patch(`/admin/users/${editingId.value}/password`, { password: formPassword.value })
    ElMessage.success('密码已更新')
    dialogPassword.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除账号 ${row.username}（ID ${row.id}）？此操作不可恢复。`, '删除账号', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  saving.value = true
  try {
    await api.delete(`/admin/users/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  } finally {
    saving.value = false
  }
}

async function unbindPhone(row, mobile) {
  try {
    await ElMessageBox.confirm(`确定解除绑定手机号 ${mobile}？`, '解绑手机', {
      type: 'warning',
      confirmButtonText: '解绑',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  saving.value = true
  try {
    await api.delete(`/admin/users/${row.id}/phones`, { params: { mobile } })
    ElMessage.success('已解绑')
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '解绑失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.phone-wrap { display: inline-flex; align-items: center; margin-right: 12px; margin-bottom: 4px; }
.phone { margin-right: 4px; }
.muted { color: var(--el-text-color-secondary); }
.pwd-plain {
  font-family: ui-monospace, monospace;
  font-size: 13px;
  word-break: break-all;
}
</style>
