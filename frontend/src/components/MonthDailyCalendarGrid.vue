<template>
  <div class="month-cal">
    <div class="cal-weekdays">
      <span v-for="h in weekLabels" :key="h" class="cal-wd">{{ h }}</span>
    </div>
    <div class="cal-cells">
      <template v-for="(cell, idx) in cells" :key="idx">
        <div v-if="!cell.day" class="cal-cell cal-cell--pad" />
        <button
          v-else
          type="button"
          class="cal-cell cal-cell--day"
          :class="{
            'cal-cell--muted': !cell.hasTxn,
            'cal-cell--has': cell.hasTxn
          }"
          @click="onCell(cell)"
        >
          <div class="cal-day">{{ cell.day }}</div>
          <div v-if="cell.hasTxn" class="cal-nums">
            <div v-if="showIncome(cell)" class="ln inc">收 {{ fmt(cell.income) }}</div>
            <div v-if="showExpense(cell)" class="ln exp">支 {{ fmt(cell.expense) }}</div>
            <div v-if="showNeutral(cell)" class="ln neu">中 {{ fmt(cell.neutral) }}</div>
          </div>
          <div v-else class="cal-none">—</div>
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  /** 已归一化的每日行，含 date、incomeTotal 等 */
  rows: { type: Array, default: () => [] }
})

const emit = defineEmits(['date-click'])

const weekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

function pad2(n) {
  return String(n).padStart(2, '0')
}

function rowAmounts(r) {
  if (!r) return { income: 0, expense: 0, neutral: 0 }
  return {
    income: Number(r.incomeTotal ?? r.income_total) || 0,
    expense: Number(r.expenseTotal ?? r.expense_total) || 0,
    neutral: Number(r.neutralTotal ?? r.neutral_total) || 0
  }
}

const rowByDate = computed(() => {
  const m = new Map()
  for (const r of props.rows || []) {
    const ds = String(r.date ?? '').slice(0, 10)
    if (ds.length >= 10) m.set(ds, r)
  }
  return m
})

const cells = computed(() => {
  const y = props.year
  const mo = props.month
  const last = new Date(y, mo, 0).getDate()
  const jsDow = new Date(y, mo - 1, 1).getDay()
  const startPad = (jsDow + 6) % 7
  const out = []
  for (let i = 0; i < startPad; i++) {
    out.push({ day: null })
  }
  for (let d = 1; d <= last; d++) {
    const ymd = `${y}-${pad2(mo)}-${pad2(d)}`
    const row = rowByDate.value.get(ymd)
    const a = rowAmounts(row)
    const hasTxn = !!(row && (a.income > 0 || a.expense > 0 || a.neutral > 0))
    out.push({
      day: d,
      ymd,
      row,
      income: a.income,
      expense: a.expense,
      neutral: a.neutral,
      hasTxn
    })
  }
  while (out.length % 7 !== 0) {
    out.push({ day: null })
  }
  return out
})

function fmt(v) {
  if (v == null || !Number.isFinite(Number(v))) return '—'
  return Number(v).toFixed(2)
}

function showIncome(cell) {
  return cell.income > 0
}
function showExpense(cell) {
  return cell.expense > 0
}
function showNeutral(cell) {
  return cell.neutral > 0
}

function onCell(cell) {
  if (!cell.day || !cell.ymd) return
  emit('date-click', { date: cell.ymd })
}
</script>

<style scoped>
.month-cal {
  width: 100%;
  box-sizing: border-box;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 8px;
}

.cal-wd {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding: 6px 2px;
}

.cal-cells {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cal-cell {
  min-height: 92px;
  margin: 0;
  padding: 8px 6px;
  text-align: left;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  cursor: default;
  box-sizing: border-box;
  font: inherit;
  transition:
    background 0.15s,
    border-color 0.15s;
}

.cal-cell--pad {
  min-height: 0;
  padding: 0;
  border: none;
  background: transparent;
  cursor: default;
  pointer-events: none;
}

.cal-cell--day {
  cursor: pointer;
  background: #fff;
  border-color: #dcdfe6;
}

.cal-cell--day:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.cal-cell--has {
  border-color: #dcdfe6;
}

.cal-cell--muted {
  background: #fcfcfc;
}

.cal-day {
  font-weight: 700;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  font-variant-numeric: tabular-nums;
}

.cal-nums {
  font-size: 11px;
  line-height: 1.35;
}

.cal-nums .ln {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cal-nums .inc {
  color: #237804;
  font-weight: 600;
}
.cal-nums .exp {
  color: #c41d1f;
  font-weight: 600;
}
.cal-nums .neu {
  color: #606266;
  font-weight: 500;
}

.cal-none {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
