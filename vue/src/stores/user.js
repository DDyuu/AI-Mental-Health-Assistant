import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/admin'

export const useUserStore = defineStore('user', () => {
    const userInfo = ref(null)
    const loading = ref(false)
    const loaded = ref(false)

    // 计算属性：是否已登录（只依赖 token）
    const isLoggedIn = computed(() => !!localStorage.getItem('token'))

    // 计算属性：是否为管理员
    const isAdmin = computed(() => userInfo.value?.userType === 2)

    // 从后端获取用户信息（页面刷新时调用）
    const fetchUserInfo = async () => {
        const token = localStorage.getItem('token')
        if (!token) {
            userInfo.value = null
            loaded.value = true
            return null
        }
        if (loading.value) return userInfo.value
        loading.value = true
        try {
            userInfo.value = await getUserInfo()
            loaded.value = true
            return userInfo.value
        } catch {
            // token 失效，清除登录态
            localStorage.removeItem('token')
            userInfo.value = null
            loaded.value = true
            return null
        } finally {
            loading.value = false
        }
    }

    // 登录成功：保存 token 到 localStorage，用户信息存 Pinia
    const login = (info, token) => {
        userInfo.value = info
        loaded.value = true
        localStorage.setItem('token', token)
    }

    // 退出登录：清除所有
    const logout = () => {
        userInfo.value = null
        loaded.value = false
        localStorage.removeItem('token')
    }

    return {
        userInfo,
        loading,
        loaded,
        isLoggedIn,
        isAdmin,
        fetchUserInfo,
        login,
        logout
    }
})
