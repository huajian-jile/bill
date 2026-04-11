<template>
  <div class="bill-data">
    <div class="page-head">
      <div class="page-title-row">
        <h2 class="page-title">账单数据</h2>
        <el-text type="info" size="small" class="page-hint">
          备份表可编辑；原始导入仅用于恢复。
          <router-link to="/import" class="link-import">去导入</router-link>
        </el-text>
      </div>

      <div class="channel-row">
        <span class="field-label">渠道</span>
        <el-radio-group v-model="channelTab" size="default" @change="onTabChange">
          <el-radio-button value="WECHAT">微信</el-radio-button>
          <el-radio-button value="ALIPAY">支付宝</el-radio-button>
          <el-radio-button value="ALL">合并</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-card shadow="never" class="panel filter-panel">
      <div class="panel-title">筛选</div>
      <el-form label-position="top" class="filter-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="10">
            <el-form-item label="交易时间">
              <el-date-picker
                v-model="timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="7">
            <el-form-item label="收 / 支">
              <el-select v-model="filters.incomeExpense" clearable placeholder="不限" style="width: 100%">
                <el-option label="不限" :value="''" />
                <el-option label="含「收入」" value="收入" />
                <el-option label="含「支出」" value="支出" />
                <el-option label="含「中性」" value="中性" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="7">
            <el-form-item label="排序">
              <div class="sort-line">
                <el-select v-model="filters.sort" style="flex: 1; min-width: 0">
                  <el-option label="交易时间" value="tradeTime" />
                  <el-option label="类型" value="tradeType" />
                  <el-option label="对方" value="counterparty" />
                  <el-option label="收/支" value="incomeExpense" />
                  <el-option label="金额" value="amountYuan" />
                </el-select>
                <el-radio-group v-model="filters.direction" size="small" class="dir-btns">
                  <el-radio-button value="desc">降</el-radio-button>
                  <el-radio-button value="asc">升</el-radio-button>
                </el-radio-group>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="5">
            <el-form-item label="类型（模糊）">
              <el-input v-model="filters.tradeType" clearable placeholder="关键词" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8" :md="5">
            <el-form-item label="对方（模糊）">
              <el-input v-model="filters.counterparty" clearable placeholder="关键词" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="4">
            <el-form-item label="金额 ≥">
              <el-input-number v-model="filters.amountMin" :controls="false" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="4">
            <el-form-item label="金额 ≤">
              <el-input-number v-model="filters.amountMax" :controls="false" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="6" class="filter-actions">
            <el-button type="primary" @click="load(0)">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="panel">
      <div class="restore-wrap">
        <span class="restore-title">从原始表恢复到备份</span>
        <el-button-group class="restore-btns">
          <el-button size="small" @click="openRestore('day')">按日</el-button>
          <el-button size="small" @click="openRestore('month')">按月</el-button>
          <el-button size="small" @click="openRestore('year')">按年</el-button>
          <el-button size="small" type="warning" @click="openRestore('all')">全部</el-button>
        </el-button-group>
      </div>

      <el-table
        :data="page.content || []"
        v-loading="loading"
        stripe
        border
        class="data-table"
        empty-text="暂无数据"
      >
        <el-table-column prop="tradeTime" label="交易时间" min-width="158" />
        <el-table-column prop="tradeType" label="类型" min-width="96" show-overflow-tooltip />
        <el-table-column prop="counterparty" label="对方" min-width="120" show-overflow-tooltip />
        <el-table-column prop="incomeExpense" label="收/支" width="86" />
        <el-table-column prop="amountYuan" label="金额(元)" width="108" align="right">
          <template #default="{ row }">
            <span :class="amountClass(row)">{{ fmt(row.amountYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="billChannel" label="渠道" width="80">
          <template #default="{ row }">{{ row.billChannel === 'ALIPAY' ? '支付宝' : '微信' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="128" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-button type="primary" plain @click="openCreate">
          <span class="btn-plus">+</span> 新增一行
        </el-button>
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="page.totalElements || 0"
          v-model:page-size="filters.size"
          v-model:current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="() => load(0)"
          @current-change="(p) => load(p - 1)"
        />
      </div>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? '编辑备份行' : '新增备份行'" width="520px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="渠道" required>
          <el-select v-model="form.billChannel" style="width: 100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="导入批次 ID" required>
          <el-input-number v-model="form.billImportId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="交易时间"><el-input v-model="form.tradeTime" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.tradeType" /></el-form-item>
        <el-form-item label="对方"><el-input v-model="form.counterparty" /></el-form-item>
        <el-form-item label="收/支"><el-input v-model="form.incomeExpense" /></el-form-item>
        <el-form-item label="金额(元)"><el-input v-model="form.amountYuan" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="restoreDlg" :title="restoreTitle" width="400px">
      <el-form v-if="restoreKind === 'day'" label-width="88px">
        <el-form-item label="日期">
          <el-date-picker v-model="restoreDay" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-form v-else-if="restoreKind === 'month'" label-width="88px">
        <el-form-item label="年"><el-input-number v-model="restoreYear" :min="2000" :max="2100" /></el-form-item>
        <el-form-item label="月"><el-input-number v-model="restoreMonth" :min="1" :max="12" /></el-form-item>
      </el-form>
      <el-form v-else-if="restoreKind === 'year'" label-width="88px">
        <el-form-item label="年"><el-input-number v-model="restoreYear" :min="2000" :max="2100" /></el-form-item>
      </el-form>
      <p v-else-if="restoreKind === 'all'" class="warn">将尝试把当前渠道下<strong>全部</strong>原始明细同步到备份表（按 source_tx 覆盖/新增）。</p>
      <template #footer>
        <el-button @click="restoreDlg = false">取消</el-button>
        <el-button type="primary" @click="doRestore">确定恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const channelTab = ref('WECHAT')
const loading = ref(false)
const page = ref({ content: [], totalElements: 0 })
const currentPage = ref(1)

/** 时间范围，同步到 tradeTimeFrom / tradeTimeTo */
const timeRange = ref(null)

const filters = reactive({
  tradeTimeFrom: null,
  tradeTimeTo: null,
  tradeType: null,
  counterparty: null,
  incomeExpense: '',
  amountMin: null,
  amountMax: null,
  sort: 'tradeTime',
  direction: 'desc',
  size: 20
})

watch(timeRange, (val) => {
  if (val && val.length === 2) {
    filters.tradeTimeFrom = val[0]
    filters.tradeTimeTo = val[1]
  } else {
    filters.tradeTimeFrom = null
    filters.tradeTimeTo = null
  }
})

const dlg = ref(false)
const form = reactive({
  id: null,
  billChannel: 'WECHAT',
  billImportId: null,
  tradeTime: '',
  tradeType: '',
  counterparty: '',
  incomeExpense: '',
  amountYuan: ''
})

const restoreDlg = ref(false)
const restoreKind = ref('day')
const restoreDay = ref(null)
const restoreYear = ref(new Date().getFullYear())
const restoreMonth = ref(new Date().getMonth() + 1)

const restoreTitle = computed(() => {
  const ch = channelTab.value === 'ALL' ? '（请在微信或支付宝页执行）' : ''
  return `恢复原始到备份 ${ch}`
})

function channelParam() {
  if (channelTab.value === 'ALL') return 'ALL'
  return channelTab.value
}

function fmt(v) {
  if (v == null) return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}

function amountClass(row) {
  const s = row.incomeExpense || ''
  if (s.includes('收入')) return 'amt-income'
  if (s.includes('支出')) return 'amt-expense'
  return ''
}

function resetFilters() {
  timeRange.value = null
  filters.tradeType = null
  filters.counterparty = null
  filters.incomeExpense = ''
  filters.amountMin = null
  filters.amountMax = null
  filters.sort = 'tradeTime'
  filters.direction = 'desc'
  load(0)
}

async function load(pageIndex) {
  loading.value = true
  try {
    const params = {
      channel: channelParam(),
      tradeTimeFrom: filters.tradeTimeFrom || undefined,
      tradeTimeTo: filters.tradeTimeTo || undefined,
      tradeType: filters.tradeType || undefined,
      counterparty: filters.counterparty || undefined,
      incomeExpense: filters.incomeExpense || undefined,
      amountMin: filters.amountMin != null ? filters.amountMin : undefined,
      amountMax: filters.amountMax != null ? filters.amountMax : undefined,
      page: pageIndex,
      size: filters.size,
      sort: filters.sort,
      direction: filters.direction
    }
    const { data } = await api.get('/bkp/transactions', { params })
    page.value = data
    currentPage.value = pageIndex + 1
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  if (channelTab.value === 'ALL') {
    ElMessage.info('合并视图展示全部渠道；恢复请切换到微信或支付宝')
  }
  load(0)
}

function openCreate() {
  if (channelTab.value === 'ALL') {
    ElMessage.warning('请先选择微信或支付宝再新增')
    return
  }
  Object.assign(form, {
    id: null,
    billChannel: channelTab.value,
    billImportId: null,
    tradeTime: '',
    tradeType: '',
    counterparty: '',
    incomeExpense: '',
    amountYuan: ''
  })
  dlg.value = true
}

function edit(row) {
  form.id = row.id
  form.billChannel = row.billChannel || 'WECHAT'
  form.billImportId = row.billImportId
  form.tradeTime = row.tradeTime || ''
  form.tradeType = row.tradeType || ''
  form.counterparty = row.counterparty || ''
  form.incomeExpense = row.incomeExpense || ''
  form.amountYuan = row.amountYuan != null ? String(row.amountYuan) : ''
  dlg.value = true
}

function payload() {
  return {
    sourceTxId: null,
    billImportId: form.billImportId,
    billChannel: form.billChannel,
    rowHash: null,
    tradeTime: form.tradeTime || null,
    tradeType: form.tradeType || null,
    counterparty: form.counterparty || null,
    product: null,
    incomeExpense: form.incomeExpense || null,
    amountYuan: form.amountYuan ? Number(form.amountYuan) : null,
    paymentMethod: null,
    status: null,
    tradeNo: null,
    merchantNo: null,
    remark: null,
    sourceFile: null,
    extraText: null,
    archived: false
  }
}

async function save() {
  if (!form.billImportId) {
    ElMessage.warning('请填写导入批次 ID')
    return
  }
  if (form.id) {
    await api.put(`/bkp/transactions/${form.id}`, payload())
  } else {
    await api.post('/bkp/transactions', payload())
  }
  dlg.value = false
  ElMessage.success('已保存')
  load(currentPage.value - 1)
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除？')
  await api.delete(`/bkp/transactions/${row.id}`)
  ElMessage.success('已删除')
  load(currentPage.value - 1)
}

function openRestore(kind) {
  if (channelTab.value === 'ALL') {
    ElMessage.warning('请切换到「微信」或「支付宝」后再恢复')
    return
  }
  restoreKind.value = kind
  restoreDlg.value = true
}

function restoreBasePath() {
  return channelTab.value === 'WECHAT' ? '/bkp/restore/wechat' : '/bkp/restore/alipay'
}

async function doRestore() {
  const base = restoreBasePath()
  try {
    if (restoreKind.value === 'day') {
      if (!restoreDay.value) {
        ElMessage.warning('请选择日期')
        return
      }
      await api.post(`${base}/day`, null, { params: { date: restoreDay.value } })
    } else if (restoreKind.value === 'month') {
      await api.post(`${base}/month`, null, {
        params: { year: restoreYear.value, month: restoreMonth.value }
      })
    } else if (restoreKind.value === 'year') {
      await api.post(`${base}/year`, null, { params: { year: restoreYear.value } })
    } else {
      await api.post(`${base}/all`)
    }
    ElMessage.success('恢复任务已完成')
    restoreDlg.value = false
    load(0)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '恢复失败')
  }
}

onMounted(() => load(0))
</script>

<style scoped>
.bill-data {
  width: 100%;
  box-sizing: border-box;
  padding: 0 0 24px;
}

.page-head {
  margin-bottom: 16px;
}

.page-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 12px 20px;
  margin-bottom: 14px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 0.02em;
}

.page-hint {
  line-height: 1.5;
}

.link-import {
  color: var(--el-color-primary);
  margin-left: 4px;
  text-decoration: none;
}
.link-import:hover {
  text-decoration: underline;
}

.channel-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.field-label {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.panel {
  border-radius: 8px;
  margin-bottom: 16px;
}

.filter-panel {
  background: var(--el-fill-color-blank);
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.filter-form :deep(.el-form-item__label) {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
  margin-bottom: 4px;
}

.sort-line {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.dir-btns {
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding-bottom: 12px;
}

@media (min-width: 992px) {
  .filter-actions {
    justify-content: flex-end;
  }
}

.restore-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
}

.restore-title {
  font-size: 13px;
  color: #606266;
}

.restore-btns {
  flex-shrink: 0;
}

.data-table {
  width: 100%;
}

.amt-income {
  color: #409eff;
}
.amt-expense {
  color: #f56c6c;
}

.table-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
}

.btn-plus {
  font-weight: 600;
  margin-right: 2px;
}

.warn {
  color: #e6a23c;
  line-height: 1.5;
}
</style>
