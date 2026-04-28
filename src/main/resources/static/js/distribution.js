const chartTheme = {
    color: ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899'],
    backgroundColor: 'transparent',
    textStyle: {
        fontFamily: 'Segoe UI, system-ui, sans-serif'
    }
};

if (echarts) {
    echarts.registerTheme('sleepTheme', chartTheme);
}

const COLORS = ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899'];

let distributionChart;
let currentInner = 'age';
let currentOuter = 'sleepDuration';
let selectedInnerName = null;

const INNER_LABELS = {
    age: '年龄段',
    gender: '性别',
    occupation: '职业',
    bmi: 'BMI区间',
    country: '国家'
};

const OUTER_LABELS = {
    sleepDuration: '睡眠时长区间',
    sleepQuality: '睡眠质量评分区间',
    remPercentage: 'REM睡眠占比区间',
    deepSleep: '深度睡眠占比区间',
    sleepLatency: '入睡潜伏期区间',
    wakeEpisodes: '夜间觉醒次数'
};

$(document).ready(function() {
    initDistributionPage();
});

function initDistributionPage() {
    distributionChart = echarts.init(document.getElementById('distributionChart'), 'sleepTheme');

    $('#innerTabs .tab-btn').on('click', function() {
        currentInner = $(this).data('type');
        selectedInnerName = null;
        $('#innerTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
        loadDistributionData();
    });

    $('#outerTabs .tab-btn').on('click', function() {
        currentOuter = $(this).data('type');
        $('#outerTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
        loadDistributionData();
    });

    $(window).resize(function() {
        distributionChart.resize();
    });

    loadDistributionData();
}

function loadDistributionData() {
    $.ajax({
        url: '/sleep/api/chart/distribution',
        data: { inner: currentInner, outer: currentOuter },
        method: 'GET',
        success: function(data) {
            updateChartTitle();
            updateDistributionChart(data.inner, data.outer);
        },
        error: function() {
            console.log('分布数据加载失败');
        }
    });
}

function updateChartTitle() {
    const innerLabel = INNER_LABELS[currentInner] || currentInner;
    const outerLabel = OUTER_LABELS[currentOuter] || currentOuter;
    $('#chartTitle').text(innerLabel + ' - ' + outerLabel);
}

function buildOuterItems(outerData, targetInnerName) {
    const uniqueOuterNames = [];
    outerData.forEach(function(item) {
        var outerName = String(item.outer_name);
        if (uniqueOuterNames.indexOf(outerName) === -1) {
            uniqueOuterNames.push(outerName);
        }
    });

    return outerData
        .filter(function(item) {
            return String(item.inner_name) === targetInnerName;
        })
        .map(function(item) {
            var outerName = String(item.outer_name);
            var colorIdx = uniqueOuterNames.indexOf(outerName);
            return {
                name: outerName,
                value: item.value,
                itemStyle: {
                    color: COLORS[colorIdx % COLORS.length]
                }
            };
        });
}

function updateDistributionChart(innerData, outerData) {
    if (selectedInnerName === null && innerData.length > 0) {
        selectedInnerName = innerData[0].name;
    }

    const innerItems = innerData.map(function(item, idx) {
        return {
            name: item.name,
            value: item.value,
            itemStyle: {
                color: COLORS[idx % COLORS.length]
            }
        };
    });

    const outerItems = buildOuterItems(outerData, selectedInnerName);

    const option = {
        tooltip: {
            trigger: 'item',
            formatter: function(params) {
                if (params.seriesName === '内层') {
                    return params.name + ': ' + params.value + '人';
                } else {
                    return params.name + ': ' + params.value + '人';
                }
            }
        },
        legend: {
            orient: 'vertical',
            right: '5%',
            top: 'center',
            textStyle: { color: '#64748b' }
        },
        series: [
            {
                name: '内层',
                type: 'pie',
                radius: ['0%', '50%'],
                center: ['40%', '50%'],
                selectedMode: 'single',
                label: {
                    position: 'inner',
                    color: '#fff',
                    fontSize: 12
                },
                labelLine: {
                    show: false
                },
                data: innerItems
            },
            {
                name: '外层',
                type: 'pie',
                radius: ['65%', '90%'],
                center: ['40%', '50%'],
                selectedMode: 'single',
                label: {
                    show: true,
                    position: 'outside',
                    formatter: '{b}',
                    color: '#64748b'
                },
                labelLine: {
                    length: 15
                },
                data: outerItems
            }
        ]
    };

    distributionChart.setOption(option, true);

    distributionChart.off('click');
    distributionChart.on('click', function(params) {
        if (params.seriesName === '内层') {
            selectedInnerName = params.name;
            var newOuterItems = buildOuterItems(outerData, selectedInnerName);
            distributionChart.setOption({
                series: [
                    { data: distributionChart.getOption().series[0].data },
                    { data: newOuterItems }
                ]
            });
        }
    });
}
