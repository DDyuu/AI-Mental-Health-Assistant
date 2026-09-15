<template>
  <div class="articleDetail-container">
    <!-- 文章详情标题 -->
    <div class="header-section">
      <div class="header-content">
        <el-image :src="iconUrl" style="width: 60px; height: 60px" />
        <h4>知识文章详情</h4>
      </div>
    </div>
    <!-- 文章详情内容 -->
    <div class="content">
      <div class="diary-card">
        <p class="title">文章信息</p>
        <div class="sub-title">
          <el-tag size="large" color="category-tag">
            {{ articleDetail.categoryName }}
          </el-tag>
          <div class="flex-box">
            <el-icon><List /></el-icon>
            <span>{{
              dayjs(articleDetail.updateTime).format("YYYY-MM-DD")
            }}</span>
          </div>
        </div>
        <h1 class="article-title">{{ articleDetail.title }}</h1>
        <div class="summary-content" v-if="articleDetail.summary">
          {{ articleDetail.summary }}
        </div>
        <div :style="{ marginTop: '20px' }" class="flex-box">
          <div class="item flex-box">
            <el-icon><Avatar /></el-icon>
            <span>{{ articleDetail.authorName }}</span>
          </div>
          <div class="item flex-box">
            <el-icon><Platform /></el-icon>
            <span>{{ articleDetail.readCount }}次阅读</span>
          </div>
        </div>
      </div>
      <div class="diary-card">
        <div class="title">正文内容</div>
        <div
          class="content-wrapper"
          v-html="formatContent(articleDetail.content)"
        ></div>
        <div
          class="tags-content"
          v-if="articleDetail.tagArray && articleDetail.tagArray.length"
        >
          <h4 class="tags-title">相关标签</h4>
          <div class="tags-list">
            <el-tag
              v-for="tag in articleDetail.tagArray"
              :key="tag"
              type="info"
              effect="light"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue";
import { getKnowledgeDetail } from "@/api/frontend";
import { dayjs } from "element-plus";
import { Avatar, Platform } from "@element-plus/icons-vue";
const iconUrl = new URL("@/assets/images/book.png", import.meta.url).href;
//获取路径参数
const props = defineProps({
  id: String,
});
const articleDetail = ref({});
//基本的HTML清理和格式化方法
const formatContent = (content) => {
  if (!content) return "";

  // 基本的HTML清理和格式化
  let formatted = content
    .replace(/\n/g, "<br>")
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.*?)\*/g, "<em>$1</em>");

  return formatted;
};
onMounted(() => {
  getKnowledgeDetail(props.id).then((res) => {
    articleDetail.value = res;
  });
  console.log(articleDetail.value, "文章详情");
});
</script>
<style lang="scss" scoped>
.articleDetail-container {
  background: var(--color-bg);
  .flex-box {
    display: flex;
    align-items: center;
    .item {
      margin-right: 20px;
      span {
        margin-left: 5px;
      }
    }
  }
  .header-section {
    background: linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-primary-dark) 100%);
    color: var(--color-text-inverse);
    padding-left: 300px;
    .header-content {
      display: flex;
      align-items: center;
      gap: 12px;
      /* 同 frontendKnowledge.vue：标题落在主色渐变上，纯白对最亮端只有 3.12:1，
         故按大字号档（--font-xl = 24px，门槛 3:1）处理；实测标题框内最差 3.67:1（1920 宽）/ 3.87:1（1440 宽）。
         base.scss 的 h4 规则会盖掉父级继承色，必须显式声明。 */
      h4 {
        font-size: var(--font-xl);
        color: var(--color-text-inverse);
      }
    }
  }
  .content {
    margin: 0 auto;
    width: 980px;
    padding: 20px;
    .diary-card {
      margin-bottom: 20px;
      background: var(--color-surface);
      border-radius: var(--radius-lg);
      padding: 20px;
      box-shadow: 0 4px 6px var(--scrim-04);
      .title {
        margin-bottom: 15px;
        font-size: 20px;
        font-weight: 600;
        color: var(--color-text);
      }
      .sub-title {
        margin-top: 20px;
        display: flex;
        align-items: center;
        .category-tag {
          margin-right: 20px;
        }
      }
      .article-title {
        font-size: 28px;
        font-weight: bold;
        color: var(--color-text);
        margin-top: 30px;
        margin-bottom: 20px;
      }
      .summary-content {
        background: var(--color-primary-wash);
        border-left: 4px solid var(--color-accent);
        padding: 10px 15px;
        border-radius: 0 8px 8px 0;
        position: relative;
        text-align: left;
      }
      .content-wrapper {
        font-size: 15px;
        color: var(--color-text);
        text-align: left;
        :deep(p) {
          margin-bottom: 10px;
        }
        :deep(h1),
        :deep(h2),
        :deep(h3),
        :deep(h4),
        :deep(h5),
        :deep(h6) {
          margin: 15px 0 10px;
          color: var(--color-text);
          font-weight: 600;
        }
        :deep(h2) {
          font-size: 15px;
          border-bottom: 2px solid var(--color-border);
          padding-bottom: 5px;
        }
        :deep(h3) {
          font-size: 13px;
        }
        :deep(ul),
        :deep(ol) {
          padding-left: 15px;
          margin-bottom: 10px;
        }
        :deep(li) {
          margin-bottom: 5px;
        }
      }
      .tags-content {
        margin-top: 20px;
        padding-top: 15px;
        border-top: 1px solid var(--color-border);
        .tags-title {
          margin-bottom: 10px;
          font-size: 14px;
          font-weight: 600;
          color: var(--color-text);
        }
        .tags-list {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
        }
      }
    }
  }
}
</style>
