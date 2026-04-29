<template>
  <div class="sub-channel-select">
    <div class="select-wrapper">
      <select v-model="selected" @change="onChange" class="select-box">
        <option value="">默认子频道</option>
        <option v-for="sub in subChannels" :key="sub" :value="sub">
          {{ sub }} 
        </option>
      </select>
      <span class="select-arrow">
        <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg>
      </span>
    </div>
  </div>
</template>

<script setup>
// --- 保持原有 script 不变 ---
import { ref, watch } from 'vue'
import { getToken, authState } from '~/utils/auth'

const props = defineProps({
  modelValue: { type: String, default: '' },
  categoryId: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue'])
const selected = ref(props.modelValue)
const subChannels = ref([])

const fetchSubChannels = async (channelId) => {
  if (!channelId) {
    subChannels.value = []
    return
  }
  try {
    const config = useRuntimeConfig()
    const token = getToken()
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    const res = await $fetch(`${config.public.apiBaseUrl}/api/sub-channels/by-channel/${channelId}`, { headers })
    subChannels.value = res.map(s => s.name).filter(name => {
      if (name === '精华' || name === '未分类' || name === '最新' || name === '火') return false
      if (name === '官方' && authState.role !== 'ADMIN') return false
      return true
    })
    if (subChannels.value.length > 0 && !subChannels.value.includes(selected.value)) {
      selected.value = subChannels.value[0]
      emit('update:modelValue', selected.value)
    }
  } catch (e) {
    console.error('加载子频道失败', e)
    subChannels.value = []
  }
}

watch(() => props.categoryId, (newId) => {
  if (newId) fetchSubChannels(newId)
}, { immediate: true })

watch(() => props.modelValue, (val) => {
  selected.value = val
})

const onChange = () => {
  emit('update:modelValue', selected.value)
}
</script>

<style scoped>
.sub-channel-select {
  min-width: 140px;
}

.select-wrapper {
  position: relative;
}

.select-box {
  width: 100%;
  padding: 8px 32px 8px 12px;
  border: 1px solid var(--normal-border-color, #e0e0e0);
  border-radius: 8px;
  background: var(--background-color, #fff);
  color: var(--text-color, #333);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  outline: none;
  appearance: none;
  -webkit-appearance: none;
  transition: all 0.2s;
}

.select-box:hover {
  border-color: var(--primary-color, #7c3aed);
}

.select-box:focus {
  border-color: var(--primary-color, #7c3aed);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.select-arrow {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: var(--text-secondary, #999);
  display: flex;
  align-items: center;
}
</style>