<template>
  <div class="placeholder">
    <h2>{{ title }}</h2>
    <p class="hint">功能开发中，敬请期待。</p>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' }
})
</script>

<style scoped>
.placeholder {
  padding: 24px;
}
.hint {
  color: #909399;
  margin-top: 8px;
}
</style>
