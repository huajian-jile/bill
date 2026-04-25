<template>
  <div class="analytics-hub group-board-page">
    <div class="top-bar">
      <div class="top-bar-left">
        <el-radio-group v-model="scope" size="default" class="scope-tabs" @change="onScopeChange">
          <el-radio-button value="day">某日</el-radio-button>
          <el-radio-button value="month">某月</el-radio-button>
          <el-radio-button value="year">某年</el-radio-button>
          <el-radio-button value="all">全部</el-radio-button>
        </el-radio-group>
        <div class="top-bar-scope-fields">
          <template v-if="scope === 'day'">
            <span class="field-lbl">日期</span>
            <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 150px"
              @change="onScopeChange"
            />
          </template>
          <template v-else-if="scope === 'month'">
            <span class="field-lbl">年</span>
            <el-input-number v-model="year" :min="2000" :max="2100" controls-position="right" @change="onScopeChange" />
            <span class="field-lbl">月</span>
            <el-input-number v-model="month" :min="1" :max="12" controls-position="right" @change="onScopeChange" />
          </template>
          <template v-else-if="scope === 'year'">
            <span class="field-lbl">年</span>
            <el-input-number v-model="yearOnly" :min="2000" :max="2100" controls-position="right" @change="onScopeChange" />
          </template>
          <span class="field-lbl">渠道</span>
          <el-select v-model="channel" style="width: 100px" @change="onScopeChange">
            <el-option label="微信" value="wechat" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="合并" value="merged" />
          </el-select>
          <span class="field-lbl">{{ multiPhone ? '手机号(多选)' : '手机号' }}</span>
          <el-select
            v-if="!multiPhone"
            v-model="phoneId"
            clearable
            placeholder="全部已绑定号码"
            style="width: 200px"
            @change="onScopeChange"
          >
            <el-option v-for="p in phones" :key="p.id" :label="p.mobileCn" :value="p.id" />
          </el-select>
          <el-select
            v-else
            v-model="phoneIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            placeholder="选多个号码"
            style="width: 260px"
            @change="onScopeChange"
          >
            <el-option v-for="p in phones" :key="p.id" :label="p.mobileCn" :value="p.id" />
          </el-select>
          <el-button :type="multiPhone ? 'primary' : 'default'" @click="toggleMultiPhone">多选号码</el-button>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </div>
      </div>
      <div class="top-bar-actions">
        <el-button plain @click="goAnalytics">分析看板</el-button>
        <el-button plain @click="goImport">导入账单</el-button>
        <el-button plain @click="goPhoneBind">绑定手机号</el-button>
      </div>
    </div>

    <div class="group-flow-wrap" v-loading="loading">
      <template v-if="board">
        <div class="day-chart-with-summary">
          <aside class="day-summary-side">
            <div class="day-summary-head day-summary-head-plain">
              <span class="day-summary-date-only">{{ rangeLabel }}</span>
            </div>
            <div class="day-summary-total-pill">交易对方 {{ filteredGroups.length }} 个</div>
            <div class="day-stat-grid">
              <div
                v-for="row in dayStatRows"
                :key="row.kind"
                class="day-stat-chip"
                :class="row.kind"
              >
                <span class="day-stat-count">{{ row.count }} 笔</span>
                <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
              </div>
            </div>
          </aside>
          <div ref="barChartEl" class="compare-chart day-chart-pane group-bar-chart" />
        </div>
      </template>
      <template v-else-if="!loading">
        <div class="day-chart-with-summary">
          <aside class="day-summary-side day-summary-empty">
            <div class="day-summary-head day-summary-head-plain">
              <span class="day-summary-date-only">{{ rangeLabel }}</span>
            </div>
            <div class="day-summary-total-pill muted">请先查询</div>
          </aside>
          <div class="compare-chart day-chart-pane chart-placeholder">暂无汇总数据</div>
        </div>
      </template>

      <div class="group-bottom-pane">
        <div class="filter-toolbar-row">
          <span class="filter-label">分组</span>
          <el-radio-group v-model="groupFlowFilter" size="small" :disabled="!board">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="expense">有支出</el-radio-button>
            <el-radio-button value="income">有收入</el-radio-button>
            <el-radio-button value="neutral">有中性</el-radio-button>
          </el-radio-group>
          <el-input
            v-model="filterCounterparty"
            clearable
            :disabled="!board"
            placeholder="交易对方"
            style="width: 160px"
            :prefix-icon="Search"
          />
          <el-input
            v-model="filterExpenseKw"
            clearable
            :disabled="!board"
            placeholder="支出合计"
            style="width: 130px"
          />
          <el-input
            v-model="filterIncomeKw"
            clearable
            :disabled="!board"
            placeholder="收入合计"
            style="width: 130px"
          />
          <el-input
            v-model="filterTimeKw"
            clearable
            :disabled="!board"
            placeholder="最近交易时间"
            style="width: 160px"
          />
          <el-button size="small" :disabled="!hasAnyLocalFilter" @click="clearLocalFilters">
            清空条件
          </el-button>
        </div>

        <div class="table-block">
          <div ref="tableGrowRef" class="table-grow">
            <el-table
              :data="filteredGroups"
              row-key="counterparty"
              border
              stripe
              class="group-table group-table-clickable"
              :max-height="tableHeight"
              highlight-current-row
              empty-text="暂无数据"
              @row-click="onTableRowClick"
            >
              <el-table-column prop="counterparty" label="交易对方" min-width="200" show-overflow-tooltip />
              <el-table-column label="支出合计(元)" width="120" align="right">
                <template #default="{ row }">
                  <span class="amt-expense">{{ fmtMoney(row.expenseTotal) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="expenseCount" label="支出笔数" width="88" align="right" />
              <el-table-column label="收入合计(元)" width="120" align="right">
                <template #default="{ row }">
                  <span class="amt-income">{{ fmtMoney(row.incomeTotal) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="incomeCount" label="收入笔数" width="88" align="right" />
              <el-table-column label="中性(元)" width="100" align="right">
                <template #default="{ row }">{{ fmtMoney(row.neutralTotal) }}</template>
              </el-table-column>
              <el-table-column prop="neutralCount" label="中性笔数" width="88" align="right" />
              <el-table-column prop="lastTradeTime" label="最近交易时间" width="168" show-overflow-tooltip />
            </el-table>
          </div>
        </div>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      size="min(960px, 96vw)"
      class="month-drill-drawer cp-detail-drawer"
      :destroy-on-close="false"
      @closed="onDrawerClosed"
    >
      <template #header>
        <div class="drawer-header">
          <span class="drawer-title">交易对方明细</span>
          <span class="drawer-sub">{{ drawerCounterparty || '—' }}</span>
        </div>
      </template>
      <div v-loading="drawerLoading" class="drawer-body">
        <div v-if="drawerSummary" class="drawer-sum">
          <span class="drawer-sum-item expense"
            >支出 {{ fmtMoney(drawerSummary.expenseTotal) }} 元（{{ drawerSummary.expenseCount }} 笔）</span
          >
          <span class="drawer-sum-item income"
            >收入 {{ fmtMoney(drawerSummary.incomeTotal) }} 元（{{ drawerSummary.incomeCount }} 笔）</span
          >
          <span class="drawer-sum-item neutral"
            >中性 {{ fmtMoney(drawerSummary.neutralTotal) }} 元（{{ drawerSummary.neutralCount }} 笔）</span
          >
          <span v-if="drawerSummary.lastTradeTime" class="drawer-sum-item time"
            >最近：{{ drawerSummary.lastTradeTime }}</span
          >
        </div>
        <AnalyticsDetailTable
          v-if="drawerDetailRows.length"
          :rows="drawerDetailRows"
          per-row-tone
          :max-height="480"
        />
        <el-empty v-else-if="!drawerLoading" description="暂无流水明细" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  watch,
  onMounted,
  onUnmounted,
  onBeforeUnmount,
  nextTick
} from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../api'
import AnalyticsDetailTable from '../components/AnalyticsDetailTable.vue'

const router = useRouter()
const loading = ref(false)
const useReal = ref(false)
const channel = ref('merged')
const phoneId = ref(null)
const phoneIds = ref([])
const multiPhone = ref(false)
const phones = ref([])
const board = ref(null)

const scope = ref('day')
const _d = new Date()
const date = ref(yesterdayYmd())
const year = ref(_d.getFullYear())
const month = ref(_d.getMonth() + 1)
const yearOnly = ref(_d.getFullYear())

const groupFlowFilter = ref('all')
const filterCounterparty = ref('')
const filterExpenseKw = ref('')
const filterIncomeKw = ref('')
const filterTimeKw = ref('')

const detailByCp = reactive({})

const drawerVisible = ref(false)
const drawerCounterparty = ref('')
const drawerLoading = ref(false)

const barChartEl = ref(null)
let chartBar = null

const tableGrowRef = ref(null)
const tableHeight = ref(400)
let tableResizeObserver = null

function yesterdayYmd() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const rangeLabel = computed(() => {
  if (scope.value === 'day') return date.value || '—'
  if (scope.value === 'month') return `${year.value}年${month.value}月`
  if (scope.value === 'year') return `${yearOnly.value}年`
  return '全部时间'
})

const groupsRaw = computed(() => board.value?.groups || [])

const hasLocalFilter = computed(() => {
  return !!(
    filterCounterparty.value?.trim() ||
    filterExpenseKw.value?.trim() ||
    filterIncomeKw.value?.trim() ||
    filterTimeKw.value?.trim()
  )
})

const hasNarrowingFilter = computed(
  () => hasLocalFilter.value || groupFlowFilter.value !== 'all'
)

const hasAnyLocalFilter = computed(
  () => hasNarrowingFilter.value
)

function amountFuzzyMatch(value, keywordRaw) {
  const keyword = (keywordRaw || '').trim().replace(/\s/g, '')
  if (!keyword) return true
  if (value == null || value === '') {
    return keyword === '0' || keyword === '0.' || keyword === '0.0' || keyword === '0.00'
  }
  const n = Number(value)
  if (!Number.isFinite(n)) return false
  const fixed = n.toFixed(2)
  const raw = String(value)
  return fixed.includes(keyword) || raw.includes(keyword)
}

const filteredGroups = computed(() => {
  let rows = groupsRaw.value
  if (groupFlowFilter.value === 'expense') {
    rows = rows.filter((r) => Number(r.expenseTotal ?? 0) > 0)
  } else if (groupFlowFilter.value === 'income') {
    rows = rows.filter((r) => Number(r.incomeTotal ?? 0) > 0)
  } else if (groupFlowFilter.value === 'neutral') {
    rows = rows.filter((r) => Number(r.neutralTotal ?? 0) > 0)
  }
  const cp = filterCounterparty.value.trim().toLowerCase()
  if (cp) {
    rows = rows.filter((r) => (r.counterparty || '').toLowerCase().includes(cp))
  }
  const ex = filterExpenseKw.value
  if (ex?.trim()) {
    rows = rows.filter((r) => amountFuzzyMatch(r.expenseTotal, ex))
  }
  const inc = filterIncomeKw.value
  if (inc?.trim()) {
    rows = rows.filter((r) => amountFuzzyMatch(r.incomeTotal, inc))
  }
  const tk = filterTimeKw.value.trim().toLowerCase()
  if (tk) {
    rows = rows.filter((r) => (r.lastTradeTime || '').toLowerCase().includes(tk))
  }
  return rows
})

const drawerSummary = computed(() => {
  const cp = drawerCounterparty.value
  if (!cp) return null
  return filteredGroups.value.find((r) => r.counterparty === cp) || null
})

const drawerDetailRows = computed(() => {
  const cp = drawerCounterparty.value
  if (!cp) return []
  return detailByCp[cp] || []
})

const dayStatRows = computed(() => {
  const rows = filteredGroups.value
  let ic = 0
  let ec = 0
  let nc = 0
  let inc = 0
  let exp = 0
  let neu = 0
  for (const r of rows) {
    ic += r.incomeCount || 0
    ec += r.expenseCount || 0
    nc += r.neutralCount || 0
    inc += Number(r.incomeTotal ?? 0)
    exp += Number(r.expenseTotal ?? 0)
    neu += Number(r.neutralTotal ?? 0)
  }
  return [
    { kind: 'expense', count: ec, amt: exp },
    { kind: 'income', count: ic, amt: inc },
    { kind: 'neutral', count: nc, amt: neu }
  ]
})

watch([filteredGroups, board], () => {
  nextTick(() => renderBarChart())
})

function renderBarChart() {
  if (!barChartEl.value) return
  const rows = [...filteredGroups.value]
    .filter((r) => Number(r.expenseTotal ?? 0) > 0 || Number(r.incomeTotal ?? 0) > 0)
    .sort((a, b) => {
      const de = Number(b.expenseTotal ?? 0) - Number(a.expenseTotal ?? 0)
      if (de !== 0) return de
      return Number(b.incomeTotal ?? 0) - Number(a.incomeTotal ?? 0)
    })
    .slice(0, 40)
  const names = rows.map((r) => (r.counterparty || '—').trim() || '—')
  const expenseVals = rows.map((r) => Number(r.expenseTotal ?? 0))
  const incomeVals = rows.map((r) => Number(r.incomeTotal ?? 0))
  const n = names.length
  /** 类目多于 12 个时启用横向滑动，一屏约 12 个，避免挤在一起 */
  const visibleRatio = n > 12 ? Math.min(100, Math.round((12 / n) * 100)) : 100

  if (!chartBar) {
    chartBar = echarts.init(barChartEl.value)
  }
  const opt = !rows.length
    ? {
        title: {
          text: '支出 / 收入（按交易对方 · 横向 Top40）',
          left: 0,
          textStyle: { fontSize: 13, color: '#606266' }
        },
        graphic: {
          type: 'text',
          left: 'center',
          top: 'middle',
          style: { text: '暂无数据', fill: '#909399', fontSize: 14 }
        },
        xAxis: { show: false },
        yAxis: { show: false },
        series: []
      }
    : {
        title: {
          text: '支出 / 收入（按交易对方 · 横向 Top40）',
          left: 0,
          textStyle: { fontSize: 13, color: '#606266' }
        },
        legend: {
          data: ['支出', '收入'],
          right: 6,
          top: 2,
          itemWidth: 12,
          itemHeight: 12,
          textStyle: { fontSize: 12 }
        },
        grid: {
          left: 8,
          right: n > 12 ? 28 : 12,
          top: 30,
          bottom: 12,
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter(params) {
            if (!params?.length) return ''
            const name = params[0].name
            const lines = params.map(
              (p) =>
                `${p.marker}${p.seriesName}：<b>${Number(p.value).toFixed(2)}</b> 元`
            )
            return `${name}<br/>${lines.join('<br/>')}`
          }
        },
        xAxis: {
          type: 'value',
          name: '金额(元)',
          nameTextStyle: { fontSize: 11, color: '#909399' },
          axisLabel: { fontSize: 11 },
          splitLine: { lineStyle: { type: 'dashed', opacity: 0.45 } }
        },
        yAxis: {
          type: 'category',
          data: names,
          inverse: true,
          axisTick: { alignWithLabel: true },
          axisLabel: {
            fontSize: 10,
            color: '#606266',
            interval: 0,
            triggerEvent: true,
            formatter(value) {
              const s = String(value)
              if (s.length <= 18) return s
              return `${s.slice(0, 18)}…`
            }
          }
        },
        dataZoom:
          n > 12
            ? [
                { type: 'inside', yAxisIndex: 0, start: 0, end: visibleRatio, filterMode: 'filter' },
                {
                  type: 'slider',
                  yAxisIndex: 0,
                  orient: 'vertical',
                  width: 12,
                  right: 4,
                  top: 36,
                  bottom: 16,
                  start: 0,
                  end: visibleRatio,
                  brushSelect: false,
                  borderColor: '#dcdfe6',
                  fillerColor: 'rgba(64, 158, 255, 0.15)',
                  handleSize: '90%',
                  handleStyle: { borderColor: '#409eff' }
                }
              ]
            : [],
        series: [
          {
            name: '支出',
            type: 'bar',
            data: expenseVals,
            barMaxWidth: 22,
            barGap: '4%',
            barCategoryGap: '6%',
            itemStyle: { color: '#f56c6c', borderRadius: [0, 3, 3, 0] }
          },
          {
            name: '收入',
            type: 'bar',
            data: incomeVals,
            barMaxWidth: 22,
            barCategoryGap: '6%',
            itemStyle: { color: '#67c23a', borderRadius: [0, 3, 3, 0] }
          }
        ]
      }
  chartBar.setOption(opt, { notMerge: true })
  bindChartClick()
}

function bindChartClick() {
  if (!chartBar) return
  chartBar.off('click')
  chartBar.on('click', (params) => {
    let cp = null
    if (params.componentType === 'series' && params.name != null && String(params.name) !== '') {
      cp = params.name
    } else if (params.componentType === 'yAxis' && params.value != null && String(params.value) !== '') {
      cp = params.value
    }
    if (cp != null) {
      openDrawerByCounterparty(String(cp))
    }
  })
}

async function openDrawerByCounterparty(counterparty) {
  if (!counterparty) return
  drawerCounterparty.value = counterparty
  drawerVisible.value = true
  if (detailByCp[counterparty]?.length) return
  drawerLoading.value = true
  try {
    const { data } = await api.get('/analytics/by-counterparty-detail', {
      params: { counterparty, ...rangeParams() }
    })
    detailByCp[counterparty] = data || []
  } catch {
    detailByCp[counterparty] = []
    ElMessage.error('明细加载失败')
  } finally {
    drawerLoading.value = false
  }
}

function onDrawerClosed() {
  drawerCounterparty.value = ''
}

function resizeChart() {
  chartBar?.resize()
}

function goImport() {
  router.push('/import')
}
function goPhoneBind() {
  router.push('/phones')
}
function goAnalytics() {
  router.push('/analytics')
}

function toggleReal() {
  useReal.value = !useReal.value
  clearDetailCache()
  load()
}

function clearLocalFilters() {
  groupFlowFilter.value = 'all'
  filterCounterparty.value = ''
  filterExpenseKw.value = ''
  filterIncomeKw.value = ''
  filterTimeKw.value = ''
}

function analyticsUserParams() {
  const p = { channel: channel.value }
  if (multiPhone.value && phoneIds.value?.length) {
    p.phoneIds = phoneIds.value.join(',')
  } else if (phoneId.value != null && phoneId.value !== '') {
    p.phoneId = phoneId.value
  }
  return p
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

function rangeParams() {
  const p = { ...analyticsUserParams() }
  if (scope.value === 'day') {
    if (date.value) {
      p.from = date.value
      p.to = date.value
    }
  } else if (scope.value === 'month') {
    const y = year.value
    const m = month.value
    const last = new Date(y, m, 0).getDate()
    p.from = `${y}-${pad2(m)}-01`
    p.to = `${y}-${pad2(m)}-${pad2(last)}`
  } else if (scope.value === 'year') {
    const y = yearOnly.value
    p.from = `${y}-01-01`
    p.to = `${y}-12-31`
  }
  return p
}

function toggleMultiPhone() {
  multiPhone.value = !multiPhone.value
  if (multiPhone.value) {
    if (phoneId.value != null && (!phoneIds.value || phoneIds.value.length === 0)) {
      phoneIds.value = [phoneId.value]
    }
    if (!phoneIds.value?.length && phones.value.length) {
      phoneIds.value = phones.value.map((p) => p.id)
    }
  } else {
    phoneIds.value = []
  }
  onScopeChange()
}

function clearDetailCache() {
  Object.keys(detailByCp).forEach((k) => delete detailByCp[k])
}

function onScopeChange() {
  clearDetailCache()
  groupFlowFilter.value = 'all'
  filterCounterparty.value = ''
  filterExpenseKw.value = ''
  filterIncomeKw.value = ''
  filterTimeKw.value = ''
  load()
}

function updateTableHeight() {
  nextTick(() => {
    const el = tableGrowRef.value
    if (!el) return
    const h = el.clientHeight
    tableHeight.value = Math.max(200, Math.floor(h))
  })
}

function bindTableResizeObserver() {
  if (typeof ResizeObserver === 'undefined') return
  nextTick(() => {
    const el = tableGrowRef.value
    if (!el) return
    if (tableResizeObserver) {
      tableResizeObserver.disconnect()
      tableResizeObserver = null
    }
    tableResizeObserver = new ResizeObserver(() => {
      updateTableHeight()
      resizeChart()
    })
    tableResizeObserver.observe(el)
    updateTableHeight()
  })
}

function fmtMoney(v) {
  if (v == null || v === '') return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}

function canViewAllBills() {
  try {
    const a = JSON.parse(localStorage.getItem('authorities') || '[]')
    return Array.isArray(a) && a.includes('PERM_VIEW_ALL_BILLS')
  } catch {
    return false
  }
}

async function loadPhones() {
  try {
    const { data } = await api.get('/me/bill-phones')
    phones.value = data || []
    if (!phones.value.length) {
      return
    }
    if (multiPhone.value) {
      if (!phoneIds.value?.length) {
        phoneIds.value = phones.value.map((p) => p.id)
      }
    } else if (!canViewAllBills() && (phoneId.value == null || phoneId.value === '')) {
      // 普通用户：默认选中一个已绑定号码（与「分析看板」一致），避免空选时误以为「全部」
      phoneId.value = phones.value[0].id
    }
  } catch {
    phones.value = []
  }
}

async function load() {
  loading.value = true
  try {
    clearDetailCache()
    const { data } = await api.get('/analytics/by-counterparty', { params: rangeParams() })
    board.value = data
    await nextTick()
    renderBarChart()
  } catch (e) {
    board.value = null
    ElMessage.error(e.response?.data?.message || e.message || '加载失败')
  } finally {
    loading.value = false
    bindTableResizeObserver()
    nextTick(() => {
      renderBarChart()
      resizeChart()
    })
  }
}

function onTableRowClick(row) {
  if (row?.counterparty != null) {
    openDrawerByCounterparty(row.counterparty)
  }
}

onMounted(async () => {
  await loadPhones()
  await load()
  window.addEventListener('resize', updateTableHeight)
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  if (chartBar) {
    chartBar.off('click')
    chartBar.dispose()
    chartBar = null
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', updateTableHeight)
  window.removeEventListener('resize', resizeChart)
  if (tableResizeObserver) {
    tableResizeObserver.disconnect()
    tableResizeObserver = null
  }
})
</script>

<style scoped>
/* 与分析看板共用版式类名，便于视觉一致 */
.analytics-hub {
  width: 100%;
  box-sizing: border-box;
  padding: 0 0 16px;
}

.group-board-page {
  min-height: calc(100vh - 52px - 24px);
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.top-bar-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  flex: 1;
  min-width: 0;
}

.top-bar-scope-fields {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
}

.field-lbl {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.top-bar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.scope-tabs {
  flex-shrink: 0;
}

.compare-chart {
  width: 100%;
  height: 360px;
  margin-bottom: 0;
}

.group-bar-chart {
  min-height: 360px;
}

.chart-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background: #fafafa;
  border: 1px dashed #e4e7ed;
  border-radius: 8px;
}

.day-summary-empty .muted {
  opacity: 0.85;
}

.group-flow-wrap {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.day-chart-with-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 12px;
  margin-bottom: 8px;
}

.day-chart-pane {
  flex: 1 1 320px;
  min-width: 260px;
  margin-bottom: 0;
}

.day-summary-side {
  flex: 0 0 200px;
  width: 200px;
  max-width: 100%;
  padding: 10px 10px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f7fa 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-summary-head-plain {
  justify-content: flex-start;
}

.day-summary-date-only {
  font-weight: 700;
  font-size: 15px;
  color: #303133;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;
}

.day-summary-total-pill {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.day-stat-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.day-stat-chip {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  align-items: center;
  padding: 6px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  border: 1px solid transparent;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.day-stat-count,
.day-stat-amt {
  text-align: right;
  font-weight: 600;
}

.day-stat-chip.expense {
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  border-color: #ffccc7;
  color: #a8071a;
}

.day-stat-chip.income {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7d5 100%);
  border-color: #b7eb8f;
  color: #237804;
}

.day-stat-chip.neutral {
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  border-color: #d9d9d9;
  color: #595959;
}

.group-bottom-pane {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 10px 8px;
  margin-top: 4px;
}

.filter-toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
}

.table-block {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.table-grow {
  flex: 1 1 auto;
  min-height: 200px;
  width: 100%;
  position: relative;
}

.group-table :deep(.el-table__header-wrapper th) {
  background: #fff !important;
}

.group-table-clickable :deep(.el-table__body tr) {
  cursor: pointer;
}

.amt-expense {
  color: #c45656;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.amt-income {
  color: #237804;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 与分析看板「某月钻取」抽屉同宽：size=min(960px,96vw) + class month-drill-drawer */
.cp-detail-drawer :deep(.el-drawer__body) {
  padding: 12px 16px 20px;
}

.drawer-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  padding-right: 40px;
}

.drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.drawer-sub {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  word-break: break-all;
}

.drawer-body {
  min-height: 120px;
}

.drawer-sum {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
}

.drawer-sum-item.expense {
  color: #c45656;
  font-weight: 600;
}

.drawer-sum-item.income {
  color: #237804;
  font-weight: 600;
}

.drawer-sum-item.neutral {
  color: #595959;
  font-weight: 500;
}

.drawer-sum-item.time {
  color: #909399;
  font-weight: 400;
}
</style>
