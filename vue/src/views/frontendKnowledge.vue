<template>
  <div class="knowledge-container">
    <div class="header-section">
      <div class="header-content">
        <el-image :src="iconUrl" style="width: 60px; height: 60px" />
        <h4>知识库</h4>
      </div>
    </div>
    <div class="content">
      <!-- 左侧菜单 -->
      <div class="recommend-section">
        <div class="section-title">推荐文章</div>
        <div class="recommend-list">
          <div
            v-for="item in recommendList"
            :key="item.id"
            class="recommend-item"
            @click="goToArticle(item.id)"
          >
            <h4>{{ item.title }}</h4>
            <p class="read-count">
              <el-icon><Histogram /></el-icon>
              阅读量{{ item.readCount }}
            </p>
          </div>
        </div>
      </div>
      <!-- 右侧文章列表 -->
      <div class="article-list">
        <div
          v-for="item in articleList"
          :key="item.id"
          class="article-item"
          @click="goToArticle(item.id)"
        >
          <el-image
            :src="getImage(item.coverImage)"
            style="width: 120px; height: 80px"
          />
          <div class="info">
            <div class="title">
              <h3>{{ item.title }}</h3>
              <el-tag Plain type="primary">{{ item.categoryName }}</el-tag>
            </div>
            <div style="margin-top: 10px" class="flex-box meta-row">
              <div class="flex-box">
                <el-icon><Avatar /></el-icon>
                <span>{{ item.authorName }}</span>
              </div>
              <div class="flex-box">
                <el-icon><List /></el-icon>
                <span>{{ dayjs(item.updateTime).format("YYYY-MM-DD") }}</span>
              </div>
              <div class="flex-box">
                <el-icon><Platform /></el-icon>
                <span>观看人数 {{ item.readCount }}</span>
              </div>
            </div>
          </div>
        </div>
        <!-- 分页组件 -->
        <div class="pagination-wrapper">
          <el-pagination
            style="margin-top: 25px"
            :page-size="pagination.size"
            :current-page="pagination.currentPage"
            layout="prev,pager,next"
            background
            :total="pagination.total"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from "vue";
import { getKnowledgeList } from "@/api/frontend";
import { useRouter } from "vue-router";
import { dayjs } from "element-plus";
import { Platform } from "@element-plus/icons-vue";
const iconUrl = new URL("@/assets/images/book.png", import.meta.url).href;
const router = useRouter();
//获取推荐列表
const recommendList = ref([]);
//右侧列表数据
const pagination = reactive({
  currentPage: 1,
  size: 4,
  total: 0,
});
//右侧列表数据
const articleList = ref([]);
//获取文章图片
const getImage = (url) => {
  return url
    ? `http://159.75.169.224.1235${url}`
    : "https://file.itndedu.com/psychology_ai.png";
};
//获取列表数据
const getPageList = () => {
  const params = {
    sortField: "publice",
    sortDirection: "desc",
    currentPage: pagination.currentPage,
    size: pagination.size,
    status: 1,  // 只显示已发布的文章
  };
  getKnowledgeList(params).then((res) => {
    articleList.value = res.records;
    pagination.total = res.total;
  });
};
//分页组件
const handlePageChange = (val) => {
  pagination.currentPage = val;
  getPageList();
};
//跳转到详情页
const goToArticle = (id) => {
  router.push({
    path: `/knowledge/article/${id}`,
    query: {
      id,
    },
  });
};
onMounted(() => {
  const params = {
    sortField: "readCount",
    sortDirection: "desc",
    currentPage: 1,
    size: 4,
    status: 1,  // 只显示已发布的文章
  };
  getKnowledgeList(params).then((res) => {
    recommendList.value = res.records;
  });
  getPageList();
});
</script>
<style lang="scss" scoped>
.knowledge-container {
  background: linear-gradient(135deg, #fafbfc 0%, #f7f9fc 50%, #f2f6fa 100%);
  .flex-box {
    display: flex;
    align-items: center;
    &.meta-row {
      gap: 20px;
    }
    span {
      margin-left: 10px;
    }
  }
  .header-section {
    background: linear-gradient(135deg, #f59e0b 0%, #8b5cf6 100%);
    color: white;
    padding-left: 300px;
    .header-content {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
  .content {
    display: flex;
    gap: 20px;
    margin: 0 auto;
    width: 1200px;
    padding: 20px;
    .recommend-section {
      width: 280px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
      padding: 15px;
      .section-title {
        font-size: 12;
        font-weight: 600;
        color: #374151;
        margin-bottom: 10px;
        display: flex;
        align-items: center;
        gap: 5px;
      }
      .recommend-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        .recommend-item {
          border-left: 4px solid #f59e0b;
          padding-left: 10px;
          cursor: pointer;
          text-align: left;
          .read-count {
            margin-top: 15px;
            font-size: 12px;
            color: #6b7280;
            display: flex;
            align-items: center;
            gap: 10px;
          }
        }
      }
    }
    .article-list {
      flex: 1;
      .article-item {
        background: white;
        border-radius: 12px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
        padding: 15px;
        margin-bottom: 20px;
        display: flex;
        align-items: center;
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
        }
        .info {
          margin-left: 20px;
          .title {
            display: flex;
            align-items: center;
            gap: 10px;
          }
        }
      }
    }
  }
  .pagination-wrapper {
    display: flex;
    justify-content: center;
    padding-bottom: 30px;
  }
}
</style>
