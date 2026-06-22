<!-- 后台布局组件 -->
<template>
  <div class="backend-layout">
    <el-container class="outer-container">
      <Sidebar />
      <el-container class="inner-container">
        <Navbar />
        <el-main class="main-content">
          <router-view class="content-container" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'

</script>

<style lang="scss" scoped>
.backend-layout {
  height: 100vh;
  width: 100%;
}

  .outer-container {
    height: 100%;
    flex: 1;
    overflow: hidden;
  }

  .inner-container {
    height: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .main-content {
    flex: 1;
    overflow-y: auto;
    background: #e4e7ec;
    padding: 10px;
  }

  .content-container {
    padding: 20px;
    background: white;
    min-height: calc(100% - 7.4vh);
  }
</style>
