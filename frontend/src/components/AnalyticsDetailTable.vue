<template>
  <el-table
    :data="rows"
    stripe
    border
    size="small"
    :max-height="maxHeight"
    class="detail-tx-table"
    :row-class-name="tableRowClassName"
  >
    <el-table-column prop="tradeTime" label="交易时间" width="158" show-overflow-tooltip />
    <el-table-column label="渠道" width="52" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.billChannel === 'alipay'" size="small" type="warning">支</el-tag>
        <el-tag v-else-if="row.billChannel === 'wechat'" size="small" type="success">微</el-tag>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column label="金额(元)" width="100" align="right">
      <template #default="{ row }">{{ fmt(row.amountYuan) }}</template>
    </el-table-column>
    <el-table-column prop="counterparty" label="交易对方" min-width="120" show-overflow-tooltip />
    <el-table-column prop="tradeType" label="交易类型" width="100" show-overflow-tooltip />
    <el-table-column prop="product" label="商品" min-width="90" show-overflow-tooltip />
    <el-table-column prop="paymentMethod" label="支付方式" width="100" show-overflow-tooltip />
    <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
  </el-table>
</template>

<script setup>
import { isNeutralIncomeExpense } from '../utils/incomeExpense'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  maxHeight: { type: [Number, String], default: 320 },
  /** 整表单一色调：收入绿、支出红、中性黄；空则按 perRowTone 或不着色 */
  tone: { type: String, default: '' },
  /** 为 true 时按行根据 incomeExpense 着色（仅在 tone 为空时生效） */
  perRowTone: { type: Boolean, default: false }
})

function classifyRow(row) {
  const ie = row.incomeExpense || ''
  if (ie.includes('收入')) return 'income'
  if (ie.includes('支出')) return 'expense'
  if (isNeutralIncomeExpense(ie)) return 'neutral'
  return 'other'
}

function tableRowClassName({ row }) {
  const t = props.tone
  if (t === 'income') return 'tx-row-income'
  if (t === 'expense') return 'tx-row-expense'
  if (t === 'neutral') return 'tx-row-neutral'
  if (props.perRowTone) {
    const k = classifyRow(row)
    if (k === 'income') return 'tx-row-income'
    if (k === 'expense') return 'tx-row-expense'
    if (k === 'neutral') return 'tx-row-neutral'
  }
  return ''
}

function fmt(v) {
  if (v == null) return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}
</script>

<style scoped>
:deep(.el-table__body tr.tx-row-income > td) {
  background: #f6ffed !important;
  color: #237804;
}
:deep(.el-table__body tr.tx-row-expense > td) {
  background: #fff2f0 !important;
  color: #a8071a;
}
:deep(.el-table__body tr.tx-row-neutral > td) {
  background: #fffbe6 !important;
  color: #ad6800;
}
:deep(.el-table__body tr.tx-row-income:hover > td) {
  background: #e6f7d5 !important;
}
:deep(.el-table__body tr.tx-row-expense:hover > td) {
  background: #ffccc7 !important;
}
:deep(.el-table__body tr.tx-row-neutral:hover > td) {
  background: #ffe58f !important;
}
</style>
