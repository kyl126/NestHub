<template>
  <header class="header">
    <div class="header-content">
      <div class="header-content-left">
        <div v-if="showMenuBtn" class="menu-btn-wrapper">
          <button class="menu-btn" ref="menuBtn" @click="$emit('toggle-menu')">
            <ToolTip content="展开/收起菜单" placement="bottom">
              <application-menu class="micon"></application-menu>
            </ToolTip>
          </button>
        </div>
        <NuxtLink class="logo-container" :to="`/`" @click="refrechData">
          <div class="logo-text">社区</div>
        </NuxtLink>
      </div>

      <ClientOnly>
        <div class="header-content-right">
          <SearchDropdown ref="searchDropdown" @close="closeSearch" />

          <ToolTip v-if="isMobile" content="搜索" placement="bottom">
            <div class="header-icon-item" @click="search">
              <search-icon class="header-icon" />
            </div>
          </ToolTip>

          <ToolTip v-if="isLogin" content="好友申请" placement="bottom">
            <FriendRequestNotification @update-count="handlePendingCount" />
          </ToolTip>

          <ToolTip v-if="isLogin" content="发帖" placement="bottom">
            <div class="header-icon-item" @click="goToNewPost">
              <edit class="header-icon" />
              <span class="header-label">发帖</span>
            </div>
          </ToolTip>

          <ToolTip v-if="isLogin" content="私信" placement="bottom">
            <div class="header-icon-item" @click="goToMessages">
              <message-emoji class="header-icon" />
              <span class="header-label">消息</span>
              <span v-if="unreadMessageCount > 0" class="unread-badge">{{ unreadMessageCount }}</span>
              <span v-else-if="hasChannelUnread" class="unread-dot"></span>
            </div>
          </ToolTip>

          <DropdownMenu v-if="isLogin" ref="userMenu" :items="headerMenuItems">
            <template #trigger>
              <div class="avatar-container">
                <BaseUserAvatar
                  class="avatar-img"
                  :user-id="authState.userId"
                  :src="authState.avatar"
                  :disable-link="true"
                  :width="32"
                />
                <down class="dropdown-arrow" />
              </div>
            </template>
          </DropdownMenu>

          <div v-if="!isLogin" class="auth-btns">
            <div class="header-content-item-main" @click="goToLogin">登录</div>
            <div class="header-content-item-secondary" @click="goToSignup">注册</div>
          </div>
        </div>
      </ClientOnly>
    </div>
  </header>
</template>

<script setup>
import { ClientOnly } from '#components'
import { computed, nextTick, ref, watch, onMounted } from 'vue'
import FriendRequestNotification from '~/components/FriendRequestNotification.vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import ToolTip from '~/components/ToolTip.vue'
import SearchDropdown from '~/components/SearchDropdown.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import { authState, clearToken } from '~/utils/auth'
import { useUnreadCount } from '~/composables/useUnreadCount'
import { useChannelsUnreadCount } from '~/composables/useChannelsUnreadCount'
import { useIsMobile } from '~/utils/screen'


const config = useRuntimeConfig()
const WEBSITE_BASE_URL = config.public.websiteBaseUrl

const props = defineProps({
  showMenuBtn: { type: Boolean, default: true },
})

const pendingRequestsCount = ref(0)
const handlePendingCount = (count) => { pendingRequestsCount.value = count }

const isLogin = computed(() => authState.loggedIn)
const isMobile = useIsMobile()
const { count: unreadMessageCount, fetchUnreadCount } = useUnreadCount()
const { hasUnread: hasChannelUnread, fetchChannelUnread } = useChannelsUnreadCount()
const showSearch = ref(false)
const searchDropdown = ref(null)
const userMenu = ref(null)
const menuBtn = ref(null)

const search = () => {
  showSearch.value = true
  nextTick(() => { searchDropdown.value.toggle() })
}
const closeSearch = () => {
  nextTick(() => { showSearch.value = false })
}

const goToLogin = () => navigateTo('/login', { replace: true })
const goToSettings = () => navigateTo('/settings', { replace: true })
const goToProfile = async () => {
  let id = authState.username || authState.id
  if (id) navigateTo(`/users/${id}`, { replace: true })
}
const goToSignup = () => navigateTo('/signup', { replace: true })
const goToLogout = () => {
  clearToken()
  navigateTo('/login', { replace: true })
}
const goToNewPost = () => navigateTo('/new-post', { replace: false })
const goToAdminChannels = () => navigateTo('/admin/channels')
const goToMessages = () => navigateTo('/message-box')
const refrechData = async () => {
  window.dispatchEvent(new Event('refresh-home'))
}

const headerMenuItems = computed(() => {
  const items = [
    { text: '设置', onClick: goToSettings },
    { text: '个人主页', onClick: goToProfile },
  ]
  if (authState.role === 'ADMIN') {
    items.push({ text: '频道管理', onClick: goToAdminChannels })
  }
  items.push({ text: '退出', onClick: goToLogout })
  return items
})


onMounted(async () => {
  const updateUnread = async () => {
    if (authState.loggedIn) {
      fetchUnreadCount()
      fetchChannelUnread()
    } else {
      fetchChannelUnread()
    }
  }
  await updateUnread()
})
</script>


<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--header-height);
  background: linear-gradient(135deg, 
    rgba(240, 215, 195, 0.90) 0%, 
    rgba(235, 210, 190, 0.78) 20%,
    rgba(240, 228, 218, 0.58) 50%, 
    rgba(248, 244, 240, 0.30) 80%,
    rgba(252, 250, 248, 0.15) 100%);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0 24px;
}

.header-content-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* LOGO */
.logo-container {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.logo-text {
  color: #463a36;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: -0.3px;
  transform: translateY(0);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.logo-container:hover .logo-text {
  transform: translateY(-3px);
  text-shadow: 0 4px 12px rgba(70, 58, 54, 0.1);
}

/* 菜单按钮 */
.menu-btn-wrapper { position: relative; }

.menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px; height: 38px;
  border: none; border-radius: 10px;
  background: transparent;
  cursor: pointer;
  color: #463a36;
  opacity: 0.6;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-btn:hover {
  transform: translateY(-3px);
  opacity: 1;
}

.micon { font-size: 20px; }

.menu-unread-dot {
  position: absolute;
  top: 4px; right: 4px;
  width: 7px; height: 7px;
  border-radius: 50%;
  background-color: #e8a0a0;
}

/* 右侧 */
.header-content-right {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
}

/* 图标按钮 */
.header-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
  color: #463a36;
  opacity: 0.6;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-icon-item:hover {
  opacity: 1;
  transform: translateY(-3px);
}

.header-icon { font-size: 20px; line-height: 1; }
.header-label { font-size: 11px; line-height: 1; white-space: nowrap; }

/* 未读 */
.unread-badge {
  position: absolute;
  top: 2px; right: 2px;
  background-color: #e8a0a0;
  color: white;
  border-radius: 10px;
  padding: 1px 5px;
  font-size: 10px; font-weight: 700;
  min-width: 16px; text-align: center;
}

.unread-dot {
  position: absolute;
  top: 6px; right: 6px;
  width: 7px; height: 7px;
  border-radius: 50%;
  background-color: #e8a0a0;
}

/* 头像 */
.avatar-container {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 4px;
  border-radius: 22px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.avatar-container:hover { transform: translateY(-3px); }

.avatar-img {
  width: 32px; height: 32px;
  border-radius: 50%; object-fit: cover;
}

.avatar-container:hover .avatar-img {
  box-shadow: 0 4px 16px rgba(70, 58, 54, 0.15);
}

.dropdown-arrow { font-size: 12px; color: #463a36; opacity: 0.5; }

/* 登录按钮 */
.auth-btns { display: flex; align-items: center; gap: 10px; }

.header-content-item-main {
  background: rgba(210, 195, 190, 0.35);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #463a36;
  padding: 9px 20px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(255,245,240,0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-content-item-main:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(70, 58, 54, 0.08);
  background: rgba(200, 185, 180, 0.45);
}

.header-content-item-secondary {
  color: #463a36;
  opacity: 0.6;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  padding: 9px 16px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-content-item-secondary:hover {
  transform: translateY(-3px);
  opacity: 1;
}
</style>