const chartTheme = {
    color: ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#14b8a6', '#a855f7', '#3b82f6'],
    backgroundColor: 'transparent',
    textStyle: {
        fontFamily: 'Segoe UI, system-ui, sans-serif'
    }
};

if (echarts) {
    echarts.registerTheme('sleepTheme', chartTheme);
}

// 内圈颜色
const INNER_COLORS = ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#84cc16'];
// 外圈颜色（与内圈不重复）
const OUTER_COLORS = ['#f97316', '#14b8a6', '#a855f7', '#3b82f6', '#fb923c', '#2dd4bf', '#c084fc', '#60a5fa'];

let distributionChart;
let draggedBarChart;
let currentInner = 'age';
let currentOuter = 'sleepDuration';
let selectedInnerName = null;

const DragState = {
    isDragging: false,
    dragging: null,
    barData: {},           // { key: { innerName, outerName, value, color } }
    barOrder: [],          // 记录拖拽顺序，确保柱状图按拖拽顺序显示
    legendHiddenData: {}   // 记录被图例隐藏的数据 { key: { innerName, outerName, value, color } }
};

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

let rawOuterData = [];
let originalInnerData = [];
let originalOuterData = [];
let currentLegendSelected = {};  // 当前图例选中状态

let dragPreviewEl = null;

$(document).ready(function() {
    initDistributionPage();
});

function initDistributionPage() {
    distributionChart = echarts.init(document.getElementById('distributionChart'), 'sleepTheme');
    initDraggedBarChart();
    initDragEvents();

    $('#innerTabs .tab-btn').on('click', function() {
        currentInner = $(this).data('type');
        selectedInnerName = null;
        $('#innerTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
        // 切换内圈时清空 bar 数据
        DragState.barData = {};
        DragState.barOrder = [];
        DragState.legendHiddenData = {};
        currentLegendSelected = {};
        loadDistributionData();
    });

    $('#outerTabs .tab-btn').on('click', function() {
        currentOuter = $(this).data('type');
        $('#outerTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
        // 切换外圈时清空 bar 数据
        DragState.barData = {};
        DragState.barOrder = [];
        DragState.legendHiddenData = {};
        currentLegendSelected = {};
        loadDistributionData();
    });

    $(window).resize(function() {
        distributionChart.resize();
        if (draggedBarChart) draggedBarChart.resize();
    });

    loadDistributionData();
}

function initDraggedBarChart() {
    draggedBarChart = echarts.init(document.getElementById('draggedBarChart'), 'sleepTheme');
    updateDraggedBarChart();

    draggedBarChart.getZr().on('mousedown', function(params) {
        const rectInfo = getRectInfo(params);

        if (rectInfo && rectInfo.name && DragState.barData[rectInfo.name]) {
            const barEntry = DragState.barData[rectInfo.name];

            DragState.isDragging = true;
            DragState.dragging = {
                type: 'bar',
                name: rectInfo.name,
                value: barEntry.value,
                color: barEntry.color,
                innerName: barEntry.innerName,
                outerName: barEntry.outerName
            };

            showDragPreview(params.event.clientX, params.event.clientY, rectInfo.name, barEntry.value, barEntry.color);

            document.addEventListener('mousemove', onBarGlobalMouseMove);
            document.addEventListener('mouseup', onBarGlobalMouseUp);
        }
    });
}

function initDragEvents() {
    distributionChart.getZr().on('mousedown', onChartMouseDown);
    distributionChart.getZr().on('mousemove', onChartMouseMove);
    distributionChart.getZr().on('mouseup', onChartMouseUp);
}

function loadDistributionData() {
    $.ajax({
        url: '/sleep/api/chart/distribution',
        data: { inner: currentInner, outer: currentOuter },
        method: 'GET',
        success: function(data) {
            updateChartTitle();
            originalInnerData = JSON.parse(JSON.stringify(data.inner));
            originalOuterData = JSON.parse(JSON.stringify(data.outer));
            rawOuterData = data.outer;

            // 重置图例选中状态
            currentLegendSelected = {};

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
                inner_name: item.inner_name,
                itemStyle: { color: OUTER_COLORS[colorIdx % OUTER_COLORS.length] }
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
            itemStyle: { color: INNER_COLORS[idx % INNER_COLORS.length] }
        };
    });

    const outerItems = buildOuterItems(outerData, selectedInnerName);

    const option = {
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { color: '#64748b' }, selectedMode: true },
        series: [
            {
                name: '内层', type: 'pie', radius: ['0%', '50%'], center: ['40%', '50%'],
                selectedMode: 'single',
                label: { position: 'inner', color: '#fff', fontSize: 12 },
                labelLine: { show: false },
                data: innerItems
            },
            {
                name: '外层', type: 'pie', radius: ['65%', '90%'], center: ['40%', '50%'],
                selectedMode: 'single',
                label: { show: true, position: 'outside', formatter: '{b}', color: '#64748b' },
                labelLine: { length: 15 },
                data: outerItems
            }
        ]
    };

    distributionChart.setOption(option, true);

    distributionChart.off('click');
    distributionChart.on('click', function(params) {
        if (params.seriesName === '内层') {
            selectedInnerName = params.name;
            var newOuterItems = buildOuterItems(rawOuterData, selectedInnerName);
            distributionChart.setOption({
                series: [
                    { data: distributionChart.getOption().series[0].data },
                    { data: newOuterItems }
                ]
            });
        }
    });

    // 图例点击事件监听
    distributionChart.off('legendselectchanged');
    distributionChart.on('legendselectchanged', function(params) {
        handleLegendToggle(params.selected);
    });
}

function getSectorInfo(offsetX, offsetY, params) {
    const target = params && params.target;

    if (target && target.type === 'sector') {
        const option = distributionChart.getOption();
        const seriesIndex = target.seriesIndex;
        const dataIndex = target.dataIndex;

        if (seriesIndex !== undefined && dataIndex !== undefined) {
            const innerData = option.series[0].data;
            const outerData = option.series[1].data;
            const targetData = seriesIndex === 0 ? innerData : outerData;

            // 创建可见扇形索引映射数组
            const lst = [];
            for (let i = 0; i < targetData.length; i++) {
                const d = targetData[i];
                if (d) {
                    const legendName = seriesIndex === 0 ? d.name : (d.inner_name || selectedInnerName) + ' ' + d.name;
                    if (currentLegendSelected[legendName] !== false) {
                        lst.push(i);
                    }
                }
            }

            // lst[dataIndex] 即为目标扇形的实际索引
            const targetIndex = lst[dataIndex];
            if (targetIndex === undefined) {
                return null;
            }

            const data = targetData[targetIndex];
            if (data) {
                return {
                    type: seriesIndex === 0 ? 'inner' : 'outer',
                    dataIndex: targetIndex,
                    name: data.name,
                    value: data.value,
                    color: data.itemStyle ? data.itemStyle.color : (seriesIndex === 0 ? INNER_COLORS[targetIndex % INNER_COLORS.length] : OUTER_COLORS[targetIndex % OUTER_COLORS.length]),
                    innerName: data.inner_name || data.name
                };
            }
        }
    }

    // return getSectorByAngle(offsetX, offsetY);
    return true; // 临时返回，允许拖拽测试
}

function getRectInfo(params) {
    const target = params && params.target;

    if (target && target.type === 'rect') {
        const option = draggedBarChart.getOption();
        const seriesIndex = target.seriesIndex;
        const dataIndex = target.dataIndex;

        if (seriesIndex !== undefined && dataIndex !== undefined) {
            const seriesData = option.series[seriesIndex].data;
            const xAxisData = option.xAxis[0].data;

            if (seriesData && seriesData[dataIndex] !== undefined && xAxisData[dataIndex] !== undefined) {
                return {
                    type: 'bar',
                    dataIndex: dataIndex,
                    name: xAxisData[dataIndex],
                    value: seriesData[dataIndex]
                };
            }
        }
    }
    return null;
}

function getSectorByAngle(x, y) {
    const chartWidth = distributionChart.getWidth();
    const chartHeight = distributionChart.getHeight();
    const centerX = chartWidth * 0.4;
    const centerY = chartHeight * 0.5;

    const dx = x - centerX;
    const dy = y - centerY;
    const distance = Math.sqrt(dx * dx + dy * dy);

    // 角度计算，修正90度偏移（从12点钟方向开始）
    let angle = Math.atan2(dy, dx);
    angle = (angle - Math.PI / 2 + 2 * Math.PI) % (2 * Math.PI);

    const innerRadiusEnd = chartHeight * 0.5 * 0.5;
    const outerRadiusStart = chartHeight * 0.5 * 0.65;
    const outerRadiusEnd = chartHeight * 0.5 * 0.9;

    if (distance >= 0 && distance <= innerRadiusEnd) {
        let cumAngle = 0;
        const totalValue = originalInnerData.reduce((s, i) => s + i.value, 0);
        if (totalValue === 0) return null;

        for (let i = 0; i < originalInnerData.length; i++) {
            const itemAngle = (originalInnerData[i].value / totalValue) * 2 * Math.PI;
            if (angle >= cumAngle && angle < cumAngle + itemAngle) {
                return {
                    type: 'inner',
                    dataIndex: i,
                    name: originalInnerData[i].name,
                    value: originalInnerData[i].value,
                    color: INNER_COLORS[i % INNER_COLORS.length]
                };
            }
            cumAngle += itemAngle;
        }
        return null;
    }

    if (distance >= outerRadiusStart && distance <= outerRadiusEnd) {
        const currentOuter = buildOuterItems(rawOuterData, selectedInnerName);
        let cumAngle = 0;
        const totalValue = currentOuter.reduce((s, i) => s + i.value, 0);
        if (totalValue === 0) return null;

        for (let i = 0; i < currentOuter.length; i++) {
            const itemAngle = (currentOuter[i].value / totalValue) * 2 * Math.PI;
            if (angle >= cumAngle && angle < cumAngle + itemAngle) {
                return {
                    type: 'outer',
                    dataIndex: i,
                    name: currentOuter[i].name,
                    value: currentOuter[i].value,
                    color: currentOuter[i].itemStyle ? currentOuter[i].itemStyle.color : OUTER_COLORS[i % OUTER_COLORS.length],
                    innerName: currentOuter[i].inner_name
                };
            }
            cumAngle += itemAngle;
        }
    }

    return null;
}

function handleLegendToggle(selected) {
    const option = distributionChart.getOption();
    const innerData = option.series[0].data;
    const outerData = option.series[1].data;

    // 获取所有图例名称
    const allLegendNames = new Set();
    innerData.forEach(d => allLegendNames.add(d.name));
    outerData.forEach(d => {
        const fullName = (d.inner_name || selectedInnerName) + ' ' + d.name;
        allLegendNames.add(fullName);
    });

    // 找出真正变化的图例（之前存在记录的）
    const changedLegends = [];
    for (const name of allLegendNames) {
        if (name in currentLegendSelected) {
            const wasSelected = currentLegendSelected[name] !== false;
            const isSelected = selected[name] !== false;
            if (wasSelected !== isSelected) {
                changedLegends.push({ name, wasSelected, isSelected });
            }
        }
    }

    // 处理变化的图例
    for (const change of changedLegends) {
        const { name, wasSelected, isSelected } = change;

        if (!wasSelected && isSelected) {
            // 图例被选中显示 - 从 barData 移除
            if (DragState.barData[name]) {
                delete DragState.barData[name];
                DragState.barOrder = DragState.barOrder.filter(k => k !== name);
            }
            delete DragState.legendHiddenData[name];
        } else if (wasSelected && !isSelected) {
            // 图例被取消隐藏 - 添加到 barData
            // 查找对应的原始数据
            const innerItem = innerData.find(d => d.name === name);
            if (innerItem) {
                DragState.barData[name] = {
                    innerName: name,
                    outerName: null,
                    value: innerItem.value,
                    color: innerItem.itemStyle ? innerItem.itemStyle.color : INNER_COLORS[innerData.indexOf(innerItem) % INNER_COLORS.length]
                };
                DragState.legendHiddenData[name] = { ...DragState.barData[name] };
                if (!DragState.barOrder.includes(name)) {
                    DragState.barOrder.push(name);
                }
            } else {
                // 外圈数据
                const outerItem = outerData.find(d => {
                    const fullName = (d.inner_name || selectedInnerName) + ' ' + d.name;
                    return fullName === name;
                });
                if (outerItem) {
                    DragState.barData[name] = {
                        innerName: outerItem.inner_name || selectedInnerName,
                        outerName: outerItem.name,
                        value: outerItem.value,
                        color: outerItem.itemStyle ? outerItem.itemStyle.color : OUTER_COLORS[outerData.indexOf(outerItem) % OUTER_COLORS.length]
                    };
                    DragState.legendHiddenData[name] = { ...DragState.barData[name] };
                    if (!DragState.barOrder.includes(name)) {
                        DragState.barOrder.push(name);
                    }
                }
            }
        }
    }

    // 更新当前状态 - 同步所有图例状态
    currentLegendSelected = { ...selected };

    updateDraggedBarChart();
}

function showDragPreview(x, y, name, value, color) {
    removeDragPreview();

    dragPreviewEl = document.createElement('div');
    dragPreviewEl.className = 'drag-preview';
    dragPreviewEl.style.backgroundColor = color || '#6366f1';
    dragPreviewEl.style.left = x + 'px';
    dragPreviewEl.style.top = y + 'px';
    dragPreviewEl.innerHTML = '<div class="preview-name">' + name + '</div><div class="preview-value">' + value + '人</div>';
    dragPreviewEl.style.display = 'block';
    document.body.appendChild(dragPreviewEl);
}

function updateDragPreviewPosition(x, y) {
    if (dragPreviewEl) {
        dragPreviewEl.style.left = x + 'px';
        dragPreviewEl.style.top = y + 'px';
    }
}

function removeDragPreview() {
    if (dragPreviewEl) {
        dragPreviewEl.remove();
        dragPreviewEl = null;
    }
}

function onChartMouseDown(params) {
    const offsetX = params.offsetX;
    const offsetY = params.offsetY;

    const sector = getSectorInfo(offsetX, offsetY, params);
    if (sector && sector.value > 0) {
        DragState.isDragging = true;
        DragState.dragging = sector;

        showDragPreview(params.event.clientX, params.event.clientY, sector.name, sector.value, sector.color);

        document.addEventListener('mousemove', onGlobalMouseMove);
        document.addEventListener('mouseup', onGlobalMouseUp);
    }
}

function onChartMouseMove(params) {
}

function onChartMouseUp(params) {
}

function onGlobalMouseMove(params) {
    if (DragState.isDragging && DragState.dragging) {
        updateDragPreviewPosition(params.clientX, params.clientY);

        const barCard = document.getElementById('draggedBarCard');
        if (barCard) {
            const barRect = barCard.getBoundingClientRect();
            const isOverBar = (
                params.clientX >= barRect.left && params.clientX <= barRect.right &&
                params.clientY >= barRect.top && params.clientY <= barRect.bottom
            );

            if (isOverBar) {
                barCard.classList.add('drag-over');
            } else {
                barCard.classList.remove('drag-over');
            }
        }
    }
}

function onGlobalMouseUp(params) {
    document.removeEventListener('mousemove', onGlobalMouseMove);
    document.removeEventListener('mouseup', onGlobalMouseUp);

    if (!DragState.isDragging || !DragState.dragging) {
        removeDragPreview();
        return;
    }

    const barCard = document.getElementById('draggedBarCard');
    const barRect = barCard.getBoundingClientRect();

    const isOverBar = (
        params.clientX >= barRect.left && params.clientX <= barRect.right &&
        params.clientY >= barRect.top && params.clientY <= barRect.bottom
    );

    barCard.classList.remove('drag-over');

    if (isOverBar) {
        transferToBar(DragState.dragging);
    }

    removeDragPreview();
    DragState.isDragging = false;
    DragState.dragging = null;
}

function onBarGlobalMouseMove(params) {
    if (DragState.isDragging && DragState.dragging && DragState.dragging.type === 'bar') {
        updateDragPreviewPosition(params.clientX, params.clientY);
    }
}

function onBarGlobalMouseUp(params) {
    document.removeEventListener('mousemove', onBarGlobalMouseMove);
    document.removeEventListener('mouseup', onBarGlobalMouseUp);

    if (!DragState.isDragging || !DragState.dragging || DragState.dragging.type !== 'bar') {
        removeDragPreview();
        return;
    }

    const chartDom = document.getElementById('distributionChart');
    const rect = chartDom.getBoundingClientRect();

    const isOverChart = (
        params.clientX >= rect.left && params.clientX <= rect.right &&
        params.clientY >= rect.top && params.clientY <= rect.bottom
    );

    if (isOverChart) {
        // 计算相对于扇形图的位置
        const offsetX = params.clientX - rect.left;
        const offsetY = params.clientY - rect.top;

        const sector = getSectorInfo(offsetX, offsetY);
        if (sector) {
            restoreFromBar(DragState.dragging, sector);
        }
    }

    removeDragPreview();
    DragState.isDragging = false;
    DragState.dragging = null;
}

function transferToBar(sector) {
    if (sector.type === 'outer') {
        const barKey = sector.innerName + ' ' + sector.name;
        DragState.barData[barKey] = {
            innerName: sector.innerName,
            outerName: sector.name,
            value: sector.value,
            color: sector.color
        };
        DragState.legendHiddenData[barKey] = {
            innerName: sector.innerName,
            outerName: sector.name,
            value: sector.value,
            color: sector.color
        };
        // 记录顺序
        if (!DragState.barOrder.includes(barKey)) {
            DragState.barOrder.push(barKey);
        }

        // 设置图例为隐藏
        distributionChart.dispatchAction({
            type: 'legendToggleSelect',
            name: sector.name
        });
        currentLegendSelected[sector.name] = false;

    } else if (sector.type === 'inner') {
        const barKey = sector.name;
        DragState.barData[barKey] = {
            innerName: sector.name,
            outerName: null,
            value: sector.value,
            color: sector.color
        };
        DragState.legendHiddenData[barKey] = {
            innerName: sector.name,
            outerName: null,
            value: sector.value,
            color: sector.color
        };
        // 记录顺序
        if (!DragState.barOrder.includes(barKey)) {
            DragState.barOrder.push(barKey);
        }

        // 设置图例为隐藏
        distributionChart.dispatchAction({
            type: 'legendToggleSelect',
            name: sector.name
        });
        currentLegendSelected[sector.name] = false;
    }

    updateDraggedBarChart();
}

function updateDraggedBarChart() {
    // 按拖拽顺序显示
    const barItems = DragState.barOrder.map(function(key) {
        const item = DragState.barData[key];
        return {
            name: key,
            value: item.value,
            itemStyle: { color: item.color }
        };
    });

    if (barItems.length === 0) {
        if (draggedBarChart) draggedBarChart.clear();
        return;
    }

    const option = {
        tooltip: {
            trigger: 'axis',
            formatter: function(params) {
                const item = params[0];
                const barEntry = DragState.barData[item.name];
                let tip = item.name + ': ' + item.value + '人';
                if (barEntry.outerName) {
                    tip += '<br/><span style="color:#999;font-size:11px;">来源: ' + barEntry.innerName + ' ' + barEntry.outerName + '</span>';
                } else {
                    tip += '<br/><span style="color:#999;font-size:11px;">来源: ' + barEntry.innerName + '</span>';
                }
                return tip;
            }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
        xAxis: {
            type: 'category',
            data: barItems.map(function(i) { return i.name; }),
            axisLabel: { color: '#64748b', rotate: 30 }
        },
        yAxis: {
            type: 'value', axisLabel: { color: '#64748b' },
            splitLine: { lineStyle: { color: '#f1f5f9' } }
        },
        series: [{
            name: '拖拽数据', type: 'bar',
            data: barItems.map(function(i) { return i.value; }),
            itemStyle: {
                color: function(params) {
                    return DragState.barData[params.name].color;
                },
                borderRadius: [4, 4, 0, 0]
            },
            barWidth: '60%'
        }]
    };

    draggedBarChart.setOption(option, true);
}

function restoreFromBar(barInfo, targetSector) {
    const barKey = barInfo.name;
    const barEntry = DragState.barData[barKey];

    if (!barEntry) return;

    delete DragState.barData[barKey];
    delete DragState.legendHiddenData[barKey];
    // 从顺序数组中移除
    DragState.barOrder = DragState.barOrder.filter(k => k !== barKey);

    // 恢复图例为选中状态
    if (barEntry.outerName) {
        distributionChart.dispatchAction({
            type: 'legendToggleSelect',
            name: barEntry.outerName
        });
        currentLegendSelected[barEntry.outerName] = true;
    } else {
        distributionChart.dispatchAction({
            type: 'legendToggleSelect',
            name: barEntry.innerName
        });
        currentLegendSelected[barEntry.innerName] = true;
    }

    updateDraggedBarChart();
}