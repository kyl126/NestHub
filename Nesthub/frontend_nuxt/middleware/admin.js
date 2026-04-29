import { authState } from '~/utils/auth'

export default defineNuxtRouteMiddleware((to, from) => {
    if (!authState.loggedIn) {
        return navigateTo('/login')
    }

    if (authState.role !== 'ADMIN') {
        return navigateTo('/')
    }
})