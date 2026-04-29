<template>
  <div class="home-page">
    <!-- 子频道导航（替代原来的父频道位置） -->
    <div class="sub-channel-bar">
      <div class="sub-channel-list">
        <div
          v-for="sub in currentSubChannels"
          :key="sub"
          class="sub-channel-item"
          :class="{ selected: selectedSubChannel === sub }"
          @click="selectSub(sub)"
        >
          <span class="sub-icon">{{ getSubIcon(sub) }}</span>
          <span class="sub-name">{{ sub }}</span>
        </div>
      </div>
    </div>

    <!-- 频道聊天入口 -->
    <div class="chat-entry-bar" @click="goToChannelChat">
      <span>聊天区</span>
      <span class="chat-arrow">→</span>
    </div>

    <!-- 当前选中频道指示 -->
    <div v-if="selectedTopic !== '全部'" class="current-channel-indicator">
      当前频道：{{ getChannelIcon(selectedTopic) }} {{ selectedTopic }}
    </div>

    <!-- 帖子列表 -->
    <div class="article-list">
      <div class="list-header">
        <div class="col-main">话题</div>
        <div class="col-participants">参与</div>
        <div class="col-replies">回复</div>
        <div class="col-views">浏览</div>
        <div class="col-activity">活动</div>
      </div>

      <div v-if="pendingFirst" class="loading-area">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton-col skeleton-main">
            <div class="skeleton-line w-60"></div>
            <div class="skeleton-line w-90"></div>
          </div>
          <div class="skeleton-col skeleton-avatars">
            <div class="skeleton-circle" v-for="j in 3" :key="j"></div>
          </div>
          <div class="skeleton-col skeleton-stat"></div>
          <div class="skeleton-col skeleton-stat"></div>
          <div class="skeleton-col skeleton-stat"></div>
        </div>
      </div>

      <div v-else-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <p class="empty-text">暂时没有帖子 :(</p>
        <p class="empty-sub">点击发帖发送第一篇相关帖子吧!</p>
      </div>

      <div v-if="!pendingFirst">
        <div
          v-for="article in articles"
          :key="article.id"
          class="article-row"
          @click="navigateTo(`/posts/${article.id}`)"
        >
          <div class="col-main">
            <NuxtLink class="article-title" :to="`/posts/${article.id}`" @click.stop>
              <span class="badges">
                <span v-if="article.pinned" class="badge badge-pin">📌</span>
                <span v-if="article.type === 'LOTTERY'" class="badge badge-lottery">🎁</span>
                <span v-else-if="article.type === 'POLL'" class="badge badge-poll">📊</span>
                <span v-else-if="article.type === 'PROPOSAL'" class="badge badge-proposal">🙌</span>
                <span v-if="!article.rssExcluded" class="badge badge-featured">⭐</span>
              </span>
              <span class="title-text">{{ article.title }}</span>
              <span v-if="article.isRestricted" class="lock-icon">🔒</span>
            </NuxtLink>
            <NuxtLink class="article-desc" :to="`/posts/${article.id}`" @click.stop>
              <span v-html="stripMarkdownWithTiebaMoji(article.description, 500)"></span>
            </NuxtLink>
            <div class="article-tags">
              <ArticleCategory :category="article.category" />
              <ArticleTags :tags="article.tags" />
            </div>
          </div>
          <div class="col-participants">
            <div class="avatar-group">
              <BaseUserAvatar
                v-for="(member, idx) in article.members.slice(0, 3)"
                :key="idx"
                class="avatar-thumb"
                :src="member.avatar"
                :user-id="member.id"
                :disable-link="true"
                :width="26"
              />
              <span v-if="article.members.length > 3" class="avatar-more">+{{ article.members.length - 3 }}</span>
            </div>
          </div>
          <div class="col-replies">{{ article.comments }}</div>
          <div class="col-views">{{ article.views }}</div>
          <div class="col-activity">{{ article.time }}</div>
        </div>
      </div>

      <InfiniteLoadMore
        v-if="articles.length > 0"
        :key="ioKey"
        :on-load="fetchNextPage"
        :pause="pendingFirst"
        root-margin="200px 0px"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ArticleCategory from '~/components/ArticleCategory.vue'
import ArticleTags from '~/components/ArticleTags.vue'
import InfiniteLoadMore from '~/components/InfiniteLoadMore.vue'
import { getToken } from '~/utils/auth'
import { stripMarkdown } from '~/utils/markdown'
import { useIsMobile } from '~/utils/screen'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import { useCurrentChannel } from '~/composables/useCurrentChannel'
import TimeManager from '~/utils/time'
import { selectedCategoryGlobal, selectedTagsGlobal } from '~/composables/postFilter'
import { stripMarkdownWithTiebaMoji } from '~/utils/markdown'

useHead({
  title: 'OpenIsle - 全面开源的自由社区',
  meta: [{ name: 'description', content: '交流社区' }],
})

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const selectedCategory = ref('')
const selectedTags = ref([])
const route = useRoute()
const currentChannel = useCurrentChannel()

const clearFilters = () => {
  selectedCategory.value = ''
  selectedTags.value = []
  selectedCategoryGlobal.value = ''
  selectedTagsGlobal.value = []
}

const categoriesData = ref([])
const selectedSubChannel = ref('最新')
const selectedTopic = ref('全部')
const articles = ref([])
const page = ref(0)
const pageSize = 10
const channelSubChannelsMap = ref({})

const allSubChannels = ['最新', '火', '官方', '精华']

// 当前选中频道的子频道列表
const currentSubChannels = computed(() => {
  if (selectedTopic.value === '全部') return allSubChannels
  return allSubChannels.concat(channelSubChannelsMap.value[selectedTopic.value] || [])
})

const goToChannelChat = () => {
  const channelId = currentChannel.value.id
  navigateTo(`/chat/${channelId}`)
}

const selectSub = (sub) => {
  selectedSubChannel.value = sub
}

// 从 URL query 恢复状态
const normalizeCategoryFromQuery = (category) => {
  if (category == null || category === '') return ''
  const raw = Array.isArray(category) ? category[0] : category
  const decoded = decodeURIComponent(raw)
  return isNaN(decoded) ? decoded : Number(decoded)
}

const normalizeTagsFromQuery = (tags) => {
  if (tags == null || tags === '') return []
  const raw = Array.isArray(tags) ? tags.join(',') : tags
  return raw.split(',').filter((v) => v).map((v) => decodeURIComponent(v)).map((v) => (isNaN(v) ? v : Number(v)))
}

const arraysShallowEqual = (a = [], b = []) => {
  if (a.length !== b.length) return false
  return a.every((v, idx) => String(v) === String(b[idx]))
}

// 加载频道和子频道数据
const loadChannels = async () => {
  try {
    const res = await $fetch(`${API_BASE_URL}/api/categories`)
    const data = Array.isArray(res) ? res : []
    // 为每个频道绑定弹窗聊天室ID（失败不影响主流程）
    const token = getToken()
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    for (const channel of data) {
      try {
        const popupRoom = await $fetch(`${API_BASE_URL}/api/chat/popup-room/${channel.id}`, { headers })
        channel.popupRoomId = popupRoom.id
      } catch (e) {
        // 弹窗接口失败时，尝试用普通聊天室作为 fallback
        try {
          const chatRoom = await $fetch(`${API_BASE_URL}/api/chat/rooms/${channel.id}`, { headers })
          channel.popupRoomId = chatRoom.id
        } catch (e2) {
          channel.popupRoomId = null
        }
      }
    }
    categoriesData.value = data
    await loadAllSubChannels()
  } catch (e) { console.error('Failed to load channels', e) }
}

const loadAllSubChannels = async () => {
  const token = getToken()
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  for (const c of categoriesData.value) {
    try {
      const res = await $fetch(`${API_BASE_URL}/api/sub-channels/by-channel/${c.id}`, { headers })
      channelSubChannelsMap.value[c.name] = res.map(s => s.name)
    } catch (e) {
      channelSubChannelsMap.value[c.name] = []
    }
  }
}

const getChannelIcon = (name) => {
  if (name === '全部') return '📋'
  const category = categoriesData.value.find(c => c.name === name)
  return category?.icon || '📁'
}

const getSubIcon = (name) => {
  const map = { '最新': '🕒', '火': '🔥', '官方': '📢', '精华': '⭐' }
  return map[name] || '💬'
}

onMounted(async () => {
  await loadChannels()
  const { category, tags, subChannel } = route.query
  if (category) {
    selectedCategory.value = normalizeCategoryFromQuery(category)
    // 反查频道名称
    const cat = categoriesData.value.find(c => c.id === Number(selectedCategory.value) || c.id === selectedCategory.value)
    if (cat) selectedTopic.value = cat.name
  }
  if (subChannel) selectedSubChannel.value = subChannel
  selectedCategoryGlobal.value = selectedCategory.value
  selectedTagsGlobal.value = selectedTags.value
  if (articles.value.length === 0) refreshFirst()

  window.addEventListener('categories-updated', loadChannels)
  window.addEventListener('refresh-home', () => { clearFilters(); refreshFirst() })
})

onBeforeUnmount(() => {
  window.removeEventListener('categories-updated', loadChannels)
  window.removeEventListener('refresh-home', () => { clearFilters(); refreshFirst() })
})

watch(() => route.query, (query) => {
  const category = query.category
  const tags = query.tags
  const subChannel = query.subChannel
  if (category) {
    selectedCategory.value = normalizeCategoryFromQuery(category)
  } else {
    selectedCategory.value = ''
    selectedCategoryGlobal.value = ''
    selectedTopic.value = '全部'
  }
  if (subChannel) selectedSubChannel.value = subChannel
  else selectedSubChannel.value = '最新'
  if (tags) { selectedTags.value = normalizeTagsFromQuery(tags) } else { selectedTags.value = []; selectedTagsGlobal.value = [] }
})

watch([selectedCategory, selectedTags], async ([category, tags]) => {
  const routeCategory = normalizeCategoryFromQuery(route.query.category)
  const routeTags = normalizeTagsFromQuery(route.query.tags)
  const categoryChanged = String(category ?? '') !== String(routeCategory ?? '')
  const tagsChanged = !arraysShallowEqual(tags || [], routeTags)
  if (!categoryChanged && !tagsChanged) return
  const nextQuery = { ...route.query }
  if (category == null || category === '') { delete nextQuery.category } else { nextQuery.category = encodeURIComponent(String(category)) }
  if (!Array.isArray(tags) || tags.length === 0) { delete nextQuery.tags } else { nextQuery.tags = tags.map((v) => encodeURIComponent(String(v))).join(',') }
  // 保留 subChannel
  if (selectedSubChannel.value !== '最新') nextQuery.subChannel = selectedSubChannel.value
  else delete nextQuery.subChannel
  await navigateTo({ path: '/', query: nextQuery })
})

const baseQuery = computed(() => ({
  categoryId: selectedCategory.value || undefined,
  tagIds: selectedTags.value.length ? selectedTags.value : undefined,
  subChannel: selectedSubChannel.value,
}))

const listApiPath = computed(() => {
  if (selectedSubChannel.value === '官方') return '/api/posts'
  if (selectedSubChannel.value === '精华') return '/api/posts/featured'
  if (selectedSubChannel.value === '火') return '/api/posts/ranking'
  return '/api/posts'
})

const buildUrl = ({ pageNo }) => {
  const url = new URL(`${API_BASE_URL}${listApiPath.value}`)
  url.searchParams.set('page', pageNo)
  url.searchParams.set('pageSize', pageSize)
  if (baseQuery.value.categoryId) url.searchParams.set('categoryId', baseQuery.value.categoryId)
  if (baseQuery.value.tagIds) for (const t of baseQuery.value.tagIds) url.searchParams.append('tagIds', t)
  if (baseQuery.value.subChannel) url.searchParams.set('subChannel', baseQuery.value.subChannel)
  return url.toString()
}

const tokenHeader = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const asyncKey = computed(() => [
  'home:firstpage',
  selectedTopic.value,
  selectedSubChannel.value,
  String(baseQuery.value.categoryId ?? ''),
  JSON.stringify(baseQuery.value.tagIds ?? []),
])

const {
  data: firstPage,
  pending: pendingFirst,
  refresh: refreshFirst,
} = await useAsyncData(
  () => asyncKey.value.join('::'),
  async () => {
    const res = await $fetch(buildUrl({ pageNo: 0 }), { headers: tokenHeader.value })
    const data = Array.isArray(res) ? res : []
    return data.map((p) => ({
      id: p.id, title: p.title, description: p.content,
      category: p.category, tags: p.tags || [],
      members: (p.participants || []).map((m) => ({ id: m.id, avatar: m.avatar })),
      comments: p.commentCount, views: p.views,
      rssExcluded: p.rssExcluded || false,
      isRestricted: p.visibleScope === 'ONLY_ME' || p.visibleScope === 'ONLY_REGISTER',
      time: TimeManager.format(p.createdAt),
      pinned: Boolean(p.pinned ?? p.pinnedAt ?? p.pinned_at),
      type: p.type,
    }))
  },
  { default: () => [], watch: [selectedSubChannel, baseQuery] },
)

watch(firstPage, (data) => { page.value = 0; articles.value = [...(data || [])] }, { immediate: true })

const fetchNextPage = async () => {
  if (pendingFirst.value) return false
  const nextPage = page.value + 1
  const res = await $fetch(buildUrl({ pageNo: nextPage }), { headers: tokenHeader.value })
  const data = Array.isArray(res) ? res : []
  const mapped = data.map((p) => ({
    id: p.id, title: p.title, description: p.content,
    category: p.category, tags: p.tags || [],
    members: (p.participants || []).map((m) => ({ id: m.id, avatar: m.avatar })),
    comments: p.commentCount, views: p.views,
    isRestricted: p.visibleScope === 'ONLY_ME' || p.visibleScope === 'ONLY_REGISTER',
    rssExcluded: p.rssExcluded || false,
    time: TimeManager.format(p.createdAt),
    pinned: Boolean(p.pinned ?? p.pinnedAt ?? p.pinned_at),
    type: p.type,
  }))
  articles.value.push(...mapped)
  const done = data.length < pageSize
  if (!done) page.value = nextPage
  return done
}

watch(() => route.fullPath, (newPath, oldPath) => {
  if (newPath === '/' && oldPath !== '/') { refreshFirst(); loadChannels() }
})

watch([selectedCategory, selectedTags], ([newCategory, newTags]) => {
  selectedCategoryGlobal.value = newCategory
  selectedTagsGlobal.value = newTags
})

const ioKey = computed(() => asyncKey.value.join('::'))
</script>

<style scoped>
.home-page {
  max-width: var(--page-max-width, 1100px);
  margin: 0 auto;
  padding: 0 24px 80px;
  container-type: inline-size;
  background: url('/bg.svg') no-repeat fixed;
  background-size: cover;
}

/* 子频道导航栏 */
.sub-channel-bar {
  position: sticky;
  top: var(--header-height, 56px);
  z-index: 10;
  background-color: transparent;
  backdrop-filter: blur(10px);
  padding-top: 12px;
}

.sub-channel-list {
  display: flex;
  align-items: center;
  gap: 2px;
  border-bottom: 1px solid var(--normal-border-color, #e5e5e5);
  flex-wrap: wrap;
}

.sub-channel-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  color: var(--text-secondary, #666);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.15s;
  position: relative;
  white-space: nowrap;
}

.sub-channel-item:hover {
  color: var(--text-color);
  background-color: var(--hover-bg, rgba(0, 0, 0, 0.03));
}

.sub-channel-item.selected {
  color: var(--primary-color);
}

.sub-channel-item.selected::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 18px;
  right: 18px;
  height: 2px;
  background-color: var(--primary-color);
  border-radius: 1px;
}

.sub-icon { font-size: 15px; }

/* 当前频道指示 */
.current-channel-indicator {
  padding: 8px 0;
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

/* 频道聊天入口 */
.chat-entry-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  margin-top: 8px;
  background: rgba(107, 107, 107, 0.05);
  border: 1px solid var(--normal-border-color, #e5e5e5);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color);
  transition: all 0.15s;
}

.chat-entry-bar:hover {
  background: rgba(107, 107, 107, 0.1);
}

.chat-arrow {
  opacity: 0.4;
  font-size: 13px;
}

.list-header {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 2px solid var(--normal-border-color, #e5e5e5);
  color: var(--text-tertiary, #999);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.col-main { flex: 1; padding-left: 12px; }
.col-participants { width: 120px; text-align: center; }
.col-replies { width: 60px; text-align: center; }
.col-views { width: 60px; text-align: center; }
.col-activity { width: 100px; text-align: right; padding-right: 12px; }

.article-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--normal-border-color, #eee);
  cursor: pointer;
  transition: background-color 0.15s;
}

.article-row:hover { background-color: var(--hover-bg, rgba(0, 0, 0, 0.015)); }

.col-main { flex: 1; padding-left: 12px; min-width: 0; }

.article-title {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  text-decoration: none;
  color: var(--text-color);
  margin-bottom: 4px;
}

.badges { display: inline-flex; gap: 4px; flex-shrink: 0; }
.badge { font-size: 12px; padding: 1px 6px; border-radius: 4px; }
.badge-pin { background-color: transparent; color: #b8956e; font-size: 12px; padding: 0; border-radius: 0; }
.badge-lottery { background-color: transparent; color: #d48585; font-size: 12px; padding: 0; border-radius: 0; }
.badge-poll { background-color: transparent; color: #8598c4; font-size: 12px; padding: 0; border-radius: 0; }
.badge-proposal { background-color: transparent; color: #a885b8; font-size: 12px; padding: 0; border-radius: 0; }
.badge-featured { background-color: transparent; color: #c4944a; font-size: 12px; padding: 0; border-radius: 0; }

.title-text {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-title:hover .title-text { color: var(--primary-color); }
.lock-icon { font-size: 13px; flex-shrink: 0; }

.article-desc {
  font-size: 12px;
  color: var(--text-tertiary, #999);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-decoration: none;
  margin-bottom: 6px;
  line-height: 1.5;
}

.article-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.col-participants { width: 120px; display: flex; justify-content: center; }
.avatar-group { display: flex; align-items: center; gap: 2px; }
.avatar-thumb { border-radius: 50%; border: 2px solid var(--background-color); margin-left: -6px; }
.avatar-thumb:first-child { margin-left: 0; }
.avatar-more { font-size: 11px; color: var(--text-tertiary, #999); margin-left: 4px; }

.col-replies, .col-views { width: 60px; text-align: center; font-size: 13px; color: var(--text-secondary, #888); font-weight: 500; }
.col-activity { width: 100px; text-align: right; padding-right: 12px; font-size: 12px; color: var(--text-tertiary, #999); }

.skeleton-row { display: flex; align-items: center; padding: 16px 0; border-bottom: 1px solid var(--normal-border-color, #eee); }
.skeleton-col { display: flex; align-items: center; }
.skeleton-main { flex: 1; padding-left: 12px; flex-direction: column; gap: 8px; align-items: flex-start; }
.skeleton-line { height: 12px; background: var(--skeleton-color, #eee); border-radius: 4px; }
.w-60 { width: 60%; } .w-90 { width: 90%; }
.skeleton-avatars { width: 120px; justify-content: center; gap: 4px; }
.skeleton-circle { width: 26px; height: 26px; border-radius: 50%; background: var(--skeleton-color, #eee); }
.skeleton-stat { width: 60px; height: 12px; background: var(--skeleton-color, #eee); border-radius: 4px; margin: 0 auto; }

.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-text { font-size: 16px; color: var(--text-secondary, #666); margin-bottom: 4px; }
.empty-sub { font-size: 13px; color: var(--text-tertiary, #999); }

@container (max-width: 800px) {
  .col-participants { width: 80px; }
  .col-views { display: none; }
  .list-header .col-views { display: none; }
  .avatar-more { display: none; }
}

@container (max-width: 600px) {
  .col-replies { display: none; }
  .list-header .col-replies { display: none; }
  .sub-channel-item { padding: 8px 12px; font-size: 12px; }
}
</style>