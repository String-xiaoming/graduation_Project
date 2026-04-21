<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  value: {
    type: [String, Number],
    required: true,
  },
  note: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <article class="metric-card">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small>{{ note }}</small>
  </article>
</template>
