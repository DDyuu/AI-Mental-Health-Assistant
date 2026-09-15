<template>
    <div class="auth-layout">
        <div class="left-section">
            <div class="content">
                <h2 class="title">心理AI助手</h2>
                <p class="text">每个深夜，每个焦虑的时刻，我们都在这里。不必独自承受，让心与心的连接温暖您的每一天。</p>
                <div class="robot">
                    <el-image style="width:90px; height: 90px;" :src="iconUrl" alt="心理AI助手" />
                </div>
            </div>
        </div>
        <div class="right-section">
            <router-view />
        </div>
    </div>
</template>

<script setup>
import iconUrl from '@/assets/images/robot.svg'

</script>

<style lang="scss" scoped>
@use '../styles/mixins' as m;

.auth-layout {
  display: flex;
  min-height: 100vh;

  .left-section {
    position: relative;
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    background: linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-primary-dark) 100%);

    /* 一层极淡径向光斑，营造"安静"的质感 */
    &::before {
      content: '';
      position: absolute;
      top: -20%;
      left: -10%;
      width: 70%;
      height: 70%;
      border-radius: var(--radius-pill);
      background: radial-gradient(circle, var(--alpha-05) 0%, transparent 70%);
      pointer-events: none;
    }

    .content {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-6);
      text-align: center;
    }

    .title {
      margin-bottom: var(--space-5);
      font-size: var(--font-2xl);
      font-weight: 700;
      color: var(--color-text-inverse);
    }

    .text {
      max-width: 460px;
      margin-bottom: var(--space-6);
      font-size: var(--font-xl);
      color: var(--color-text-inverse);
    }

    .robot {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 160px;
      height: 160px;
      border: 1px solid var(--alpha-20);
      border-radius: var(--radius-pill);
      background: linear-gradient(135deg, var(--alpha-15) 0%, var(--alpha-05) 100%);
      box-shadow: var(--shadow-lg), inset 0 1px 0 var(--alpha-30);
    }
  }

  .right-section {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background-color: var(--color-surface);
  }

  @include m.below-md {
    .left-section {
      display: none; /* 窄屏只保留表单，避免表单被挤压 */
    }

    .right-section {
      padding: var(--space-5);
    }
  }
}
</style>
