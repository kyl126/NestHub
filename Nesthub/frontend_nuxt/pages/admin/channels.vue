<template>
  <div class="admin-channels">
    <div class="page-header">
      <h1>频道管理</h1>
      <button class="btn-primary" @click="openCreateModal">
        <span class="btn-icon">+</span> 新建频道
      </button>
    </div>
    
    <div class="channels-list">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-else class="channel-grid">
        <div v-for="channel in channels" :key="channel.id" class="channel-card">
          <div class="card-top">
            <div class="channel-info">
              <span class="channel-icon">{{ channel.icon || '📁' }}</span>
              <div class="channel-text">
                <span class="channel-name">{{ channel.name }}</span>
                <span class="channel-desc">{{ channel.description || '暂无描述' }}</span>
              </div>
            </div>
            <div class="channel-actions">
              <button class="action-btn" @click="openEditModal(channel)" title="编辑">✏️</button>
              <button class="action-btn" @click="manageSubChannels(channel)" title="子频道">📋</button>
              <button class="action-btn" @click="openPostListModal(channel)" title="帖子">📄</button>
              <button class="action-btn action-danger" @click="deleteChannel(channel.id)" title="删除">🗑️</button>
            </div>
          </div>
          
          <div class="card-bottom">
            <span class="meta-item">排序: {{ channel.sortOrder || 0 }}</span>
            <button
              class="status-toggle"
              :class="{ enabled: channel.enabled }"
              @click="toggleStatus(channel)"
            >
              {{ channel.enabled ? '已启用' : '已禁用' }}
            </button>
          </div>
          
          <div v-if="channel.subChannels?.length" class="sub-preview">
            <span v-for="sub in channel.subChannels.slice(0, 5)" :key="sub.id" class="sub-tag">
              {{ sub.icon || '💬' }} {{ sub.name }}
            </span>
            <span v-if="channel.subChannels.length > 5" class="sub-more">
              +{{ channel.subChannels.length - 5 }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editingChannel ? '编辑频道' : '新建频道' }}</h2>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>频道名称 <span class="required">*</span></label>
            <input v-model="form.name" placeholder="输入频道名称" maxlength="20">
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="form.description" placeholder="输入描述（可选）" maxlength="100">
          </div>
          <div class="form-group">
            <label>图标</label>
            <div class="icon-picker-trigger" @click="showIconPicker = !showIconPicker">
              <span class="icon-preview">{{ form.icon || '📁' }}</span>
              <span class="icon-label">{{ form.icon || '选择图标' }}</span>
              <span class="picker-arrow">▾</span>
            </div>
            <div v-if="showIconPicker" class="icon-picker-dropdown">
              <div class="icon-grid">
                <span
                  v-for="icon in iconList" :key="icon"
                  class="icon-option"
                  :class="{ active: form.icon === icon }"
                  @click="selectIcon(icon)"
                >{{ icon }}</span>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="form.sortOrder" type="number" placeholder="数字越小越靠前">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeModal">取消</button>
          <button class="btn-primary" @click="saveChannel" :disabled="!form.name">保存</button>
        </div>
      </div>
    </div>

    <!-- 子频道管理弹窗 -->
    <div v-if="showSubModal" class="modal-overlay" @click.self="closeSubModal">
      <div class="modal modal-lg">
        <div class="modal-header">
          <h2>子频道管理 - {{ currentChannel?.name }}</h2>
          <button class="close-btn" @click="closeSubModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="sub-toolbar">
            <button class="btn-primary btn-sm" @click="openCreateSubModal">+ 新建子频道</button>
            <button class="btn-secondary btn-sm" @click="saveSubChannelOrder" :disabled="!subOrderChanged">保存排序</button>
          </div>
          
          <div v-if="subLoading" class="loading-state">
            <div class="spinner"></div>
            <span>加载中...</span>
          </div>
          
          <div v-else class="sub-table">
            <div class="sub-table-header">
              <span class="th-drag"></span>
              <span class="th-icon"></span>
              <span class="th-name">名称</span>
              <span class="th-type">类型</span>
              <span class="th-permission">权限</span>
              <span class="th-sort">排序</span>
              <span class="th-status">状态</span>
              <span class="th-actions">操作</span>
            </div>
            <div
              v-for="(sub, index) in subChannels" :key="sub.id"
              class="sub-table-row"
              :class="{ disabled: !sub.enabled }"
            >
              <span class="td-drag" @mousedown="startDrag(index)">⋮⋮</span>
              <span class="td-icon">{{ sub.icon || '💬' }}</span>
              <span class="td-name">{{ sub.name }}</span>
              <span class="td-type">
                <span class="type-badge" :class="sub.type">{{ getTypeName(sub.type) }}</span>
              </span>
              <span class="td-permission">{{ getPermissionName(sub.postPermission) }}</span>
              <span class="td-sort">{{ sub.sortOrder }}</span>
              <span class="td-status">
                <button class="status-toggle sm" :class="{ enabled: sub.enabled }" @click="toggleSubStatus(sub)">
                  {{ sub.enabled ? '启用' : '禁用' }}
                </button>
              </span>
              <span class="td-actions">
                <button class="action-btn sm" @click="openEditSubModal(sub)" :disabled="sub.type !== 'CUSTOM'">✏️</button>
                <button class="action-btn sm danger" @click="deleteSubChannel(sub)" :disabled="sub.type !== 'CUSTOM'">🗑️</button>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑子频道弹窗 -->
    <div v-if="showSubFormModal" class="modal-overlay" @click.self="closeSubFormModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editingSubChannel ? '编辑子频道' : '新建子频道' }}</h2>
          <button class="close-btn" @click="closeSubFormModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>名称 <span class="required">*</span></label>
            <input v-model="subForm.name" placeholder="输入名称" maxlength="20">
          </div>
          <div class="form-group">
            <label>图标</label>
            <div class="icon-picker-trigger" @click="showSubIconPicker = !showSubIconPicker">
              <span class="icon-preview">{{ subForm.icon || '💬' }}</span>
              <span class="icon-label">{{ subForm.icon || '选择图标' }}</span>
              <span class="picker-arrow">▾</span>
            </div>
            <div v-if="showSubIconPicker" class="icon-picker-dropdown">
              <div class="icon-grid">
                <span
                  v-for="icon in subIconList" :key="icon"
                  class="icon-option"
                  :class="{ active: subForm.icon === icon }"
                  @click="selectSubIcon(icon)"
                >{{ icon }}</span>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>发帖权限</label>
            <select v-model="subForm.postPermission" class="form-select">
              <option value="ALL">所有人可发帖</option>
              <option value="ADMIN_ONLY">仅管理员可发帖</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeSubFormModal">取消</button>
          <button class="btn-primary" @click="saveSubChannel" :disabled="!subForm.name">保存</button>
        </div>
      </div>
    </div>

    <!-- 帖子列表弹窗 -->
    <div v-if="showPostListModal" class="modal-overlay" @click.self="showPostListModal = false">
      <div class="modal modal-lg">
        <div class="modal-header">
          <h2>{{ currentChannel?.name }} - 帖子管理</h2>
          <button class="close-btn" @click="showPostListModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="postListLoading" class="loading-state">
            <div class="spinner"></div>
            <span>加载中...</span>
          </div>
          <div v-else>
            <div v-if="selectedPosts.length > 0" class="batch-bar">
              <span>已选 {{ selectedPosts.length }} 篇</span>
              <select v-model="moveTargetCategoryId" class="form-select inline">
                <option value="">移动到...</option>
                <option v-for="cat in allCategories" :key="cat.id" :value="cat.id" :disabled="cat.id === currentChannel?.id">
                  {{ cat.name }}
                </option>
              </select>
              <button class="btn-primary btn-sm" @click="batchMovePosts" :disabled="!moveTargetCategoryId">移动</button>
            </div>
            <div v-for="post in postList" :key="post.id" class="post-check-item">
              <label class="checkbox-label">
                <input type="checkbox" v-model="selectedPosts" :value="post.id">
                <span class="post-title">{{ post.title }}</span>
              </label>
            </div>
            <div v-if="postList.length === 0" class="empty-state">该频道暂无帖子</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// --- 保持原有 script 完全不变 ---
import { ref, onMounted } from 'vue'
import { getToken } from '~/utils/auth'

definePageMeta({ middleware: ['auth', 'admin'] })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const channels = ref([])
const subChannels = ref([])
const loading = ref(false)
const subLoading = ref(false)
const showModal = ref(false)
const showSubModal = ref(false)
const showSubFormModal = ref(false)
const showIconPicker = ref(false)
const showSubIconPicker = ref(false)
const editingChannel = ref(null)
const editingSubChannel = ref(null)
const currentChannel = ref(null)
const subOrderChanged = ref(false)

const form = ref({ name: '', description: '', icon: '📁', sortOrder: 0, enabled: true })
const subForm = ref({ name: '', icon: '💬', type: 'CUSTOM', postPermission: 'ALL' })

const iconList = [
  '📁', '📢', '💬', '🎮', '🎵', '🎬', '📚', '💻',
  '🖥️', '🎨', '🎭', '🎪', '🎤', '🎧', '🎼', '🎹',
  '🎸', '⚽', '🏀', '🏈', '⚾', '🎾', '🏐', '🎱',
  '🔥', '⭐', '✨', '💡', '🎯', '🏆', '🎖️', '🏅',
  '📰', '📌', '📎', '🔖', '❤️', '💚', '💙', '💜'
]

const subIconList = [
  '💬', '📝', '📢', '🔥', '⭐', '🆕', '📌',
  '💡', '❓', '💭', '🗣️', '📣', '🔔', '💎'
]

let draggedIndex = -1
const startDrag = (index) => { draggedIndex = index; document.addEventListener('mouseup', onDragEnd) }
const onDragEnd = () => { document.removeEventListener('mouseup', onDragEnd); subOrderChanged.value = true }

const fetchChannels = async () => {
  loading.value = true
  try {
    const token = getToken()
    const data = await $fetch(`${API_BASE_URL}/api/admin/channels`, { headers: { Authorization: `Bearer ${token}` } })
    channels.value = data.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) { console.error('获取频道失败', e) } finally { loading.value = false }
}

const openCreateModal = () => {
  editingChannel.value = null
  form.value = { name: '', description: '', icon: '📁', sortOrder: 0, enabled: true }
  showModal.value = true
}

const openEditModal = (channel) => {
  editingChannel.value = channel
  form.value = { name: channel.name, description: channel.description || '', icon: channel.icon || '📁', sortOrder: channel.sortOrder || 0, enabled: channel.enabled }
  showModal.value = true
}

const closeModal = () => { showModal.value = false; showIconPicker.value = false }
const selectIcon = (icon) => { form.value.icon = icon; showIconPicker.value = false }

const saveChannel = async () => {
  const token = getToken()
  try {
    if (editingChannel.value) {
      await $fetch(`${API_BASE_URL}/api/admin/channels/${editingChannel.value.id}`, { method: 'PUT', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: form.value })
    } else {
      await $fetch(`${API_BASE_URL}/api/admin/channels`, { method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: form.value })
    }
    closeModal(); fetchChannels()
    window.dispatchEvent(new Event('categories-updated'))
  } catch (e) { console.error('保存失败', e); alert('保存失败') }
}

const toggleStatus = async (channel) => {
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/channels/${channel.id}`, { method: 'PUT', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: { ...channel, enabled: !channel.enabled } })
    fetchChannels()
  } catch (e) { console.error('切换状态失败', e) }
}

const manageSubChannels = async (channel) => {
  currentChannel.value = channel; showSubModal.value = true; await fetchSubChannels(channel.id)
}

const fetchSubChannels = async (channelId) => {
  subLoading.value = true
  try {
    const token = getToken()
    const data = await $fetch(`${API_BASE_URL}/api/admin/channels/${channelId}/sub-channels`, { headers: { Authorization: `Bearer ${token}` } })
    subChannels.value = data.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) { console.error('获取子频道失败', e) } finally { subLoading.value = false }
}

const closeSubModal = () => { showSubModal.value = false; currentChannel.value = null; subChannels.value = []; subOrderChanged.value = false }
const openCreateSubModal = () => { editingSubChannel.value = null; subForm.value = { name: '', icon: '💬', type: 'CUSTOM', postPermission: 'ALL' }; showSubFormModal.value = true }
const openEditSubModal = (sub) => { editingSubChannel.value = sub; subForm.value = { name: sub.name, icon: sub.icon || '💬', type: sub.type, postPermission: sub.postPermission }; showSubFormModal.value = true }
const closeSubFormModal = () => { showSubFormModal.value = false; showSubIconPicker.value = false }
const selectSubIcon = (icon) => { subForm.value.icon = icon; showSubIconPicker.value = false }

const saveSubChannel = async () => {
  const token = getToken()
  try {
    if (editingSubChannel.value) {
      await $fetch(`${API_BASE_URL}/api/admin/sub-channels/${editingSubChannel.value.id}`, { method: 'PUT', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: subForm.value })
    } else {
      await $fetch(`${API_BASE_URL}/api/admin/channels/${currentChannel.value.id}/sub-channels`, { method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: subForm.value })
    }
    closeSubFormModal(); fetchSubChannels(currentChannel.value.id); fetchChannels()
    window.dispatchEvent(new Event('categories-updated'))
  } catch (e) { console.error('保存失败', e); alert('保存失败') }
}

const toggleSubStatus = async (sub) => {
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/sub-channels/${sub.id}`, { method: 'PUT', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: { ...sub, enabled: !sub.enabled } })
    fetchSubChannels(currentChannel.value.id)
  } catch (e) { console.error('切换状态失败', e) }
}

const deleteSubChannel = async (sub) => {
  if (!confirm(`确定删除子频道"${sub.name}"吗？`)) return
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/sub-channels/${sub.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
    fetchSubChannels(currentChannel.value.id); fetchChannels()
  } catch (e) { console.error('删除失败', e); alert('删除失败') }
}

const deleteChannel = async (channelId) => {
  const token = getToken()
  let subCount = 0, postCount = 0
  try { const subs = await $fetch(`${API_BASE_URL}/api/admin/sub-channels/by-channel/${channelId}`, { headers: { Authorization: `Bearer ${token}` } }); subCount = Array.isArray(subs) ? subs.length : 0 } catch {}
  try { const res = await $fetch(`${API_BASE_URL}/api/admin/channels/${channelId}/posts-count`, { headers: { Authorization: `Bearer ${token}` } }); postCount = res?.count || 0 } catch {}
  let msg = `确定删除该频道吗？`
  if (subCount > 0 || postCount > 0) { msg += `\n\n⚠️ 将同时删除：`; if (subCount > 0) msg += `\n  · ${subCount} 个子频道`; if (postCount > 0) msg += `\n  · ${postCount} 个帖子`; msg += `\n\n此操作不可恢复！` }
  if (!confirm(msg)) return
  const cascadeDelete = (subCount > 0 || postCount > 0) ? confirm(`⚠️ 最终确认：是否同时删除所有子频道和帖子？\n\n"确定"=级联删除  "取消"=仅删除空频道`) : true
  try {
    await $fetch(`${API_BASE_URL}/api/admin/channels/${channelId}?cascade=${cascadeDelete}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
    fetchChannels(); window.dispatchEvent(new Event('categories-updated'))
  } catch (e) { console.error('删除失败', e); alert('删除失败') }
}

const saveSubChannelOrder = async () => {
  const token = getToken()
  try {
    const orderedIds = subChannels.value.map(s => s.id)
    await $fetch(`${API_BASE_URL}/api/admin/sub-channels/reorder`, { method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: orderedIds })
    subOrderChanged.value = false; alert('排序已保存')
  } catch (e) { console.error('保存排序失败', e); alert('保存排序失败') }
}

const showPostListModal = ref(false)
const postList = ref([])
const postListLoading = ref(false)
const selectedPosts = ref([])
const moveTargetCategoryId = ref('')
const allCategories = ref([])

const openPostListModal = async (channel) => {
  currentChannel.value = channel; showPostListModal.value = true; postListLoading.value = true
  selectedPosts.value = []; moveTargetCategoryId.value = ''
  const token = getToken()
  try {
    const [posts, cats] = await Promise.all([
      $fetch(`${API_BASE_URL}/api/admin/channels/${channel.id}/posts`, { headers: { Authorization: `Bearer ${token}` } }),
      $fetch(`${API_BASE_URL}/api/admin/channels`, { headers: { Authorization: `Bearer ${token}` } })
    ])
    postList.value = Array.isArray(posts) ? posts : []; allCategories.value = Array.isArray(cats) ? cats : []
  } catch (e) { console.error('加载帖子失败', e) } finally { postListLoading.value = false }
}

const batchMovePosts = async () => {
  if (!moveTargetCategoryId.value || selectedPosts.value.length === 0) return
  const token = getToken()
  try {
    await $fetch(`${API_BASE_URL}/api/admin/posts/batch-move-category`, { method: 'PUT', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: { postIds: selectedPosts.value, targetCategoryId: moveTargetCategoryId.value } })
    selectedPosts.value = []; moveTargetCategoryId.value = ''
    openPostListModal(currentChannel.value)
  } catch (e) { console.error('移动失败', e); alert('移动失败') }
}

const getTypeName = (type) => {
  const names = { CUSTOM: '自定义', LATEST: '最新', HOT: '热门', OFFICIAL: '官方' }
  return names[type] || type
}
const getPermissionName = (permission) => permission === 'ALL' ? '所有人' : '仅管理员'

onMounted(() => { fetchChannels() })
</script>

<style scoped>
.admin-channels {
  max-width: var(--page-max-width, 1100px);
  margin: 0 auto;
  padding: 24px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

/* 按钮系统 */
.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
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

.btn-icon { font-size: 18px; font-weight: 400; }

.btn-secondary {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--normal-border-color, #e0e0e0);
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-color);
  font-size: 13px;
  transition: all 0.2s;
}

.btn-secondary:hover { background: var(--hover-bg, rgba(0, 0, 0, 0.04)); }
.btn-secondary:disabled { opacity: 0.4; cursor: not-allowed; }

.btn-sm { padding: 6px 14px; font-size: 12px; }

/* 卡片网格 */
.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.channel-card {
  background: var(--bg-secondary, #fafafa);
  border: 1px solid var(--normal-border-color, #e5e5e5);
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.2s;
}

.channel-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.channel-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.channel-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.channel-name {
  font-size: 16px;
  font-weight: 600;
}

.channel-desc {
  font-size: 12px;
  color: var(--text-tertiary, #999);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.channel-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.action-btn:hover { background: var(--hover-bg, rgba(0, 0, 0, 0.06)); }
.action-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.action-btn.danger:hover,
.action-danger:hover { background: #ffebee; border-color: #ef5350; }

.action-btn.sm { width: 28px; height: 28px; font-size: 12px; }

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--normal-border-color, #eee);
}

.meta-item {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.status-toggle {
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.status-toggle.enabled {
  background: #e8f5e9;
  color: #2e7d32;
  border-color: #a5d6a7;
}

.status-toggle.sm { padding: 3px 10px; font-size: 11px; }

/* 子频道预览 */
.sub-preview {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--normal-border-color, #eee);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sub-tag {
  padding: 3px 10px;
  background: var(--bg-primary, #fff);
  border: 1px solid var(--normal-border-color, #e0e0e0);
  border-radius: 14px;
  font-size: 12px;
}

.sub-more {
  padding: 3px 10px;
  color: var(--text-tertiary, #999);
  font-size: 12px;
}

/* 加载态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
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
  width: 500px;
  max-width: calc(100vw - 40px);
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.modal-lg { width: 800px; }

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--normal-border-color, #eee);
}

.modal-header h2 { margin: 0; font-size: 18px; font-weight: 600; }

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

.modal-body { padding: 24px; overflow-y: auto; flex: 1; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--normal-border-color, #eee);
}

/* 表单 */
.form-group { margin-bottom: 18px; }
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

.form-select.inline { width: auto; }

.required { color: #ef5350; }

/* 图标选择器 */
.icon-picker-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--normal-border-color, #ddd);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.icon-picker-trigger:hover { border-color: var(--primary-color); }
.icon-preview { font-size: 28px; }
.icon-label { color: var(--text-secondary, #999); font-size: 13px; }
.picker-arrow { margin-left: auto; color: var(--text-tertiary, #ccc); font-size: 12px; }

.icon-picker-dropdown {
  margin-top: 8px;
  padding: 12px;
  background: var(--background-color);
  border: 1px solid var(--normal-border-color, #ddd);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  max-height: 260px;
  overflow-y: auto;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
}

.icon-option {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  font-size: 22px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.icon-option:hover { background: var(--hover-bg, rgba(0, 0, 0, 0.04)); }
.icon-option.active {
  border-color: var(--primary-color);
  background: var(--primary-color-light, rgba(124, 58, 237, 0.06));
}

/* 子频道表格 */
.sub-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.sub-table { border: 1px solid var(--normal-border-color, #e0e0e0); border-radius: 10px; overflow: hidden; }

.sub-table-header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: var(--bg-secondary, #f9f9f9);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary, #999);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.sub-table-row {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-top: 1px solid var(--normal-border-color, #eee);
  font-size: 13px;
  transition: background-color 0.15s;
}

.sub-table-row:hover { background: var(--hover-bg, rgba(0, 0, 0, 0.02)); }
.sub-table-row.disabled { opacity: 0.5; }

.th-drag, .td-drag { width: 30px; text-align: center; }
.td-drag { color: var(--text-tertiary, #ccc); cursor: grab; font-size: 16px; }
.th-icon, .td-icon { width: 44px; text-align: center; font-size: 18px; }
.th-name, .td-name { flex: 1; min-width: 0; font-weight: 500; }
.th-type, .td-type { width: 80px; }
.th-permission, .td-permission { width: 90px; color: var(--text-secondary, #888); }
.th-sort, .td-sort { width: 50px; color: var(--text-tertiary, #999); }
.th-status, .td-status { width: 80px; }
.th-actions, .td-actions { width: 80px; display: flex; gap: 4px; justify-content: flex-end; }

.type-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: #e0e0e0;
}

.type-badge.CUSTOM { background: #e3f2fd; color: #1565c0; }
.type-badge.OFFICIAL { background: #fce4ec; color: #c62828; }

/* 批量操作 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-secondary, #f9f9f9);
  border-radius: 10px;
  margin-bottom: 14px;
  font-size: 13px;
}

/* 帖子选择 */
.post-check-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--normal-border-color, #eee);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
}

.checkbox-label input[type="checkbox"] {
  width: 16px; height: 16px;
  accent-color: var(--primary-color);
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-tertiary, #999);
}

/* 容器查询自适应 */
@container (max-width: 700px) {
  .channel-grid { grid-template-columns: 1fr; }
}
</style>