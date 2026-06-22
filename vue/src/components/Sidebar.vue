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
.sidebar-wrapper {
    width: 200px;
    transition: width 0.3s ease;
    overflow: hidden;
    flex-shrink: 0;

    @media (min-width: 1440px) {
        width: 220px;
    }

    @media (min-width: 1920px) {
        width: 240px;
    }

    &.collapsed {
        width: 64px;
    }

    .sidebar-menu {
        height: 100%;

        .brand {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            padding: 12px 10px;
            border-bottom: 1px solid var(--el-menu-border-color, #e6e6e6);
            height: 56px;
            box-sizing: border-box;

            .logo-img {
                width: 28px;
                height: 28px;
                flex-shrink: 0;
            }

            .info-card {
                display: flex;
                flex-direction: column;
                justify-content: start;
                align-items: start;
                min-width: 0;
                overflow: hidden;
            }

            .info-card h1 {
                margin: 0;
                font-size: 14px;
                font-weight: 600;
                letter-spacing: 0.026vw;
                line-height: 1.5;
                color: var(--el-text-color-primary, #333);
                white-space: nowrap;
            }

            .info-card p {
                margin: 2px 0 0;
                font-size: 12px;
                line-height: 1.3;
                color: var(--el-text-color-secondary, #909399);
                white-space: nowrap;
            }
        }
    }

    :deep(.is-active) {
        background-color: #e6ecf7 !important;
    }

    :deep(.el-menu-item) {
        height: 44px;
        line-height: 44px;
    }
}
</style>
