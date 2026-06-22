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
        color: "#2d3436",
      },
      left: "center",
      top: 10,
    },
    tooltip: {
      // 提示框
      trigger: "axis", // 触发类型：坐标轴触发
      borderColor: "#fab1a0",
      borderWidth: 1,
      textStyle: {
        color: "#2d3436",
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
          color: "rgba(244, 162, 97, 0.3)",
        },
      },
    },
    yAxis: [
      {
        type: "value",
        name: "情绪评分",
        position: "left",
        axisLabel: {
          color: "#636e72",
        },
        axisLine: {
          lineStyle: {
            color: "rgba(244, 162, 97, 0.3)",
          },
        },
        splitLine: {
          lineStyle: {
            color: "rgba(244, 162, 97, 0.1)",
          },
        },
      },
      {
        type: "value",
        name: "记录数量",
        position: "right",
        axisLabel: {
          color: "#636e72",
        },
        axisLine: {
          lineStyle: {
            color: "rgba(244, 162, 97, 0.3)",
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
          color: "#ffeaa7",
        },
        itemStyle: {
          color: "#ffeaa7",
        },
      },
      {
        name: "记录数量",
        type: "line", // 折线图
        data: TrendData.map((item) => item.recordCount),
        smooth: true,
        lineStyle: {
          width: 3,
          color: "#fab1a0",
        },
        itemStyle: {
          color: "#fab1a0",
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
        color: "#2d3436",
      },
      left: "center",
      top: 10,
    },
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(255, 255, 255, 0.95)",
      borderColor: "#fab1a0",
      borderWidth: 1,
      textStyle: {
        color: "#2d3436",
      },
    },
    legend: {
      data: ["会话数量", "参与用户数"],
      top: 40,
      textStyle: {
        color: "#636e72",
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
          color: "rgba(244, 162, 97, 0.3)",
        },
      },
      axisLabel: {
        color: "#636e72",
      },
    },
    yAxis: {
      type: "value",
      axisLabel: {
        color: "#636e72",
      },
      axisLine: {
        lineStyle: {
          color: "rgba(244, 162, 97, 0.3)",
        },
      },
      splitLine: {
        lineStyle: {
          color: "rgba(244, 162, 97, 0.1)",
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
              { offset: 0, color: "#74b9ff" },
              { offset: 1, color: "#0984e3" },
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
              { offset: 0, color: "#fdcb6e" },
              { offset: 1, color: "#f39c12" },
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
        color: "#2d3436",
      },
      left: "center",
      top: 10,
    },
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(255, 255, 255, 0.95)",
      borderColor: "#fab1a0",
      borderWidth: 1,
      textStyle: {
        color: "#2d3436",
      },
    },
    legend: {
      data: ["活跃用户", "新增用户", "日记用户", "咨询用户"],
      top: 40,
      textStyle: {
        color: "#636e72",
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
          color: "rgba(244, 162, 97, 0.3)",
        },
      },
      axisLabel: {
        color: "#636e72",
      },
    },
    yAxis: {
      type: "value",
      axisLabel: {
        color: "#636e72",
      },
      axisLine: {
        lineStyle: {
          color: "rgba(244, 162, 97, 0.3)",
        },
      },
      splitLine: {
        lineStyle: {
          color: "rgba(244, 162, 97, 0.1)",
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
          color: "#a29bfe",
        },
        itemStyle: {
          color: "#a29bfe",
        },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(162, 155, 254, 0.4)" },
              { offset: 1, color: "rgba(162, 155, 254, 0.1)" },
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
          color: "#fdcb6e",
        },
        itemStyle: {
          color: "#fdcb6e",
        },
      },
      {
        name: "日记用户",
        type: "line",
        data: activityData.map((item) => item.diaryUsers),
        smooth: true,
        lineStyle: {
          width: 3,
          color: "#00b894",
        },
        itemStyle: {
          color: "#00b894",
        },
      },
      {
        name: "咨询用户",
        type: "line",
        data: activityData.map((item) => item.consultationUsers),
        smooth: true,
        lineStyle: {
          width: 3,
          color: "#fab1a0",
        },
        itemStyle: {
          color: "#fab1a0",
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
      console.log("API Response:", res);
      aiData.value = res;
      console.log("Data loaded:", aiData.value);
      // 延迟执行图表初始化，确保数据已完全设置
      setTimeout(() => {
        console.log("Initializing charts...");
        initCharts();
        console.log("Charts initialized");
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
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;

      @media (max-width: 480px) {
        width: 50px;
        height: 50px;
      }

      &.users {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
      &.like {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }
      &.comments {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      }
      &.smile {
        background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
      }
    }

    .info {
      .title {
        font-size: 14px;
        color: #7f8c8d;
        margin-bottom: 4px;
      }
      .number {
        font-size: 24px;
        font-weight: 700;
        color: #2c3e50;
        margin-bottom: 4px;
      }
      .subtitle-title {
        font-size: 12px;
        color: #95a5a6;
      }
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

    canvas {
      width: 100% !important;
      height: 100% !important;
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
          color: #7f8c8d;
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 18px;
          font-weight: 600;
          color: #2c3e50;
        }
      }
    }
  }

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: #2c3e50;
  }
}
</style>
