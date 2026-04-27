// ========== 图表主题配置 ==========
const chartTheme = {
    color: ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899'],
    backgroundColor: 'transparent',
    textStyle: {
        fontFamily: 'Segoe UI, system-ui, sans-serif'
    }
};

// ECharts 主题应用
if (echarts) {
    echarts.registerTheme('sleepTheme', chartTheme);
}

// ========== 全局变量 ==========
let ageDistributionChart, disorderRiskChart, occupationChart, durationQualityChart, stressChart, chronotypeChart;

// ========== 页面初始化 ==========
$(document).ready(function() {
    loadStats();
    initCharts();
    loadChartData();
});

// ========== 加载统计数据 ==========
function loadStats() {
    $.ajax({
        url: '/sleep/api/stats',
        method: 'GET',
        success: function(data) {
            $('#totalRecords').text(data.totalRecords || 0);
            $('#avgSleepDuration').text((data.avgSleepDuration || 0).toFixed(2) + ' 小时');
            $('#avgSleepQuality').text((data.avgSleepQuality || 0).toFixed(2) + ' 分');
            $('#avgStressScore').text((data.avgStressScore || 0).toFixed(1));
        },
        error: function() {
            console.log('统计数据加载失败');
        }
    });
}

// ========== 初始化图表 ==========
function initCharts() {
    // 年龄分布 - 柱状图
    ageDistributionChart = echarts.init(document.getElementById('ageDistributionChart'), 'sleepTheme');

    // 睡眠障碍风险 - 环形图
    disorderRiskChart = echarts.init(document.getElementById('disorderRiskChart'), 'sleepTheme');

    // 职业睡眠时长 - 水平柱状图
    occupationChart = echarts.init(document.getElementById('occupationChart'), 'sleepTheme');

    // 睡眠时长 vs 质量 - 散点图
    durationQualityChart = echarts.init(document.getElementById('durationQualityChart'), 'sleepTheme');

    // 压力分布 - 柱状图
    stressChart = echarts.init(document.getElementById('stressChart'), 'sleepTheme');

    // 作息类型质量 - 折线图
    chronotypeChart = echarts.init(document.getElementById('chronotypeChart'), 'sleepTheme');

    // 响应式
    $(window).resize(function() {
        ageDistributionChart.resize();
        disorderRiskChart.resize();
        occupationChart.resize();
        durationQualityChart.resize();
        stressChart.resize();
        chronotypeChart.resize();
    });
}

// ========== 加载图表数据 ==========
function loadChartData() {
    loadAgeDistribution();
    loadDisorderRisk();
    loadOccupationSleep();
    loadDurationQuality();
    loadStressDistribution();
    loadChronotypeQuality();
}

function loadAgeDistribution() {
    $.ajax({
        url: '/sleep/api/chart/age-distribution',
        method: 'GET',
        success: function(data) {
            const xData = data.map(function(item) { return item.age_group; });
            const yData = data.map(function(item) { return item.count; });

            const option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    top: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: xData,
                    axisLabel: { color: '#64748b' },
                    axisLine: { lineStyle: { color: '#e2e8f0' } }
                },
                yAxis: {
                    type: 'value',
                    axisLabel: { color: '#64748b' },
                    splitLine: { lineStyle: { color: '#f1f5f9' } }
                },
                series: [{
                    name: '人数',
                    type: 'bar',
                    data: yData,
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: '#6366f1' },
                            { offset: 1, color: '#8b5cf6' }
                        ]),
                        borderRadius: [4, 4, 0, 0]
                    },
                    barWidth: '50%'
                }]
            };

            ageDistributionChart.setOption(option);
        }
    });
}

function loadDisorderRisk() {
    $.ajax({
        url: '/sleep/api/chart/sleep-disorder-risk',
        method: 'GET',
        success: function(data) {
            const option = {
                tooltip: {
                    trigger: 'item',
                    formatter: '{b}: {c} ({d}%)'
                },
                legend: {
                    orient: 'vertical',
                    right: '5%',
                    top: 'center',
                    textStyle: { color: '#64748b' }
                },
                series: [{
                    name: '睡眠障碍风险',
                    type: 'pie',
                    radius: ['40%', '70%'],
                    center: ['35%', '50%'],
                    avoidLabelOverlap: false,
                    itemStyle: {
                        borderRadius: 6,
                        borderColor: '#fff',
                        borderWidth: 2
                    },
                    label: {
                        show: false
                    },
                    emphasis: {
                        label: {
                            show: true,
                            fontSize: 14,
                            fontWeight: 'bold'
                        }
                    },
                    data: data.map(function(item, index) {
                        return {
                            value: item.value,
                            name: item.name,
                            itemStyle: {
                                color: ['#10b981', '#f59e0b', '#ef4444'][index] || '#6366f1'
                            }
                        };
                    })
                }]
            };

            disorderRiskChart.setOption(option);
        }
    });
}

function loadOccupationSleep() {
    $.ajax({
        url: '/sleep/api/chart/sleep-duration-by-occupation',
        method: 'GET',
        success: function(data) {
            const xData = data.map(function(item) { return item.name; });
            const yData = data.map(function(item) { return item.value; });

            const option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' },
                    formatter: '{b}: {c} 小时'
                },
                grid: {
                    left: '3%',
                    right: '8%',
                    bottom: '3%',
                    top: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'value',
                    axisLabel: {
                        color: '#64748b',
                        formatter: '{value}h'
                    },
                    splitLine: { lineStyle: { color: '#f1f5f9' } }
                },
                yAxis: {
                    type: 'category',
                    data: xData.reverse(),
                    axisLabel: { color: '#64748b', width: 100, overflow: 'truncate' },
                    axisLine: { lineStyle: { color: '#e2e8f0' } }
                },
                series: [{
                    name: '睡眠时长',
                    type: 'bar',
                    data: yData.reverse(),
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                            { offset: 0, color: '#6366f1' },
                            { offset: 1, color: '#8b5cf6' }
                        ]),
                        borderRadius: [0, 4, 4, 0]
                    },
                    barWidth: '60%'
                }]
            };

            occupationChart.setOption(option);
        }
    });
}

function loadDurationQuality() {
    $.ajax({
        url: '/sleep/api/chart/sleep-duration-vs-quality',
        method: 'GET',
        success: function(data) {
            const scatterData = data.map(function(item) {
                return [item.x, item.y];
            });

            const option = {
                tooltip: {
                    formatter: function(params) {
                        return '睡眠时长: ' + params.value[0].toFixed(1) + '小时<br>睡眠质量: ' + params.value[1].toFixed(1) + '分';
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    top: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'value',
                    name: '睡眠时长 (小时)',
                    nameLocation: 'center',
                    nameGap: 30,
                    axisLabel: { color: '#64748b' },
                    splitLine: { lineStyle: { color: '#f1f5f9' } }
                },
                yAxis: {
                    type: 'value',
                    name: '睡眠质量 (分)',
                    nameLocation: 'center',
                    nameGap: 40,
                    axisLabel: { color: '#64748b' },
                    splitLine: { lineStyle: { color: '#f1f5f9' } }
                },
                series: [{
                    type: 'scatter',
                    symbolSize: 8,
                    data: scatterData,
                    itemStyle: {
                        color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.5, [
                            { offset: 0, color: 'rgba(99, 102, 241, 0.8)' },
                            { offset: 1, color: 'rgba(99, 102, 241, 0.2)' }
                        ])
                    },
                    emphasis: {
                        itemStyle: {
                            color: '#6366f1'
                        }
                    }
                }]
            };

            durationQualityChart.setOption(option);
        }
    });
}

function loadStressDistribution() {
    $.ajax({
        url: '/sleep/api/chart/stress-distribution',
        method: 'GET',
        success: function(data) {
            const xData = data.map(function(item) { return item.name; });
            const yData = data.map(function(item) { return item.value; });

            const option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    top: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: xData,
                    axisLabel: { color: '#64748b' },
                    axisLine: { lineStyle: { color: '#e2e8f0' } }
                },
                yAxis: {
                    type: 'value',
                    axisLabel: { color: '#64748b' },
                    splitLine: { lineStyle: { color: '#f1f5f9' } }
                },
                series: [{
                    name: '人数',
                    type: 'bar',
                    data: yData,
                    itemStyle: {
                        color: function(params) {
                            const colors = ['#10b981', '#6366f1', '#f59e0b', '#ef4444'];
                            return colors[params.dataIndex] || '#6366f1';
                        },
                        borderRadius: [4, 4, 0, 0]
                    },
                    barWidth: '50%'
                }]
            };

            stressChart.setOption(option);
        }
    });
}

function loadChronotypeQuality() {
    $.ajax({
        url: '/sleep/api/chart/sleep-quality-by-chronotype',
        method: 'GET',
        success: function(data) {
            const xData = data.map(function(item) { return item.name; });
            const yData = data.map(function(item) { return item.value; });

            const option = {
                tooltip: {
                    trigger: 'axis',
                    formatter: '{b}: {c} 分'
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    top: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: xData,
                    axisLabel: { color: '#64748b' },
                    axisLine: { lineStyle: { color: '#e2e8f0' } },
                    boundaryGap: false
                },
                yAxis: {
                    type: 'value',
                    name: '睡眠质量',
                    axisLabel: { color: '#64748b' },
                    splitLine: { lineStyle: { color: '#f1f5f9' } }
                },
                series: [{
                    name: '睡眠质量',
                    type: 'line',
                    data: yData,
                    smooth: true,
                    symbol: 'circle',
                    symbolSize: 8,
                    lineStyle: {
                        color: '#8b5cf6',
                        width: 3
                    },
                    itemStyle: {
                        color: '#8b5cf6'
                    },
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: 'rgba(139, 92, 246, 0.3)' },
                            { offset: 1, color: 'rgba(139, 92, 246, 0.05)' }
                        ])
                    }
                }]
            };

            chronotypeChart.setOption(option);
        }
    });
}
