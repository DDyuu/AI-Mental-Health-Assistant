<template>
  <div class="frontend-layout">
    <div class="navbar-container">
      <div class="navbar-inner">
        <div class="brand-section">
          <el-image class="brand-logo" :src="iconUrl" alt="品牌logo" />
          <h1 class="brand-name">心理健康AI助手</h1>
        </div>
        <div class="nav-section">
          <router-link to="/home" class="nav-link">首页</router-link>
          <router-link to="/consultation" class="nav-link" v-if="userStore.isLoggedIn"
            >AI咨询</router-link
          >
          <router-link to="/emotionDiary" class="nav-link" v-if="userStore.isLoggedIn"
            >情绪日记</router-link
          >
          <router-link to="/knowledge" class="nav-link">知识库</router-link>
          <el-button v-if="userStore.isLoggedIn" class="logout-btn" @click="handleLogout"
            >退出登录</el-button
          >
          <template v-else>
            <router-link to="/auth/login" class="nav-link">登录</router-link>
            <router-link to="/auth/register" class="nav-link nav-link--cta">
              <el-button type="primary">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </div>
    <div class="main-content">
      <router-view></router-view>
    </div>
    <div class="footer-container">
      <div class="footer-bottom">
        <p>&copy; 2026 心理健康AI助手 All rights reserved.</p>
      </div>
    </div>
  </div>
</template>
<script setup>
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { logout } from "@/api/admin";
const router = useRouter();
const iconUrl = new URL("@/assets/images/logo.svg", import.meta.url).href;
const userStore = useUserStore();
//用户端退出登录
const handleLogout = () => {
  logout().then(() => {
    userStore.logout()
    router.push("/auth/login");
  });
};
</script>
<style lang="scss" scoped>
@use '../styles/mixins' as m;

.frontend-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: var(--color-bg);

  .navbar-container {
    background: var(--alpha-90);
    border-bottom: 1px solid var(--color-border);
    backdrop-filter: blur(8px);
  }

  .navbar-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-5);
    max-width: 1200px;
    margin: 0 auto;
    padding: var(--space-3) var(--space-5);
  }

  .brand-section {
    display: flex;
    align-items: center;
    gap: var(--space-3);

    .brand-logo {
      width: 40px;
      height: 40px;
    }

    .brand-name {
      font-size: var(--font-lg);
      font-weight: 600;
      color: var(--color-text);
    }
  }

  .nav-section {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;

    .nav-link {
      padding: var(--space-2) var(--space-3);
      border-radius: var(--radius-pill);
      font-size: var(--font-sm);
      font-weight: 500;
      color: var(--color-text-secondary);
      transition: color var(--transition-base), background-color var(--transition-base);

      &:hover {
        color: var(--color-primary);
        background: var(--color-primary-wash);
      }

      /* 选中态：解决"用户不知道自己在哪一页" */
      &.router-link-active {
        color: var(--color-primary);
        background: var(--color-primary-light);
      }
    }

    .nav-link--cta {
      padding: 0;
      background: none;

      &:hover {
        background: none;
      }
    }

    .logout-btn {
      margin-left: var(--space-2);
    }
  }

  .main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
  }

  .footer-container {
    margin-top: auto;
    padding: var(--space-5) 0;
    background: var(--color-surface);
    border-top: 1px solid var(--color-border);

    .footer-bottom {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 var(--space-5);
      text-align: center;
      font-size: var(--font-sm);
      color: var(--color-text-secondary);
    }
  }

  @include m.below-sm {
    .navbar-inner {
      flex-direction: column;
      align-items: flex-start;
      padding: var(--space-3) var(--space-4);
    }

    .brand-section .brand-name {
      font-size: var(--font-md);
    }

    .nav-section {
      width: 100%;
      justify-content: flex-start;
      gap: var(--space-1);
    }
  }
}
</style>
