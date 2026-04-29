<template>
  <transition name="slide">
    <nav v-if="visible" class="menu">
      <div class="menu-content">
        <!-- 常用 -->
        <div class="menu-section">
          <div class="menu-section-title">常用</div>
          <NuxtLink class="menu-item" exact-active-class="selected" to="/" @click="handleItemClick">
            <hashtag-key class="menu-item-icon" />
            <span class="menu-item-text">话题</span>
          </NuxtLink>
          <NuxtLink class="menu-item" exact-active-class="selected" to="/new-post" @click="handleItemClick">
            <edit class="menu-item-icon" />
            <span class="menu-item-text">发帖</span>
          </NuxtLink>
          <NuxtLink class="menu-item" exact-active-class="selected" to="/message" @click="handleItemClick">
            <remind class="menu-item-icon" />
            <span class="menu-item-text">通知</span>
            <span v-if="unreadCount > 0" class="unread-badge">{{ showUnreadCount }}</span>
          </NuxtLink>
        </div>

        <!-- 频道（默认折叠） -->
        <div class="menu-section">
          <div class="menu-section-title">频道</div>
          
          <!-- 频道总折叠按钮 -->
          <div
            class="menu-item channel-toggle"
            :class="{ expanded: channelsExpanded }"
            @click="channelsExpanded = !channelsExpanded"
          >
            <all-application class="menu-item-icon" />
            <span class="menu-item-text">所有频道</span>
            <span class="channel-toggle-arrow" :class="{ expanded: channelsExpanded }">▾</span>
          </div>

          <!-- 频道列表 -->
          <transition name="collapse-all">
            <div v-if="channelsExpanded" class="channels-container">
              <div
                v-for="channel in channels"
                :key="channel.name"
                class="menu-item channel-item"
                :class="{ active: currentChannel.name === channel.name }"
                @click="selectChannel(channel)"
              >
                <span class="channel-dot"></span>
                <span class="menu-item-icon channel-icon">{{ getChannelIcon(channel.name) }}</span>
                <span class="menu-item-text">{{ channel.name }}</span>
              </div>
            </div>
          </transition>
        </div>

        <!-- 管理 -->
        <div v-if="authState.role === 'ADMIN'" class="menu-section">
          <div class="menu-section-title">管理</div>
          <NuxtLink class="menu-item" exact-active-class="selected" to="/admin/channels" @click="handleItemClick">
            <setting-icon class="menu-item-icon" />
            <span class="menu-item-text">频道管理</span>
          </NuxtLink>
          <NuxtLink class="menu-item" exact-active-class="selected" to="/admin/users" @click="handleItemClick">
            <people class="menu-item-icon" />
            <span class="menu-item-text">用户管理</span>
          </NuxtLink>
        </div>
      </div>

      <ClientOnly v-if="!isMobile">
        <div class="menu-footer">
          <div class="menu-footer-btn" @click="cycleTheme">
            <component :is="iconClass" class="menu-item-icon" />
          </div>
        </div>
      </ClientOnly>
    </nav>
  </transition>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { authState, fetchCurrentUser, getToken } from '~/utils/auth'
import { fetchUnreadCount, notificationState } from '~/utils/notification'
import { useIsMobile } from '~/utils/screen'
import { cycleTheme, ThemeMode, themeState } from '~/utils/theme'
import { useCurrentChannel } from '~/composables/useCurrentChannel'

const isMobile = useIsMobile()
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const route = useRoute()

const props = defineProps({ visible: { type: Boolean, default: true } })
const emit = defineEmits(['item-click'])

const myPoint = ref(null)
const channels = ref([{ name: '全部', id: 0 }])
const categoriesData = ref([])

// 使用共享状态
const currentChannel = useCurrentChannel()

// 频道总折叠状态（默认折叠）
const channelsExpanded = ref(false)

const iconClass = computed(() => {
  switch (themeState.mode) {
    case ThemeMode.DARK: return 'Moon'
    case ThemeMode.LIGHT: return 'SunOne'
    default: return 'ComputerOne'
  }
})

const unreadCount = computed(() => notificationState.unreadCount)
const showUnreadCount = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value))

const loadPoint = async () => {
  if (authState.loggedIn) {
    const user = await fetchCurrentUser()
    myPoint.value = user ? user.point : null
  } else { myPoint.value = null }
}

const updateCount = async () => {
  if (authState.loggedIn) { await fetchUnreadCount() } else { notificationState.unreadCount = 0 }
}

// 加载频道列表
const loadChannels = async () => {
  try {
    const res = await $fetch(`${API_BASE_URL}/api/categories`)
    const data = Array.isArray(res) ? res : []
    categoriesData.value = data
    channels.value = [
      { name: '全部', id: 0 },
      ...data.filter(c => c.name !== '未分类').map(c => ({ name: c.name, id: c.id }))
    ]
  } catch (e) { console.error('Failed to load channels', e) }
}

const getChannelIcon = (name) => {
  if (name === '全部') return '📋'
  const category = categoriesData.value.find(c => c.name === name)
  return category?.icon || '📁'
}

// 点击频道 —— 同时更新共享状态里的频道信息
const selectChannel = (channel) => {
  currentChannel.value = { name: channel.name, id: channel.id }
  
  // 导航到首页并带上筛选参数
  if (channel.name === '全部') {
    navigateTo({ path: '/' })
  } else {
    navigateTo({ path: '/', query: { category: encodeURIComponent(String(channel.id)) } })
  }
  
  // 移动端关闭菜单
  if (window.innerWidth <= 768) emit('item-click')
}

onMounted(async () => {
  await Promise.all([updateCount(), loadPoint(), loadChannels()])
  watch(() => authState.loggedIn, () => { updateCount(); loadPoint() })
  window.addEventListener('categories-updated', loadChannels)
})

onBeforeUnmount(() => {
  window.removeEventListener('categories-updated', loadChannels)
})

const handleItemClick = () => {
  if (window.innerWidth <= 768) emit('item-click')
}
</script>

<style scoped>
.menu {
  position: sticky;
  top: var(--header-height, 56px);
  width: 220px;
  height: calc(100vh - var(--header-height, 56px));
  background: linear-gradient(160deg,
    rgba(240, 215, 195, 0.75) 0%,
    rgba(235, 210, 190, 0.60) 25%,
    rgba(240, 228, 218, 0.42) 55%,
    rgba(248, 244, 240, 0.25) 80%,
    rgba(252, 250, 248, 0.12) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,0,0,0.08) transparent;
  flex-shrink: 0;
}

.menu::-webkit-scrollbar { width: 4px; }
.menu::-webkit-scrollbar-track { background: transparent; }
.menu::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 4px; }

.menu-content {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.menu-section {
  margin-bottom: 8px;
}

.menu-section-title {
  font-size: 10px;
  font-weight: 600;
  color: #8a7a72;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding: 6px 12px 4px;
  opacity: 0.7;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  text-decoration: none;
  color: #463a36;
  opacity: 0.55;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
}

.menu-item:hover {
  background: rgba(210, 195, 190, 0.18);
  opacity: 0.85;
}

.menu-item.selected,
.menu-item.active {
  background: rgba(200, 185, 180, 0.25);
  color: #463a36;
  opacity: 1;
  font-weight: 600;
}

.menu-item-icon { width: 18px; height: 18px; flex-shrink: 0; text-align: center; }
.menu-item-text { flex: 1; }

/* 频道总折叠按钮 */
.channel-toggle {
  position: relative;
}

.channel-toggle-arrow {
  font-size: 10px;
  opacity: 0.4;
  transition: transform 0.25s ease;
  flex-shrink: 0;
}

.channel-toggle-arrow.expanded {
  transform: rotate(180deg);
}

/* 频道列表容器 */
.channels-container {
  margin-top: 2px;
  padding-left: 2px;
}

/* 频道项 */
.channel-item {
  position: relative;
  padding-left: 28px;
}

.channel-dot {
  position: absolute;
  left: 14px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(70, 58, 54, 0.3);
  transition: background 0.2s;
}

.channel-item.active .channel-dot {
  background: var(--primary-color, #b8856e);
}

.channel-icon {
  font-size: 13px;
}

.unread-badge {
  background-color: #e0a8a8;
  color: white;
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}

/* 折叠动画 */
.collapse-all-enter-active {
  transition: all 0.25s ease-out;
}
.collapse-all-leave-active {
  transition: all 0.2s ease-in;
}
.collapse-all-enter-from,
.collapse-all-leave-to {
  opacity: 0;
  max-height: 0;
}

/* 菜单底部 */
.menu-footer {
  padding: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.menu-footer-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  opacity: 0.5;
  transition: all 0.2s ease;
}

.menu-footer-btn:hover {
  background: rgba(210, 195, 190, 0.18);
  opacity: 0.85;
}

.slide-enter-active, .slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-enter-from, .slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}
</style>