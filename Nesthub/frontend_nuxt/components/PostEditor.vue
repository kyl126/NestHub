<template>
  <div class="post-editor-container">
    <div :id="editorId" ref="vditorElement"></div>
    <div v-if="loading" class="editor-loading-overlay">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
  </div>
</template>

<script>
import { onMounted, onUnmounted, ref, useId, watch } from 'vue'
import { clearVditorStorage } from '~/utils/clearVditorStorage'
import { themeState } from '~/utils/theme'
import {
  createVditor,
  getEditorTheme as getEditorThemeUtil,
  getPreviewTheme as getPreviewThemeUtil,
} from '~/utils/vditor'
import '~/assets/global.css'

export default {
  name: 'PostEditor',
  emits: ['update:modelValue', 'update:loading'],
  props: {
    modelValue: {
      type: String,
      default: '',
    },
    editorId: {
      type: String,
      default: '',
    },
    loading: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  setup(props, { emit }) {
    const vditorInstance = ref(null)
    let vditorRender = false
    const editorId = ref(props.editorId)
    if (!editorId.value) {
      editorId.value = 'post-editor-' + useId()
    }

    const getEditorTheme = getEditorThemeUtil
    const getPreviewTheme = getPreviewThemeUtil
    const applyTheme = () => {
      if (vditorInstance.value) {
        vditorInstance.value.setTheme(getEditorTheme(), getPreviewTheme())
      }
    }

    watch(
      () => props.loading,
      (val) => {
        if (!vditorRender) return
        if (val) {
          vditorInstance.value.disabled()
        } else {
          vditorInstance.value.enable()
        }
      },
    )

    watch(
      () => props.disabled,
      (val) => {
        if (!vditorInstance.value) return
        if (val) {
          vditorInstance.value.disabled()
        } else if (!props.loading) {
          vditorInstance.value.enable()
        }
      },
    )

    watch(
      () => props.modelValue,
      (val) => {
        if (vditorInstance.value && vditorInstance.value.getValue() !== val) {
          vditorInstance.value.setValue(val)
        }
      },
    )

    watch(
      () => themeState.mode,
      () => {
        applyTheme()
      },
    )

    onMounted(() => {
      emit('update:loading', true)
      vditorInstance.value = createVditor(editorId.value, {
        placeholder: '请输入正文...',
        input(value) {
          emit('update:modelValue', value)
        },
        after() {
          vditorRender = true
          emit('update:loading', false)
          vditorInstance.value.setValue(props.modelValue)
          if (props.loading || props.disabled) {
            vditorInstance.value.disabled()
          }
          applyTheme()
        },
      })
      // applyTheme()
    })

    onUnmounted(() => {
      clearVditorStorage()
    })

    return { editorId }
  },
}
</script>

<style scoped>
.post-editor-container {
  position: relative;
  min-height: 200px;
  background: rgba(255, 252, 250, 0.6);
  border-radius: 12px;
}

.editor-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 252, 250, 0.8);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: all;
  z-index: 10;
  border-radius: 12px;
}

/* ===== Vditor 内部覆盖 ===== */
:deep(.vditor) {
  border: none !important;
  background: transparent !important;
}

:deep(.vditor-reset img) {
  max-width: 300px;
  height: auto;
}

:deep(.vditor-toolbar) {
  background: rgba(255, 252, 250, 0.8) !important;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e8e0da !important;
  border-radius: 12px 12px 0 0;
}

:deep(.vditor-toolbar__item) {
  color: #8a7a72 !important;
}

:deep(.vditor-toolbar__item:hover) {
  background: rgba(192, 176, 168, 0.12) !important;
}

:deep(.vditor-toolbar__item--current) {
  background: rgba(192, 176, 168, 0.18) !important;
}

:deep(.vditor-content) {
  background: transparent !important;
}

:deep(.vditor-reset) {
  color: #463a36 !important;
  font-size: 15px;
  line-height: 1.8;
}

:deep(.vditor-irn) {
  color: #463a36 !important;
}

:deep(.vditor-input) {
  background: transparent !important;
  color: #463a36 !important;
}

:deep(.vditor-placeholder) {
  color: #c0b5ae !important;
}

:deep(.vditor-panel) {
  background: #faf7f5 !important;
  border: 1px solid #e5ddd8 !important;
  box-shadow: 0 8px 24px rgba(70, 58, 54, 0.08);
  border-radius: 8px;
}

:deep(.vditor-pre) {
  background: rgba(245, 242, 240, 0.6) !important;
}

:deep(.vditor-reset pre) {
  background: rgba(245, 242, 240, 0.6) !important;
}

:deep(.vditor-reset code) {
  background: rgba(245, 242, 240, 0.6) !important;
  color: #8a6a5a !important;
}

:deep(.vditor-reset blockquote) {
  border-left-color: #d5c8c0 !important;
  color: #8a7a72 !important;
}

:deep(.vditor-reset table) {
  border-color: #e5ddd8 !important;
}

:deep(.vditor-reset th) {
  background: rgba(192, 176, 168, 0.15) !important;
  border-color: #e5ddd8 !important;
}

:deep(.vditor-reset td) {
  border-color: #e5ddd8 !important;
}

:deep(.vditor-reset hr) {
  border-color: #e5ddd8 !important;
}

/* 即时渲染输入区 */
:deep(.vditor-ir) {
  background: transparent !important;
}

:deep(.vditor-ir .vditor-ir__node) {
  color: #463a36 !important;
}

:deep(.vditor-ir .vditor-ir__node--expand) {
  background: rgba(255, 252, 250, 0.5) !important;
}

:deep(.vditor-ir pre),
:deep(.vditor-ir code) {
  background: rgba(245, 242, 240, 0.5) !important;
  color: #8a6a5a !important;
}

/* 即时渲染输入框本体 */
:deep(.vditor-ir .vditor-reset) {
  background: transparent !important;
  color: #463a36 !important;
}


:deep(.vditor-ir__marker) {
  color: #b0a098 !important;
}

:deep(.vditor-ir__node[data-type="code-block"]) {
  background: rgba(245, 242, 240, 0.5) !important;
  border-radius: 6px;
}

/* 工具栏图标 */
:deep(.vditor-toolbar__item svg) {
  fill: #8a7a72 !important;
}

:deep(.vditor-toolbar__item:hover svg) {
  fill: #463a36 !important;
}

/* 计数器 */
:deep(.vditor-counter) {
  color: #b0a098 !important;
}

@media (max-width: 768px) {
  .post-editor-container {
    min-height: 100px;
  }
}
</style>