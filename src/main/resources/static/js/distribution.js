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
let draggedBarChart;
let currentInner = 'age';
let currentOuter = 'sleepDuration';
let selectedInnerName = null;

const DragState = {
    isDragging: false,
    dragging: null,
    barData: {},
    pieStates: {}
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

let dragPreviewEl = null;

$(document).ready(function() {
    initDistributionPage();
});

function initDistributionPage() {
    distributionChart = echarts.init(document.getElementById('distributionChart'), 'sleepTheme');
    initDraggedBarChart();
    initDragEvents();

    $('#innerTabs .tab-btn').on('click', function() {
        saveCurrentPieState();
        currentInner = $(this).data('type');
        selectedInnerName = null;
        $('#innerTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
        loadDistributionData();
    });

    $('#outerTabs .tab-btn').on('click', function() {
        saveCurrentPieState();
        currentOuter = $(this).data('type');
        $('#outerTabs .tab-btn').removeClass('active');
        $(this).addClass('active');
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

function getPieKey() {
    return currentInner + ' ' + currentOuter;
}

function saveCurrentPieState() {
    if (!distributionChart) return;
    const key = getPieKey();

    DragState.pieStates[key] = {
        innerData: JSON.parse(JSON.stringify(originalInnerData)),
        outerData: JSON.parse(JSON.stringify(rawOuterData))
    };
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

            const key = getPieKey();
            if (DragState.pieStates[key]) {
                applySavedState(data.inner, data.outer);
            } else {
                applyBarDataToFreshData(data.inner, data.outer);
            }

            updateDistributionChart(data.inner, data.outer);
        },
        error: function() {
            console.log('分布数据加载失败');
        }
    });
}

function applySavedState(innerData, outerData) {
    const key = getPieKey();
    const saved = DragState.pieStates[key];

    if (saved) {
        for (let i = 0; i < innerData.length; i++) {
            const match = saved.innerData.find(s => s.name === innerData[i].name);
            if (match) innerData[i].value = match.value;
        }

        for (let i = 0; i < outerData.length; i++) {
            const match = saved.outerData.find(s =>
                s.name === outerData[i].outer_name && s.inner_name === outerData[i].inner_name);
            if (match) outerData[i].value = match.value;
        }
    }

    applyBarDataToFreshData(innerData, outerData);
}

function applyBarDataToFreshData(innerData, outerData) {
    for (let i = 0; i < innerData.length; i++) {
        const barKey = innerData[i].name;
        if (DragState.barData[barKey]) {
            innerData[i].value = Math.max(0, innerData[i].value - DragState.barData[barKey].value);
        }
    }

    for (let i = 0; i < outerData.length; i++) {
        const barKey = outerData[i].inner_name + ' ' + outerData[i].outer_name;
        if (DragState.barData[barKey]) {
            outerData[i].value = Math.max(0, outerData[i].value - DragState.barData[barKey].value);
        }
    }
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
                itemStyle: { color: COLORS[colorIdx % COLORS.length] }
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
            itemStyle: { color: COLORS[idx % COLORS.length] }
        };
    });

    const outerItems = buildOuterItems(outerData, selectedInnerName);

    const option = {
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { color: '#64748b' } },
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
}

function getSectorInfo(offsetX, offsetY, params) {
    // 使用 params.target 检测扇形元素
    const target = params && params.target;

    if (target && target.type === 'sector') {
        // 从扇形元素获取数据
        const option = distributionChart.getOption();
        const seriesIndex = target.seriesIndex;
        const dataIndex = target.dataIndex;

        if (seriesIndex !== undefined && dataIndex !== undefined) {
            const seriesType = seriesIndex === 0 ? 'inner' : 'outer';
            const data = option.series[seriesIndex].data[dataIndex];

            if (data && data.value > 0) {
                return {
                    type: seriesType,
                    dataIndex: dataIndex,
                    name: data.name,
                    value: data.value,
                    color: data.itemStyle ? data.itemStyle.color : COLORS[dataIndex % COLORS.length],
                    innerName: data.inner_name || data.name
                };
            }
        }
    }

    // 回退：使用旧的角度计算方法
    return getSectorByAngle(offsetX, offsetY);
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
    const option = distributionChart.getOption();
    const innerData = option.series[0].data;
    const outerData = option.series[1].data;

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
        const totalValue = innerData.reduce((s, i) => s + i.value, 0);
        if (totalValue === 0) return null;

        for (let i = 0; i < innerData.length; i++) {
            const itemAngle = (innerData[i].value / totalValue) * 2 * Math.PI;
            if (angle >= cumAngle && angle < cumAngle + itemAngle) {
                return {
                    type: 'inner',
                    dataIndex: i,
                    name: innerData[i].name,
                    value: innerData[i].value,
                    color: innerData[i].itemStyle.color
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
                    color: currentOuter[i].itemStyle.color,
                    innerName: currentOuter[i].inner_name
                };
            }
            cumAngle += itemAngle;
        }
    }

    return null;
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

        recalculateAfterOuterRemove(sector);
    } else if (sector.type === 'inner') {
        const barKey = sector.name;
        DragState.barData[barKey] = {
            innerName: sector.name,
            outerName: null,
            value: sector.value,
            color: sector.color
        };

        recalculateAfterInnerRemove(sector.name);
    }

    updateDraggedBarChart();
    saveCurrentPieState();
    updateDistributionChartFromState();
}

function recalculateAfterOuterRemove(removedSector) {
    const key = getPieKey();
    let state = DragState.pieStates[key];

    if (!state) {
        state = { innerData: JSON.parse(JSON.stringify(originalInnerData)), outerData: JSON.parse(JSON.stringify(rawOuterData)) };
        DragState.pieStates[key] = state;
    }

    // 标记该外圈扇形被移除
    if (!state.removedOuterSectors) {
        state.removedOuterSectors = [];
    }
    state.removedOuterSectors.push({
        inner_name: removedSector.innerName,
        name: removedSector.name
    });
}

function recalculateAfterInnerRemove(innerName) {
    const key = getPieKey();
    let state = DragState.pieStates[key];

    if (!state) {
        state = { innerData: JSON.parse(JSON.stringify(originalInnerData)), outerData: JSON.parse(JSON.stringify(rawOuterData)) };
        DragState.pieStates[key] = state;
    }

    // 标记该内圈扇形被移除
    if (!state.removedInnerSectors) {
        state.removedInnerSectors = [];
    }
    state.removedInnerSectors.push(innerName);

    // 标记相关的外圈扇形也被移除
    if (!state.removedOuterSectors) {
        state.removedOuterSectors = [];
    }
    const relatedOuter = rawOuterData.filter(o => o.inner_name === innerName);
    relatedOuter.forEach(o => {
        state.removedOuterSectors.push({
            inner_name: innerName,
            name: o.outer_name
        });
    });
}

function updateDistributionChartFromState() {
    const key = getPieKey();
    const state = DragState.pieStates[key];

    if (!state) return;

    // 从原始数据开始
    const innerData = JSON.parse(JSON.stringify(originalInnerData));
    const outerData = JSON.parse(JSON.stringify(rawOuterData));

    // 过滤掉被移除的内圈扇形
    const filteredInner = innerData.filter(item => {
        return !state.removedInnerSectors || !state.removedInnerSectors.includes(item.name);
    });

    // 过滤掉被移除的外圈扇形
    const filteredOuter = outerData.filter(item => {
        if (!state.removedOuterSectors) return true;
        return !state.removedOuterSectors.some(r =>
            r.inner_name === item.inner_name && r.name === item.outer_name
        );
    });

    // 如果有barData，进一步扣除
    for (let i = 0; i < filteredInner.length; i++) {
        const barKey = filteredInner[i].name;
        if (DragState.barData[barKey]) {
            filteredInner[i].value = Math.max(0, filteredInner[i].value - DragState.barData[barKey].value);
        }
    }

    for (let i = 0; i < filteredOuter.length; i++) {
        const barKey = filteredOuter[i].inner_name + ' ' + filteredOuter[i].outer_name;
        if (DragState.barData[barKey]) {
            filteredOuter[i].value = Math.max(0, filteredOuter[i].value - DragState.barData[barKey].value);
        }
    }

    // 计算剩余内圈的总值
    const totalInnerValue = filteredInner.reduce((s, i) => s + i.value, 0);
    // 如果内圈全被拖走，则不显示内圈（设置为空数组）
    const finalInner = totalInnerValue > 0 ? filteredInner : [];

    // 计算剩余外圈的总值
    const totalOuterValue = filteredOuter.reduce((s, i) => s + i.value, 0);
    // 如果外圈全被拖走，则不显示外圈（设置为空数组）
    const finalOuter = totalOuterValue > 0 ? filteredOuter : [];

    selectedInnerName = finalInner.length > 0 ? finalInner[0].name : null;

    updateDistributionChart(
        finalInner.map(item => ({ name: item.name, value: item.value })),
        finalOuter.map(item => ({ inner_name: item.inner_name, outer_name: item.outer_name, value: item.value }))
    );
}

function updateDraggedBarChart() {
    const barKeys = Object.keys(DragState.barData);
    const barItems = barKeys.map(function(key) {
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

    const key = getPieKey();
    let state = DragState.pieStates[key];

    if (!state) {
        state = {
            innerData: JSON.parse(JSON.stringify(originalInnerData)),
            outerData: JSON.parse(JSON.stringify(rawOuterData)),
            removedInnerSectors: [],
            removedOuterSectors: []
        };
        DragState.pieStates[key] = state;
    }

    if (!state.removedInnerSectors) state.removedInnerSectors = [];
    if (!state.removedOuterSectors) state.removedOuterSectors = [];

    if (barEntry.outerName) {
        // 恢复外圈：从removedOuterSectors中移除
        state.removedOuterSectors = state.removedOuterSectors.filter(r =>
            !(r.inner_name === barEntry.innerName && r.name === barEntry.outerName)
        );

    } else {
        // 恢复内圈：从removedInnerSectors中移除
        state.removedInnerSectors = state.removedInnerSectors.filter(n => n !== barEntry.innerName);

        // 同时恢复相关的外圈
        state.removedOuterSectors = state.removedOuterSectors.filter(r =>
            r.inner_name !== barEntry.innerName
        );
    }

    delete DragState.barData[barKey];

    updateDraggedBarChart();
    saveCurrentPieState();
    updateDistributionChartFromState();
}