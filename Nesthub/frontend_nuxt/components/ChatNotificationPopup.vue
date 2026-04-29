<template>
  <transition-group name="toast-slide" tag="div" class="chat-toast-container">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="chat-toast"
      @click="handleClick(toast)"
    >
      <div class="toast-header">
        <img :src="toast.avatar || '/default-avatar.png'" class="toast-avatar" />
        <span class="toast-username">{{ toast.username }}</span>
        <span class="toast-room">{{ toast.roomName }}</span>
        <close-icon class="toast-close" @click.stop="dismiss(toast.id)" />
      </div>
      <div class="toast-body">{{ toast.content }}</div>
    </div>
  </transition-group>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { getToken, authState } from '~/utils/auth'
import { useCurrentChannel } from '~/composables/useCurrentChannel'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const toasts = ref([])
let stompClient = null
let toastId = 0
const currentChannel = useCurrentChannel()
const isChannelMuted = (channelId) => {
  // 免打扰列表存储在 localStorage
  try {
    const saved = localStorage.getItem('popup_muted_channels')
    if (saved) {
      const muted = JSON.parse(saved)
      return muted.includes(channelId)
    }
  } catch (e) {}
  return false
}

const loadSetting = () => {
  if (import.meta.client) {
    const saved = localStorage.getItem('chat_toast_enabled')
    return saved === null ? true : saved === 'true'
  }
  return true
}

const dismiss = (id) => {
  toasts.value = toasts.value.filter(t => t.id !== id)
}

const handleClick = (toast) => {
  dismiss(toast.id)
  navigateTo(`/chat/${toast.roomId || toast.channelId || 0}`)
}

const connectAndSubscribe = () => {
  const token = getToken()
  if (!token) {
    console.warn('[ChatToast] 未登录，无法连接弹窗服务')
    return
  }

  if (stompClient) {
    console.log('[ChatToast] 断开旧连接')
    stompClient.deactivate()
  }

  const config = useRuntimeConfig()
  const socketUrl = `${config.public.apiBaseUrl}/ws`
  console.log('[ChatToast] 正在连接弹窗服务:', socketUrl)

  stompClient = new Client({
    webSocketFactory: () => new SockJS(socketUrl),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    debug: (msg) => console.log('[ChatToast STOMP]', msg),
    onConnect: () => {
      console.log('[ChatToast] ✅ 弹窗服务已连接')
      stompClient.subscribe('/topic/chat-notification', (message) => {
        console.log('[ChatToast] 📨 收到弹窗消息:', message.body)
        try {
          const data = JSON.parse(message.body)
          console.log('[ChatToast] 解析后的消息:', data)
          
          // 如果全局弹窗设置关闭，不显示任何弹窗
          if (!loadSetting()) {
            console.log('[ChatToast] 全局弹窗已关闭，跳过')
            return
          }
          
          // 免打扰频道不显示弹窗
          if (isChannelMuted(data.channelId)) {
            console.log('[ChatToast] 频道已免打扰，跳过:', data.channelId)
            return
          }

          console.log('[ChatToast] 当前频道:', JSON.stringify(currentChannel.value), '消息频道:', data.channelId)
          
          // 不显示当前所在频道的弹窗（已经在聊天室看到了）
          if (data.channelId === currentChannel.value.id) {
            console.log('[ChatToast] 当前所在频道的消息，跳过:', data.channelId)
            return
          }
          
          const id = ++toastId
          console.log('[ChatToast] 🎉 显示弹窗:', data)
          toasts.value.push({
            id,
            username: data.sender?.username || '未知用户',
            avatar: data.sender?.avatar || '',
            content: data.content || '',
            roomName: data.roomName || '',
            channelId: data.channelId,
            roomId: data.roomId,
          })
          
          // 5秒后自动消失
          setTimeout(() => {
            console.log('[ChatToast] 自动关闭弹窗:', id)
            dismiss(id)
          }, 5000)
        } catch (e) {
          console.error('[ChatToast] ❌ 解析消息失败', e, message.body)
        }
      })
      console.log('[ChatToast] 已订阅 /topic/chat-notification')
    },
    onStompError: (err) => console.error('[ChatToast] ❌ STOMP 错误:', err),
    onWebSocketClose: (evt) => console.log('[ChatToast] WebSocket 关闭:', evt),
    onWebSocketError: (evt) => console.error('[ChatToast] WebSocket 错误:', evt)
  })
  stompClient.activate()
}

// 监听频道切换事件
onMounted(() => {
  const token = getToken()
  if (token) {
    connectAndSubscribe()
  } else {
    // 没 token 时等 watch 触发
    console.log('[ChatToast] 等待登录...')
  }
  
  // 监听频道切换
  window.addEventListener('channel-changed', (event) => {
    const { channelId, popupRoomId } = event.detail
    console.log('[ChatToast] 频道切换至:', channelId, '弹窗频道:', popupRoomId)
  })
})

watch(() => authState.loggedIn, (loggedIn) => {
  if (loggedIn) connectAndSubscribe()
})

onBeforeUnmount(() => {
  if (stompClient) stompClient.deactivate()
})
</script>

<style scoped>
.chat-toast-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column-reverse;
  gap: 12px;
  max-width: 380px;
  pointer-events: none;
}

.chat-toast {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: auto;
  min-width: 320px;
}

.chat-toast:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16), 0 4px 12px rgba(0, 0, 0, 0.08);
}

.toast-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.toast-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.toast-username {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a1a;
}

.toast-room {
  font-size: 12px;
  color: #999;
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 8px;
  border-radius: 6px;
  margin-left: auto;
}

.toast-close {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  color: #ccc;
  cursor: pointer;
  transition: color 0.2s;
}

.toast-close:hover {
  color: #666;
}

.toast-body {
  font-size: 13px;
  color: #555;
  line-height: 1.5;
  padding-left: 46px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 进入/离开动画 */
.toast-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(100px) scale(0.9);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(100px) scale(0.9);
}
</style>