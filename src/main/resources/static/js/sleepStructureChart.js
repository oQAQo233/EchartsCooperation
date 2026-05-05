let sleepStructureChart;
let currentMinAge;
let currentMaxAge;
let sliderInstance = null;
let cachedData = null;
let isLoading = false;
let pendingLoad = null;

// Category visibility state
let categoryVisible = {
    'gender': true,
    'chronotype': true,
    'mentalHealth': true
};

const COLORS = {
    deep: '#2c5282',
    rem: '#4299e1',
    light: '#cbd5e0',
    quality: '#f6ad55',
    text: '#4a5568'
};

$(document).ready(function() {
    currentMinAge = parseInt($('#ageRangeText span:first').text()) || 0;
    currentMaxAge = parseInt($('#ageRangeText span:last').text()) || 100;

    initChart();
    initSlider();
    initCategoryLegend();
    loadChartData(currentMinAge, currentMaxAge);
});

function initChart() {
    sleepStructureChart = echarts.init(document.getElementById('sleepStructureChart'));

    $(window).resize(function() {
        sleepStructureChart.resize();
    });
}

function initSlider() {
    const sliderDom = document.getElementById('ageSlider');
    sliderInstance = echarts.init(sliderDom);

    const baseMinAge = currentMinAge;
    const baseMaxAge = currentMaxAge;

    const option = {
        xAxis: {
            type: 'value',
            min: baseMinAge,
            max: baseMaxAge,
            splitLine: { show: false },
            axisLine: { lineStyle: { color: '#e2e8f0' } },
            axisLabel: { color: COLORS.text }
        },
        yAxis: {
            type: 'value',
            show: false,
            min: 0,
            max: 100
        },
        dataZoom: [{
            type: 'slider',
            xAxisIndex: 0,
            start: 0,
            end: 100,
            height: 40,
            bottom: 10,
            borderColor: '#e2e8f0',
            backgroundColor: '#f7fafc',
            fillerColor: 'rgba(99, 102, 241, 0.1)',
            handleStyle: {
                color: '#6366f1',
                borderColor: '#6366f1'
            },
            textStyle: {
                color: COLORS.text
            },
            dataBackground: {
                lineStyle: { color: '#e2e8f0' },
                areaStyle: { color: '#f7fafc' }
            }
        }],
        series: [{
            type: 'line',
            data: [],
            symbol: 'none'
        }]
    };

    sliderInstance.setOption(option);

    // 处理滑块移动
    sliderInstance.on('datazoom', function(params) {
        const opt = sliderInstance.getOption();
        const xAxis = opt.xAxis[0];
        const totalMin = xAxis.min;
        const totalMax = xAxis.max;
        const range = totalMax - totalMin;

        const start = params.start;
        const end = params.end;
        const newMin = Math.round(totalMin + (range * start / 100));
        const newMax = Math.round(totalMin + (range * end / 100));

        currentMinAge = newMin;
        currentMaxAge = newMax;
        updateAgeLabel();

        if (cachedData) {
            renderChart(cachedData);
        }

        clearTimeout(window.sliderTimeout);
        window.sliderTimeout = setTimeout(function() {
            loadChartData(newMin, newMax);
        }, 50);
    });

    // 双击重置整个视图
    sleepStructureChart.getZr().on('dblclick', function() {
        currentMinAge = parseInt($('#ageRangeText span:first').text()) || 0;
        currentMaxAge = parseInt($('#ageRangeText span:last').text()) || 100;

        sliderInstance.dispatchAction({
            type: 'dataZoom',
            xAxisIndex: 0,
            start: 0,
            end: 100
        });

        if (cachedData) {
            renderChart(cachedData);
        }
    });
}

function updateAgeLabel() {
    const range = currentMaxAge - currentMinAge;
    const totalRange = currentMaxAge - currentMinAge;
    if (range >= totalRange) {
        $('#currentAgeRange').text('全年龄段');
    } else {
        $('#currentAgeRange').text(currentMinAge + '-' + currentMaxAge + ' 岁');
    }
    $('#ageRangeText').html('年龄范围 <span>' + currentMinAge + '</span>-<span>' + currentMaxAge + '</span> 岁');
}

function initCategoryLegend() {
    $('.category-legend-item').on('click', function() {
        const category = $(this).data('category');
        const isInactive = $(this).hasClass('inactive');

        if (isInactive) {
            $(this).removeClass('inactive');
            categoryVisible[category] = true;
        } else {
            $(this).addClass('inactive');
            categoryVisible[category] = false;
        }
        loadChartData(currentMinAge, currentMaxAge);
    });
}

function loadChartData(minAge, maxAge) {
    // 取消旧请求，防止请求积压
    if (window.currentController) {
        window.currentController.abort();
    }
    window.currentController = new AbortController();

    if (isLoading) {
        pendingLoad = { minAge: minAge, maxAge: maxAge };
        return;
    }

    isLoading = true;

    $.ajax({
        url: '/sleep/sleep-structure/api/chart-data',
        method: 'GET',
        data: { minAge: minAge, maxAge: maxAge },
        signal: window.currentController.signal,
        success: function(data) {
            cachedData = data;
            renderChart(data);

            if (pendingLoad && (pendingLoad.minAge !== minAge || pendingLoad.maxAge !== maxAge)) {
                const next = pendingLoad;
                pendingLoad = null;
                isLoading = false;
                loadChartData(next.minAge, next.maxAge);
            } else {
                pendingLoad = null;
                isLoading = false;
            }
        },
        error: function() {
            isLoading = false;
            pendingLoad = null;
        }
    });
}

function renderChart(data) {
    // 后端已按固定顺序返回数据，前端直接使用即可
    const categories = [];
    const deepData = [];
    const remData = [];
    const lightData = [];
    const qualityData = [];
    const wakeData = [];

    function processGroup(items, type) {
        items.forEach(function(item) {
            // 注意：根据后端返回的字段名调整，此处兼容常见的 key
            const label = formatLabel(item.gender || item.chronotype || item.mental_health_condition, type);
            categories.push(label);
            const duration = parseFloat(item.avg_duration) || 0;
            const deepPct = parseFloat(item.avg_deep) || 0;
            const remPct = parseFloat(item.avg_rem) || 0;
            const lightPct = Math.max(0, 100 - deepPct - remPct);

            deepData.push(duration * deepPct / 100);
            remData.push(duration * remPct / 100);
            lightData.push(duration * lightPct / 100);
            qualityData.push(parseFloat(item.avg_quality) || 0);
            wakeData.push(parseFloat(item.avg_wake_episodes) || 0);
        });
    }

    if (categoryVisible.gender) {
        processGroup(data.gender, 'gender');
    }
    if (categoryVisible.chronotype) {
        processGroup(data.chronotype, 'chronotype');
    }
    if (categoryVisible.mentalHealth) {
        processGroup(data.mentalHealth, 'mental');
    }

    const maxDuration = deepData.length > 0
        ? Math.max(...deepData.map((v, i) => v + (remData[i] || 0) + (lightData[i] || 0)), 1)
        : 10;

    const option = {
        animation: true,
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e2e8f0',
            borderWidth: 1,
            padding: [12, 16],
            textStyle: { color: COLORS.text },
            formatter: function(params) {
                if (!params || params.length === 0) return '';
                let result = '<div style="font-weight: 600; margin-bottom: 8px;">' + params[0].name + '</div>';
                let totalDuration = 0;
                params.forEach(function(p) {
                    if (p.seriesName !== '睡眠质量评分') {
                        totalDuration += p.value || 0;
                    }
                });
                result += '<div style="margin-bottom: 4px;">总睡眠时长: <strong>' + totalDuration.toFixed(2) + ' 小时</strong></div>';
                params.forEach(function(p) {
                    if (p.seriesName !== '睡眠质量评分') {
                        const pct = totalDuration > 0 ? (p.value / totalDuration * 100).toFixed(1) : 0;
                        result += '<div style="color: ' + p.color + '; margin-bottom: 2px;">' + p.seriesName + ': ' + (p.value || 0).toFixed(2) + ' 小时 (' + pct + '%)</div>';
                    }
                });
                const qualityParam = params.find(p => p.seriesName === '睡眠质量评分');
                if (qualityParam) {
                    result += '<div style="margin-top: 6px; color: #f6ad55;">睡眠质量评分: <strong>' + (qualityParam.value || 0).toFixed(1) + ' 分</strong></div>';
                }
                const wakeParam = params.find(p => p.seriesName === '夜间醒来次数');
                if (wakeParam) {
                    result += '<div style="color: #ef4444;">夜间醒来次数: <strong>' + (wakeParam.value || 0).toFixed(1) + ' 次</strong></div>';
                }
                return result;
            }
        },
        legend: {
            show: true,
            top: 0,
            itemGap: 30,
            textStyle: { color: COLORS.text },
            itemWidth: 20,
            itemHeight: 14
        },
        grid: {
            left: '3%',
            right: '8%',
            bottom: '12%',
            top: 40,
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: categories,
            axisLine: { lineStyle: { color: '#e2e8f0' } },
            axisLabel: {
                color: COLORS.text,
                interval: 0,
                fontSize: 12
            },
            axisTick: { show: false }
        },
        yAxis: [
            {
                type: 'value',
                name: '平均总睡眠时长',
                nameTextStyle: { color: COLORS.text, fontSize: 12 },
                min: 0,
                max: Math.ceil(maxDuration + 1),
                splitLine: { lineStyle: { color: '#f1f5f9' } },
                axisLine: { show: false },
                axisLabel: { color: COLORS.text }
            },
            {
                type: 'value',
                name: '平均睡眠质量评分',
                nameTextStyle: { color: COLORS.text, fontSize: 12 },
                min: 0,
                max: 10,
                splitLine: { show: false },
                axisLine: { show: false },
                axisLabel: { color: COLORS.text }
            }
        ],
        series: [
            {
                name: '深度睡眠',
                type: 'bar',
                stack: 'sleep',
                large: true,
                barMaxWidth: 50,
                itemStyle: {
                    color: COLORS.deep,
                    borderRadius: [0, 0, 0, 0]
                },
                data: deepData
            },
            {
                name: 'REM睡眠',
                type: 'bar',
                stack: 'sleep',
                large: true,
                barMaxWidth: 50,
                itemStyle: {
                    color: COLORS.rem,
                    borderRadius: [0, 0, 0, 0]
                },
                data: remData
            },
            {
                name: '浅睡眠',
                type: 'bar',
                stack: 'sleep',
                large: true,
                barMaxWidth: 50,
                itemStyle: {
                    color: COLORS.light,
                    borderRadius: [4, 4, 0, 0]
                },
                data: lightData
            },
            {
                name: '睡眠质量评分',
                type: 'line',
                yAxisIndex: 1,
                smooth: false,
                symbol: 'circle',
                symbolSize: 8,
                lineStyle: { color: COLORS.quality, width: 2 },
                itemStyle: { color: COLORS.quality },
                data: qualityData,
                z: 10
            },
            {
                name: '夜间醒来次数',
                type: 'line',
                yAxisIndex: 1,
                smooth: false,
                symbol: 'diamond',
                symbolSize: 8,
                lineStyle: { color: '#ef4444', width: 2 },
                itemStyle: { color: '#ef4444' },
                data: wakeData,
                z: 11
            }
        ]
    };

    sleepStructureChart.setOption(option, true);
}

function formatLabel(value, type) {
    const labels = {
        gender: {
            'male': '男',
            'female': '女',
            'other': '其他'
        },
        chronotype: {
            'Morning': '早鸟型',
            'Evening': '夜猫子型',
            'Neutral': '中间型'
        },
        mental: {
            'Healthy': '健康',
            'Anxiety': '焦虑',
            'Depression': '抑郁',
            'Both': '焦虑+抑郁'
        }
    };
    return labels[type]?.[value] || value;
}