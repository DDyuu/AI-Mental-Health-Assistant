<template>
  <div class="home-container">
    <div class="content">
      <div class="text">
        <h2 class="title">
          一次温暖的对话<br />
          <span class="highlight-text">化孤独为慰籍</span>
        </h2>
        <p class="description">
          每个深夜，每个焦虑的时刻，我们都在这里，不必独自承受，让心与心的连接温暖您的每一天
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/consultation')"
            >开始倾诉，获得陪伴</el-button
          >
          <el-button
            class="ghost-btn"
            size="large"
            plain
            @click="$router.push('/emotionDiary')"
            >记录心情，释放情感</el-button
          >
        </div>
      </div>
      <div class="robot">
        <el-image class="robot-img" :src="iconUrl" alt="机器人" />
      </div>
    </div>

    <div class="trust-points">
      <div class="trust-item" v-for="item in trustPoints" :key="item.title">
        <span class="trust-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <h3 class="trust-title">{{ item.title }}</h3>
        <p class="trust-desc">{{ item.desc }}</p>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue";
const iconUrl = new URL("@/assets/images/robot.svg", import.meta.url).href;
const trustPoints = [
  {
    icon: 'Lock',
    title: '隐私保护',
    desc: '对话内容加密存储，只有你可以看到'
  },
  {
    icon: 'Clock',
    title: '随时可用',
    desc: '深夜、通勤、焦虑来袭，随时打开就能说'
  },
  {
    icon: 'Reading',
    title: '专业内容',
    desc: '由心理学知识库支撑的回应与自助内容'
  }
]
</script>
<style lang="scss" scoped>
@use '../styles/mixins' as m;

.home-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-8);
  padding: var(--space-8) var(--space-5);
  background: linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-primary-dark) 100%);
  color: var(--color-text-inverse);

  .content {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-6);
    flex-wrap: wrap;
    width: 100%;
    max-width: 1200px;

    .text {
      flex: 1;
      min-width: 300px;
      max-width: 520px;

      .title {
        margin-bottom: var(--space-4);
        font-size: var(--font-3xl);
        font-weight: 700;
        color: var(--color-text-inverse);

        .highlight-text {
          /* 44px 属大字号档（≥3:1）。--color-accent-light 在渐变最亮处只有 2.78:1，
             只在其实际所在的中段位置（≈3.5:1）达标。实施时必须实测该元素的实际背景对比度：
             若不足 3:1，改用 var(--color-text-inverse)（全渐变最低 3.12:1）并保留 font-weight: 700。 */
          color: var(--color-accent-light);
        }
      }

      .description {
        /* 24px 纯白：进入大字号档（门槛 3:1）。18px + --alpha-90 在柔和渐变上仅 3.58:1，不满足正文的 4.5:1。
           纯白在整条渐变上最低 3.119:1（Task 2 复审实测），故 24px 档安全。 */
        font-size: var(--font-xl);
        line-height: 1.7;
        color: var(--color-text-inverse);
      }

      .hero-actions {
        display: flex;
        flex-wrap: wrap;
        gap: var(--space-3);
        margin-top: var(--space-6);
      }

      .ghost-btn {
        background: transparent;
        border-color: var(--alpha-60);
        color: var(--color-text-inverse);

        &:hover,
        &:focus {
          background: var(--alpha-10);
          border-color: var(--color-text-inverse);
          color: var(--color-text-inverse);
        }
      }
    }

    .robot {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 260px;
      height: 260px;
      border: 1px solid var(--alpha-20);
      border-radius: var(--radius-pill);
      background: linear-gradient(135deg, var(--alpha-15) 0%, var(--alpha-05) 100%);
      box-shadow: var(--shadow-lg), inset 0 1px 0 var(--alpha-30);
      flex-shrink: 0;

      .robot-img {
        width: 150px;
        height: 150px;
      }
    }
  }

  .trust-points {
    display: flex;
    justify-content: center;
    gap: var(--space-5);
    flex-wrap: wrap;
    width: 100%;
    max-width: 1200px;

    .trust-item {
      flex: 1 1 220px;
      max-width: 320px;
      padding: var(--space-5);
      /* 95% 白卡而非 10% 白玻璃：玻璃底上的浅色文字无法达到 4.5:1（14px 正文）。
         近白卡底让深色文字拿到 ≈11:1，且视觉上仍是"浮在渐变上的玻璃卡"。 */
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      background: var(--alpha-95);

      .trust-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        margin-bottom: var(--space-3);
        border-radius: var(--radius-md);
        background: var(--color-primary-light);
        font-size: var(--font-lg);
        color: var(--color-primary);
      }

      .trust-title {
        margin-bottom: var(--space-2);
        font-size: var(--font-md);
        font-weight: 600;
        color: var(--color-text);
      }

      .trust-desc {
        font-size: var(--font-sm);
        line-height: 1.7;
        color: var(--color-text-secondary);
      }
    }
  }

  @include m.below-sm {
    gap: var(--space-6);
    padding: var(--space-6) var(--space-4);

    .content {
      flex-direction: column-reverse;
      text-align: center;

      .text {
        .title {
          font-size: var(--font-2xl);
        }

        .hero-actions {
          justify-content: center;
        }
      }

      .robot {
        width: 180px;
        height: 180px;

        .robot-img {
          width: 104px;
          height: 104px;
        }
      }
    }
  }
}
</style>
