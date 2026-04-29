<template>
  <div class="friend-request-notification">
    <div class="notification-icon" @click="showDropdown = !showDropdown">
      <span class="bell-label">好友</span>
      <span class="bell-label">申请</span>
      <span v-if="pendingRequests.length > 0" class="badge">{{ pendingRequests.length }}</span>
    </div>
    
    <div v-if="showDropdown" class="dropdown-menu">
      <div class="dropdown-header">好友申请</div>
      <div v-if="pendingRequests.length === 0" class="empty">暂无好友申请</div>
      <div v-else>
        <div v-for="request in pendingRequests" :key="request.id" class="request-item">
          <NuxtLink :to="`/users/${request.sender.id}`" class="sender-name">
            {{ request.sender.username }}
          </NuxtLink>
          <span class="message">{{ request.message || '请求添加你为好友' }}</span>
          <div class="actions">
            <button class="accept-btn" @click="acceptRequest(request.id)">同意</button>
            <button class="reject-btn" @click="rejectRequest(request.id)">拒绝</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getToken } from '~/utils/auth'
import { toast } from '~/main'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const pendingRequests = ref([])
const showDropdown = ref(false)
let intervalId = null

const emit = defineEmits(['update-count'])

const fetchPendingRequests = async () => {
  const token = getToken()
  if (!token) return
  
  try {
    const res = await $fetch(`${API_BASE_URL}/api/friends/requests/pending`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    pendingRequests.value = res
    emit('update-count', res.length)
  } catch (e) {
    console.error('获取好友申请失败', e)
  }
}

const acceptRequest = async (requestId) => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  
  try {
    await $fetch(`${API_BASE_URL}/api/friends/request/${requestId}/accept`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    })
    toast.success('已同意好友申请')
    await fetchPendingRequests()
    window.dispatchEvent(new Event('friend-status-changed'))
  } catch (e) {
    console.error('同意申请失败', e)
    toast.error(e.data?.error || '操作失败')
  }
}

const rejectRequest = async (requestId) => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  
  try {
    await $fetch(`${API_BASE_URL}/api/friends/request/${requestId}/reject`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    })
    toast.success('已拒绝好友申请')
    await fetchPendingRequests()
    window.dispatchEvent(new Event('friend-status-changed'))
  } catch (e) {
    console.error('拒绝申请失败', e)
    toast.error(e.data?.error || '操作失败')
  }
}

const handleClickOutside = (event) => {
  const dropdown = document.querySelector('.friend-request-notification')
  if (dropdown && !dropdown.contains(event.target)) {
    showDropdown.value = false
  }
}

const handleFriendRequestSent = () => {
  fetchPendingRequests()
}

onMounted(() => {
  fetchPendingRequests()
  intervalId = setInterval(fetchPendingRequests, 10000)
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('friend-request-sent', handleFriendRequestSent)
})

onUnmounted(() => {
  if (intervalId) {
    clearInterval(intervalId)
  }
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('friend-request-sent', handleFriendRequestSent)
})
</script>

<style scoped>
.friend-request-notification {
  position: relative;
}

.notification-icon {
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 6px 12px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-icon:hover {
  transform: translateY(-3px);
}

.notification-icon:active {
  transform: translateY(-1px);
}

.bell-label {
  font-size: 12px;
  color: #888;
  line-height: 1.2;
  white-space: nowrap;
  transition: color 0.3s;
}

.notification-icon:hover .bell-label {
  color: #444;
}

.badge {
  position: absolute;
  top: 0px;
  right: 2px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 1px 5px;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.4;
  min-width: 16px;
  text-align: center;
}

.dropdown-menu {
  position: absolute;
  top: 52px;
  left: 50%;
  transform: translateX(-50%);
  width: 300px;
  max-width: calc(100vw - 40px);
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 400px;
  overflow-y: auto;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.dropdown-header {
  padding: 14px 16px;
  font-weight: 700;
  font-size: 14px;
  border-bottom: 1px solid var(--normal-border-color);
}

.empty {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.request-item {
  padding: 14px 16px;
  border-bottom: 1px solid var(--normal-border-color);
}

.request-item:last-child {
  border-bottom: none;
}

.sender-name {
  font-weight: 600;
  text-decoration: none;
  color: var(--primary-color);
  font-size: 14px;
}

.sender-name:hover {
  text-decoration: underline;
}

.message {
  display: block;
  margin: 4px 0;
  font-size: 13px;
  color: var(--text-secondary-color, #999);
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.accept-btn {
  padding: 6px 16px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.accept-btn:hover {
  background-color: var(--primary-color-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.accept-btn:active {
  transform: translateY(0);
}

.reject-btn {
  padding: 6px 16px;
  background-color: transparent;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-color);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.reject-btn:hover {
  background-color: var(--secondary-color-hover, rgba(0, 0, 0, 0.04));
  transform: translateY(-2px);
}

.reject-btn:active {
  transform: translateY(0);
}
</style>