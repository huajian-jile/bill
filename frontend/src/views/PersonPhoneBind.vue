<template>
  <div class="page">
    <h3>绑定所属人与账单号码</h3>
    <p class="hint">
      一个<strong>所属人</strong>可关联多个<strong>手机号</strong>，同一号码也可关联多个所属人（多对多）。分析页按所属人筛选时，会汇总这些号码下的微信/支付宝流水。
      登录账号仅对应<strong>一个手机号</strong>；其它号码来自导入账单的号码维度，在此绑定到所属人即可纳入分析。
    </p>

    <el-table :data="links" border v-loading="loading" style="max-width: 920px" class="mt">
      <el-table-column prop="personLabel" label="所属人" min-width="140" />
      <el-table-column prop="mobileCn" label="号码" width="120" />
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button type="danger" link size="small" :loading="removing === row.linkId" @click="remove(row)">
            解除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-form class="form" @submit.prevent="add">
      <div class="form-title">新增关联</div>
      <el-form-item label="所属人">
        <el-select v-model="form.personId" placeholder="选择所属人" filterable clearable style="width: 280px">
          <el-option v-for="p in persons" :key="p.id" :label="p.label" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="账单号码">
        <el-select v-model="form.phoneId" placeholder="选择号码" filterable clearable style="width: 280px">
          <el-option v-for="ph in phones" :key="ph.id" :label="ph.mobileCn" :value="ph.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving" :disabled="!form.personId || !form.phoneId">
          绑定
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(false)
const saving = ref(false)
const removing = ref(null)
const links = ref([])
const persons = ref([])
const phones = ref([])
const form = reactive({ personId: null, phoneId: null })

async function loadAll() {
  loading.value = true
  try {
    const [pl, ph, lk] = await Promise.all([
      api.get('/me/bill-persons'),
      api.get('/me/person-phones/linkable-phones'),
      api.get('/me/person-phones/links')
    ])
    persons.value = pl.data || []
    phones.value = ph.data || []
    links.value = lk.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function add() {
  saving.value = true
  try {
    await api.post('/me/person-phones', {
      personId: form.personId,
      phoneId: form.phoneId
    })
    ElMessage.success('已绑定')
    form.personId = null
    form.phoneId = null
    await loadAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '绑定失败')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定解除「${row.personLabel}」与 ${row.mobileCn} 的关联？`, '确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  removing.value = row.linkId
  try {
    await api.delete(`/me/person-phones/${row.linkId}`)
    ElMessage.success('已解除')
    await loadAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    removing.value = null
  }
}

onMounted(loadAll)
</script>

<style scoped>
.page {
  padding: 0 8px 24px;
  max-width: 960px;
}
.hint {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 8px;
}
.mt {
  margin-top: 16px;
}
.form {
  margin-top: 28px;
  max-width: 520px;
}
.form-title {
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
