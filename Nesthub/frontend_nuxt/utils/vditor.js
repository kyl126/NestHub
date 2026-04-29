import Vditor from 'vditor'
import { getToken, authState } from './auth'
import { searchUsers, fetchFollowings, fetchAdmins } from './user'
import { tiebaEmoji } from './tiebaEmoji'
import vditorPostCitation from './vditorPostCitation.js'
import { checkFileSize, formatFileSize } from './videoCompressor.js'

export function getEditorTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'classic'
}

export function getPreviewTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

export function createVditor(editorId, options = {}) {
  const { placeholder = '', preview = {}, input, after } = options
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl
  const WEBSITE_BASE_URL = config.public.websiteBaseUrl

  const fetchMentions = async (value) => {
    if (!value) {
      const [followings, admins] = await Promise.all([
        fetchFollowings(authState.username),
        fetchAdmins(),
      ])
      const combined = [...followings, ...admins]
      const seen = new Set()
      return combined.filter((u) => {
        if (seen.has(u.id)) return false
        seen.add(u.id)
        return true
      })
    }
    return searchUsers(value)
  }

  const isMobile = window.innerWidth <= 768
  const toolbar = isMobile
    ? ['emoji', 'upload']
    : [
        'emoji',
        'bold',
        'italic',
        'strike',
        '|',
        'list',
        'line',
        'quote',
        'code',
        'inline-code',
        '|',
        'undo',
        'redo',
        '|',
        'link',
        'upload',
      ]

  let vditor
  vditor = new Vditor(editorId, {
    placeholder,
    height: 'auto',
    theme: getEditorTheme(),
    preview: Object.assign(
      {
        theme: { current: getPreviewTheme() },
      },
      preview,
    ),
    hint: {
      emoji: tiebaEmoji,
      extend: [
        {
          key: '@',
          hint: async (key) => {
            const list = await fetchMentions(key)
            return list.map((u) => ({
              value: `@[${u.username}]`,
              html: `<BaseImage src="${u.avatar}" /> @${u.username}`,
            }))
          },
        },
        vditorPostCitation(API_BASE_URL, WEBSITE_BASE_URL),
      ],
    },
    cdn: 'https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/vditor',
    toolbar,
    upload: {
      accept: 'image/*,video/*',
      multiple: false,
      fieldName: 'file',
      url: `${API_BASE_URL}/api/upload`,
      headers: { Authorization: `Bearer ${getToken()}` },
      format(files, responseText) {
          const res = JSON.parse(responseText)
          if (res.code === 0) {
              // 把相對路徑轉成完整 URL
              let url = res.data.url
              if (url && url.startsWith('/uploads/')) {
                  url = API_BASE_URL + url
                }
                return JSON.stringify({
                  code: 0,
                  msg: '',
                        data: {
                      errFiles: [],
                      succMap: { [files[0].name]: url }
                        }
              })
          } else {
              return JSON.stringify({
                  code: 1,
                  msg: res.msg || '上传失败',
                  data: { errFiles: files.map(f => f.name), succMap: {} }
              })
          }
      }
    },
    // upload: {
    //   fieldName: 'file',
    //   url: `${API_BASE_URL}/api/upload`,
    //   accept: 'image/*,video/*',
    //   multiple: false,
    //   headers: { Authorization: `Bearer ${getToken()}` },
    //   format(files, responseText) {
    //     const res = JSON.parse(responseText)
    //     if (res.code === 0) {
    //       return JSON.stringify({
    //         code: 0,
    //         msg: '',
    //         data: {
    //           errFiles: [],
    //           succMap: { [files[0].name]: res.data.url }
    //         }
    //       })
    //     } else {
    //       return JSON.stringify({
    //         code: 1,
    //         msg: '上传失败',
    //         data: { errFiles: files.map(f => f.name), succMap: {} }
    //       })
    //     }
    //   }
    // },
    toolbarConfig: { pin: true },
    cache: { enable: false },
    input,
    after,
  })

  return vditor
}
