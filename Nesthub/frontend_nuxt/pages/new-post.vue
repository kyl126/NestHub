<template>
  <div class="new-post-page">
    <div class="new-post-form">
      <input class="post-title-input" v-model="title" placeholder="标题" />
      <div class="post-editor-container">
        <PostEditor v-model="content" v-model:loading="isAiLoading" :disabled="!isLogin" />
        <LoginOverlay v-if="!isLogin" />
      </div>
      <div class="post-options">
        <div class="post-options-left">
          <CategorySelect v-model="selectedCategory" />
          <SubChannelSelect v-model="selectedSubChannel" :categoryId="Number(selectedCategory)" />
          <PostTypeSelect v-model="postType" />
          <PostVisibleScopeSelect v-model="postVisibleScope"/>
        </div>
        <div class="post-options-right">
          <div class="post-clear" @click="clearPost"><clear-icon /> 清空</div>
          <div class="ai-generate" @click="aiGenerate">
            <smart-optimization />
            MD 格式优化
          </div>
          <div class="post-draft" @click="saveDraft">
            <save-icon />
            存草稿
          </div>
          <div
            v-if="!isWaitingPosting"
            class="post-submit"
            :class="{ disabled: !isLogin }"
            @click="submitPost"
          >
            发布
          </div>
          <div v-else class="post-submit-loading">
            <loading-four class="loading-icon" /> 发布中...
          </div>
        </div>
      </div>
      <LotteryForm v-if="postType === 'LOTTERY'" :data="lottery" />
      <PollForm v-if="postType === 'POLL'" :data="poll" />
      <ProposalForm v-if="postType === 'PROPOSAL'" :data="proposal" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue'
import SubChannelSelect from '~/components/SubChannelSelect.vue'
import CategorySelect from '~/components/CategorySelect.vue'
import LoginOverlay from '~/components/LoginOverlay.vue'
import PostEditor from '~/components/PostEditor.vue'
import PostTypeSelect from '~/components/PostTypeSelect.vue'
import LotteryForm from '~/components/LotteryForm.vue'
import PollForm from '~/components/PollForm.vue'
import ProposalForm from '~/components/ProposalForm.vue'
import { toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import PostVisibleScopeSelect from '~/components/PostVisibleScopeSelect.vue'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const title = ref('')
const content = ref('')
const selectedCategory = ref(1)
const selectedTags = ref([1])
const selectedSubChannel = ref('最新')
const postType = ref('NORMAL')
const postVisibleScope = ref('ALL')
const lottery = reactive({
  prizeIcon: '',
  prizeIconFile: null,
  tempPrizeIcon: '',
  showPrizeCropper: false,
  prizeDescription: '',
  prizeCount: 1,
  pointCost: 0,
  endTime: null,
})
const poll = reactive({
  options: ['', ''],
  endTime: null,
  multiple: false,
})
const proposal = reactive({
  proposedName: '',
  proposalDescription: '',
})
const startTime = ref(new Date().toISOString().slice(0, 16).replace('T', ' '))
const isWaitingPosting = ref(false)
const isAiLoading = ref(false)
const isLogin = computed(() => authState.loggedIn)

const loadDraft = async () => {
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/drafts/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (res.ok && res.status !== 204) {
      const data = await res.json()
      title.value = data.title || ''
      content.value = data.content || ''
      selectedCategory.value = data.categoryId || ''
      selectedSubChannel.value = data.subChannel || '最新'
      selectedTags.value = data.tagIds || []
      postVisibleScope.value = data.visiblescope

      toast.success('草稿已加载')
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadDraft)

const clearPost = async () => {
  title.value = ''
  content.value = ''
  selectedCategory.value = 1
  selectedSubChannel.value = '最新'
  selectedTags.value = []
  postVisibleScope.value = 'ALL'
  postType.value = 'NORMAL'
  lottery.prizeIcon = ''
  lottery.prizeIconFile = null
  lottery.tempPrizeIcon = ''
  lottery.showPrizeCropper = false
  lottery.prizeDescription = ''
  lottery.prizeCount = 1
  lottery.pointCost = 0
  lottery.endTime = null
  startTime.value = null
  poll.options = ['', '']
  poll.endTime = null
  poll.multiple = false
  proposal.proposedName = ''
  proposal.proposalDescription = ''

  // 删除草稿
  const token = getToken()
  if (token) {
    const res = await fetch(`${API_BASE_URL}/api/drafts/me`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
    if (res.ok) {
      toast.success('草稿已清空')
    } else {
      toast.error('云端草稿清空失败, 请稍后重试')
    }
  }
}

const saveDraft = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    const tagIds = selectedTags.value.filter((t) => typeof t === 'number')
    const res = await fetch(`${API_BASE_URL}/api/drafts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title.value,
        content: content.value,
        categoryId: selectedCategory.value || null,
        tagIds,
        postVisibleScopeType:postVisibleScope.value
      }),
    })
    if (res.ok) {
      toast.success('草稿已保存')
    } else {
      toast.error('保存失败')
    }
  } catch (e) {
    toast.error('保存失败')
  }
}
const ensureTags = async (token) => {
  for (let i = 0; i < selectedTags.value.length; i++) {
    const t = selectedTags.value[i]
    if (typeof t === 'string' && t.startsWith('__new__:')) {
      const name = t.slice(8)
      const res = await fetch(`${API_BASE_URL}/api/tags`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ name, description: '' }),
      })
      if (res.ok) {
        const data = await res.json()
        selectedTags.value[i] = data.id
        // update local TagSelect options handled by component
      } else {
        let data
        try {
          data = await res.json()
        } catch (e) {
          data = null
        }
        toast.error((data && data.error) || '创建标签失败')
        throw new Error('create tag failed')
      }
    }
  }
}

const aiGenerate = async () => {
  if (!content.value.trim()) {
    toast.error('内容为空，无法优化')
    return
  }
  isAiLoading.value = true
  try {
    toast.info('AI 优化中...')
    const token = getToken()
    const res = await fetch(`${API_BASE_URL}/api/ai/format`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ text: content.value }),
    })
    if (res.ok) {
      const data = await res.json()
      content.value = data.content || ''
    } else if (res.status === 429) {
      toast.error('今日AI优化次数已用尽')
    } else {
      toast.error('AI 优化失败')
    }
  } catch (e) {
    toast.error('AI 优化失败')
  } finally {
    isAiLoading.value = false
  }
}

const submitPost = async () => {
  if (!title.value.trim()) {
    toast.error('标题不能为空')
    return
  }
  if (!content.value.trim()) {
    toast.error('内容不能为空')
    return
  }
  if (!selectedCategory.value) {
    toast.error('请选择分类')
    return
  }
  if (selectedTags.value.length === 0) {
    toast.error('请选择标签')
    return
  }
  if (postType.value === 'LOTTERY') {
    if (!lottery.prizeIcon && !lottery.prizeIconFile) {
      toast.error('请上传奖品图片')
      return
    }
    if (!lottery.prizeCount || lottery.prizeCount < 1) {
      toast.error('奖品数量必须大于0')
      return
    }
    if (!lottery.prizeDescription) {
      toast.error('请输入奖品描述')
      return
    }
    if (!lottery.endTime) {
      toast.error('请选择抽奖结束时间')
      return
    }
    if (lottery.pointCost < 0 || lottery.pointCost > 100) {
      toast.error('参与积分需在0到100之间')
      return
    }
  }
  if (postType.value === 'POLL') {
    if (poll.options.length < 2 || poll.options.some((o) => !o.trim())) {
      toast.error('请填写至少两个投票选项')
      return
    }
    if (!poll.endTime) {
      toast.error('请选择投票结束时间')
      return
    }
  }
  if (postType.value === 'PROPOSAL') {
    if (!proposal.proposedName.trim()) {
      toast.error('请填写拟议分类名称')
      return
    }
  }
  try {
    const token = getToken()
    await ensureTags(token)
    isWaitingPosting.value = true
    let prizeIconUrl = lottery.prizeIcon

    // 只有选中了新文件才上传
    if (postType.value === 'LOTTERY' && lottery.prizeIconFile) {
      console.log('准备上传文件:', lottery.prizeIconFile)
      const form = new FormData()
      form.append('file', lottery.prizeIconFile)
      const uploadRes = await fetch(`${API_BASE_URL}/api/upload`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form,
      })
      const uploadData = await uploadRes.json()
      console.log('上传响应:', uploadData)
      if (!uploadRes.ok) {
        toast.error(uploadData.error || '奖品图片上传失败')
        return
      }
      // 后端返回格式可能是 { url: "..." } 或 { data: { url: "..." } }
          prizeIconUrl = uploadData.url || uploadData.data?.url || ''
      // 把相對路徑轉成完整 URL
      if (prizeIconUrl && prizeIconUrl.startsWith('/uploads/')) {
            prizeIconUrl = API_BASE_URL + prizeIconUrl
      }
            console.log('最终 prizeIconUrl:', prizeIconUrl)
      if (!prizeIconUrl) {
          toast.error('图片上传成功但未获取到URL')
          return
      }
    }

    const toUtcString = (value) => {
      if (!value) return undefined
      return new Date(new Date(value).getTime() + 8.02 * 60 * 60 * 1000).toISOString()
    }

    const payload = {
      title: title.value,
      content: content.value,
      categoryId: selectedCategory.value,
      tagIds: selectedTags.value,
      type: postType.value,
      postVisibleScopeType: postVisibleScope.value,
      subChannel: selectedSubChannel.value,
    }

    if (postType.value === 'LOTTERY') {
      payload.prizeIcon = prizeIconUrl
      payload.prizeCount = lottery.prizeCount
      payload.prizeDescription = lottery.prizeDescription
      payload.pointCost = lottery.pointCost
      payload.startTime = startTime.value ? new Date(startTime.value).toISOString() : undefined
      payload.endTime = toUtcString(lottery.endTime)
    } else if (postType.value === 'POLL') {
      payload.options = poll.options
      payload.multiple = poll.multiple
      payload.endTime = toUtcString(poll.endTime)
    } else if (postType.value === 'PROPOSAL') {
      payload.proposedName = proposal.proposedName
      payload.proposalDescription = proposal.proposalDescription
    }

    const res = await fetch(`${API_BASE_URL}/api/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
    })
    
    const data = await res.json()
    if (res.ok) {
      if (data.reward && data.reward > 0) {
        toast.success(`发布成功，获得 ${data.reward} 经验值`)
      } else {
        toast.success('发布成功')
      }
      if (data.id) {
        await navigateTo(`/posts/${data.id}`)
      }
            // 清空奖品图片临时数据
      lottery.prizeIcon = ''
      lottery.prizeIconFile = null
      lottery.tempPrizeIcon = ''
      lottery.showPrizeCropper = false
    } else if (res.status === 429) {
      toast.error('发布过于频繁，请稍后再试')
    } else {
      toast.error(data.error || '发布失败')
    }
  } catch (e) {
    toast.error('发布失败')
  } finally {
    isWaitingPosting.value = false
  }
}
</script>

<style scoped>
.new-post-page {
  display: flex;
  justify-content: center;
  background-color: var(--background-color);
  padding: 0 20px;
}

.new-post-form {
  width: 100%;
  max-width: 900px;
}

/* 标题输入框 */
.post-title-input {
  border: none;
  outline: none;
  padding: 24px 0 16px;
  background-color: transparent;
  font-size: 36px;
  width: 100%;
  font-weight: 700;
  color: #463a36;
}

.post-title-input::placeholder {
  color: #c0b5ae;
}

/* 编辑器容器 - 加边框区分 */
.post-editor-container {
  position: relative;
  border: 1px solid #e5ddd8;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 252, 250, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.post-editor-container:focus-within {
  border-color: #c0b0a8;
  box-shadow: 0 0 0 3px rgba(192, 176, 168, 0.15);
}

/* 底部操作栏 */
.post-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 20px;
  padding-bottom: 50px;
  gap: 12px;
}

.post-options-left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.post-options-right {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

/* 操作按钮 */
.post-clear,
.ai-generate,
.post-draft {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #8a7a72;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s;
}

.post-clear:hover,
.ai-generate:hover,
.post-draft:hover {
  color: #463a36;
  background: rgba(192, 176, 168, 0.12);
}

.post-clear { opacity: 0.6; }
.post-clear:hover { opacity: 1; }

/* 发布按钮 */
.post-submit {
  background: #8a7a72;
  color: #fff;
  padding: 10px 24px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.post-submit:hover {
  background: #6b5b55;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 91, 85, 0.2);
}

.post-submit:active {
  transform: translateY(0);
}

.post-submit.disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.post-submit.disabled:hover {
  background: #ccc;
  transform: none;
  box-shadow: none;
}

.post-submit-loading {
  color: white;
  background: #b0a098;
  padding: 10px 24px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: not-allowed;
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .new-post-page {
    padding: 0 10px;
  }

  .post-title-input {
    font-size: 24px;
    padding: 16px 0 12px;
  }

  .post-options {
    margin-top: 12px;
    flex-direction: column;
    align-items: stretch;
  }

  .post-options-right {
    justify-content: flex-end;
  }
}
</style>