<template>
  <el-table :data="rows" stripe border max-height="480" class="data-table">
    <el-table-column v-if="mode === 'month'" label="日期" width="120">
      <template #default="{ row }">{{ String(row.date).slice(0, 10) }}</template>
    </el-table-column>
    <el-table-column v-else prop="monthLabel" label="月份" width="100" />
    <el-table-column label="收入">
      <template #default="{ row }">{{ fmt(row.incomeTotal) }}</template>
    </el-table-column>
    <el-table-column label="支出">
      <template #default="{ row }">{{ fmt(row.expenseTotal) }}</template>
    </el-table-column>
    <el-table-column label="中性">
      <template #default="{ row }">{{ fmt(row.neutralTotal) }}</template>
    </el-table-column>
    <template v-if="mode === 'month'">
      <el-table-column label="收入涨幅%">
        <template #default="{ row }">{{ row.incomeGrowthPercent ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="支出涨幅%">
        <template #default="{ row }">{{ row.expenseGrowthPercent ?? '—' }}</template>
      </el-table-column>
    </template>
  </el-table>
</template>

<script setup>
defineProps({
  rows: { type: Array, default: () => [] },
  mode: { type: String, default: 'month' }
})

function fmt(v) {
  if (v == null) return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}
</script>
