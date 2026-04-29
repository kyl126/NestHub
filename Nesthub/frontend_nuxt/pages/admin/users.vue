<template>
  <div class="admin-users">
    <div class="page-header">
      <h1>用户管理</h1>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input
        v-model="searchKeyword"
        @input="onSearch"
        placeholder="搜索用户名或邮箱..."
        class="search-input"
      />
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else class="users-table">
      <div class="table-header">
        <span class="th-id">ID</span>
        <span class="th-name">用户名</span>
        <span class="th-email">邮箱</span>
        <span class="th-role">角色</span>
        <span class="th-status">状态</span>
        <span class="th-time">注册时间</span>
        <span class="th-actions">操作</span>
      </div>

      <div v-if="filteredUsers.length === 0" class="empty-row">无匹配用户</div>

      <div v-for="user in filteredUsers" :key="user.id" class="table-row" :class="{ banned: user.banned }">
        <span class="td-id">{{ user.id }}</span>
        <span class="td-name">{{ user.username }}</span>
        <span class="td-email">{{ user.email }}</span>
        <span class="td-role">
          <span class="role-badge" :class="user.role">{{ user.role }}</span>
        </span>
        <span class="td-status">
          <span v-if="user.banned" class="status-badge danger">已封禁</span>
          <span v-else-if="user.mutedUntil && new Date(user.mutedUntil) > new Date()" class="status-badge warning">
            禁言至 {{ formatTime(user.mutedUntil) }}
          </span>
          <span v-else class="status-badge success">正常</span>
        </span>
        <span class="td-time">{{ formatTime(user.createdAt) }}</span>
        <span class="td-actions">
          <button v-if="!user.banned" class="action-btn danger" @click="banUser(user)">封禁</button>
          <button v-else class="action-btn" @click="unbanUser(user)">解封</button>
          <button v-if="!user.mutedUntil || new Date(user.mutedUntil) <= new Date()" class="action-btn warning" @click="openMuteModal(user)">禁言</button>
          <button v-else class="action-btn" @click="unmuteUser(user)">解除禁言</button>
        </span>
      </div>
    </div>

    <!-- 禁言弹窗 -->
    <div v-if="showMuteModal" class="modal-overlay" @click.self="showMuteModal = false">
      <div class="modal">
        <div class="modal-header">
          <h2>禁言用户 - {{ muteTarget?.username }}</h2>
          <button class="close-btn" @click="showMuteModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>禁言时长</label>
            <select v-model="muteDuration" class="form-select">
              <option :value="1">1 天</option>
              <option :value="3">3 天</option>
              <option :value="7">7 天</option>
              <option :value="14">14 天</option>
              <option :value="30">30 天</option>
              <option :value="0">永久禁言</option>
            </select>
          </div>
          <div class="form-group" v-if="muteDuration > 0">
            <label>或自定义天数</label>
            <input v-model.number="muteDays" type="number" min="1" max="365" placeholder="自定义天数">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showMuteModal = false">取消</button>
          <button class="btn-primary" @click="muteUser">确认禁言</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getToken } from '~/utils/auth'

definePageMeta({ middleware: ['auth', 'admin'] })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const users = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const showMuteModal = ref(false)
const muteTarget = ref(null)
const muteDuration = ref(7)
const muteDays = ref(7)

let debounceTimer = null

const filteredUsers = computed(() => {
  if (!searchKeyword.value.trim()) return users.value
  const kw = searchKeyword.value.toLowerCase()
  return users.value.filter(u =>
    u.username?.toLowerCase().includes(kw) ||
    u.email?.toLowerCase().includes(kw) ||
    String(u.id).includes(kw)
  )
})

const onSearch = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    // 本地过滤，无需额外请求
  }, 200)
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const token = getToken()
    const data = await $fetch(`${API_BASE_URL}/api/admin/users`, { headers: { Authorization: `Bearer ${token}` } })
    users.value = Array.isArray(data) ? data : []
  } catch (e) { console.error('获取用户列表失败', e) } finally { loading.value = false }
}

const banUser = async (user) => {
  if (!confirm(`确定封禁用户 ${user.username} 吗？`)) return
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/users/${user.id}/ban`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
    user.banned = true
  } catch (e) { console.error('封禁失败', e); alert('封禁失败') }
}

const unbanUser = async (user) => {
  if (!confirm(`确定解封用户 ${user.username} 吗？`)) return
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/users/${user.id}/unban`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
    user.banned = false
  } catch (e) { console.error('解封失败', e); alert('解封失败') }
}

const openMuteModal = (user) => {
  muteTarget.value = user
  muteDuration.value = 7
  muteDays.value = 7
  showMuteModal.value = true
}

const muteUser = async () => {
  if (!muteTarget.value) return
  const days = muteDuration.value === 0 ? 36500 : (muteDays.value || muteDuration.value)
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/users/${muteTarget.value.id}/mute`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: { days }
    })
    muteTarget.value.mutedUntil = new Date(Date.now() + days * 86400000).toISOString()
    showMuteModal.value = false
    muteTarget.value = null
  } catch (e) { console.error('禁言失败', e); alert('禁言失败') }
}

const unmuteUser = async (user) => {
  if (!confirm(`确定解除 ${user.username} 的禁言吗？`)) return
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/users/${user.id}/unmute`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
    user.mutedUntil = null
  } catch (e) { console.error('解除禁言失败', e); alert('解除禁言失败') }
}

const formatTime = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}

onMounted(() => { fetchUsers() })
</script>

<style scoped>
.admin-users {
  max-width: var(--page-max-width, 1200px);
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

/* 搜索栏 */
.search-bar {
  margin-bottom: 20px;
}

.search-input {
  width: 100%;
  max-width: 400px;
  padding: 10px 16px;
  border: 1px solid var(--normal-border-color, #ddd);
  border-radius: 10px;
  font-size: 14px;
  background: var(--background-color);
  color: var(--text-color);
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.08);
}

.search-input::placeholder { color: var(--text-tertiary, #999); }

/* 加载态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px;
  color: var(--text-secondary, #666);
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--normal-border-color, #ddd);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* 表格 */
.users-table {
  border: 1px solid var(--normal-border-color, #e5e5e5);
  border-radius: 12px;
  overflow: hidden;
}

.table-header {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  background: var(--bg-secondary, #f9f9f9);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary, #999);
  text-transform: uppercase;
  letter-spacing: 0.4px;
  border-bottom: 2px solid var(--normal-border-color, #e5e5e5);
}

.table-row {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--normal-border-color, #eee);
  transition: background-color 0.15s;
  font-size: 13px;
}

.table-row:last-child { border-bottom: none; }
.table-row:hover { background-color: var(--hover-bg, rgba(0, 0, 0, 0.015)); }
.table-row.banned { opacity: 0.5; background: #fff5f5; }

.empty-row {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-tertiary, #999);
  font-size: 14px;
}

/* 列宽 */
.th-id, .td-id { width: 60px; }
.th-name, .td-name { width: 130px; font-weight: 500; }
.th-email, .td-email { flex: 1; min-width: 0; color: var(--text-secondary, #888); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.th-role, .td-role { width: 80px; }
.th-status, .td-status { width: 140px; font-size: 12px; }
.th-time, .td-time { width: 120px; color: var(--text-secondary, #888); font-size: 12px; }
.th-actions, .td-actions { width: 180px; display: flex; gap: 6px; justify-content: flex-end; }

/* 角色徽章 */
.role-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  background: #e0e0e0;
}

.role-badge.ADMIN { background: #fce4ec; color: #c62828; }
.role-badge.USER { background: #e3f2fd; color: #1565c0; }
.role-badge.MODERATOR { background: #fff3e0; color: #e65100; }

/* 状态徽章 */
.status-badge {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.status-badge.success { background: #e8f5e9; color: #2e7d32; }
.status-badge.warning { background: #fff3e0; color: #e65100; }
.status-badge.danger { background: #ffebee; color: #c62828; }

/* 操作按钮 */
.action-btn {
  padding: 5px 12px;
  border: 1px solid var(--normal-border-color, #ddd);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  background: white;
  transition: all 0.15s;
  white-space: nowrap;
}

.action-btn:hover { background: var(--hover-bg, rgba(0, 0, 0, 0.05)); }

.action-btn.danger:hover { background: #ffebee; border-color: #ef5350; color: #c62828; }
.action-btn.warning:hover { background: #fff3e0; border-color: #ff9800; color: #e65100; }

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.modal {
  background: var(--background-color);
  border-radius: 16px;
  width: 420px;
  max-width: calc(100vw - 40px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--normal-border-color, #eee);
}

.modal-header h2 { margin: 0; font-size: 17px; font-weight: 600; }

.close-btn {
  width: 32px; height: 32px;
  border: none; border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  color: var(--text-secondary, #999);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-btn:hover { background: var(--hover-bg, rgba(0, 0, 0, 0.06)); color: var(--text-color); }

.modal-body { padding: 24px; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--normal-border-color, #eee);
}

/* 表单 */
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 500; font-size: 13px; }
.form-group input,
.form-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--normal-border-color, #ddd);
  border-radius: 8px;
  font-size: 14px;
  background: var(--background-color);
  color: var(--text-color);
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.08);
}

.btn-primary {
  padding: 10px 20px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-primary:hover { background: var(--primary-color-hover); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-secondary {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--normal-border-color, #ddd);
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-color);
  font-size: 13px;
  transition: all 0.2s;
}

.btn-secondary:hover { background: var(--hover-bg, rgba(0,0,0,0.04)); }
</style>