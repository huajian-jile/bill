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

function rowCounts(r) {
  if (!r) return { income: 0, expense: 0, neutral: 0 }
  return {
    income: Number(r.incomeCount ?? r.income_count) || 0,
    expense: Number(r.expenseCount ?? r.expense_count) || 0,
    neutral: Number(r.neutralCount ?? r.neutral_count) || 0
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
    const c = rowCounts(row)
    const hasTxn = !!(
      row &&
      (a.income > 0 ||
        a.expense > 0 ||
        a.neutral > 0 ||
        c.income > 0 ||
        c.expense > 0 ||
        c.neutral > 0)
    )
    out.push({
      day: d,
      ymd,
      row,
      income: a.income,
      expense: a.expense,
      neutral: a.neutral,
      incomeCount: c.income,
      expenseCount: c.expense,
      neutralCount: c.neutral,
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
  return cell.income > 0 || cell.incomeCount > 0
}
function showExpense(cell) {
  return cell.expense > 0 || cell.expenseCount > 0
}
function showNeutral(cell) {
  return cell.neutral > 0 || cell.neutralCount > 0
}

function onCell(cell) {
  if (!cell.day || !cell.ymd) return
  emit('date-click', { date: cell.ymd })
}
</script>

<style scoped>
/* 整体：多层径向 + 底色，偏「极光」浅色系 */
.month-cal {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 12px 16px;
  border-radius: 18px;
  background:
    radial-gradient(ellipse 90% 70% at 90% -10%, rgba(167, 139, 250, 0.35) 0%, transparent 55%),
    radial-gradient(ellipse 70% 50% at -5% 105%, rgba(56, 189, 248, 0.28) 0%, transparent 50%),
    radial-gradient(ellipse 50% 40% at 50% 100%, rgba(52, 211, 153, 0.18) 0%, transparent 45%),
    linear-gradient(155deg, #eef2ff 0%, #f8fafc 38%, #ecfeff 72%, #f0fdf4 100%);
  border: 1px solid rgba(148, 163, 184, 0.38);
  box-shadow:
    0 8px 32px rgba(15, 23, 42, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 12px;
  padding: 10px 8px;
  border-radius: 12px;
  background: linear-gradient(
    100deg,
    rgba(99, 102, 241, 0.12) 0%,
    rgba(168, 85, 247, 0.1) 22%,
    rgba(236, 72, 153, 0.08) 48%,
    rgba(14, 165, 233, 0.11) 72%,
    rgba(52, 211, 153, 0.1) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.cal-wd {
  text-align: center;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  padding: 5px 2px;
  color: #334155;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.9);
}

.cal-cells {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 9px;
}

.cal-cell {
  min-height: 98px;
  margin: 0;
  padding: 9px 7px;
  text-align: left;
  border-radius: 12px;
  cursor: default;
  box-sizing: border-box;
  font: inherit;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    filter 0.18s ease;
}

.cal-cell--pad {
  min-height: 0;
  padding: 0;
  border: none;
  background: transparent;
  cursor: default;
  pointer-events: none;
}

/* 普通日期：玻璃感浅色渐变 */
.cal-cell--day {
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: linear-gradient(
    165deg,
    rgba(255, 255, 255, 0.97) 0%,
    rgba(248, 250, 252, 0.92) 45%,
    rgba(241, 245, 249, 0.98) 100%
  );
  box-shadow:
    0 2px 10px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.cal-cell--day:hover {
  border-color: rgba(147, 197, 253, 0.95);
  background: linear-gradient(
    165deg,
    #ffffff 0%,
    rgba(239, 246, 255, 0.95) 55%,
    rgba(224, 242, 254, 0.85) 100%
  );
  box-shadow:
    0 8px 22px rgba(59, 130, 246, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transform: translateY(-2px);
}

/* 有流水：暖→冷 柔和渐变 */
.cal-cell--has {
  border: 1px solid rgba(251, 191, 36, 0.35);
  background: linear-gradient(
    135deg,
    #fffbeb 0%,
    #fef9c3 18%,
    #ecfdf5 42%,
    #e0f2fe 68%,
    #ede9fe 100%
  );
  box-shadow:
    0 4px 16px rgba(245, 158, 11, 0.1),
    0 2px 8px rgba(15, 23, 42, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.cal-cell--has:hover {
  border-color: rgba(56, 189, 248, 0.55);
  background: linear-gradient(
    135deg,
    #fff7ed 0%,
    #fef08a 12%,
    #d1fae5 40%,
    #bae6fd 72%,
    #ddd6fe 100%
  );
  box-shadow:
    0 10px 28px rgba(14, 165, 233, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
}

/* 无流水：灰蓝渐变压低 */
.cal-cell--muted {
  background: linear-gradient(175deg, rgba(248, 250, 252, 0.75) 0%, rgba(226, 232, 240, 0.55) 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  opacity: 1;
  box-shadow: inset 0 1px 2px rgba(148, 163, 184, 0.12);
}

.cal-day {
  font-weight: 800;
  font-size: 15px;
  margin-bottom: 6px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  color: #0f172a;
}

.cal-cell--muted .cal-day {
  color: #94a3b8;
}

.cal-nums {
  font-size: 11px;
  line-height: 1.42;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.cal-nums .ln {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 2px 4px;
  padding: 4px 6px;
  border-radius: 8px;
  font-weight: 700;
}

.cal-nums .ln-lbl {
  flex-shrink: 0;
}

.cal-nums .ln-cnt {
  font-size: 10px;
  font-weight: 600;
  opacity: 0.92;
}

.cal-nums .ln-amt {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.cal-nums .inc {
  color: #047857;
  background: linear-gradient(95deg, rgba(52, 211, 153, 0.45) 0%, rgba(167, 243, 208, 0.35) 100%);
  border: 1px solid rgba(16, 185, 129, 0.35);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.cal-nums .exp {
  color: #b91c1c;
  background: linear-gradient(95deg, rgba(252, 165, 165, 0.55) 0%, rgba(254, 202, 202, 0.35) 100%);
  border: 1px solid rgba(248, 113, 113, 0.4);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

.cal-nums .neu {
  color: #a16207;
  background: linear-gradient(95deg, rgba(253, 224, 71, 0.55) 0%, rgba(254, 240, 138, 0.4) 100%);
  border: 1px solid rgba(234, 179, 8, 0.45);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.cal-none {
  font-size: 12px;
  color: #cbd5e1;
  padding-top: 2px;
}
</style>
