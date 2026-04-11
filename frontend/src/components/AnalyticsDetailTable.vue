<template>
  <el-table :data="rows" stripe border size="small" :max-height="maxHeight" class="detail-tx-table">
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
defineProps({
  rows: { type: Array, default: () => [] },
  maxHeight: { type: [Number, String], default: 320 }
})

function fmt(v) {
  if (v == null) return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}
</script>
