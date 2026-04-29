<template>
  <div>
    <NotificationSettingPopup :visible="showNotificationPopup" @close="closeNotificationPopup" />
    <MessagePopup :visible="showMessagePopup" @close="closeMessagePopup" />
  </div>
</template>

<script setup>
import NotificationSettingPopup from '~/components/NotificationSettingPopup.vue'
import MessagePopup from '~/components/MessagePopup.vue'
import { authState } from '~/utils/auth'

const showNotificationPopup = ref(false)
const showMessagePopup = ref(false)

onMounted(async () => {
  await checkMessageFeature()
  if (showMessagePopup.value) return

  await checkNotificationSetting()
})

const checkMessageFeature = async () => {
  if (!import.meta.client) return
  if (!authState.loggedIn) return
  if (localStorage.getItem('messageFeaturePopupShown')) return
  showMessagePopup.value = true
}

const closeMessagePopup = () => {
  if (!import.meta.client) return
  localStorage.setItem('messageFeaturePopupShown', 'true')
  showMessagePopup.value = false
}

const checkNotificationSetting = async () => {
  if (!import.meta.client) return
  if (!authState.loggedIn) return
  if (localStorage.getItem('notificationSettingPopupShown')) return
  showNotificationPopup.value = true
}

const closeNotificationPopup = () => {
  if (!import.meta.client) return
  localStorage.setItem('notificationSettingPopupShown', 'true')
  showNotificationPopup.value = false
}
</script>