<template>
  <div class="frontend-layout">
    <div class="navbar-container">
      <div class="brand-section">
        <el-image
          style="width: 50px; height: 50px"
          :src="iconUrl"
          alt="品牌logo"
        />
        <h1 class="brand-name">心理健康AI助手</h1>
      </div>
      <div class="nav-section">
        <router-link to="/" class="nav-link">首页</router-link>
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
          <router-link to="/auth/register" class="nav-link">
            <el-button type="primary">注册</el-button>
          </router-link>
        </template>
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
.frontend-layout {
  background-color: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
  .navbar-container {
    height: 30px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 0;
    width: 100%;
    background: linear-gradient(90deg, #ffffff 0%, #4096ff 100%);
    .brand-section {
      display: flex;
      align-items: center;
      .brand-name {
        margin-left: 10px;
        font-size: 24px;
        font-weight: 600;
        color: #333;
      }
    }
    .nav-section {
      display: flex;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
      padding-right: 20px;
      .nav-link {
        color: #4b5563;
        font-size: 16px;
        font-weight: 500;
        &:hover {
          color: #4a90e2;
        }
      }
    }
  }

  .main-content {
    flex: 1;
    width: 100%;
  }

  .footer-container {
    background: #1f2937;
    color: white;
    padding: 15px 0;
    margin-top: auto;
    .footer-bottom {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      text-align: center;
    }
  }

  @media (max-width: 768px) {
    .navbar-container {
      flex-direction: column;
      height: auto;
      padding: 15px 20px;
      gap: 10px;
      .brand-section {
        .brand-name {
          font-size: 20px;
        }
      }
      .nav-section {
        justify-content: center;
        gap: 15px;
        .nav-link {
          font-size: 14px;
        }
      }
    }

    .main-content {
      padding: 10px;
    }
  }
}
</style>
