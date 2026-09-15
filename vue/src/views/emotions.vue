<template> 
  <div>
    <PageHead title="情感日志" />
    <TableSearch :formItem="formItem" @search="handleSearch" />
    <!-- 列表数据-->
    <el-table :data="tableData" style="width: 100%">
      <el-table-column prop="id" label="用户ID" width="80" />
      <el-table-column label="会话ID" width="80">
        <template #default="scope">
          <el-avatar >{{ scope.row.nickname }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="diaryDate" label="记录日期" width="120"/>
      <el-table-column label="情绪评分" >
        <template #default="scope">
          <el-rate :model-value="scope.row.moodScore" :max="10" disabled/>
        </template>
      </el-table-column>
      <el-table-column label="生活指标" width="80">
        <template #default="scope">
          <div>
            <p>
              睡眠：{{ scope.row.sleepQuality }}
            </p>
            <p>
              压力：{{ scope.row.stressLevel }}
            </p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="emotionTriggers" label="情绪触发因素" width="120"/>
      <el-table-column prop="diaryContent" label="日记内容" width="250"/>
      <el-table-column label="操作" width="160" fixed="right"> 
        <template #default="scope">
          <el-button @click="viewSessionDetail(scope.row)" text type="primary">详情</el-button>
          <el-button @click="handleDelete(scope.row)" text type="danger">删除</el-button>
        </template>
      </el-table-column> 
    </el-table>
    <!-- 分页数据-->
    <el-pagination 
      style="margin-top: 25px;"
      :page-size="pagination.size"
      :current-page="pagination.currentPage"
      layout="prev,pager,next"
      :total="pagination.total"
      @current-change="handlePageChange"
    />
    <!-- 详情弹窗-->
    <el-dialog
      v-model="detailDialogVisible"
      title="情感日志详情" 
      width="800px"
    >
      <div class="detail-content" v-if="currentDetail">
        <div class="detail-section">
          <h4>用户信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户名">{{ currentDetail.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ currentDetail.nickname }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ currentDetail.userId }}</el-descriptions-item>
            <el-descriptions-item label="记录日期">{{ currentDetail.diaryDate }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="detail-section">
          <h4>情绪状态</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="情绪评分">
              <el-rate :model-value="currentDetail.moodScore" :max="10" disabled/>
            </el-descriptions-item>
            <el-descriptions-item label="主要情绪">
              <el-tag :type="getEmotionTagType(currentDetail.dominantEmotion)">{{ currentDetail.dominantEmotion||'-'  }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="睡眠质量">{{ currentDetail.sleepQuality || '-' }}</el-descriptions-item>
            <el-descriptions-item label="压力水平">{{ currentDetail.stressLevel || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="detail-section">
          <h4>日记内容</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="情绪触发因素">{{ currentDetail.emotionTriggers || '无' }}</el-descriptions-item>
            <el-descriptions-item label="日记内容">{{ currentDetail.diaryContent || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="detail-section">
          <h4>AI情绪分析结果</h4>
          <div class="ai-analysis-result">
            <el-descriptions :column="2" border>
            <el-descriptions-item label="主要情绪">
              <el-tag :type="getAiEmotionTagType(aiData.primaryEmotion)">{{ aiData.primaryEmotion || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="情绪强度">
              <el-progress :percentage="aiData.emotionScore" :color="getEmotionScoreColor(aiData.emotionScore)" :stroke-width="8" />
            </el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="getRiskLevelTagType(aiData.riskLevel)">{{ getRiskLevelText(aiData.riskLevel) || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="情绪性质">
              <el-tag :type="aiData.isNegative ? 'danger' : 'success'">{{ aiData.isNegative ? '负面情绪' : '正面情绪' }}</el-tag>
            </el-descriptions-item>
            </el-descriptions>
            <div class="ai-suggestion-section">
          <h5>专业建议</h5>
          <div class="suggestion-content">{{ aiData.suggestion || '无' }}</div>
            </div>
            <div class="ai-risk-section">
              <h5>风险描述</h5>
              <div class="risk-content">{{ aiData.riskDescription || '无' }}</div>
            </div>
            <div class="ai-improvements-section">
              <h5>改善建议</h5>
              <ul class="improvement-list">
              <li v-for="item in aiData.improvementSuggestions" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="detail-section">
          <h4>时间信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="创建时间">{{ currentDetail.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ currentDetail.updatedAt }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <div class="emotion-container"></div>
  </div>
</template>
<script setup> 
import { onMounted, ref, reactive } from 'vue'
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import { getEmotionPage,deleteEmotion } from '@/api/admin'
import { chartColors } from '@/styles/chart-palette'
import {ElMessageBox,ElMessage} from 'element-plus'
// 标签颜色
const getEmotionTagType = (emotion) => {
  const emotionTypes = {
    '快乐': 'success',
    '平静': 'info',
    '兴奋': 'warning',
    '愤怒': 'danger',
    '悲伤': 'info',
    '焦虑': 'warning'
  }
  return emotionTypes[emotion] || 'info'
}

const getAiEmotionTagType = (emotion) => {
  const emotionTagMap = {
    '快乐': 'success',
    '平静': 'success',
    '兴奋': 'warning',
    '满足': 'success',
    '愤怒': 'danger',
    '悲伤': 'info',
    '焦虑': 'warning',
    '恐惧': 'danger',
    '沮丧': 'info',
    '压力': 'warning'
  }
  return emotionTagMap[emotion] || 'info'
}

const getEmotionScoreColor = (score) => {
  if (score >= 80) return chartColors.danger
  if (score >= 60) return chartColors.warning
  if (score >= 40) return chartColors.textSecondary
  return chartColors.success
}

const getRiskLevelTagType = (riskLevel) => {
  const riskTagMap = {
    0: 'success',
    1: 'info',
    2: 'warning',
    3: 'danger'
  }
  return riskTagMap[riskLevel] || 'info'
}

const getRiskLevelText = (riskLevel) => {
  const riskTextMap = {
    0: '正常',
    1: '关注',
    2: '预警',
    3: '危机'
  }
  return riskTextMap[riskLevel] || '未知风险等级'
}
// 表单数据
const formItem = [
  {
    comp:'input',//输入框
    prop: 'userId',//属性名
    label: '用户ID',
    placeholder: '请输入用户ID',
  },
  {
    comp:'select',//下拉选择框
    prop: 'moodScreRange',
    label: '情绪评分',
    placeholder: '请选择评分范围',
    options:[
      {label:'低分(1-3分)',value:'1-3'},
      {label:'中分(4-6分)',value:'4-6'},
      {label:'高分(7-10分)',value:'7-10'},
    ]
  },
]
//列表数据
const tableData = ref([])
// 分页
const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
})
const handlePageChange = (page) => {
  pagination.currentPage = page;
  handleSearch();
}
// 获取列表数据
const handleSearch = async (formData) => {
  const params = {
    ...pagination,
    ...formData,
  }
  const res = await getEmotionPage(params)
  console.log('getEmotionPage返回数据:', res);
  tableData.value = res.records
  pagination.total = res.total
}

// 详情
const detailDialogVisible = ref(false)
const currentDetail = ref(null)
const aiData=ref(null)
const viewSessionDetail = (row) => {
  currentDetail.value = row
  if(row.aiEmotionAnalysis){
    aiData.value=JSON.parse(row.aiEmotionAnalysis)
  }else{
    aiData.value={}
  }
  detailDialogVisible.value = true
  console.log('AI情绪分析结果:', aiData.value);
}
// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该条记录吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'danger'
  }).then(async () => {
    await deleteEmotion(row.id)
    ElMessage.success('删除成功')
    handleSearch()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

onMounted(() => {
  handleSearch()
})
</script>
<style lang="scss" scoped>
.detail-content {
  .detail-section {
    margin-bottom: 24px;
    
    h4 {
      margin: 0 0 16px 0;
      color: var(--color-text);
      font-size: 16px;
      
      i {
        margin-right: 8px;
        color: var(--color-primary);
      }
    }
  }
}

// AI分析相关样式
.ai-analysis-status {
  .ai-status-tag {
    margin-bottom: 4px;
    
    i {
      margin-right: 4px;
    }
  }
  
  .ai-analysis-preview {
    font-size: 11px;
    /* brief 原映射为 --color-text-placeholder，但此处是 11px 说明文字（非表单占位符），
       该令牌白底 2.47:1 < 4.5:1；按已记录的裁定（progress.md 第 185 行，
       与 Task 7 元信息灰字同类处置）改用 --color-text-secondary = 5.29:1。 */
    color: var(--color-text-secondary);
    margin-top: 2px;
  }
}

.ai-analysis-result {
  .ai-keywords-section,
  .ai-suggestion-section,
  .ai-risk-section,
  .ai-improvements-section {
    margin-top: 16px;
    padding: 12px;
    background-color: var(--color-border-light);
    border-radius: 4px;
    
    h5 {
      margin: 0 0 8px 0;
      color: var(--color-text-secondary);
      font-size: 14px;
      font-weight: 600;
      
      i {
        margin-right: 6px;
        color: var(--color-text-secondary);
      }
    }
  }
  
  .keywords-container {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    
    /* 文字用 --color-text 而非 brief 的 --color-success：后者落在 --color-success-light 上实测
       4.41:1（12px 文字需 4.5:1）；与 consultation.vue `.error-message` 的既有裁定同构
       （深阶语义色作描边 4.41:1 ≥ 3:1，文字改深色）。语义由描边承载。 */
    .keyword-tag {
      background-color: var(--color-success-light);
      color: var(--color-text);
      border-color: var(--color-success);
    }
  }
  
  .suggestion-content,
  .risk-content {
    line-height: 1.6;
    color: var(--color-text-secondary);
    background-color: var(--color-surface);
    padding: 8px;
    border-radius: 4px;
    border: 1px solid var(--color-border);
  }
  
  .improvement-list {
    margin: 0;
    padding-left: 20px;
    
    li {
      margin-bottom: 4px;
      color: var(--color-text-secondary);
      line-height: 1.5;
    }
  }
  
  .ai-analysis-meta {
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid var(--color-border);
    
    .analysis-time {
      margin: 0;
      font-size: 12px;
      color: var(--color-text-secondary);
      
      i {
        margin-right: 4px;
      }
    }
  }
  
  /* brief 要求清除全站强制覆盖，该声明原先的最高优先级强制覆盖已一并移除，且未用新的
     强制覆盖替代（本文件此后不含任何强制覆盖声明）。
     注：本仓库把 `.vue` 中该 CSS 关键字的出现次数当作 0 值硬指标，在注释里写出它会让这个
     计数不再等于"实际声明数"；故此处的措辞是明示取舍，不是换个拼写绕过度量。
     本选择器特异性 (0,3,0)（scoped 加属性选择器后 (0,4,0)）已高于 Element 自身的
     `.el-progress__text` (0,1,0)，不存在"输给低特异性规则"的情况。
     实测该 12px 从未生效，移除强制覆盖不改变渲染结果：
     1) el-progress 把字号写成行内 style（12 + strokeWidth*0.4 = 15.2px），行内样式优先于任何
        选择器，只有最高优先级的强制声明能盖过它；
     2) `.el-progress__text` 由子组件渲染，不带本组件的 scope 属性，
        `.el-progress .el-progress__text[data-v-*]` 实际不命中。 */
  .el-progress {
    .el-progress__text {
      font-size: 12px;
    }
  }
}
</style>