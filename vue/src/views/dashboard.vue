<template>
  <div class="dashboard-container">
    <!-- 系统统计卡片 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card v-if="aiData.systemOverview">
          <div class="card-content">
            <div class="avatar users">
              <el-image style="width: 40px; height: 40px" :src="iconUrli" />
            </div>
            <div class="info">
              <p class="title">总用户数</p>
              <p class="number">{{ aiData.systemOverview.totalUsers }}</p>
              <p class="subtitle-title">
                活跃用户: {{ aiData.systemOverview.activeUsers }}
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card v-if="aiData.systemOverview">
          <div class="card-content">
            <div class="avatar like">
              <el-image style="width: 40px; height: 40px" :src="iconLike" />
            </div>
            <div class="info">
              <p class="title">情绪日志</p>
              <p class="number">{{ aiData.systemOverview.totalDiaries }}</p>
              <p class="subtitle-title">
                今日新增: {{ aiData.systemOverview.todayNewDiaries }}
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card v-if="aiData.systemOverview">
          <div class="card-content">
            <div class="avatar comments">
              <el-image style="width: 40px; height: 40px" :src="iconComments" />
            </div>
            <div class="info">
              <p class="title">咨询会话</p>
              <p class="number">{{ aiData.systemOverview.totalSessions }}</p>
              <p class="subtitle-title">
                今日新增: {{ aiData.systemOverview.todayNewSessions }}
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card v-if="aiData.systemOverview">
          <div class="card-content">
            <div class="avatar smile">
              <el-image style="width: 40px; height: 40px" :src="iconSmile" />
            </div>
            <div class="info">
              <p class="title">平均情绪</p>
              <p class="number">{{ aiData.systemOverview.avgMoodScore }}/10</p>
              <p class="subtitle-title">情绪健康指数</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 趋势分析卡片 -->
    <el-row style="margin-top: 20px" :gutter="20">
      <!-- 情绪趋势分析卡片 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card style="width: 100%">
          <!-- 卡片里自定义标题用具名插槽的写法 -->
          <template #header>
            <div class="card-header">情绪趋势分析</div>
          </template>
          <div class="chart-content">
            <div ref="emotionChartRef" style="width: 100%; height: 300px"></div>
          </div>
        </el-card>
      </el-col>
      <!-- 咨询会话统计卡片 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card style="width: 100%">
          <template #header>
            <div class="card-header">咨询会话统计</div>
          </template>
          <div class="chart-content">
            <div v-if="aiData.consultationStats" class="consultation-stats">
              <div class="stat-item">
                <div class="stat-label">总会话数</div>
                <div class="stat-value">
                  {{ aiData.consultationStats?.totalSessions || 0 }}
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-label">平均时长</div>
                <div class="stat-value">
                  {{ aiData.consultationStats?.avgDurationMinutes || 0 }}
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-label">活跃用户</div>
                <div class="stat-value">
                  {{ aiData.systemOverview?.activeUsers || 0 }}
                </div>
              </div>
            </div>
            <div
              ref="consultationChartRef"
              style="width: 100%; height: 260px"
            ></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 用户活跃度趋势卡片 -->
    <el-row style="margin-top: 20px">
      <el-col :xs="24" :sm="24" :md="24" :lg="24">
        <el-card style="width: 100%">
          <!-- 卡片里自定义标题用具名插槽的写法 -->
          <template #header>
            <div class="card-header">用户活跃度趋势</div>
          </template>
          <div class="chart-content">
            <div
              ref="userActivityChartRef"
              style="width: 100%; height: 300px"
            ></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue";
import { getAnalyticsOverview } from "@/api/admin";
import * as echarts from "echarts";
import { chartColors } from "@/styles/chart-palette";
//统计图片引入
const iconUrli = new URL("@/assets/images/users.png", import.meta.url).href;
const iconLike = new URL("@/assets/images/like.png", import.meta.url).href;
const iconComments = new URL("@/assets/images/comments.png", import.meta.url)
  .href;
const iconSmile = new URL("@/assets/images/smile.png", import.meta.url).href;
//控制台数据
const aiData = ref({});

// 初始化图标
const initCharts = () => {
  initEmotionChart();
  initConsultationChart();
  initUserActivityChart();
};
//情绪趋势分析
const emotionChart = ref(null);
const emotionChartRef = ref(null);
const initEmotionChart = () => {
  if (!emotionChartRef.value) return;

  // 确保DOM元素有宽度和高度
  const dom = emotionChartRef.value;
  if (!dom || dom.clientWidth === 0 || dom.clientHeight === 0) {
    // 延迟执行，确保DOM已经渲染
    setTimeout(initEmotionChart, 100);
    return;
  }

  // 确保情绪趋势数据存在
  if (!aiData.value || !aiData.value.emotionTrend) return;

  //销毁现有的图表
  if (emotionChart.value) emotionChart.value.dispose();

  //创建echarts实例
  emotionChart.value = echarts.init(dom);

  //获取情绪趋势的数据
  const TrendData = aiData.value.emotionTrend;

  //配置图表
  const option = {
    title: {
      text: "情绪趋势分析",
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: chartColors.text,
      },
      left: "center",
      top: 10,
    },
    tooltip: {
      // 提示框
      trigger: "axis", // 触发类型：坐标轴触发
      borderColor: chartColors.accent,
      borderWidth: 1,
      textStyle: {
        color: chartColors.text,
      },
    },
    legend: {
      // 图例组件
      data: ["平均情绪评分", "记录数量"],
      top: 40,
    },
    grid: {
      // 设置显示容器位置
      left: "3%",
      right: "4%",
      bottom: "3%",
      top: 80,
    },
    xAxis: {
      type: "category",
      data: TrendData.map((item) => item.date),
      axisLine: {
        lineStyle: {
          color: chartColors.accent,
        },
      },
    },
    yAxis: [
      {
        type: "value",
        name: "情绪评分",
        position: "left",
        axisLabel: {
          color: chartColors.textSecondary,
        },
        axisLine: {
          lineStyle: {
            color: chartColors.accent,
          },
        },
        splitLine: {
          lineStyle: {
            color: chartColors.accentLight,
          },
        },
      },
      {
        type: "value",
        name: "记录数量",
        position: "right",
        axisLabel: {
          color: chartColors.textSecondary,
        },
        axisLine: {
          lineStyle: {
            color: chartColors.accent,
          },
        },
        splitLine: {
          show: false,
        },
      },
    ],
    series: [
      {
        name: "平均情绪评分",
        type: "line", // 折线图
        data: TrendData.map((item) => item.avgMoodScore),
        smooth: true, // 平滑曲线
        lineStyle: {
          width: 3,
          color: chartColors.warning,
        },
        itemStyle: {
          color: chartColors.warning,
        },
      },
      {
        name: "记录数量",
        type: "line", // 折线图
        data: TrendData.map((item) => item.recordCount),
        smooth: true,
        lineStyle: {
          width: 3,
          color: chartColors.accent,
        },
        itemStyle: {
          color: chartColors.accent,
        },
      },
    ],
  };

  //设置图表选项
  emotionChart.value.setOption(option);
};

//咨询会话统计
const consultationChart = ref(null);
const consultationChartRef = ref(null);
const initConsultationChart = () => {
  if (!consultationChartRef.value) return;

  // 确保DOM元素有宽度和高度
  const dom = consultationChartRef.value;
  if (!dom || dom.clientWidth === 0 || dom.clientHeight === 0) {
    // 延迟执行，确保DOM已经渲染
    setTimeout(initConsultationChart, 100);
    return;
  }

  // 确保会话统计数据存在
  if (
    !aiData.value ||
    !aiData.value.consultationStats ||
    !aiData.value.consultationStats.dailyTrend
  )
    return;

  //销毁现有的图表
  if (consultationChart.value) consultationChart.value.dispose();

  //创建echarts实例
  consultationChart.value = echarts.init(dom);

  //获取会话统计的数据
  const dailyTrend = aiData.value.consultationStats.dailyTrend;
  const option = {
    title: {
      text: "咨询活动统计",
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: chartColors.text,
      },
      left: "center",
      top: 10,
    },
    tooltip: {
      trigger: "axis",
      backgroundColor: chartColors.surface,
      borderColor: chartColors.accent,
      borderWidth: 1,
      textStyle: {
        color: chartColors.text,
      },
    },
    legend: {
      data: ["会话数量", "参与用户数"],
      top: 40,
      textStyle: {
        color: chartColors.textSecondary,
      },
    },
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      top: 80,
      containLabel: true,
    },
    xAxis: {
      type: "category",
      data: dailyTrend.map((item) => item.date),
      axisLine: {
        lineStyle: {
          color: chartColors.accent,
        },
      },
      axisLabel: {
        color: chartColors.textSecondary,
      },
    },
    yAxis: {
      type: "value",
      axisLabel: {
        color: chartColors.textSecondary,
      },
      axisLine: {
        lineStyle: {
          color: chartColors.accent,
        },
      },
      splitLine: {
        lineStyle: {
          color: chartColors.accentLight,
        },
      },
    },
    series: [
      {
        name: "会话数量",
        type: "bar",
        data: dailyTrend.map((item) => item.sessionCount),
        itemStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: chartColors.info },
              { offset: 1, color: chartColors.primary },
            ],
          },
        },
        barWidth: "40%",
      },
      {
        name: "参与用户数",
        type: "bar",
        data: dailyTrend.map((item) => item.userCount),
        itemStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: chartColors.warning },
              { offset: 1, color: chartColors.warning },
            ],
          },
        },
        barWidth: "40%",
      },
    ],
  };
  //设置图表选项
  consultationChart.value.setOption(option);
};
//用户活跃度趋势
const userActivityChart = ref(null);
const userActivityChartRef = ref(null);
const initUserActivityChart = () => {
  if (!userActivityChartRef.value) return;
  // 确保DOM元素有宽度和高度
  const dom = userActivityChartRef.value;
  if (!dom || dom.clientWidth === 0 || dom.clientHeight === 0) {
    // 延迟执行，确保DOM已经渲染
    setTimeout(initUserActivityChart, 100);
    return;
  }

  // 确保用户活跃度数据存在
  if (
    !aiData.value ||
    !aiData.value.userActivity ||
    !Array.isArray(aiData.value.userActivity)
  )
    return;

  // 检查数据结构
  console.log("User activity data structure:", {
    hasUserActivity: !!aiData.value.userActivity,
    isArray: Array.isArray(aiData.value.userActivity),
    trendLength: aiData.value.userActivity.length,
  });

  //销毁现有的图表
  if (userActivityChart.value) userActivityChart.value.dispose();

  //创建echarts实例
  userActivityChart.value = echarts.init(dom);

  //获取用户活跃度趋势的数据
  const activityData = aiData.value.userActivity;
  const option = {
    title: {
      text: "用户活跃度趋势",
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: chartColors.text,
      },
      left: "center",
      top: 10,
    },
    tooltip: {
      trigger: "axis",
      backgroundColor: chartColors.surface,
      borderColor: chartColors.accent,
      borderWidth: 1,
      textStyle: {
        color: chartColors.text,
      },
    },
    legend: {
      data: ["活跃用户", "新增用户", "日记用户", "咨询用户"],
      top: 40,
      textStyle: {
        color: chartColors.textSecondary,
      },
    },
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      top: 80,
      containLabel: true,
    },
    xAxis: {
      type: "category",
      data: activityData.map((item) => item.date),
      axisLine: {
        lineStyle: {
          color: chartColors.accent,
        },
      },
      axisLabel: {
        color: chartColors.textSecondary,
      },
    },
    yAxis: {
      type: "value",
      axisLabel: {
        color: chartColors.textSecondary,
      },
      axisLine: {
        lineStyle: {
          color: chartColors.accent,
        },
      },
      splitLine: {
        lineStyle: {
          color: chartColors.accentLight,
        },
      },
    },
    series: [
      {
        name: "活跃用户",
        type: "line",
        data: activityData.map((item) => item.activeUsers),
        smooth: true,
        lineStyle: {
          width: 3,
          color: chartColors.info,
        },
        itemStyle: {
          color: chartColors.info,
        },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: chartColors.infoWashStrong },
              { offset: 1, color: chartColors.infoLight },
            ],
          },
        },
      },
      {
        name: "新增用户",
        type: "line",
        data: activityData.map((item) => item.newUsers),
        smooth: true,
        lineStyle: {
          width: 3,
          color: chartColors.warning,
        },
        itemStyle: {
          color: chartColors.warning,
        },
      },
      {
        name: "日记用户",
        type: "line",
        data: activityData.map((item) => item.diaryUsers),
        smooth: true,
        lineStyle: {
          width: 3,
          color: chartColors.success,
        },
        itemStyle: {
          color: chartColors.success,
        },
      },
      {
        name: "咨询用户",
        type: "line",
        data: activityData.map((item) => item.consultationUsers),
        smooth: true,
        lineStyle: {
          width: 3,
          color: chartColors.accent,
        },
        itemStyle: {
          color: chartColors.accent,
        },
      },
    ],
  };
  //设置图表选项
  userActivityChart.value.setOption(option);
};
onMounted(() => {
  getAnalyticsOverview()
    .then((res) => {
      aiData.value = res;
      // 延迟执行图表初始化，确保DOM已完全设置
      setTimeout(() => {
        initCharts();
      }, 100);
    })
    .catch((error) => {
      console.error("Error fetching analytics data:", error);
    });
});
</script>
<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;

  @media (max-width: 768px) {
    padding: 10px;
  }

  /* 指标卡：原 4 个高饱和渐变卡 → 浅底卡片。
     卡面（el-card 表面）取语义浅阶，用 :has() 按图标块的类名逐卡着色，
     使数字与副标题都落在该浅阶底色上（而不是白卡上）。
     :has() 不可用的环境退化为白卡 + 深阶图标块，两种情形对比度都已实算。 */
  .el-card:has(.avatar.users) {
    background-color: var(--color-primary-light);
  }
  .el-card:has(.avatar.like) {
    background-color: var(--color-accent-light);
  }
  .el-card:has(.avatar.comments) {
    background-color: var(--color-info-light);
  }
  .el-card:has(.avatar.smile) {
    background-color: var(--color-success-light);
  }

  .card-content {
    display: flex;
    align-items: center;

    @media (max-width: 480px) {
      flex-direction: column;
      text-align: center;

      .avatar {
        margin-right: 0;
        margin-bottom: 12px;
      }
    }

    .avatar {
      margin-right: 12px;
      width: 60px;
      height: 60px;
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      display: flex;
      align-items: center;
      justify-content: center;

      @media (max-width: 480px) {
        width: 50px;
        height: 50px;
      }

      /* 图标块：4 个图标是纯白 PNG（透明底，实读像素 R=G=B=255），
         原来的高饱和渐变换成语义深阶色，白色图标才有对比度。
         深阶作底、白图标压在其上，是这套令牌本来的用法。 */
      &.users {
        background: var(--color-primary);
      }
      &.like {
        background: var(--color-accent-text);
      }
      &.comments {
        background: var(--color-info);
      }
      &.smile {
        background: var(--color-success);
      }
    }

    .info {
      .title {
        font-size: 14px;
        color: var(--color-text-secondary);
        margin-bottom: 4px;
      }
      .number {
        font-size: 24px;
        font-weight: 700;
        color: var(--color-text);
        margin-bottom: 4px;
      }
      .subtitle-title {
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }

    /* 四张卡的数字都取各自的语义深阶色。上面的 .number 是 24px / 700，
       按 spec §3.5 属**大字号档**，门槛 3:1（不是正文的 4.5:1）。
       四对数字对各自卡面浅底的实算：primary 4.56:1、accent-text 4.31:1、
       info 4.65:1、success 4.41:1 —— 四者全部 ≥3:1，故无需换成中性文字色。 */
    .avatar.users ~ .info .number {
      color: var(--color-primary);
    }
    .avatar.like ~ .info .number {
      color: var(--color-accent-text);
    }
    .avatar.comments ~ .info .number {
      color: var(--color-info);
    }
    .avatar.smile ~ .info .number {
      color: var(--color-success);
    }
  }

  .chart-content {
    padding: 20px;
    height: 300px;
    position: relative;

    @media (max-width: 768px) {
      padding: 10px;
      height: 250px;
    }

    @media (max-width: 480px) {
      height: 200px;
    }

    .consultation-stats {
      display: flex;
      justify-content: space-around;
      margin-bottom: 20px;

      @media (max-width: 480px) {
        flex-direction: column;
        gap: 10px;
      }

      .stat-item {
        text-align: center;

        .stat-label {
          font-size: 12px;
          color: var(--color-text-secondary);
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 18px;
          font-weight: 600;
          color: var(--color-text);
        }
      }
    }
  }

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text);
  }
}
</style>
