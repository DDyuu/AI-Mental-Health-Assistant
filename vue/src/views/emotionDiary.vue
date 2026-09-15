<template>
  <div class="emotionDiary-container">
    <div class="header-section">
      <!-- 页面标题 -->
      <div class="header-content">
        <el-image :src="iconUrl" style="width: 60px; height: 60px" />
        <h4>情感日记</h4>
      </div>
      </div>
      <!-- 页面内容 -->
      <div class="content">
        <!-- 情绪评分 -->
        <div class="diary-card">
          <div class="title">今日情绪评分</div>
          <div class="section">
            <p>您今天的整体情绪状态如何？</p>
            <div class="rate">
              <el-rate
                v-model="diaryForm.moodScore"
                :texts="emotionStatus"
                show-texts
                :max="10"
                size="large"
              />
            </div>
          </div>
        </div>
        <!-- 主要情绪 -->
        <div class="diary-card">
          <div class="title">主要情绪</div>
          <div class="emotion-grid">
            <div
              v-for="emotion in emotionDptions"
              :key="emotion.name"
              class="emotion-card"
              :class="{ selected: emotion.name === diaryForm.dominantEmotion }"
              @click="selectEmotion(emotion.name)"
            >
              <el-image :src="emotion.url" style="width: 50px; height: 50px" />
              <div class="emotion-name">{{ emotion.name }}</div>
            </div>
          </div>
        </div>
        <!-- 详情记录 -->
        <div class="diary-card">
          <div class="title">详细记录</div>
          <div class="detail-form">
            <div class="form-group">
              <div class="form-label">情绪触发因素</div>
              <el-input
                v-model="diaryForm.emotionTriggers"
                placeholder="今天什么事情影响了您的情绪？"
                type="textarea"
                :rows="3"
                maxLength="1000"
                show-word-limit
              />
            </div>
            <div class="form-group">
              <div class="form-label">今日感想</div>
              <el-input
                v-model="diaryForm.diaryContent"
                placeholder="写下您今天的想法，感受或发生的有趣事情..."
                type="textarea"
                :rows="3"
                maxLength="2000"
                show-word-limit
              />
            </div>
            <!-- 生活指标 -->
            <div class="life-indicators">
              <div class="indicator-group">
                <div class="form-label">睡眠质量</div>
                <el-select
                  v-model="diaryForm.sleepQuality"
                  placeholder="请选择"
                >
                  <el-option label="很差" value="1"></el-option>
                  <el-option label="较差" value="2"></el-option>
                  <el-option label="一般" value="3"></el-option>
                  <el-option label="良好" value="4"></el-option>
                  <el-option label="优秀" value="5"></el-option>
                </el-select>
              </div>
              <div class="indicator-group">
                <div class="form-label">压力水平</div>
                <el-select v-model="diaryForm.stressLevel" placeholder="请选择">
                  <el-option label="很低" value="1"></el-option>
                  <el-option label="较低" value="2"></el-option>
                  <el-option label="中等" value="3"></el-option>
                  <el-option label="较高" value="4"></el-option>
                  <el-option label="很高" value="5"></el-option>
                </el-select>
              </div>
            </div>
            <div class="action-button">
              <el-button @click="resetForm">重置</el-button>
              <el-button type="primary" @click="submit">提交</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
</template>
<script setup>
import {  reactive } from "vue";
import { dayjs } from "element-plus";
import { addEmotionDiary } from "@/api/frontend";
import { ElMessage } from "element-plus";
//情绪评分
const emotionStatus = [
  "绝望崩溃",
  "消沉抑郁",
  "焦虑烦躁",
  "低落不悦",
  "平静淡然",
  "轻松惬意",
  "愉悦舒心",
  "欢欣满足",
  "兴奋欣喜",
  "极致幸福",
];
//情绪选项
const emotionDptions = [
  {
    name: "开心",
    url: new URL("@/assets/images/开心.png", import.meta.url).href,
  },
  {
    name: "平静",
    url: new URL("@/assets/images/平静.png", import.meta.url).href,
  },
  {
    name: "焦虑",
    url: new URL("@/assets/images/焦虑.png", import.meta.url).href,
  },
  {
    name: "悲伤",
    url: new URL("@/assets/images/悲伤.png", import.meta.url).href,
  },
  {
    name: "兴奋",
    url: new URL("@/assets/images/兴奋.png", import.meta.url).href,
  },
  {
    name: "疲惫",
    url: new URL("@/assets/images/疲惫.png", import.meta.url).href,
  },
  {
    name: "惊讶",
    url: new URL("@/assets/images/惊讶.png", import.meta.url).href,
  },
  {
    name: "困惑",
    url: new URL("@/assets/images/困惑.png", import.meta.url).href,
  },
];
//高亮选择情绪
const selectEmotion = (emotion) => {
  diaryForm.dominantEmotion = emotion;
};
const diaryForm = reactive({
  diaryDate: dayjs().format("YYYY-MM-DD"),
  moodScore: null,
  dominantEmotion: "",
  emotionTriggers: "",
  diaryContent: "",
  sleepQuality: null,
  stressLevel: null,
});

//重置
const resetForm = () => {
  Object.assign(diaryForm, {
    diaryDate: dayjs().format("YYYY-MM-DD"),
    moodScore: null,
    dominantEmotion: "",
    emotionTriggers: "",
    diaryContent: "",
    sleepQuality: null,
    stressLevel: null,
  });
};
//提交
const submit = () => {
  console.log(diaryForm);
  if (!diaryForm.dominantEmotion) {
    ElMessage.error("请选择主要情绪");
    return;
  }
  addEmotionDiary(diaryForm).then(() => {
    ElMessage.success("提交成功");
    resetForm();
  });
};

const iconUrl = new URL("@/assets/images/like.png", import.meta.url).href;
</script>
<style lang="scss" scoped>
.emotionDiary-container {
  background: var(--color-bg);
  .header-section {
    background: linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-accent) 100%);
    color: var(--color-text-inverse);
    padding-left:300px;
    .header-content {
      display: flex;
      align-items: center;
      gap: 12px;
      /* 标题落在主色/accent 渐变上：该渐变最亮端（--color-primary-soft）对纯白只有 3.12:1，
         低于正文 4.5:1 且为色板数学限制（spec §3.1），故标题按大字号档处理（--font-xl = 24px，门槛 3:1）。
         实测标题框内最差 3.25:1（1920 宽）/ 3.29:1（1440 宽），整条渐变最低 3.12:1。
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
        margin-bottom: 20px;
        font-size: 25px;
        font-weight: 600;
        color: var(--color-text);
      }
      .section {
        margin-bottom: 20px;
        p {
          font-size: 15px;
          color: var(--color-text-secondary);
          margin-bottom: 15px;
        }
      }
      .emotion-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        .emotion-card {
          padding: 15px;
          border: 2px solid var(--color-border);
          border-radius: var(--radius-lg);
          text-align: center;
          cursor: pointer;
          background: var(--color-border-light);
          .emotion-name {
            margin-top: 10px;
            padding: 0 75px;
            color: var(--color-text);
          }
          /* 选中态：主色描边 + 主色浅底（Task 12 沿用同一约定）。
             描边即状态指示，对卡片白底 5.16:1、对浅底 4.56:1，均 ≥3:1；
             未选中态的 --color-border 描边属 spec §3.5 例外二（中性结构描边）。 */
          &.selected {
            border-color: var(--color-primary);
            background: var(--color-primary-light);
            transform: translateY(-3px);
          }
        }
      }
      .detail-form {
        .form-label {
          margin: 10px 0;
          color: var(--color-text);
        }
        .life-indicators {
          display: flex;
          gap: 20px;
          .indicator-group {
            flex: 1;
          }
        }
        .action-buttons {
          margin-top: 40px;
        }
      }
    }
  }
}
</style>
