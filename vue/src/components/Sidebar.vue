<template>
    <div class="sidebar-wrapper" :class="{ collapsed: isCollapse }">
        <el-menu :default-active="router.currentRoute.value.path" router class="sidebar-menu" 
            :collapse="isCollapse">
            <div class="brand">
                <el-image :src="logoSrc" alt="logo" class="logo-img" />
                <div class="info-card" v-show="!isCollapse">
                    <h1>AI心理健康助手</h1>
                    <p>管理后台</p>
                </div>
            </div>
            <el-menu-item v-for="item in router.options.routes[0].children" :index="'/back/' + item.path">
                <el-icon>
                    <component :is="item.meta.icon" />
                </el-icon>
                <template #title>
                    <span>{{ item.meta.title }}</span>
                </template>
            </el-menu-item>
        </el-menu>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import logoSrc from '@/assets/images/logo.svg'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const isCollapse = computed(() => adminStore.isCollapse)
</script>

<style lang="scss" scoped>
@use '../styles/mixins' as m;

.sidebar-wrapper {
  width: 200px;
  flex-shrink: 0;
  overflow: hidden;
  transition: width 0.3s ease;

  @include m.above-md {
    width: 220px;
  }

  &.collapsed {
    width: 64px;
  }

  .sidebar-menu {
    height: 100%;
    border-right: none;

    .brand {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      height: 56px;
      padding: var(--space-3) var(--space-2);
      box-sizing: border-box;
      border-bottom: 1px solid var(--color-border);

      .logo-img {
        width: 28px;
        height: 28px;
        flex-shrink: 0;
      }

      .info-card {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        justify-content: flex-start;
        min-width: 0;
        overflow: hidden;
      }

      .info-card h1 {
        margin: 0;
        font-size: var(--font-sm);
        font-weight: 600;
        line-height: 1.5;
        white-space: nowrap;
        color: var(--color-text);
      }

      .info-card p {
        margin: var(--space-1) 0 0;
        font-size: var(--font-xs);
        line-height: 1.3;
        white-space: nowrap;
        color: var(--color-text-secondary);
      }
    }
  }

  /* 菜单项：圆角 + 左右内缩，选中态用主色浅底 + 左侧主色竖条 */
  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    margin: 0 var(--space-2);
    border-radius: var(--radius-md);

    &:hover {
      color: var(--color-primary);
      background-color: var(--color-primary-wash);
    }

    &.is-active {
      color: var(--color-primary);
      background-color: var(--color-primary-light);
      font-weight: 600;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        width: 3px;
        height: 20px;
        transform: translateY(-50%);
        border-radius: var(--radius-pill);
        background: var(--color-primary);
      }
    }
  }
}
</style>
