<template>
  <div class="chat-page">
    <div class="chat-header">
      <NuxtLink to="/" class="back-btn">← 返回</NuxtLink>
      <span class="chat-title">{{ roomName }}</span>
      <span class="chat-room-id">频道聊天</span>
    </div>

    <div class="chat-messages" ref="msgContainer">
      <div v-if="loading" class="chat-loading">加载中...</div>
      <div v-else-if="messages.length === 0" class="chat-empty">暂无消息，发送第一条消息吧</div>
      <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ mine: msg.senderId === myUserId }">
        <div class="msg-block">
          <div v-if="msg.senderId !== myUserId" class="msg-sender-name">{{ msg.sender?.username }}</div>
          <NuxtLink :to="`/users/${msg.sender?.id}`" class="msg-avatar-link">
            <img :src="msg.sender?.avatar || '/default-avatar.png'" class="msg-avatar" />
          </NuxtLink>
        </div>
        <div class="msg-bubble">
          <div class="msg-text">{{ msg.content }}</div>
        </div>
      </div>
    </div>

    <div class="chat-input-bar">
      <input v-model="input" @keyup.enter="send" placeholder="输入消息..." />
      <button @click="send">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getToken } from '~/utils/auth'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useCurrentChannel } from '~/composables/useCurrentChannel'

const route = useRoute()
const config = useRuntimeConfig()
const API = config.public.apiBaseUrl
const routeChannelId = Number(route.params.channelId)
const roomId = ref(routeChannelId)
const roomName = ref('聊天')
const messages = ref([])
const input = ref('')
const loading = ref(true)
const msgContainer = ref(null)
const myUserId = ref(null)
let stompClient = null

const fetchMyUser = async () => {
  try {
    const token = getToken()
    const res = await $fetch(`${API}/api/users/me`, { headers: { Authorization: `Bearer ${token}` } })
    myUserId.value = Number(res.id)
  } catch (e) { console.error('获取用户失败', e) }
}

const fetchRoom = async () => {
  try {
    const token = getToken()
    const channelId = Number(route.params.channelId)
    if (channelId === 0) {
      roomId.value = 6
      roomName.value = '全站聊天'
      return
    }
    const res = await $fetch(`${API}/api/chat/rooms/${channelId}`, { headers: { Authorization: `Bearer ${token}` } })
    roomName.value = res.name
    roomId.value = res.id
  } catch (e) {
    console.error('获取房间失败', e)
    roomName.value = '聊天'
  }
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const token = getToken()
    const res = await $fetch(`${API}/api/chat/rooms/${roomId.value}/messages?limit=50`, { headers: { Authorization: `Bearer ${token}` } })
    messages.value = res.reverse()
  } catch (e) {
    console.error('获取消息失败', e)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const send = async () => {
  if (!input.value.trim()) return
  try {
    const token = getToken()
    const res = await $fetch(`${API}/api/chat/rooms/${roomId.value}/messages`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ content: input.value })
    })
    input.value = ''
    scrollToBottom()
  } catch (e) {
    console.error('发送失败', e)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  })
}

const connectWebSocket = () => {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${API}/ws`),
    debug: (msg) => console.log('STOMP:', msg),
    onConnect: () => {
      client.subscribe(`/topic/chat/${roomId.value}`, (msg) => {
        const newMsg = JSON.parse(msg.body)
        const exists = messages.value.some(m => m.id === newMsg.id)
        if (!exists) {
          messages.value.push(newMsg)
          scrollToBottom()
        }
      })
    },
    onStompError: (err) => console.error('STOMP 错误', err)
  })
  stompClient = client
  client.activate()
}

const disconnectWebSocket = () => {
  if (stompClient) stompClient.deactivate()
}

onMounted(async () => {
  await fetchMyUser()
  await fetchRoom()
  currentChannel.value = { name: roomName.value, id: routeChannelId }
  await fetchMessages()
  connectWebSocket()
})

const currentChannel = useCurrentChannel()

onBeforeUnmount(() => {
  currentChannel.value = { name: '全部', id: -1 }
  disconnectWebSocket()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height));
  background: transparent;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--normal-border-color);
  flex-shrink: 0;
  background: transparent;
}

.back-btn { text-decoration: none; color: var(--primary-color); font-weight: 500; font-size: 14px; }
.chat-title { font-weight: 600; font-size: 16px; }
.chat-room-id { margin-left: auto; font-size: 12px; color: var(--text-tertiary); }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,0,0,0.10) transparent;
}

.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-track { background: transparent; }
.chat-messages::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.10); border-radius: 4px; }
.chat-messages::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.18); }

.chat-loading, .chat-empty {
  text-align: center;
  color: var(--text-tertiary);
  margin: auto;
  font-size: 14px;
}

.msg-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.msg-row.mine {
  flex-direction: row-reverse;
}

.msg-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  min-width: 40px;
}

.msg-sender-name {
  font-size: 11px;
  color: #9e9e9e;
  text-align: center;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-avatar-link { flex-shrink: 0; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }

.msg-bubble {
  background: rgba(255, 250, 245, 0.7);
  padding: 10px 14px;
  border-radius: 14px;
  max-width: 65%;
}
.msg-row.mine .msg-bubble { background: rgba(107, 107, 107, 0.12); }
.msg-text { font-size: 14px; word-break: break-word; }

.chat-input-bar {
  display: flex;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--normal-border-color);
  flex-shrink: 0;
  background: transparent;
}

.chat-input-bar input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid var(--normal-border-color);
  border-radius: 10px;
  outline: none;
  background: transparent;
  color: var(--text-color);
  font-size: 14px;
}

.chat-input-bar input::placeholder { color: var(--text-tertiary); }

.chat-input-bar button {
  padding: 10px 20px;
  background: var(--primary-color);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}
</style>