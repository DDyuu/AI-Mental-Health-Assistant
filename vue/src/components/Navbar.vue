<template>
    <div class="navbar">
        <div class="flex-box">
            <el-button @click="handleCollapse">
                <el-icon>
                    <Expand />
                </el-icon>
            </el-button>
            <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="flex-box user-area">
            <el-dropdown @command="handleCommand">
                <div class="flex-box">
                    <el-avatar :src="userStore.userInfo?.avatar" alt="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" size="medium" />
                    <span class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</span>
                    <el-icon><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
</template>

<script setup>
import { useAdminStore } from '@/stores/admin'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { logout } from '@/api/admin'

const router = useRouter()
const route = useRoute()

const userStore = useUserStore()

const handleCommand = (command) => {
    if(command === 'logout') {
        ElMessageBox.confirm('确认退出登录?', '确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => {
            logout().then(() => {
                userStore.logout()
                router.push('/auth/login')
            })
        }).catch(() => {})
    }
}

const handleCollapse = () => {
    useAdminStore().toggleCollapse()
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 55px;
  width: 100%;
  padding: 0 var(--space-5);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);

  .flex-box {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .page-title {
    margin-left: var(--space-4);
    font-size: var(--font-xl);
    font-weight: 600;
    color: var(--color-text);
  }

  .user-area {
    gap: var(--space-2);
    padding-right: var(--space-4);
  }
}
</style>
