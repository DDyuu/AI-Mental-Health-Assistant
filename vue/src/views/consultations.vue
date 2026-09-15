<template> 
  <div style="height: 100%; display: flex; flex-direction: column; margin-bottom: 20px; padding-bottom: 20px;">
    <PageHead title="咨询记录" />
    <el-table :data="tableData" style="width: 100%; flex: 1;" height="calc(100vh - 220px)">
      <el-table-column label="用户名" width="200" >
        <template #default="scope">
          <p>{{ scope.row.userNickname }}</p>
        </template>
      </el-table-column>
      <el-table-column label="情绪日志" min-width="300">
        <template #default="scope">
          <div class="session-title">{{ scope.row.sessionTitle }}</div>
          <div class="session-preview">{{ scope.row.lastMessageContent }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="messageCount" label="消息数" width="100" />
      <el-table-column label="时间" width="180">
        <template #default="scope">
          <span>{{ formatTime(scope.row.lastMessageTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" >
        <template #default="scope">
          <el-button type="primary" text @click="viewSessionDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination 
      style="margin-top: 25px;"
      :page-size="pagination.size"
      :current-page="pagination.currentPage"
      layout="prev,pager,next"
      :total="pagination.total"
      @current-change="handlePageChange"
    />
    <el-dialog 
      v-model="showDetailDialog"
      title="咨询会话详情"
      width="70%"
      align-center
    >
      <div class="session-detail">
        <div class="detail-header">
          <div class="detail-row">
            <div class="detail-label">用户:</div>
            <div class="detail-value">{{ sessionDetail.userNickname }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-label">开始时间:</div>
            <div class="detail-value">{{ formatTime(sessionDetail.startedAt) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-label">消息数:</div>
            <div class="detail-value">{{ sessionDetail.messageCount }}</div>
          </div>
        </div>
        <div class="messages-container">
          <div class="messages-header">
            <h4>对话记录</h4>
          </div>
          <div class="messages-list" v-loading="loadingMessages">
            <div v-for="message in sessionMessages" :key="message.Id" class="message-item" :class="message.senderType === 1 ? 'user-message' : 'ai-message'">
                <div class="message-header">
                  <span class="sender">{{ message.senderType === 1 ? '用户' : 'AI助手' }}</span>
                  <span class="time">{{ formatTime(message.createdAt) }}</span>
                </div>
                <div class="message-content">{{ message.content }}</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted ,ref, reactive} from 'vue';
import { getConsultationPage,getSessionDetail } from '@/api/admin';
import PageHead from '@/components/PageHead.vue';
const tableData = ref([])
//
const showDetailDialog = ref(false)
const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
})
// 查看会话详情
const sessionDetail = ref('')
const sessionMessages = ref([])
const loadingMessages = ref(false)

const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

const viewSessionDetail = (row) => {
  loadingMessages.value = true
  showDetailDialog.value = true
  getSessionDetail(row.id).then(res => {
    loadingMessages.value = false
    sessionMessages.value = res
    sessionDetail.value = row
  })
  console.log(sessionMessages.value);
}

// 分页
const handlePageChange = (page) => {
  pagination.currentPage = page
  handleSearch()
}
// 搜索
const handleSearch=()=>{
  getConsultationPage(pagination).then(res => {
    const {records,total} = res
    tableData.value = records
    pagination.total = total
  })
}
// 获取数据
onMounted(() => {
  handleSearch()
})
</script>
<style lang="scss" scoped>
.session-title {
    font-weight: 500;
    color: var(--color-text);
    margin-bottom: 4px;
  }
  .session-preview {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: 4px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .session-detail {
    max-height: 70vh;
    overflow-y: auto;
    .detail-header {
      margin-bottom: 20px;
      padding: 16px;
      background: var(--color-border-light);
      border-radius: 8px;
      border: 1px solid var(--color-border);
    }

    .detail-row {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      :last-child {
        margin-bottom: 0;
      }
      .detail-label {
        font-weight: 500;
        color: var(--color-text-secondary);
        min-width: 80px;
        margin-right: 8px;
      }

      .detail-value {
        color: var(--color-text);
      }
    }

    .messages-container {
      margin-top: 20px;
      .messages-header {
        margin-bottom: 16px;
        h4 {
          margin: 0;
          color: var(--color-text);
          font-size: 16px;
          font-weight: 500;
        }
      }
      .messages-list {
        max-height: 400px;
        overflow-y: auto;
        border: 1px solid var(--color-border);
        border-radius: 8px;
        padding: 16px;
        background: var(--color-surface);
        .message-item {
          margin-bottom: 12px;
          padding: 12px;
          border-radius: 8px;
          background: var(--color-border-light);
          border: 1px solid var(--color-border);
          &:last-child {
            margin-bottom: 0;
          }
          /* 气泡配色与 /consultation 实时对话页（Task 7 接口）保持一致：
             另一侧 = surface 底 + 中性描边 + 主文字，用户侧 = 主色底 + inverse 字。 */
          &.user-message {
            background: var(--color-primary);
            color: var(--color-text-inverse);
            /* 显式重置描边与阴影：基础 .message-item 带中性描边，
               不清掉用户气泡就会带上“另一侧”白底描边的观感。 */
            border: none;
            box-shadow: none;

            /* 下方 .sender/.time/.message-content 各自声明了深色，特异性更低但需在此一并覆盖，
               否则用户气泡内的文字仍是深色，落在主色底上不可读。 */
            .message-header {
              .sender,
              .time {
                color: var(--color-text-inverse);
              }
            }

            .message-content {
              color: var(--color-text-inverse);
            }
          }
          &.ai-message {
            background: var(--color-surface);
            border: 1px solid var(--color-border);
            color: var(--color-text);
          }
          .message-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
            .sender {
              font-weight: 500;
              color: var(--color-text);
              display: flex;
              align-items: center;
              gap: 4px;
            }
            .time {
              font-size: 12px;
              color: var(--color-text-secondary);
            }
          }
          .message-content {
            color: var(--color-text);
            line-height: 1.6;
            white-space: pre-wrap;
            margin-top: 8px;
            font-size: 14px;
          }
        }
      }
    }
  }
</style>