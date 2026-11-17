
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  getProjectScenarioExecutionTrends,
  type ScenarioExecutionDailyTrend
} from '../api/scenario'

const props = defineProps<{
  projectId: number
}>()

type DateRangeType = '7' | '30' | 'custom'

const loading = ref(false)
const rangeType = ref<DateRangeType>('7')

const startDate = ref('')
const endDate = ref('')

const trends = ref<ScenarioExecutionDailyTrend[]>([])
const totalCount = ref(0)
const passedCount = ref(0)
const failedCount = ref(0)
const overallPassRate = ref(0)

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function setDateRange(days: number) {
  const end = new Date()
  const start = new Date()

  start.setDate(start.getDate() - days + 1)

  startDate.value = formatDate(start)
  endDate.value = formatDate(end)
}

function changeRange(value: DateRangeType) {
  rangeType.value = value

  if (value === '7') {
    setDateRange(7)
  } else if (value === '30') {
    setDateRange(30)
  }
}

async function loadTrends() {
  if (!props.projectId) return

  if (!startDate.value || !endDate.value) {
    Message.warning('请选择统计日期范围')
    return
  }

  if (startDate.value > endDate.value) {
    Message.warning('开始日期不能晚于结束日期')
    return
  }

  const days =
    (new Date(`${endDate.value}T00:00:00`).getTime() -
      new Date(`${startDate.value}T00:00:00`).getTime()) /
    86400000

  if (days > 90) {
    Message.warning('统计范围不能超过90天')
    return
  }

  loading.value = true

  try {
    const res = await getProjectScenarioExecutionTrends(
      props.projectId,
      startDate.value,
      endDate.value
    )

    const data = res.data.data

    trends.value = data.trends || []
    totalCount.value = data.totalCount || 0
    passedCount.value = data.passedCount || 0
    failedCount.value = data.failedCount || 0
    overallPassRate.value = data.overallPassRate || 0
  } catch {
    Message.error('测试趋势数据加载失败')
  } finally {
    loading.value = false
  }
}

const maxExecutionCount = computed(() => {
  return Math.max(
    1,
    ...trends.value.map(item => item.executionCount)
  )
})

const maxDuration = computed(() => {
  return Math.max(
    1,
    ...trends.value.map(item => item.avgDuration)
  )
})

function getExecutionBarHeight(value: number) {
  return `${(value / maxExecutionCount.value) * 100}%`
}



function formatDuration(value: number) {
  if (!value) return '0 ms'
  if (value < 1000) return `${value} ms`
  return `${(value / 1000).toFixed(2)} 秒`
}

function formatShortDate(value: string) {
  return value.slice(5)
}

function getPassRateColor(value: number) {
  if (value >= 80) return '#00b42a'
  if (value >= 60) return '#ff7d00'
  return '#f53f3f'
}

watch(
  () => props.projectId,
  () => {
    setDateRange(rangeType.value === '30' ? 30 : 7)
    loadTrends()
  },
  { immediate: true }
)
</script>

<template>
  <div class="trend-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>测试趋势分析</h2>
        <p>通过历史执行数据分析项目自动化测试质量变化</p>
      </div>

      <a-button
        type="primary"
        :loading="loading"
        @click="loadTrends"
      >
        <template #icon>
          <icon-refresh />
        </template>
        刷新数据
      </a-button>
    </div>

    <!-- 日期筛选 -->
    <a-card class="filter-card">
      <div class="filter-content">
        <div class="range-buttons">
          <a-button
            :type="rangeType === '7' ? 'primary' : 'outline'"
            @click="changeRange('7')"
          >
            近7天
          </a-button>

          <a-button
            :type="rangeType === '30' ? 'primary' : 'outline'"
            @click="changeRange('30')"
          >
            近30天
          </a-button>

          <a-button
            :type="rangeType === 'custom' ? 'primary' : 'outline'"
            @click="rangeType = 'custom'"
          >
            自定义
          </a-button>
        </div>

        <div class="date-inputs">
          <a-date-picker
            v-model="startDate"
            value-format="YYYY-MM-DD"
            :disabled="rangeType !== 'custom'"
            placeholder="开始日期"
          />

          <span class="date-separator">至</span>

          <a-date-picker
            v-model="endDate"
            value-format="YYYY-MM-DD"
            :disabled="rangeType !== 'custom'"
            placeholder="结束日期"
          />

          <a-button
            type="primary"
            :loading="loading"
            @click="loadTrends"
          >
            查询
          </a-button>
        </div>
      </div>
    </a-card>

    <a-spin :loading="loading" class="trend-content">
      <!-- 统计卡片 -->
      <a-grid
        :cols="4"
        :col-gap="16"
        :row-gap="16"
        class="statistics"
      >
        <a-grid-item>
          <a-card class="stat-card">
            <div class="stat-label">执行总次数</div>
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-description">统计范围内的场景执行总数</div>
          </a-card>
        </a-grid-item>

        <a-grid-item>
          <a-card class="stat-card">
            <div class="stat-label">成功次数</div>
            <div class="stat-value success">{{ passedCount }}</div>
            <div class="stat-description">执行成功的场景数量</div>
          </a-card>
        </a-grid-item>

        <a-grid-item>
          <a-card class="stat-card">
            <div class="stat-label">失败次数</div>
            <div class="stat-value failed">{{ failedCount }}</div>
            <div class="stat-description">执行失败的场景数量</div>
          </a-card>
        </a-grid-item>

        <a-grid-item>
          <a-card class="stat-card">
            <div class="stat-label">整体通过率</div>
            <div class="stat-value primary">
              {{ overallPassRate }}%
            </div>
            <div class="stat-description">成功次数 / 总执行次数</div>
          </a-card>
        </a-grid-item>
      </a-grid>

      <!-- 每日执行次数 -->
      <a-card class="chart-card">
        <template #title>
          <div class="chart-title">
            <span>每日执行次数趋势</span>
            <span class="chart-unit">单位：次</span>
          </div>
        </template>

        <div v-if="trends.length" class="bar-chart">
          <div class="chart-y-axis">
            <span>{{ maxExecutionCount }}</span>
            <span>{{ Math.ceil(maxExecutionCount * 0.75) }}</span>
            <span>{{ Math.ceil(maxExecutionCount * 0.5) }}</span>
            <span>{{ Math.ceil(maxExecutionCount * 0.25) }}</span>
            <span>0</span>
          </div>

          <div class="chart-main">
            <div class="chart-grid-lines">
              <span v-for="n in 5" :key="n"></span>
            </div>

            <div class="bar-list">
              <div
                v-for="item in trends"
                :key="item.date"
                class="bar-item"
              >
                <div class="bar-value">
                  {{ item.executionCount }}
                </div>

                <div class="bar-track">
                  <div
                    class="execution-bar"
                    :style="{
                      height: getExecutionBarHeight(item.executionCount)
                    }"
                  ></div>
                </div>

                <div class="bar-label">
                  {{ formatShortDate(item.date) }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <a-empty
          v-else
          description="当前日期范围暂无执行数据"
        />
      </a-card>

      <!-- 成功失败趋势 -->
      <a-card class="chart-card">
        <template #title>
          <div class="chart-title">
            <span>每日成功与失败次数</span>
            <div class="chart-legend">
              <span><i class="legend-dot passed-dot"></i>成功</span>
              <span><i class="legend-dot failed-dot"></i>失败</span>
            </div>
          </div>
        </template>

        <div v-if="trends.length" class="stacked-chart">
          <div
            v-for="item in trends"
            :key="item.date"
            class="stacked-item"
          >
            <div class="stacked-bars">
              <div
                class="stacked-bar passed-bar"
                :style="{
                  height: getExecutionBarHeight(item.passedCount)
                }"
                :title="`成功：${item.passedCount}`"
              ></div>

              <div
                class="stacked-bar failed-bar"
                :style="{
                  height: getExecutionBarHeight(item.failedCount)
                }"
                :title="`失败：${item.failedCount}`"
              ></div>
            </div>

            <div class="stacked-label">
              {{ formatShortDate(item.date) }}
            </div>

            <div class="stacked-total">
              {{ item.executionCount }}
            </div>
          </div>
        </div>

        <a-empty
          v-else
          description="当前日期范围暂无执行数据"
        />
      </a-card>

      <!-- 每日通过率 -->
      <a-card class="chart-card">
        <template #title>
          <div class="chart-title">
            <span>每日通过率趋势</span>
            <span class="chart-unit">单位：%</span>
          </div>
        </template>

        <div v-if="trends.length" class="rate-list">
          <div
            v-for="item in trends"
            :key="item.date"
            class="rate-item"
          >
            <div class="rate-date">
              {{ formatShortDate(item.date) }}
            </div>

            <div class="rate-track">
              <div
                class="rate-progress"
                :style="{
                  width: `${item.passRate}%`,
                  background: getPassRateColor(item.passRate)
                }"
              ></div>
            </div>

            <div
              class="rate-number"
              :style="{ color: getPassRateColor(item.passRate) }"
            >
              {{ item.passRate }}%
            </div>
          </div>
        </div>

        <a-empty
          v-else
          description="当前日期范围暂无执行数据"
        />
      </a-card>

      <!-- 平均耗时 -->
      <a-card class="chart-card">
        <template #title>
          <div class="chart-title">
            <span>每日平均执行耗时</span>
            <span class="chart-unit">单位：毫秒</span>
          </div>
        </template>

        <div v-if="trends.length" class="duration-list">
          <div
            v-for="item in trends"
            :key="item.date"
            class="duration-item"
          >
            <div class="duration-date">
              {{ formatShortDate(item.date) }}
            </div>

            <div class="duration-track">
              <div
                class="duration-bar"
                :style="{
                  width: `${(item.avgDuration / maxDuration) * 100}%`
                }"
              ></div>
            </div>

            <div class="duration-value">
              {{ formatDuration(item.avgDuration) }}
            </div>
          </div>
        </div>

        <a-empty
          v-else
          description="当前日期范围暂无执行数据"
        />
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.trend-page {
  width: 100%;
  min-width: 0;
  color: #1d2129;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #86909c;
  font-size: 13px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 6px;
}

.filter-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.range-buttons,
.date-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.date-separator {
  color: #86909c;
}

.trend-content {
  display: block;
  width: 100%;
}

.statistics {
  margin-bottom: 20px;
}

.stat-card {
  min-height: 125px;
  height: 100%;
  border-radius: 6px;
}

.stat-label {
  color: #86909c;
  font-size: 13px;
}

.stat-value {
  margin-top: 12px;
  font-size: 29px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-description {
  margin-top: 9px;
  color: #a9aeb8;
  font-size: 12px;
}

.success {
  color: #00b42a;
}

.failed {
  color: #f53f3f;
}

.primary {
  color: #165dff;
}

.chart-card {
  margin-bottom: 20px;
  border-radius: 6px;
  min-width: 0;
}

.chart-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}

.chart-unit {
  color: #86909c;
  font-size: 12px;
  font-weight: 400;
}

.chart-legend {
  display: flex;
  gap: 16px;
  color: #4e5969;
  font-size: 12px;
  font-weight: 400;
}

.chart-legend span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.passed-dot {
  background: #00b42a;
}

.failed-dot {
  background: #f53f3f;
}

.bar-chart {
  display: flex;
  height: 260px;
  padding-top: 12px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 35px;
  padding-bottom: 25px;
  color: #86909c;
  font-size: 11px;
  text-align: right;
}

.chart-main {
  position: relative;
  flex: 1;
  min-width: 0;
}

.chart-grid-lines {
  position: absolute;
  inset: 0 0 25px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.chart-grid-lines span {
  border-top: 1px dashed #e5e6eb;
}

.bar-list {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: space-around;
  gap: 5px;
}

.bar-item {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  align-items: center;
}

.bar-value {
  height: 18px;
  color: #4e5969;
  font-size: 11px;
}

.bar-track {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  flex: 1;
  width: 100%;
  min-height: 0;
}

.execution-bar {
  width: min(30px, 65%);
  min-height: 0;
  background: #4080ff;
  border-radius: 5px 5px 0 0;
  transition: height 0.3s ease;
}

.bar-label {
  height: 25px;
  padding-top: 7px;
  color: #86909c;
  font-size: 11px;
  white-space: nowrap;
}

.stacked-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 8px;
  min-height: 230px;
  padding: 15px 0 0;
}

.stacked-item {
  display: flex;
  flex: 1;
  min-width: 0;
  height: 210px;
  flex-direction: column;
  align-items: center;
}

.stacked-bars {
  display: flex;
  flex: 1;
  width: 100%;
  align-items: flex-end;
  justify-content: center;
  gap: 5px;
}

.stacked-bar {
  width: min(24px, 40%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.passed-bar {
  background: #00b42a;
}

.failed-bar {
  background: #f53f3f;
}

.stacked-label {
  padding-top: 9px;
  color: #86909c;
  font-size: 11px;
  white-space: nowrap;
}

.stacked-total {
  padding-top: 3px;
  color: #4e5969;
  font-size: 11px;
}

.rate-list,
.duration-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.rate-item,
.duration-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.rate-date,
.duration-date {
  width: 55px;
  flex-shrink: 0;
  color: #4e5969;
  font-size: 12px;
}

.rate-track,
.duration-track {
  flex: 1;
  height: 12px;
  overflow: hidden;
  border-radius: 10px;
  background: #f2f3f5;
}

.rate-progress,
.duration-bar {
  height: 100%;
  border-radius: 10px;
  transition: width 0.3s ease;
}

.duration-bar {
  background: #4080ff;
}

.rate-number,
.duration-value {
  width: 75px;
  flex-shrink: 0;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
}

.duration-value {
  color: #4e5969;
  font-weight: 400;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
  }

  .filter-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .date-inputs {
    width: 100%;
  }

  .date-inputs :deep(.arco-picker) {
    flex: 1;
    min-width: 0;
  }

  .chart-title {
    align-items: flex-start;
  }

  .rate-item,
  .duration-item {
    gap: 8px;
  }
}
</style>
