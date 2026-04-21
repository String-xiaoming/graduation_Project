<script setup>
defineProps({
  text: {
    type: String,
    default: '数据加载中',
  },
})
</script>

<template>
  <div class="loading-block">
    <span></span>
    <p>{{ text }}</p>
  </div>
</template>
