<template>
  <img
    :src="src"
    :alt="alt"
    loading="lazy"
    :class="['base-image', $attrs.class, { 'is-loaded': loaded }]"
    @load="onLoad"
    @error="onError"
    v-bind="$attrs"
  />
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
})

const loaded = ref(false)

function onLoad() {
  loaded.value = true
}

function onError() {
  loaded.value = true
}
</script>

<style scoped>
.base-image {
  transition:
    filter 0.35s ease,
    transform 0.35s ease,
    opacity 0.35s ease;
  opacity: 0.92;
}

.base-image.is-loaded {
  transform: none;
  opacity: 1;
}
</style>