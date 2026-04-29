const comparisonTheme = {
    color: ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#84cc16'],
    backgroundColor: 'transparent',
    textStyle: {
        fontFamily: 'Segoe UI, system-ui, sans-serif'
    }
};

if (typeof echarts !== 'undefined') {
    echarts.registerTheme('comparisonTheme', comparisonTheme);
}

const COMPARISON_COLORS = ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#84cc16'];
const DIMENSIONS = ['age', 'gender', 'occupation', 'bmi', 'country'];

let barChart;
let radarChart;
let currentDimension = 'age';
let allCategories = [];
let allMetricAverages = {};
let allMetricLabels = [];
let allMetricMaxValues = {};
let selectedCategories = [];
let barValues = [];
let dataCache = {};

$(document).ready(function() {
    initComparisonPage();
});

function initComparisonPage() {
    barChart = echarts.init(document.getElementById('barChart'), 'comparisonTheme');
    radarChart = echarts.init(document.getElementById('radarChart'), 'comparisonTheme');

    preloadAllDimensions();

    $('#dimensionTabs .tab-btn').on('click', function() {
        currentDimension = $(this).data('type');
        selectedCategories = [];
        $('#dimensionTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
        useCachedData(currentDimension);
    });

    $(window).resize(function() {
        barChart.resize();
        radarChart.resize();
    });
}

function preloadAllDimensions() {
    const promises = DIMENSIONS.map(function(dim) {
        return $.ajax({
            url: '/sleep/api/chart/comparison',
            data: { dimension: dim },
            method: 'GET'
        }).then(function(data) {
            dataCache[dim] = data;
        });
    });

    $.when.apply($, promises).done(function() {
        useCachedData('age');
    }).fail(function() {
        $('#barChart').html('<div class="text-center text-danger p-5">数据加载失败，请刷新页面重试</div>');
    });
}

function useCachedData(dimension) {
    const data = dataCache[dimension];
    if (!data) return;

    allCategories = data.categories;
    allMetricAverages = data.metricAverages;
    allMetricLabels = data.metricLabels;
    allMetricMaxValues = data.metricMaxValues || {};
    barValues = data.barChart.counts.slice();
    
    renderBarChart(data.barChart);
    renderCheckboxes();
    updateRadarChart();
}

function renderBarChart(barData) {
    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: function(params) {
                return params[0].name + ': ' + params[0].value + '人';
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '15%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: barData.categories,
            axisLabel: { rotate: 45, fontSize: 11 }
        },
        yAxis: {
            type: 'value',
            name: '人数'
        },
        series: [{
            name: '人数',
            type: 'bar',
            data: barData.counts.map(function(val) {
                return {
                    value: val,
                    itemStyle: {
                        color: '#e2e8f0'
                    }
                };
            })
        }]
    };

    barChart.setOption(option, true);
}

function updateBarChartColors() {
    const newData = barValues.map(function(val, idx) {
        const isSelected = selectedCategories.indexOf(allCategories[idx]) !== -1;
        return {
            value: val,
            itemStyle: {
                color: isSelected ? COMPARISON_COLORS[idx % COMPARISON_COLORS.length] : '#e2e8f0'
            }
        };
    });
    barChart.setOption({
        series: [{ data: newData }]
    });
}

function renderCheckboxes() {
    const container = $('#categoryCheckboxes');
    container.empty();
    
    const checkboxGroup = $('<div class="checkbox-group"></div>');
    
    allCategories.forEach(function(category, idx) {
        const label = $('<label class="checkbox-label"></label>');
        const checkbox = $('<input type="checkbox">');
        checkbox.addClass('category-checkbox');
        checkbox.attr('data-index', idx);
        
        const colorSpan = $('<span class="color-dot"></span>');
        colorSpan.css('background-color', COMPARISON_COLORS[idx % COMPARISON_COLORS.length]);
        
        const textSpan = $('<span>').text(category);
        
        checkbox.on('change', function() {
            const catIdx = parseInt($(this).data('index'));
            const cat = allCategories[catIdx];
            if ($(this).is(':checked')) {
                if (selectedCategories.indexOf(cat) === -1) {
                    selectedCategories.push(cat);
                }
            } else {
                const idx = selectedCategories.indexOf(cat);
                if (idx !== -1) {
                    selectedCategories.splice(idx, 1);
                }
            }
            
            updateBarChartColors();
            updateRadarChart();
        });
        
        label.append(checkbox);
        label.append(colorSpan);
        label.append(textSpan);
        checkboxGroup.append(label);
    });
    
    container.append(checkboxGroup);
}

function updateRadarChart() {
    if (selectedCategories.length === 0) {
        radarChart.clear();
        radarChart.setOption({
            title: {
                text: '请选择区间',
                left: 'center',
                top: 'middle',
                textStyle: { color: '#94a3b8', fontSize: 14 }
            },
            radar: {
                indicator: allMetricLabels.map(function(label) {
                    return { name: label, max: 100 };
                })
            },
            series: []
        });
        return;
    }

    const metricKeys = Object.keys(allMetricAverages);
    
    const series = selectedCategories.map(function(category) {
        const categoryIdx = allCategories.indexOf(category);
        const color = COMPARISON_COLORS[categoryIdx % COMPARISON_COLORS.length];
        const normalizedValues = metricKeys.map(function(key) {
            const rawValue = allMetricAverages[key][categoryIdx];
            const maxValue = allMetricMaxValues[key] || 100;
            return Math.round((rawValue / maxValue) * 1000) / 10;
        });
        
        return {
            name: category,
            type: 'radar',
            data: [{
                value: normalizedValues,
                name: category,
                itemStyle: { color: color },
                lineStyle: { color: color },
                areaStyle: { color: color + '40' }
            }],
            symbol: 'circle',
            symbolSize: 6
        };
    });

    const indicator = allMetricLabels.map(function(label) {
        return { name: label, max: 100 };
    });

    radarChart.setOption({
        title: { show: false },
        tooltip: {
            trigger: 'item',
            formatter: function(params) {
                const categoryIdx = allCategories.indexOf(params.seriesName);
                let html = '<div class="text-start">' + params.seriesName + '<br/>';
                metricKeys.forEach(function(key, i) {
                    const rawValue = allMetricAverages[key][categoryIdx];
                    html += allMetricLabels[i] + ': ' + rawValue + '<br/>';
                });
                html += '</div>';
                return html;
            }
        },
        legend: {
            data: selectedCategories,
            bottom: 0,
            textStyle: { color: '#64748b' }
        },
        radar: {
            indicator: indicator,
            shape: 'polygon',
            splitNumber: 5,
            axisName: { color: '#64748b', fontSize: 11 },
            splitLine: { lineStyle: { color: '#e2e8f0' } },
            splitArea: {
                show: true,
                areaStyle: { color: ['#f8fafc', '#ffffff'] }
            }
        },
        series: series
    }, true);
}

$.whenAll = function() {
    var dfd = $.Deferred();
    $.when.apply($, arguments).done(function() {
        dfd.resolveWith(this, arguments);
    }).fail(function() {
        dfd.resolveWith(this, arguments);
    });
    return dfd.promise();
};