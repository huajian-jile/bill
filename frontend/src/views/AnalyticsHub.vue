<template>
  <div class="analytics-hub">
    <div class="top-bar">
      <div class="top-bar-left">
        <el-radio-group v-model="scope" size="default" class="scope-tabs" @change="onScopeChange">
          <el-radio-button value="day">某日</el-radio-button>
          <el-radio-button value="month">某月</el-radio-button>
          <el-radio-button value="year">某年</el-radio-button>
          <el-radio-button value="all">全部</el-radio-button>
        </el-radio-group>
        <div class="top-bar-scope-fields">
          <template v-if="scope === 'day'">
            <span class="field-lbl">日期</span>
            <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 150px"
              @change="onDayInputsChange"
            />
            <span class="field-lbl">对比日</span>
            <el-date-picker
              v-model="compareDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="可选，对半对比"
              clearable
              style="width: 150px"
              @change="onDayInputsChange"
            />
          </template>
          <template v-else-if="scope === 'month'">
            <span class="field-lbl">年</span>
            <el-input-number
              v-model="year"
              :min="2000"
              :max="2100"
              controls-position="right"
              @change="onMonthYearChange"
            />
            <span class="field-lbl">月</span>
            <el-input-number
              v-model="month"
              :min="1"
              :max="12"
              controls-position="right"
              @change="onMonthYearChange"
            />
          </template>
          <template v-else-if="scope === 'year'">
            <span class="field-lbl">年</span>
            <el-input-number
              v-model="yearOnly"
              :min="2000"
              :max="2100"
              controls-position="right"
              @change="onYearChange"
            />
            <span class="field-lbl">对比</span>
            <el-switch v-model="yearCompareOn" @change="load" />
            <template v-if="yearCompareOn">
              <span class="field-lbl">对比年</span>
              <el-input-number
                v-model="cmpYearOnly"
                :min="2000"
                :max="2100"
                controls-position="right"
                @change="load"
              />
            </template>
          </template>
          <span class="field-lbl">渠道</span>
          <el-select v-model="channel" style="width: 100px" @change="load">
            <el-option label="微信" value="wechat" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="合并" value="merged" />
          </el-select>
          <span class="field-lbl">{{ multiPhone ? '手机号(多选)' : '手机号' }}</span>
          <el-select
            v-if="!multiPhone"
            v-model="phoneId"
            clearable
            placeholder="全部"
            style="width: 200px"
            @change="load"
          >
            <el-option v-for="p in phones" :key="p.id" :label="p.mobileCn" :value="p.id" />
          </el-select>
          <el-select
            v-else
            v-model="phoneIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            placeholder="选多个号码"
            style="width: 260px"
            @change="load"
          >
            <el-option v-for="p in phones" :key="p.id" :label="p.mobileCn" :value="p.id" />
          </el-select>
          <el-button :type="multiPhone ? 'primary' : 'default'" @click="toggleMultiPhone">多选号码</el-button>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </div>
      </div>
      <div class="top-bar-actions">
        <el-button
          :type="useReal ? 'primary' : 'default'"
          plain
          class="real-btn"
          title="在当前统计与流水列表中剔除「同一天、同金额、同一交易对方」的一收一支（如转账与原路退回）；无此类成对记录时数据不变。"
          @click="toggleReal"
        >
          真实收支
        </el-button>
        <el-button plain @click="goImport">导入账单</el-button>
        <el-button plain @click="goPhoneBind">绑定手机号</el-button>
      </div>
    </div>

    <!-- 汇总一行：保留给未来扩展；当前四种范围均在下方内容区展示汇总 -->
    <div class="summary-row" v-loading="loading" v-if="summaryRowVisible">
      <div class="sum-row-three">
        <div class="sum-card income">
          <div class="sum-label">收入总计</div>
          <div class="sum-val">{{ fmtMoney(summary.income) }}</div>
        </div>
        <div class="sum-card expense">
          <div class="sum-label">支出总计</div>
          <div class="sum-val">{{ fmtMoney(summary.expense) }}</div>
        </div>
        <div class="sum-card neutral">
          <div class="sum-label">中性合计</div>
          <div class="sum-val">{{ fmtMoney(summary.neutral) }}</div>
        </div>
      </div>
    </div>

    <!-- 某日：顶部趋势图 + 明细筛选 + 分栏或单栏 -->
    <div v-if="scope === 'day'" class="day-flow-wrap" v-loading="loading">
      <template v-if="dayDetail">
        <div v-show="showDayTrendChart" class="day-chart-with-summary">
          <aside v-if="dayDetail.day" class="day-summary-side">
            <div class="day-summary-head day-summary-head-plain">
              <span class="day-summary-date-only">{{ formatDateWithWeekday(date) }}</span>
            </div>
            <div class="day-summary-total-pill">当日流水 {{ mergeSliceRows(dayDetail.day).length }} 笔</div>
            <div class="day-stat-grid">
              <div
                v-for="row in dayStatRows(sliceBreakdown(dayDetail.day))"
                :key="row.kind"
                class="day-stat-chip"
                :class="row.kind"
              >
                <span class="day-stat-count">{{ row.count }} 笔</span>
                <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
              </div>
            </div>
          </aside>
          <div ref="dayTrendChartEl" class="compare-chart day-chart-pane" />
        </div>
        <div v-show="showDayCompareChart" class="day-chart-with-summary day-chart-with-summary--compare">
          <aside v-if="dayDetail.day && dayDetail.compareDay" class="day-summary-side day-summary-compare">
            <div class="day-summary-block">
              <div class="day-summary-head day-summary-head-plain">
                <span class="day-summary-date-only">{{ formatDateWithWeekday(date) }}</span>
              </div>
              <div class="day-summary-total-pill">当日流水 {{ mergeSliceRows(dayDetail.day).length }} 笔</div>
              <div class="day-stat-grid">
                <div
                  v-for="row in dayStatRows(sliceBreakdown(dayDetail.day))"
                  :key="'b-' + row.kind"
                  class="day-stat-chip"
                  :class="row.kind"
                >
                  <span class="day-stat-count">{{ row.count }} 笔</span>
                  <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                </div>
              </div>
            </div>
            <div class="day-summary-block">
              <div class="day-summary-head day-summary-head-plain">
                <span class="day-summary-date-only">{{ formatDateWithWeekday(compareDate) }}</span>
              </div>
              <div class="day-summary-total-pill">当日流水 {{ mergeSliceRows(dayDetail.compareDay).length }} 笔</div>
              <div class="day-stat-grid">
                <div
                  v-for="row in dayStatRows(sliceBreakdown(dayDetail.compareDay))"
                  :key="'c-' + row.kind"
                  class="day-stat-chip"
                  :class="row.kind"
                >
                  <span class="day-stat-count">{{ row.count }} 笔</span>
                  <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                </div>
              </div>
            </div>
          </aside>
          <div ref="compareChartEl" class="compare-chart day-chart-pane" />
        </div>
      <div class="filter-toolbar-row">
        <span class="filter-label">流水</span>
        <el-radio-group v-model="dayFilter" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="income">收入</el-radio-button>
          <el-radio-button value="expense">支出</el-radio-button>
          <el-radio-button value="neutral">中性</el-radio-button>
        </el-radio-group>
        <el-select
          v-model="txFilter.tradeType"
          clearable
          filterable
          allow-create
          default-first-option
          placeholder="交易类型"
          style="width: 160px"
        >
          <el-option v-for="t in tradeTypeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="txFilter.paymentMethod" clearable placeholder="支付方式" style="width: 140px">
          <el-option v-for="p in paymentOptions" :key="p" :label="p" :value="p" />
        </el-select>
        <span class="tx-lbl">金额</span>
        <el-input-number v-model="txFilter.amountMin" :controls="false" :precision="2" placeholder="≥" />
        <span class="tx-sep">—</span>
        <el-input-number v-model="txFilter.amountMax" :controls="false" :precision="2" placeholder="≤" />
        <el-button size="small" @click="timeAscending = !timeAscending">
          {{ timeAscending ? '正序' : '倒序' }}
        </el-button>
        <el-button
          size="small"
          :type="rollingIncomeVisible ? 'primary' : 'default'"
          plain
          @click="rollingIncomeVisible = !rollingIncomeVisible"
        >
          近30天收入
        </el-button>
        <el-button
          size="small"
          :type="rollingExpenseVisible ? 'primary' : 'default'"
          plain
          @click="rollingExpenseVisible = !rollingExpenseVisible"
        >
          近30天支出
        </el-button>
      </div>

      <div :class="dayCompareLayoutClass">
        <div v-for="pane in dayPanes" :key="pane.key" class="compare-pane">
          <div class="table-block">
            <AnalyticsDetailTable
              :rows="procDaySlice(pane.slice)"
              :max-height="420"
              :tone="dayTableTone"
              :per-row-tone="dayPerRowTone"
            />
            <template v-if="pane.key === 'base' && rollingData">
              <template v-if="rollingIncomeVisible">
                <div class="rolling-h">近 30 天收入明细（截止 {{ date }}）</div>
                <AnalyticsDetailTable :rows="proc(rollingData.incomeTransactions)" :max-height="240" />
              </template>
              <template v-if="rollingExpenseVisible">
                <div class="rolling-h">近 30 天支出明细（截止 {{ date }}）</div>
                <AnalyticsDetailTable :rows="proc(rollingData.expenseTransactions)" :max-height="240" />
              </template>
            </template>
          </div>
        </div>
      </div>
      </template>
    </div>

    <!-- 某月：每日消费趋势图 + 每日收入支出表 -->
    <div v-if="scope === 'month'" class="month-flow-wrap" v-loading="loading">
      <template v-if="monthRows.length">
        <div class="filter-toolbar-row month-flow-toolbar">
          <span class="filter-label">流水</span>
          <el-radio-group v-model="monthFlowFilter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="income">收入</el-radio-button>
            <el-radio-button value="expense">支出</el-radio-button>
            <el-radio-button value="neutral">中性</el-radio-button>
          </el-radio-group>
        </div>
        <div class="compare-pane">
          <div class="day-chart-with-summary">
            <aside class="day-summary-side">
              <div class="day-summary-head day-summary-head-plain">
                <span class="day-summary-date-only">{{ year }}-{{ pad(month) }}</span>
              </div>
              <div class="day-summary-total-pill">当月流水 {{ totalTxnCount(monthStatsAgg) }} 笔</div>
              <div class="day-stat-grid">
                <div
                  v-for="row in dayStatRows(monthStatsAgg)"
                  :key="'m-' + row.kind"
                  class="day-stat-chip"
                  :class="row.kind"
                >
                  <span class="day-stat-count">{{ row.count }} 笔</span>
                  <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                </div>
              </div>
            </aside>
            <div
              ref="monthDailyTrendChartEl"
              class="compare-chart day-chart-pane month-trend-chart"
              title="点击折线节点查看当日明细"
            />
          </div>
          <div class="pane-title pane-sub">每日汇总（周一至周日对齐；点击某日格查看当日明细）</div>
          <MonthDailyCalendarGrid
            :year="year"
            :month="month"
            :rows="monthRowsDisplay"
            @date-click="openMonthDayDrill"
          />
        </div>
      </template>
    </div>

    <!-- 某年：左侧汇总 + 月度趋势图 + 表格（固定 1–12 月，与某月日历格对齐思路一致） -->
    <div v-if="scope === 'year'" class="month-flow-wrap" v-loading="loading">
      <template v-if="yearMonthRows.length">
        <div :class="yearCompareOn && yearMonthRowsCompare.length ? 'compare-split' : ''">
          <div class="compare-pane" v-if="yearMonthRows.length">
            <div class="day-chart-with-summary">
              <aside class="day-summary-side">
                <div class="day-summary-head day-summary-head-plain">
                  <span class="day-summary-date-only">{{ yearOnly }}</span>
                </div>
                <div class="day-summary-total-pill">本年流水 {{ totalTxnCount(yearStatsAgg) }} 笔</div>
                <div class="day-stat-grid">
                  <div class="day-stat-head"><span>笔数</span><span>金额</span></div>
                  <div
                    v-for="row in dayStatRows(yearStatsAgg)"
                    :key="'y-' + row.kind"
                    class="day-stat-chip"
                    :class="row.kind"
                  >
                    <span class="day-stat-count">{{ row.count }} 笔</span>
                    <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                  </div>
                </div>
              </aside>
              <div ref="yearTrendChartEl" class="compare-chart day-chart-pane month-trend-chart" />
            </div>
            <div class="pane-title pane-sub">
              月度汇总（12 个月，每行 5 张卡片；收/支/中金额与笔数；点击卡片或图表节点在侧栏抽屉查看该月；图例可打开「中性」）
            </div>
            <PeriodMetricsCardsGrid
              :columns="5"
              :rows="yearMonthRowsDisplay"
              @select="openDrillMonthFromYearRow"
            />
          </div>
          <div class="compare-pane" v-if="yearCompareOn && yearMonthRowsCompare.length">
            <div class="day-chart-with-summary">
              <aside class="day-summary-side">
                <div class="day-summary-head day-summary-head-plain">
                  <span class="day-summary-date-only">{{ cmpYearOnly }}</span>
                </div>
                <div class="day-summary-total-pill">本年流水 {{ totalTxnCount(yearStatsAggCompare) }} 笔</div>
                <div class="day-stat-grid">
                  <div
                    v-for="row in dayStatRows(yearStatsAggCompare)"
                    :key="'yc-' + row.kind"
                    class="day-stat-chip"
                    :class="row.kind"
                  >
                    <span class="day-stat-count">{{ row.count }} 笔</span>
                    <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                  </div>
                </div>
              </aside>
              <div ref="yearTrendChartCompareEl" class="compare-chart day-chart-pane month-trend-chart" />
            </div>
            <div class="pane-title pane-sub">
              月度汇总（12 个月，每行 5 张卡片；收/支/中金额与笔数；点击卡片或图表节点在侧栏抽屉查看该月；图例可打开「中性」）
            </div>
            <PeriodMetricsCardsGrid
              :columns="5"
              :rows="yearMonthRowsCompareDisplay"
              @select="openDrillMonthFromYearRow"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- 全部：历年汇总（与某年结构一致，按自然年一行） -->
    <div v-if="scope === 'all'" class="month-flow-wrap" v-loading="loading">
      <template v-if="allYearRows.length">
        <div class="filter-toolbar-row month-flow-toolbar">
          <span class="filter-label">流水</span>
          <el-radio-group v-model="allFlowFilter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="income">收入</el-radio-button>
            <el-radio-button value="expense">支出</el-radio-button>
            <el-radio-button value="neutral">中性</el-radio-button>
          </el-radio-group>
        </div>
        <div class="compare-pane">
          <div class="day-chart-with-summary">
            <aside class="day-summary-side">
              <div class="day-summary-head day-summary-head-plain">
                <span class="day-summary-date-only">全部</span>
              </div>
              <div class="day-summary-total-pill">累计流水 {{ totalTxnCount(allStatsAgg) }} 笔</div>
              <div class="day-stat-grid">
                <div
                  v-for="row in dayStatRows(allStatsAgg)"
                  :key="'a-' + row.kind"
                  class="day-stat-chip"
                  :class="row.kind"
                >
                  <span class="day-stat-count">{{ row.count }} 笔</span>
                  <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                </div>
              </div>
            </aside>
            <div ref="allYearTrendChartEl" class="compare-chart day-chart-pane month-trend-chart" />
          </div>
          <div class="pane-title pane-sub">
            年度汇总（每行 5 张卡片；收/支/中金额与笔数；点击卡片或图表节点在侧栏抽屉查看该年；图例可打开「中性」）
          </div>
          <PeriodMetricsCardsGrid :columns="5" :rows="allYearRowsDisplay" @select="openDrillYearFromAllRow" />
        </div>
      </template>
    </div>

    <el-drawer
      v-model="monthDrillVisible"
      :title="monthDrillDrawerTitle"
      direction="rtl"
      size="min(960px, 96vw)"
      class="month-drill-drawer"
      @closed="onMonthDrillClosed"
    >
      <div v-loading="monthDrillLoading" class="month-drill-inner">
        <!-- 某日：与主页面「某日」一致 -->
        <template v-if="drillMode === 'day'">
          <p v-if="monthDrillDetail?.day" class="drill-hint">与「某日」相同渠道与所属人筛选；以下为该日按小时趋势与流水。</p>
          <div v-show="monthDrillDetail?.day" ref="drillDayTrendChartEl" class="compare-chart drill-day-chart" />
          <template v-if="monthDrillDetail?.day">
            <div class="filter-toolbar-row drill-toolbar">
              <span class="filter-label">流水</span>
              <el-radio-group v-model="drillDayFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="income">收入</el-radio-button>
                <el-radio-button value="expense">支出</el-radio-button>
                <el-radio-button value="neutral">中性</el-radio-button>
              </el-radio-group>
              <el-select
                v-model="drillTxFilter.tradeType"
                clearable
                filterable
                allow-create
                default-first-option
                placeholder="交易类型"
                style="width: 160px"
              >
                <el-option v-for="t in drillTradeTypeOptions" :key="t" :label="t" :value="t" />
              </el-select>
              <el-select v-model="drillTxFilter.paymentMethod" clearable placeholder="支付方式" style="width: 140px">
                <el-option v-for="p in drillPaymentOptions" :key="p" :label="p" :value="p" />
              </el-select>
              <span class="tx-lbl">金额</span>
              <el-input-number v-model="drillTxFilter.amountMin" :controls="false" :precision="2" placeholder="≥" />
              <span class="tx-sep">—</span>
              <el-input-number v-model="drillTxFilter.amountMax" :controls="false" :precision="2" placeholder="≤" />
              <el-button size="small" @click="drillTimeAscending = !drillTimeAscending">
                {{ drillTimeAscending ? '正序' : '倒序' }}
              </el-button>
            </div>
            <div class="table-block">
              <div class="table-caption all-tx">
                当日流水（{{ mergeSliceRows(monthDrillDetail.day).length }} 笔）
                <span class="inline-slice-stats">{{ sliceStatsInline(monthDrillDetail.day) }}</span>
              </div>
              <AnalyticsDetailTable
                :rows="drillTableRows"
                :max-height="480"
                :tone="drillTableTone"
                :per-row-tone="drillPerRowTone"
              />
            </div>
          </template>
        </template>

        <!-- 某月：与主页面「某月」一致 -->
        <template v-else-if="drillMode === 'month' && drillMonthRows.length">
          <p class="drill-hint">与主页面「某月」相同渠道与所属人；点击日期可继续查看当日明细。</p>
          <div class="day-chart-with-summary drill-nested-flow">
            <aside class="day-summary-side">
              <div class="day-summary-head day-summary-head-plain">
                <span class="day-summary-date-only">{{ drillMonthY }}-{{ pad(drillMonthM) }}</span>
              </div>
              <div class="day-summary-total-pill">当月流水 {{ totalTxnCount(drillMonthStatsAgg) }} 笔</div>
              <div class="day-stat-grid">
                <div
                  v-for="row in dayStatRows(drillMonthStatsAgg)"
                  :key="'dm-' + row.kind"
                  class="day-stat-chip"
                  :class="row.kind"
                >
                  <span class="day-stat-count">{{ row.count }} 笔</span>
                  <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                </div>
              </div>
            </aside>
            <div ref="drillMonthTrendChartEl" class="compare-chart day-chart-pane month-trend-chart" />
          </div>
          <div class="filter-toolbar-row month-flow-toolbar drill-toolbar">
            <span class="filter-label">流水</span>
            <el-radio-group v-model="drillMonthFlowFilter" size="small">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="income">收入</el-radio-button>
              <el-radio-button value="expense">支出</el-radio-button>
              <el-radio-button value="neutral">中性</el-radio-button>
            </el-radio-group>
          </div>
          <div class="pane-title pane-sub">每日汇总（周一至周日对齐；点击某日格查看当日明细）</div>
          <MonthDailyCalendarGrid
            :year="drillMonthY"
            :month="drillMonthM"
            :rows="drillMonthRowsDisplay"
            @date-click="openMonthDayDrill"
          />
        </template>

        <!-- 某年：与主页面「某年」一致 -->
        <template v-else-if="drillMode === 'year' && drillYearMonthRows.length">
          <p class="drill-hint">与主页面「某年」相同渠道与所属人；点击月份可继续查看该月明细。</p>
          <div class="day-chart-with-summary drill-nested-flow">
            <aside class="day-summary-side">
              <div class="day-summary-head day-summary-head-plain">
                <span class="day-summary-date-only">{{ drillYearOnly }}</span>
              </div>
              <div class="day-summary-total-pill">本年流水 {{ totalTxnCount(drillYearStatsAgg) }} 笔</div>
              <div class="day-stat-grid">
                <div class="day-stat-head"><span>笔数</span><span>金额</span></div>
                <div
                  v-for="row in dayStatRows(drillYearStatsAgg)"
                  :key="'dy-' + row.kind"
                  class="day-stat-chip"
                  :class="row.kind"
                >
                  <span class="day-stat-count">{{ row.count }} 笔</span>
                  <span class="day-stat-amt">{{ fmtMoney(row.amt) }} 元</span>
                </div>
              </div>
            </aside>
            <div ref="drillYearTrendChartEl" class="compare-chart day-chart-pane month-trend-chart" />
          </div>
          <div class="pane-title pane-sub">
            月度汇总（每行 5 张卡片；收/支/中金额与笔数；点击卡片或图表节点查看该月明细）
          </div>
          <PeriodMetricsCardsGrid
            :columns="5"
            :rows="drillYearMonthRowsDisplay"
            @select="openDrillMonthFromYearRow"
          />
        </template>
      </div>
    </el-drawer>

    <el-empty v-if="showEmptyHint" description="暂无数据，请选择条件后查询" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '../api'
import { isNeutralIncomeExpense } from '../utils/incomeExpense'
import AnalyticsDetailTable from '../components/AnalyticsDetailTable.vue'
import MonthDailyCalendarGrid from '../components/MonthDailyCalendarGrid.vue'
import PeriodMetricsCardsGrid from '../components/PeriodMetricsCardsGrid.vue'

const router = useRouter()
const scope = ref('day')
const useReal = ref(false)
const loading = ref(false)

function goImport() {
  router.push('/import')
}
function goPhoneBind() {
  router.push('/phones')
}

function yesterdayYmd() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const date = ref(yesterdayYmd())
const compareDate = ref(null)
const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const yearOnly = ref(new Date().getFullYear())
const phoneId = ref(null)
/** 多选时的 phone_number.id 列表 */
const phoneIds = ref([])
const channel = ref('merged')
/** 多选号码，对应后端逗号分隔 phoneIds */
const multiPhone = ref(false)
const phones = ref([])

const _now = new Date()
const yearCompareOn = ref(false)
const cmpYearOnly = ref(new Date().getFullYear() - 1)

const dayDetail = ref(null)
const rollingData = ref(null)
/** 近 30 天收入/支出明细块显隐，默认隐藏；点击按钮展开 */
const rollingIncomeVisible = ref(false)
const rollingExpenseVisible = ref(false)
const dayFilter = ref('all')
/** 某月每日表：流水维度（全部/收入/支出/中性），仅筛选表格展示行 */
const monthFlowFilter = ref('all')
/** 全部：按年汇总表的流水筛选 */
const allFlowFilter = ref('all')
const drillMonthFlowFilter = ref('all')
/** 当日流水表整表色调：收入绿、支出红、中性黄 */
const dayTableTone = computed(() => {
  if (dayFilter.value === 'income') return 'income'
  if (dayFilter.value === 'expense') return 'expense'
  if (dayFilter.value === 'neutral') return 'neutral'
  return ''
})
/** 「全部」时按行着色：收入绿、支出红、中性黄 */
const dayPerRowTone = computed(() => dayFilter.value === 'all')

/** 某月表格点击日期 → 抽屉内「某日」明细；抽屉内可嵌套「某月」「某年」视图 */
const monthDrillVisible = ref(false)
const monthDrillLoading = ref(false)
const monthDrillDate = ref('')
const monthDrillDetail = ref(null)
/** 'day' | 'month' | 'year' — 与主页面「某日/某月/某年」布局对应 */
const drillMode = ref('day')
const drillMonthRows = ref([])
const drillYearMonthRows = ref([])
const drillMonthY = ref(null)
const drillMonthM = ref(null)
const drillYearOnly = ref(null)
const drillMonthTrendChartEl = ref(null)
const drillYearTrendChartEl = ref(null)
let chartDrillMonth = null
let chartDrillYear = null
const drillDayFilter = ref('all')
const drillTxFilter = reactive({ tradeType: '', paymentMethod: '', amountMin: null, amountMax: null })
const drillTimeAscending = ref(true)
const drillDayTrendChartEl = ref(null)
let chartDrillDay = null

const drillPaymentOptions = computed(() => {
  const set = new Set()
  const add = (rows) => {
    ;(rows || []).forEach((r) => {
      if (r.paymentMethod) set.add(r.paymentMethod)
    })
  }
  const d = monthDrillDetail.value?.day
  if (!d) return []
  add(d.incomeTransactions)
  add(d.expenseTransactions)
  add(d.neutralTransactions)
  return [...set].sort()
})

const drillTradeTypeOptions = computed(() => {
  const set = new Set()
  const add = (rows) => {
    ;(rows || []).forEach((r) => {
      if (r.tradeType) set.add(r.tradeType)
    })
  }
  const d = monthDrillDetail.value?.day
  if (!d) return []
  add(d.incomeTransactions)
  add(d.expenseTransactions)
  add(d.neutralTransactions)
  return [...set].sort()
})

const drillTableTone = computed(() => {
  if (drillDayFilter.value === 'income') return 'income'
  if (drillDayFilter.value === 'expense') return 'expense'
  if (drillDayFilter.value === 'neutral') return 'neutral'
  return ''
})

const drillPerRowTone = computed(() => drillDayFilter.value === 'all')

const drillMonthStatsAgg = computed(() => aggregateTxnStatsFromRows(drillMonthRows.value))
const drillYearStatsAgg = computed(() => aggregateTxnStatsFromRows(drillYearMonthRows.value))

const monthDrillDrawerTitle = computed(() => {
  if (drillMode.value === 'day')
    return `${formatDateWithWeekday(monthDrillDate.value)} 当日明细`
  if (drillMode.value === 'month' && drillMonthY.value != null && drillMonthM.value != null) {
    return `${drillMonthY.value}-${pad(drillMonthM.value)} 月`
  }
  if (drillMode.value === 'year' && drillYearOnly.value != null) {
    return `${drillYearOnly.value} 年`
  }
  return '明细'
})

const monthRows = ref([])
const monthRowsCompare = ref([])
const yearMonthRows = ref([])
const yearMonthRowsCompare = ref([])
/** 「全部」：按自然年汇总（有流水或笔数的年份才展示行） */
const allYearRows = ref([])

/** 某月 / 某年表格行汇总（笔数、金额），供左侧统计块使用 */
const monthRowsDisplay = computed(() => filterMonthDailyRows(monthRows.value, monthFlowFilter.value))
const monthRowsCompareDisplay = computed(() =>
  filterMonthDailyRows(monthRowsCompare.value, monthFlowFilter.value)
)
const monthStatsAgg = computed(() => aggregateTxnStatsFromRows(monthRows.value))
const monthStatsAggCompare = computed(() => aggregateTxnStatsFromRows(monthRowsCompare.value))
const drillMonthRowsDisplay = computed(() =>
  filterMonthDailyRows(drillMonthRows.value, drillMonthFlowFilter.value)
)
const yearMonthRowsDisplay = computed(() =>
  ensureTwelveMonthRows(yearOnly.value, yearMonthRows.value)
)
const yearMonthRowsCompareDisplay = computed(() =>
  ensureTwelveMonthRows(cmpYearOnly.value, yearMonthRowsCompare.value)
)
/** 抽屉内「某年」月度表：与主页面一致固定 12 行 */
const drillYearMonthRowsDisplay = computed(() =>
  drillYearOnly.value != null
    ? ensureTwelveMonthRows(drillYearOnly.value, drillYearMonthRows.value)
    : []
)
const allYearRowsDisplay = computed(() =>
  filterMonthDailyRows(allYearRows.value, allFlowFilter.value)
)
const yearStatsAgg = computed(() => aggregateTxnStatsFromRows(yearMonthRows.value))
const yearStatsAggCompare = computed(() => aggregateTxnStatsFromRows(yearMonthRowsCompare.value))
const allStatsAgg = computed(() => aggregateTxnStatsFromRows(allYearRows.value))

const summary = ref({ income: null, expense: null, neutral: null })
/** 对比时右侧月的汇总 */
const summaryCompare = ref({ income: null, expense: null, neutral: null })

/** true=时间正序（早→晚，默认）；false=倒序 */
const timeAscending = ref(true)
const txFilter = reactive({ tradeType: '', paymentMethod: '', amountMin: null, amountMax: null })

const dayTrendChartEl = ref(null)
const compareChartEl = ref(null)
const monthDailyTrendChartEl = ref(null)
const yearTrendChartEl = ref(null)
const yearTrendChartCompareEl = ref(null)
const allYearTrendChartEl = ref(null)
let chartDay = null
let chartDaySingle = null
let chartMonthDaily = null
let chartYearTrend = null
let chartYearTrendCmp = null
let chartAllTrend = null

function analyticsUserParams() {
  const p = { channel: channel.value, excludeRefundPairs: useReal.value }
  if (multiPhone.value && phoneIds.value?.length) {
    p.phoneIds = phoneIds.value.join(',')
  } else if (phoneId.value != null && phoneId.value !== '') {
    p.phoneId = phoneId.value
  }
  return p
}

function toggleMultiPhone() {
  multiPhone.value = !multiPhone.value
  if (multiPhone.value) {
    if (phoneId.value != null && (!phoneIds.value || phoneIds.value.length === 0)) {
      phoneIds.value = [phoneId.value]
    }
  } else {
    phoneIds.value = []
  }
  load()
}

const dayCompareLayoutClass = computed(() =>
  compareDate.value && dayDetail.value?.compareDay ? 'compare-split' : ''
)

const dayPanes = computed(() => {
  if (!dayDetail.value?.day) return []
  const base = { key: 'base', slice: dayDetail.value.day }
  if (compareDate.value && dayDetail.value.compareDay) {
    return [base, { key: 'cmp', slice: dayDetail.value.compareDay }]
  }
  return [base]
})

const paymentOptions = computed(() => {
  const set = new Set()
  const add = (rows) => {
    ;(rows || []).forEach((r) => {
      if (r.paymentMethod) set.add(r.paymentMethod)
    })
  }
  if (!dayDetail.value?.day) return [...set]
  add(dayDetail.value.day.incomeTransactions)
  add(dayDetail.value.day.expenseTransactions)
  add(dayDetail.value.day.neutralTransactions)
  if (dayDetail.value.compareDay) {
    add(dayDetail.value.compareDay.incomeTransactions)
    add(dayDetail.value.compareDay.expenseTransactions)
    add(dayDetail.value.compareDay.neutralTransactions)
  }
  return [...set].sort()
})

const tradeTypeOptions = computed(() => {
  const set = new Set()
  const add = (rows) => {
    ;(rows || []).forEach((r) => {
      if (r.tradeType) set.add(r.tradeType)
    })
  }
  if (!dayDetail.value?.day) return [...set]
  add(dayDetail.value.day.incomeTransactions)
  add(dayDetail.value.day.expenseTransactions)
  add(dayDetail.value.day.neutralTransactions)
  if (dayDetail.value.compareDay) {
    add(dayDetail.value.compareDay.incomeTransactions)
    add(dayDetail.value.compareDay.expenseTransactions)
    add(dayDetail.value.compareDay.neutralTransactions)
  }
  return [...set].sort()
})

/** 无对比日时：基准日按小时收入/支出折线 */
const showDayTrendChart = computed(
  () =>
    scope.value === 'day' &&
    !!dayDetail.value?.day &&
    !(compareDate.value && dayDetail.value?.compareDay)
)
const showDayCompareChart = computed(
  () => scope.value === 'day' && !!(compareDate.value && dayDetail.value?.compareDay)
)
/** 某月：每日折线图（有月度日数据即绘制） */
const showMonthDailyTrendChart = computed(
  () => scope.value === 'month' && monthRows.value.length > 0
)
/** 某年：月度折线图（有年度月数据即绘制） */
const showYearMonthlyTrendChart = computed(
  () => scope.value === 'year' && yearMonthRows.value.length > 0
)
/** 全部：按年折线图 */
const showAllYearsTrendChart = computed(
  () => scope.value === 'all' && allYearRows.value.length > 0
)

const showCompareChart = computed(
  () =>
    showDayTrendChart.value ||
    showDayCompareChart.value ||
    showMonthDailyTrendChart.value ||
    showYearMonthlyTrendChart.value ||
    showAllYearsTrendChart.value
)

/** 顶部大卡片区：某日/某月/某年/全部在各自内容区内展示汇总，此处不显示 */
const summaryRowVisible = computed(() => {
  if (
    scope.value === 'day' ||
    scope.value === 'month' ||
    scope.value === 'year' ||
    scope.value === 'all'
  ) {
    return false
  }
  return true
})

function pad(m) {
  return String(m).padStart(2, '0')
}

/** 如 2026-04-12 周六 */
function formatDateWithWeekday(dateStr) {
  if (dateStr == null || String(dateStr).trim() === '') return ''
  const s = String(dateStr).trim().slice(0, 10)
  const d = new Date(s.replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(dateStr)
  const w = ['日', '一', '二', '三', '四', '五', '六']
  return `${s} 周${w[d.getDay()]}`
}

const showEmptyHint = computed(() => {
  if (loading.value) return false
  if (scope.value === 'day') return !dayDetail.value
  if (scope.value === 'month') return !monthRows.value.length
  if (scope.value === 'year') return !yearMonthRows.value.length && !yearMonthRowsCompare.value.length
  if (scope.value === 'all') return !allYearRows.value.length
  return false
})

function fmtMoney(v) {
  if (v === null || v === undefined) return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}

function num(v) {
  if (v == null) return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/** 后端若启用 snake_case 序列化，与 camelCase 二选一读取 */
function rowAmountTriplet(r) {
  return {
    income: num(r?.incomeTotal ?? r?.income_total),
    expense: num(r?.expenseTotal ?? r?.expense_total),
    neutral: num(r?.neutralTotal ?? r?.neutral_total)
  }
}

function rowCountTriplet(r) {
  return {
    incomeCount: num(r?.incomeCount ?? r?.income_count),
    expenseCount: num(r?.expenseCount ?? r?.expense_count),
    neutralCount: num(r?.neutralCount ?? r?.neutral_count)
  }
}

function filterMonthDailyRows(rows, f) {
  if (!rows?.length) return []
  if (f === 'all') return rows
  return rows.filter((r) => {
    const t = rowAmountTriplet(r)
    if (f === 'income') return t.income > 0
    if (f === 'expense') return t.expense > 0
    if (f === 'neutral') return t.neutral > 0
    return true
  })
}

function normalizeMonthDailyRow(r) {
  if (!r || typeof r !== 'object') return r
  const a = rowAmountTriplet(r)
  const c = rowCountTriplet(r)
  return {
    ...r,
    incomeTotal: a.income,
    expenseTotal: a.expense,
    neutralTotal: a.neutral,
    incomeCount: c.incomeCount,
    expenseCount: c.expenseCount,
    neutralCount: c.neutralCount,
    incomeGrowthPercent: r.incomeGrowthPercent ?? r.income_growth_percent,
    expenseGrowthPercent: r.expenseGrowthPercent ?? r.expense_growth_percent
  }
}

function sumRows(rows) {
  let income = 0
  let expense = 0
  let neutral = 0
  for (const r of rows) {
    const t = rowAmountTriplet(r)
    income += t.income
    expense += t.expense
    neutral += t.neutral
  }
  return { income, expense, neutral }
}

function procRowsWith(rows, txF, timeAsc) {
  if (!rows) return []
  let r = rows.filter((row) => {
    if (txF.tradeType && !(row.tradeType || '').includes(txF.tradeType)) return false
    if (txF.paymentMethod && (row.paymentMethod || '') !== txF.paymentMethod) return false
    const a = Number(row.amountYuan)
    if (txF.amountMin != null && (Number.isNaN(a) || a < txF.amountMin)) return false
    if (txF.amountMax != null && (Number.isNaN(a) || a > txF.amountMax)) return false
    return true
  })
  const desc = !timeAsc
  r = [...r].sort((a, b) => {
    const ta = a.tradeTime || ''
    const tb = b.tradeTime || ''
    const c = ta.localeCompare(tb)
    return desc ? -c : c
  })
  return r
}

function proc(rows) {
  return procRowsWith(rows, txFilter, timeAscending.value)
}

function classifyTxKind(row) {
  const ie = row.incomeExpense || ''
  if (ie.includes('收入')) return 'income'
  if (ie.includes('支出')) return 'expense'
  if (isNeutralIncomeExpense(ie)) return 'neutral'
  return 'other'
}

function mergeSliceRows(slice) {
  const out = []
  for (const r of slice?.incomeTransactions || []) out.push(r)
  for (const r of slice?.expenseTransactions || []) out.push(r)
  for (const r of slice?.neutralTransactions || []) out.push(r)
  return out
}

/** 与后端分栏一致：支出(笔数)（金额）收入…中性… */
function sliceStatsInline(slice) {
  if (!slice) return ''
  const exp = slice.expenseTransactions?.length ?? 0
  const inc = slice.incomeTransactions?.length ?? 0
  const neu = slice.neutralTransactions?.length ?? 0
  const sumAmt = (rows) => (rows || []).reduce((s, r) => s + num(r.amountYuan), 0)
  const ee = sumAmt(slice.expenseTransactions)
  const ei = sumAmt(slice.incomeTransactions)
  const en = sumAmt(slice.neutralTransactions)
  return `支出(${exp}笔)（${fmtMoney(ee)}元）收入(${inc}笔)（${fmtMoney(ei)}元）中性(${neu}笔)（${fmtMoney(en)}元）`
}

function sliceBreakdown(slice) {
  if (!slice) {
    return {
      expenseCount: 0,
      expenseAmt: 0,
      incomeCount: 0,
      incomeAmt: 0,
      neutralCount: 0,
      neutralAmt: 0
    }
  }
  const sumAmt = (rows) => (rows || []).reduce((s, r) => s + num(r.amountYuan), 0)
  return {
    expenseCount: slice.expenseTransactions?.length ?? 0,
    expenseAmt: sumAmt(slice.expenseTransactions),
    incomeCount: slice.incomeTransactions?.length ?? 0,
    incomeAmt: sumAmt(slice.incomeTransactions),
    neutralCount: slice.neutralTransactions?.length ?? 0,
    neutralAmt: sumAmt(slice.neutralTransactions)
  }
}

function dayStatRows(b) {
  return [
    { kind: 'expense', label: '支出', count: b.expenseCount, amt: b.expenseAmt },
    { kind: 'income', label: '收入', count: b.incomeCount, amt: b.incomeAmt },
    { kind: 'neutral', label: '中性', count: b.neutralCount, amt: b.neutralAmt }
  ]
}

function sumCountsFromDailyRows(rows) {
  let incomeCount = 0
  let expenseCount = 0
  let neutralCount = 0
  for (const r of rows || []) {
    const c = rowCountTriplet(r)
    incomeCount += c.incomeCount
    expenseCount += c.expenseCount
    neutralCount += c.neutralCount
  }
  return { incomeCount, expenseCount, neutralCount }
}

function mapYearMonthRow(y, m, rows) {
  const t = sumRows(rows)
  const c = sumCountsFromDailyRows(rows)
  return {
    monthLabel: `${y}-${pad(m)}`,
    incomeTotal: t.income,
    expenseTotal: t.expense,
    neutralTotal: t.neutral,
    incomeCount: c.incomeCount,
    expenseCount: c.expenseCount,
    neutralCount: c.neutralCount
  }
}

function emptyYearMonthRow(y, m) {
  return {
    monthLabel: `${y}-${pad(m)}`,
    incomeTotal: 0,
    expenseTotal: 0,
    neutralTotal: 0,
    incomeCount: 0,
    expenseCount: 0,
    neutralCount: 0
  }
}

/** 某年表格/图表：始终 1–12 月一行不少，无数据月份补零（与某月日历铺满格子的体验一致） */
function ensureTwelveMonthRows(yearNum, rows) {
  const y = Number(yearNum)
  if (!Number.isFinite(y)) return []
  const byM = new Map()
  for (const r of rows || []) {
    const lab = String(r?.monthLabel ?? '').trim()
    const mm = lab.match(/^(\d{4})-(\d{2})$/)
    if (!mm) continue
    const rowY = parseInt(mm[1], 10)
    const m = parseInt(mm[2], 10)
    if (rowY !== y || m < 1 || m > 12) continue
    byM.set(m, r)
  }
  const out = []
  for (let m = 1; m <= 12; m++) {
    out.push(byM.has(m) ? { ...byM.get(m) } : emptyYearMonthRow(y, m))
  }
  return out
}

/** 由「日」或「月」行列表汇总支出/收入/中性的笔数与金额 */
function aggregateTxnStatsFromRows(rows) {
  if (!rows?.length) {
    return {
      expenseCount: 0,
      expenseAmt: 0,
      incomeCount: 0,
      incomeAmt: 0,
      neutralCount: 0,
      neutralAmt: 0
    }
  }
  let expenseCount = 0
  let incomeCount = 0
  let neutralCount = 0
  let expenseAmt = 0
  let incomeAmt = 0
  let neutralAmt = 0
  for (const r of rows) {
    const c = rowCountTriplet(r)
    const a = rowAmountTriplet(r)
    expenseCount += c.expenseCount
    incomeCount += c.incomeCount
    neutralCount += c.neutralCount
    expenseAmt += a.expense
    incomeAmt += a.income
    neutralAmt += a.neutral
  }
  return {
    expenseCount,
    expenseAmt,
    incomeCount,
    incomeAmt,
    neutralCount,
    neutralAmt
  }
}

function totalTxnCount(b) {
  if (!b) return 0
  return num(b.expenseCount) + num(b.incomeCount) + num(b.neutralCount)
}

/** 某日合并流水 + 收/支筛选 + 与 proc 相同的类型/金额筛选 */
function procDaySlice(slice) {
  let rows = mergeSliceRows(slice)
  if (dayFilter.value !== 'all') {
    const want = dayFilter.value
    rows = rows.filter((r) => classifyTxKind(r) === want)
  }
  return procRowsWith(rows, txFilter, timeAscending.value)
}

function procDrillDaySlice() {
  const slice = monthDrillDetail.value?.day
  if (!slice) return []
  let rows = mergeSliceRows(slice)
  if (drillDayFilter.value !== 'all') {
    rows = rows.filter((r) => classifyTxKind(r) === drillDayFilter.value)
  }
  return procRowsWith(rows, drillTxFilter, drillTimeAscending.value)
}

function disposeDrillChartsOnly() {
  chartDrillDay?.dispose()
  chartDrillMonth?.dispose()
  chartDrillYear?.dispose()
  chartDrillDay = chartDrillMonth = chartDrillYear = null
}

/** 主页面「某年」或抽屉内「某年」：打开该月抽屉视图 */
async function openDrillMonthFromYearRow(row) {
  const label = String(row.monthLabel || '')
  const m = label.match(/^(\d{4})-(\d{2})$/)
  if (!m) return
  const y = parseInt(m[1], 10)
  const mo = parseInt(m[2], 10)
  disposeDrillChartsOnly()
  drillMode.value = 'month'
  drillMonthY.value = y
  drillMonthM.value = mo
  drillMonthFlowFilter.value = 'all'
  drillYearMonthRows.value = []
  monthDrillDetail.value = null
  monthDrillVisible.value = true
  monthDrillLoading.value = true
  drillMonthRows.value = []
  try {
    const { data } = await api.get('/analytics/month', {
      params: { year: y, month: mo, ...analyticsUserParams() }
    })
    drillMonthRows.value = (data || []).map(normalizeMonthDailyRow)
    await nextTick()
    await nextTick()
    if (drillMonthTrendChartEl.value && drillMonthRows.value.length) {
      chartDrillMonth = echarts.init(drillMonthTrendChartEl.value)
      chartDrillMonth.setOption(buildMonthDailyTrendOption(drillMonthRows.value, `${y}-${pad(mo)} 月`))
      bindMonthTrendChartClick(chartDrillMonth, () => drillMonthRows.value)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载月度数据失败')
  } finally {
    monthDrillLoading.value = false
  }
}

/** 主页面「全部」：打开该年抽屉视图 */
async function openDrillYearFromAllRow(row) {
  const y = parseInt(String(row.monthLabel ?? '').trim(), 10)
  if (!Number.isFinite(y) || y < 1900 || y > 2100) return
  disposeDrillChartsOnly()
  drillMode.value = 'year'
  drillYearOnly.value = y
  drillMonthRows.value = []
  monthDrillDetail.value = null
  monthDrillVisible.value = true
  monthDrillLoading.value = true
  drillYearMonthRows.value = []
  try {
    const tasks = []
    for (let mo = 1; mo <= 12; mo++) {
      tasks.push(
        api
          .get('/analytics/month', { params: { year: y, month: mo, ...analyticsUserParams() } })
          .then((r) => ({ m: mo, rows: r.data }))
      )
    }
    const results = await Promise.all(tasks)
    drillYearMonthRows.value = results.map(({ m, rows }) =>
      mapYearMonthRow(y, m, (rows || []).map(normalizeMonthDailyRow))
    )
    await nextTick()
    await nextTick()
    if (drillYearTrendChartEl.value && drillYearMonthRows.value.length) {
      const yRows = ensureTwelveMonthRows(y, drillYearMonthRows.value)
      chartDrillYear = echarts.init(drillYearTrendChartEl.value)
      chartDrillYear.setOption(buildYearMonthlyTrendOption(yRows))
      bindYearMonthChartClick(chartDrillYear, () =>
        ensureTwelveMonthRows(drillYearOnly.value, drillYearMonthRows.value)
      )
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载年度数据失败')
  } finally {
    monthDrillLoading.value = false
  }
}

async function openMonthDayDrill(row) {
  const ds = String(row.date).slice(0, 10)
  monthDrillDate.value = ds
  drillDayFilter.value = 'all'
  drillTxFilter.tradeType = ''
  drillTxFilter.paymentMethod = ''
  drillTxFilter.amountMin = null
  drillTxFilter.amountMax = null
  drillTimeAscending.value = true
  disposeDrillChartsOnly()
  drillMode.value = 'day'
  drillMonthRows.value = []
  drillYearMonthRows.value = []
  monthDrillVisible.value = true
  monthDrillLoading.value = true
  monthDrillDetail.value = null
  try {
    const { data } = await api.get('/analytics/day-detail', { params: { date: ds, ...analyticsUserParams() } })
    monthDrillDetail.value = data
    await nextTick()
    await nextTick()
    if (drillDayTrendChartEl.value && data?.day) {
      chartDrillDay = echarts.init(drillDayTrendChartEl.value)
      chartDrillDay.setOption(buildDaySingleHourlyLineOption(data.day))
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载当日明细失败')
  } finally {
    monthDrillLoading.value = false
  }
}

function onMonthDrillClosed() {
  disposeDrillChartsOnly()
  monthDrillDetail.value = null
  drillMode.value = 'day'
  drillMonthRows.value = []
  drillYearMonthRows.value = []
  drillMonthY.value = null
  drillMonthM.value = null
  drillYearOnly.value = null
}

const drillTableRows = computed(() => procDrillDaySlice())

function disposeCharts() {
  chartDay?.dispose()
  chartDaySingle?.dispose()
  chartMonthDaily?.dispose()
  chartYearTrend?.dispose()
  chartYearTrendCmp?.dispose()
  chartAllTrend?.dispose()
  chartDay = chartDaySingle = chartMonthDaily = chartYearTrend = chartYearTrendCmp = chartAllTrend = null
}

/** 从交易时间字符串解析小时 0-23 */
function parseTradeHour(tradeTime) {
  if (tradeTime == null) return null
  const s = String(tradeTime).trim()
  const m = s.match(/\d{4}-\d{2}-\d{2}[ T](\d{2}):/)
  if (m) return parseInt(m[1], 10)
  const d = new Date(s.replace(/-/g, '/'))
  if (!Number.isNaN(d.getTime())) return d.getHours()
  return null
}

/** 将某日切片按小时汇总收入、支出（用于对比折线图） */
function aggregateHourlyIncomeExpense(slice) {
  const incomeByHour = Array.from({ length: 24 }, () => 0)
  const expenseByHour = Array.from({ length: 24 }, () => 0)
  const addRows = (rows, target) => {
    for (const r of rows || []) {
      const h = parseTradeHour(r.tradeTime)
      if (h == null || h < 0 || h > 23) continue
      const a = Number(r.amountYuan)
      if (!Number.isFinite(a)) continue
      target[h] += a
    }
  }
  addRows(slice?.incomeTransactions, incomeByHour)
  addRows(slice?.expenseTransactions, expenseByHour)
  return { incomeByHour, expenseByHour }
}

/** 两日对比：横轴仅包含有流水的小时，四条折线（各日收入/支出） */
function buildDayHourlyCompareLineOption(labelA, labelB, sliceA, sliceB) {
  const aggA = aggregateHourlyIncomeExpense(sliceA)
  const aggB = aggregateHourlyIncomeExpense(sliceB)
  const hourIndexes = []
  for (let h = 0; h < 24; h++) {
    const sum =
      num(aggA.incomeByHour[h]) +
      num(aggA.expenseByHour[h]) +
      num(aggB.incomeByHour[h]) +
      num(aggB.expenseByHour[h])
    if (sum > 0) hourIndexes.push(h)
  }
  if (hourIndexes.length === 0) {
    hourIndexes.push(0)
  }
  const labels = hourIndexes.map((h) => `${h}时`)
  const pick = (arr) => hourIndexes.map((h) => num(arr[h]))
  const sA = `基准 ${labelA}`
  const sB = `对比 ${labelB}`
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: [`${sA} 收入`, `${sA} 支出`, `${sB} 收入`, `${sB} 支出`],
      top: 6,
      left: 'center',
      orient: 'horizontal'
    },
    grid: { left: '2%', right: '1%', bottom: 12, top: 46, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      { name: `${sA} 收入`, type: 'line', smooth: true, data: pick(aggA.incomeByHour), itemStyle: { color: '#409eff' } },
      { name: `${sA} 支出`, type: 'line', smooth: true, data: pick(aggA.expenseByHour), itemStyle: { color: '#e6a23c' } },
      { name: `${sB} 收入`, type: 'line', smooth: true, data: pick(aggB.incomeByHour), itemStyle: { color: '#67c23a' } },
      { name: `${sB} 支出`, type: 'line', smooth: true, data: pick(aggB.expenseByHour), itemStyle: { color: '#f56c6c' } }
    ]
  }
}

/** 单日：按小时收入/支出，横轴仅有数据的小时 */
function buildDaySingleHourlyLineOption(slice) {
  const agg = aggregateHourlyIncomeExpense(slice)
  const hourIndexes = []
  for (let h = 0; h < 24; h++) {
    if (num(agg.incomeByHour[h]) + num(agg.expenseByHour[h]) > 0) hourIndexes.push(h)
  }
  if (hourIndexes.length === 0) hourIndexes.push(0)
  const labels = hourIndexes.map((h) => `${h}时`)
  const pick = (arr) => hourIndexes.map((h) => num(arr[h]))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'], top: 8, left: 'center', orient: 'horizontal' },
    grid: { left: '2%', right: '1%', bottom: 12, top: 40, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      { name: '收入', type: 'line', smooth: true, data: pick(agg.incomeByHour), itemStyle: { color: '#67c23a' } },
      { name: '支出', type: 'line', smooth: true, data: pick(agg.expenseByHour), itemStyle: { color: '#f56c6c' } }
    ]
  }
}

function monthDayLabel(row) {
  const d = row.date
  if (d == null) return ''
  const s = String(d)
  if (s.length >= 10) return `${parseInt(s.slice(8, 10), 10)}日`
  return s
}

/** 某月：按日收入/支出/中性折线 */
function buildMonthDailyTrendOption(rows, title) {
  const labels = (rows || []).map(monthDayLabel)
  const val = (r, k) => num(r[k])
  return {
    title: { text: title, left: 'center', top: 4, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['收入', '支出', '中性'],
      selected: { 收入: true, 支出: true, 中性: false },
      top: 30,
      left: 'center',
      orient: 'horizontal'
    },
    grid: { left: '3%', right: '4%', bottom: 24, top: 62, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      {
        name: '收入',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data: rows.map((r) => val(r, 'incomeTotal')),
        itemStyle: { color: '#67c23a' },
        emphasis: { focus: 'series' }
      },
      {
        name: '支出',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data: rows.map((r) => val(r, 'expenseTotal')),
        itemStyle: { color: '#f56c6c' },
        emphasis: { focus: 'series' }
      },
      {
        name: '中性',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data: rows.map((r) => val(r, 'neutralTotal')),
        itemStyle: { color: '#909399' },
        emphasis: { focus: 'series' }
      }
    ]
  }
}

function yearMonthAxisLabel(row) {
  if (row?.monthLabel != null) return String(row.monthLabel)
  return ''
}

/** 折线图 tooltip：金额保留两位小数，避免浮点长尾 */
function axisTooltipMoneyFormatter(params) {
  if (!params?.length) return ''
  const head = params[0].axisValueLabel ?? params[0].name ?? ''
  let html = `${head}<br/>`
  for (const p of params) {
    const v = p.value
    const n = Number(v)
    const s = Number.isFinite(n) ? n.toFixed(2) : '—'
    html += `${p.marker}${p.seriesName}: ${s}<br/>`
  }
  return html
}

/** 某年：按月份收入/支出/中性折线（与某月按日折线同结构） */
function buildYearMonthlyTrendOption(rows) {
  const list = rows || []
  const labels = list.map(yearMonthAxisLabel)
  const val = (r, k) => num(r[k])
  return {
    tooltip: { trigger: 'axis', formatter: axisTooltipMoneyFormatter },
    legend: {
      data: ['收入', '支出', '中性'],
      selected: { 收入: true, 支出: true, 中性: false },
      top: 8,
      left: 'center',
      orient: 'horizontal'
    },
    grid: { left: '3%', right: '4%', bottom: 28, top: 42, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      {
        name: '收入',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data: list.map((r) => val(r, 'incomeTotal')),
        itemStyle: { color: '#67c23a' },
        emphasis: { focus: 'series' }
      },
      {
        name: '支出',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data: list.map((r) => val(r, 'expenseTotal')),
        itemStyle: { color: '#f56c6c' },
        emphasis: { focus: 'series' }
      },
      {
        name: '中性',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data: list.map((r) => val(r, 'neutralTotal')),
        itemStyle: { color: '#e6a23c' },
        emphasis: { focus: 'series' }
      }
    ]
  }
}

function bindMonthTrendChartClick(chart, rowsGetter) {
  chart.off('click')
  chart.on('click', (params) => {
    const rows = rowsGetter()
    if (!rows?.length) return
    let idx = params.dataIndex
    if ((idx == null || idx < 0) && params.name) {
      idx = rows.findIndex((r) => monthDayLabel(r) === params.name)
    }
    if (idx == null || idx < 0) return
    const row = rows[idx]
    if (row) openMonthDayDrill(row)
  })
}

function bindYearMonthChartClick(chart, rowsGetter) {
  chart.off('click')
  chart.on('click', (params) => {
    const rows = rowsGetter()
    if (!rows?.length) return
    let idx = params.dataIndex
    if ((idx == null || idx < 0) && params.name) {
      idx = rows.findIndex((r) => yearMonthAxisLabel(r) === params.name)
    }
    if (idx == null || idx < 0) return
    const row = rows[idx]
    if (row) openDrillMonthFromYearRow(row)
  })
}

function bindAllYearsChartClick(chart, rowsGetter) {
  chart.off('click')
  chart.on('click', (params) => {
    const rows = rowsGetter()
    if (!rows?.length) return
    let idx = params.dataIndex
    if ((idx == null || idx < 0) && params.name) {
      idx = rows.findIndex((r) => yearMonthAxisLabel(r) === params.name)
    }
    if (idx == null || idx < 0) return
    const row = rows[idx]
    if (row) openDrillYearFromAllRow(row)
  })
}

async function refreshCompareCharts() {
  await nextTick()
  await nextTick()
  disposeCharts()
  if (!showCompareChart.value) return

  if (scope.value === 'day' && dayDetail.value?.day) {
    if (dayDetail.value.compareDay && compareChartEl.value) {
      const d0 = dayDetail.value.day
      const d1 = dayDetail.value.compareDay
      chartDay = echarts.init(compareChartEl.value)
      chartDay.setOption(buildDayHourlyCompareLineOption(date.value, compareDate.value, d0, d1))
    } else if (dayTrendChartEl.value) {
      chartDaySingle = echarts.init(dayTrendChartEl.value)
      chartDaySingle.setOption(buildDaySingleHourlyLineOption(dayDetail.value.day))
    }
  }
  if (scope.value === 'month' && monthRows.value.length) {
    if (monthDailyTrendChartEl.value) {
      chartMonthDaily = echarts.init(monthDailyTrendChartEl.value)
      chartMonthDaily.setOption(
        buildMonthDailyTrendOption(monthRows.value, `${year.value}-${pad(month.value)} 月`)
      )
      bindMonthTrendChartClick(chartMonthDaily, () => monthRows.value)
    }
  }
  if (scope.value === 'year' && yearMonthRows.value.length) {
    const yRows = ensureTwelveMonthRows(yearOnly.value, yearMonthRows.value)
    if (yearTrendChartEl.value) {
      chartYearTrend = echarts.init(yearTrendChartEl.value)
      chartYearTrend.setOption(buildYearMonthlyTrendOption(yRows))
      bindYearMonthChartClick(chartYearTrend, () =>
        ensureTwelveMonthRows(yearOnly.value, yearMonthRows.value)
      )
    }
    if (yearCompareOn.value && yearMonthRowsCompare.value.length && yearTrendChartCompareEl.value) {
      const yRowsCmp = ensureTwelveMonthRows(cmpYearOnly.value, yearMonthRowsCompare.value)
      chartYearTrendCmp = echarts.init(yearTrendChartCompareEl.value)
      chartYearTrendCmp.setOption(buildYearMonthlyTrendOption(yRowsCmp))
      bindYearMonthChartClick(chartYearTrendCmp, () =>
        ensureTwelveMonthRows(cmpYearOnly.value, yearMonthRowsCompare.value)
      )
    }
  }
  if (scope.value === 'all' && allYearRows.value.length) {
    if (allYearTrendChartEl.value) {
      chartAllTrend = echarts.init(allYearTrendChartEl.value)
      chartAllTrend.setOption(buildYearMonthlyTrendOption(allYearRows.value))
      bindAllYearsChartClick(chartAllTrend, () => allYearRows.value)
    }
  }
}

function onResize() {
  chartDay?.resize()
  chartDaySingle?.resize()
  chartMonthDaily?.resize()
  chartYearTrend?.resize()
  chartYearTrendCmp?.resize()
  chartAllTrend?.resize()
  chartDrillDay?.resize()
  chartDrillMonth?.resize()
  chartDrillYear?.resize()
}

function toggleReal() {
  useReal.value = !useReal.value
  load()
}

function onScopeChange() {
  monthDrillVisible.value = false
  dayDetail.value = null
  rollingData.value = null
  monthRows.value = []
  monthRowsCompare.value = []
  yearMonthRows.value = []
  yearMonthRowsCompare.value = []
  allYearRows.value = []
  monthFlowFilter.value = 'all'
  allFlowFilter.value = 'all'
  drillMonthFlowFilter.value = 'all'
  summary.value = { income: null, expense: null, neutral: null }
  summaryCompare.value = { income: null, expense: null, neutral: null }
  disposeCharts()
  load()
}

function onDayInputsChange() {
  load()
}

function onMonthYearChange() {
  monthFlowFilter.value = 'all'
  load()
}

function onYearChange() {
  if (!yearCompareOn.value) load()
}

async function loadPhones() {
  try {
    const { data } = await api.get('/me/bill-phones')
    phones.value = data || []
    if (phones.value.length) {
      if (multiPhone.value) {
        if (!phoneIds.value?.length) {
          phoneIds.value = phones.value.map((p) => p.id)
        }
      } else if (phoneId.value == null || phoneId.value === '') {
        phoneId.value = phones.value[0].id
      }
    }
  } catch {
    phones.value = []
  }
}

async function loadByTypeTotals(from, to) {
  const base = analyticsUserParams()
  const [inc, exp, neu] = await Promise.all([
    api.get('/analytics/by-type', { params: { type: 'income', from, to, ...base } }),
    api.get('/analytics/by-type', { params: { type: 'expense', from, to, ...base } }),
    api.get('/analytics/by-type', { params: { type: 'neutral', from, to, ...base } })
  ])
  return {
    income: inc.data.totalAmount,
    expense: exp.data.totalAmount,
    neutral: neu.data.totalAmount
  }
}

/** 历年「全部」视图：按自然年汇总时需金额与笔数 */
const ALL_YEAR_MIN = 2000

async function loadByTypeTotalsWithCounts(from, to) {
  const base = analyticsUserParams()
  const [inc, exp, neu] = await Promise.all([
    api.get('/analytics/by-type', { params: { type: 'income', from, to, ...base } }),
    api.get('/analytics/by-type', { params: { type: 'expense', from, to, ...base } }),
    api.get('/analytics/by-type', { params: { type: 'neutral', from, to, ...base } })
  ])
  const ic = inc.data?.transactionCount ?? inc.data?.transaction_count
  const ec = exp.data?.transactionCount ?? exp.data?.transaction_count
  const nc = neu.data?.transactionCount ?? neu.data?.transaction_count
  return {
    income: inc.data.totalAmount,
    expense: exp.data.totalAmount,
    neutral: neu.data.totalAmount,
    incomeCount: num(ic),
    expenseCount: num(ec),
    neutralCount: num(nc)
  }
}

async function loadRolling() {
  if (!date.value) {
    rollingData.value = null
    return
  }
  try {
    const { data } = await api.get('/analytics/rolling-income-expense', {
      params: { endDate: date.value, ...analyticsUserParams() }
    })
    rollingData.value = data
  } catch {
    rollingData.value = null
  }
}

async function load() {
  loading.value = true
  try {
    disposeCharts()
    if (scope.value === 'day') {
      const params = { date: date.value, ...analyticsUserParams() }
      const cd = compareDate.value
      if (cd != null && String(cd).trim() !== '') params.compareDate = cd
      const { data } = await api.get('/analytics/day-detail', { params })
      dayDetail.value = data
      summary.value = {
        income: data.day?.incomeTotal,
        expense: data.day?.expenseTotal,
        neutral: data.day?.neutralTotal
      }
      if (data.compareDay) {
        summaryCompare.value = {
          income: data.compareDay.incomeTotal,
          expense: data.compareDay.expenseTotal,
          neutral: data.compareDay.neutralTotal
        }
      } else {
        summaryCompare.value = { income: null, expense: null, neutral: null }
      }
      await loadRolling()
      monthRows.value = []
      monthRowsCompare.value = []
      yearMonthRows.value = []
      yearMonthRowsCompare.value = []
      allYearRows.value = []
      await refreshCompareCharts()
      return
    }

    rollingData.value = null
    dayDetail.value = null

    if (scope.value === 'month') {
      const { data } = await api.get('/analytics/month', {
        params: { year: year.value, month: month.value, ...analyticsUserParams() }
      })
      const rows = (data || []).map(normalizeMonthDailyRow)
      monthRows.value = rows
      const s = sumRows(rows)
      summary.value = { income: s.income, expense: s.expense, neutral: s.neutral }
      monthRowsCompare.value = []
      summaryCompare.value = { income: null, expense: null, neutral: null }
      yearMonthRows.value = []
      yearMonthRowsCompare.value = []
      allYearRows.value = []
      await refreshCompareCharts()
      return
    }

    if (scope.value === 'year') {
      const y = yearOnly.value
      summary.value = await loadByTypeTotals(`${y}-01-01`, `${y}-12-31`)
      const tasks = []
      for (let m = 1; m <= 12; m++) {
        tasks.push(
          api.get('/analytics/month', { params: { year: y, month: m, ...analyticsUserParams() } }).then((r) => ({ m, rows: r.data }))
        )
      }
      const results = await Promise.all(tasks)
      yearMonthRows.value = results.map(({ m, rows }) =>
        mapYearMonthRow(y, m, (rows || []).map(normalizeMonthDailyRow))
      )
      if (yearCompareOn.value) {
        const y2 = cmpYearOnly.value
        summaryCompare.value = await loadByTypeTotals(`${y2}-01-01`, `${y2}-12-31`)
        const tasks2 = []
        for (let m = 1; m <= 12; m++) {
          tasks2.push(
            api.get('/analytics/month', { params: { year: y2, month: m, ...analyticsUserParams() } }).then((r) => ({ m, rows: r.data }))
          )
        }
        const results2 = await Promise.all(tasks2)
        yearMonthRowsCompare.value = results2.map(({ m, rows }) =>
          mapYearMonthRow(y2, m, (rows || []).map(normalizeMonthDailyRow))
        )
      } else {
        yearMonthRowsCompare.value = []
        summaryCompare.value = { income: null, expense: null, neutral: null }
      }
      monthRows.value = []
      monthRowsCompare.value = []
      allYearRows.value = []
      await refreshCompareCharts()
      return
    }

    if (scope.value === 'all') {
      summary.value = await loadByTypeTotals(undefined, undefined)
      const cy = new Date().getFullYear()
      const tasks = []
      for (let y = ALL_YEAR_MIN; y <= cy; y++) {
        tasks.push(
          loadByTypeTotalsWithCounts(`${y}-01-01`, `${y}-12-31`).then((t) => ({ y, t }))
        )
      }
      const results = await Promise.all(tasks)
      allYearRows.value = results
        .filter(
          ({ t }) =>
            num(t.income) + num(t.expense) + num(t.neutral) > 0 ||
            num(t.incomeCount) + num(t.expenseCount) + num(t.neutralCount) > 0
        )
        .map(({ y, t }) => ({
          monthLabel: String(y),
          incomeTotal: num(t.income),
          expenseTotal: num(t.expense),
          neutralTotal: num(t.neutral),
          incomeCount: num(t.incomeCount),
          expenseCount: num(t.expenseCount),
          neutralCount: num(t.neutralCount)
        }))
      monthRows.value = []
      monthRowsCompare.value = []
      yearMonthRows.value = []
      yearMonthRowsCompare.value = []
      await refreshCompareCharts()
      return
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadPhones()
  await load()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

<style scoped>
.analytics-hub {
  width: 100%;
  box-sizing: border-box;
  padding: 0 0 24px;
}

.top-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.top-bar-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  flex: 1;
  min-width: 0;
}

.top-bar-scope-fields {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
}

.field-lbl {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.top-bar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.scope-tabs {
  flex-shrink: 0;
}

.compare-chart {
  width: 100%;
  height: 260px;
  margin-bottom: 16px;
}

/* 某日视图：折线绘图区贴底、整体少留白 */
.day-flow-wrap .compare-chart {
  margin-bottom: 4px;
}

.summary-row {
  margin-bottom: 20px;
}

.sum-row-three {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  width: 100%;
}

@media (max-width: 768px) {
  .sum-row-three {
    grid-template-columns: 1fr;
  }
}

.sum-card {
  border-radius: 8px;
  padding: 14px 18px;
  border: 1px solid #ebeef5;
}

.sum-card.income {
  border-color: #b3d8ff;
  background: #ecf5ff;
}
.sum-card.expense {
  border-color: #fbc4c4;
  background: #fef0f0;
}
.sum-card.neutral {
  border-color: #ffe58f;
  background: #fffbe6;
}

.sum-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}

.sum-val {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.meta-extra {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.filter-label {
  font-size: 13px;
  color: #606266;
}

.filter-toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.sum-compare-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 900px) {
  .sum-compare-split {
    grid-template-columns: 1fr;
  }
}

.sum-compare-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.sum-compare-col .sum-row-three {
  width: 100%;
}

.tx-lbl,
.tx-sep {
  font-size: 13px;
  color: #909399;
}

.compare-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 900px) {
  .compare-split {
    grid-template-columns: 1fr;
  }
}

.compare-pane {
  min-width: 0;
}

.pane-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
  font-size: 14px;
}
.pane-title.pane-sub {
  margin-top: 16px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.month-flow-wrap {
  margin-bottom: 16px;
}

.month-trend-chart {
  cursor: pointer;
}

.month-drill-inner {
  min-height: 200px;
}
.drill-hint {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
  line-height: 1.5;
}
.drill-day-chart {
  height: 240px;
  margin-bottom: 8px;
}
.drill-toolbar {
  margin-top: 4px;
}

.table-block {
  margin-bottom: 12px;
}

.table-caption {
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 13px;
}
.table-caption.income {
  color: #409eff;
}
.table-caption.expense {
  color: #f56c6c;
}
.table-caption.neutral {
  color: #ad6800;
}
.table-caption.all-tx {
  color: #303133;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 8px;
}
.inline-slice-stats {
  font-weight: 500;
  color: #606266;
  font-size: 13px;
}

.day-flow-wrap {
  margin-bottom: 8px;
}

.day-chart-with-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 16px;
  margin-bottom: 10px;
  width: 100%;
  box-sizing: border-box;
}

/* 某日对比：左侧汇总宽度封顶，趋势图占满剩余横向空间；勿与图等高拉伸，避免汇总框底部大块留白 */
.day-chart-with-summary--compare {
  flex-wrap: nowrap;
  align-items: flex-start;
  gap: 8px;
}

@media (max-width: 1100px) {
  .day-chart-with-summary--compare {
    flex-wrap: wrap;
  }
}

.day-chart-with-summary--compare .day-chart-pane {
  flex: 1 1 0%;
  min-width: 0;
  width: 0;
}

.day-chart-pane {
  flex: 1 1 0%;
  min-width: 0;
  margin-bottom: 0;
}

.day-summary-side {
  flex: 0 0 200px;
  width: 200px;
  max-width: 100%;
  padding: 10px 10px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f7fa 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-summary-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.day-summary-head-plain {
  justify-content: flex-start;
}

.day-summary-date-only {
  font-weight: 700;
  font-size: 15px;
  color: #303133;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;
}

.day-summary-total-pill {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.day-stat-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.day-stat-chip {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  align-items: center;
  padding: 6px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  border: 1px solid transparent;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.day-stat-count,
.day-stat-amt {
  text-align: right;
  font-weight: 600;
}

.day-stat-chip.expense {
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  border-color: #ffccc7;
  color: #a8071a;
}

.day-stat-chip.income {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7d5 100%);
  border-color: #b7eb8f;
  color: #237804;
}

.day-stat-chip.neutral {
  background: linear-gradient(135deg, #fffbe6 0%, #fff1b8 100%);
  border-color: #ffe58f;
  color: #ad6800;
}

.day-summary-side.day-summary-compare {
  flex: 0 0 auto;
  width: auto;
  min-width: 0;
  max-width: min(100%, 520px);
  flex-direction: row;
  flex-wrap: wrap;
  padding: 12px;
  gap: 12px;
  align-content: flex-start;
}

.day-summary-block {
  flex: 1 1 200px;
  min-width: 180px;
  max-width: 220px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f7fa 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rolling-h {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin: 12px 0 8px;
}

.data-table {
  width: 100%;
}
</style>
