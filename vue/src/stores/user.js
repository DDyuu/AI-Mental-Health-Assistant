import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    const userInfo = ref(null)

    // 计算属性：是否已登录
    const isLoggedIn = computed(() => !!userInfo.value && !!localStorage.getItem('token'))

    // 计算属性：是否为管理员
    const isAdmin = computed(() => userInfo.value?.userType === 2)

    // 初始化：从 localStorage 恢复
    const init = () => {
        const stored = localStorage.getItem('userInfo')
        if (stored) {
            try {
                userInfo.value = JSON.parse(stored)
            } catch {
                localStorage.removeItem('userInfo')
            }
        }
    }

    // 登录成功：保存用户信息和 token
    const login = (info, token) => {
        userInfo.value = info
        localStorage.setItem('token', token)
        localStorage.setItem('userInfo', JSON.stringify(info))
    }

    // 退出登录：清除用户信息和 token
    const logout = () => {
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return {
        userInfo,
        isLoggedIn,
        isAdmin,
        init,
        login,
        logout
    }
})
