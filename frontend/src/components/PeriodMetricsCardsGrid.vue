<template>
  <div class="pm-wrap">
    <div class="pm-grid" :style="gridStyle">
      <button
        v-for="cell in cells"
        :key="cell.key"
        type="button"
        class="pm-card"
        :class="{ 'pm-card--empty': !cell.hasTxn }"
        @click="onSelect(cell)"
      >
        <div class="pm-card-h" :class="headerClassFor(cell.row)">{{ cell.label }}</div>
        <div class="pm-card-body">
          <template v-if="cell.hasTxn">
            <div v-if="showIncome(cell)" class="ln inc">
              <span class="ln-lbl">收</span>
              <span class="ln-cnt">{{ cell.incomeCount }} 笔</span>
              <span class="ln-amt">{{ fmt(cell.income) }}</span>
            </div>
            <div v-if="showExpense(cell)" class="ln exp">
              <span class="ln-lbl">支</span>
              <span class="ln-cnt">{{ cell.expenseCount }} 笔</span>
              <span class="ln-amt">{{ fmt(cell.expense) }}</span>
            </div>
            <div v-if="showNeutral(cell)" class="ln neu">
              <span class="ln-lbl">中</span>
              <span class="ln-cnt">{{ cell.neutralCount }} 笔</span>
              <span class="ln-amt">{{ fmt(cell.neutral) }}</span>
            </div>
          </template>
          <div v-else class="pm-empty">暂无流水</div>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 每行卡片列数 */
  columns: { type: Number, default: 5 },
  /** 行数据：monthLabel、收入支出中性金额与笔数 */
  rows: { type: Array, default: () => [] }
})

const emit = defineEmits(['select'])

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${props.columns}, minmax(0, 1fr))`
}))

function rowAmounts(r) {
  if (!r) return { income: 0, expense: 0, neutral: 0 }
  return {
    income: Number(r.incomeTotal ?? r.income_total) || 0,
    expense: Number(r.expenseTotal ?? r.expense_total) || 0,
    neutral: Number(r.neutralTotal ?? r.neutral_total) || 0
  }
}

function rowCounts(r) {
  if (!r) return { income: 0, expense: 0, neutral: 0 }
  return {
    income: Number(r.incomeCount ?? r.income_count) || 0,
    expense: Number(r.expenseCount ?? r.expense_count) || 0,
    neutral: Number(r.neutralCount ?? r.neutral_count) || 0
  }
}

const cells = computed(() => {
  return (props.rows || []).map((row, idx) => {
    const a = rowAmounts(row)
    const c = rowCounts(row)
    const hasTxn =
      a.income > 0 ||
      a.expense > 0 ||
      a.neutral > 0 ||
      c.income > 0 ||
      c.expense > 0 ||
      c.neutral > 0
    const label = String(row?.monthLabel ?? row?.label ?? '').trim() || '—'
    return {
      key: label + '-' + idx,
      row,
      label,
      income: a.income,
      expense: a.expense,
      neutral: a.neutral,
      incomeCount: c.income,
      expenseCount: c.expense,
      neutralCount: c.neutral,
      hasTxn
    }
  })
})

function fmt(v) {
  if (v == null || !Number.isFinite(Number(v))) return '—'
  return Number(v).toFixed(2)
}

function showIncome(cell) {
  return cell.income > 0 || cell.incomeCount > 0
}
function showExpense(cell) {
  return cell.expense > 0 || cell.expenseCount > 0
}
function showNeutral(cell) {
  return cell.neutral > 0 || cell.neutralCount > 0
}

/** 月份 1–12 用 12 套色；纯年份四位数按年循环同一套色 */
function headerClassFor(row) {
  const lab = String(row?.monthLabel ?? row?.label ?? '').trim()
  const mm = lab.match(/^(\d{4})-(\d{2})$/)
  if (mm) {
    const m = parseInt(mm[2], 10)
    if (m >= 1 && m <= 12) return 'pm-h tone-' + m
  }
  if (/^\d{4}$/.test(lab)) {
    const y = parseInt(lab, 10)
    const r = ((y % 12) + 12) % 12
    const slot = r === 0 ? 12 : r
    return 'pm-h tone-' + slot
  }
  return 'pm-h tone-def'
}

function onSelect(cell) {
  if (cell?.row) emit('select', cell.row)
}
</script>

<style scoped>
.pm-wrap {
  width: 100%;
  box-sizing: border-box;
}

.pm-grid {
  display: grid;
  gap: 14px;
  width: 100%;
}

.pm-card {
  margin: 0;
  padding: 0;
  text-align: left;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font: inherit;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.07);
  border: 1px solid rgba(226, 232, 240, 0.95);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.pm-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.12);
}

.pm-card--empty {
  opacity: 0.92;
}

.pm-card-h {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.18);
  font-variant-numeric: tabular-nums;
}

/* 标题条 12 色（月份与年份共用 tone-1…tone-12） */
.pm-h.tone-1 {
  background: linear-gradient(135deg, #7c9cf0 0%, #5b7cfa 100%);
}
.pm-h.tone-2 {
  background: linear-gradient(135deg, #8ec5fc 0%, #6b9df5 100%);
}
.pm-h.tone-3 {
  background: linear-gradient(135deg, #6ee7d8 0%, #3db8a8 100%);
}
.pm-h.tone-4 {
  background: linear-gradient(135deg, #86efac 0%, #3cb371 100%);
}
.pm-h.tone-5 {
  background: linear-gradient(135deg, #c4e86a 0%, #8fbc24 100%);
}
.pm-h.tone-6 {
  background: linear-gradient(135deg, #fde047 0%, #eab308 100%);
  color: #422006;
  text-shadow: none;
}
.pm-h.tone-7 {
  background: linear-gradient(135deg, #fdba74 0%, #ea580c 100%);
}
.pm-h.tone-8 {
  background: linear-gradient(135deg, #fb923c 0%, #dc2626 100%);
}
.pm-h.tone-9 {
  background: linear-gradient(135deg, #f472b6 0%, #db2777 100%);
}
.pm-h.tone-10 {
  background: linear-gradient(135deg, #c4b5fd 0%, #7c3aed 100%);
}
.pm-h.tone-11 {
  background: linear-gradient(135deg, #a5b4fc 0%, #6366f1 100%);
}
.pm-h.tone-12 {
  background: linear-gradient(135deg, #94a3e8 0%, #4f46e5 100%);
}
.pm-h.tone-def {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

.pm-card-body {
  padding: 10px 12px 12px;
  min-height: 88px;
}

.pm-empty {
  font-size: 12px;
  color: #c0c4cc;
  padding: 8px 0;
}

.ln {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 6px;
  font-size: 12px;
  line-height: 1.45;
  margin-bottom: 4px;
}
.ln:last-child {
  margin-bottom: 0;
}

.ln-lbl {
  font-weight: 700;
  min-width: 1em;
}
.ln-cnt {
  font-size: 11px;
  font-weight: 600;
  opacity: 0.92;
}
.ln-amt {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  margin-left: auto;
}

.ln.inc {
  color: #237804;
  background: rgba(82, 196, 26, 0.08);
  border-radius: 6px;
  padding: 4px 6px;
}
.ln.exp {
  color: #c41d1f;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 6px;
  padding: 4px 6px;
}
.ln.neu {
  color: #ad6800;
  background: rgba(250, 173, 20, 0.12);
  border-radius: 6px;
  padding: 4px 6px;
}
</style>
