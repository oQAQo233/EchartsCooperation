$(document).ready(function () {
    const dom = document.getElementById('heatmapChart');
    if (!dom) return;

    const chart = echarts.init(dom);

    function showLoading() {
        chart.showLoading('default', {
            text: '加载中...',
            color: '#6c63ff',
            textColor: '#666',
            maskColor: 'rgba(255, 255, 255, 0.6)'
        });
    }

    function hideLoading() {
        chart.hideLoading();
    }

    function render(payload) {
        const xLabels = payload && payload.xLabels ? payload.xLabels : [];
        const yLabels = payload && payload.yLabels ? payload.yLabels : [];
        const data = payload && payload.data ? payload.data : [];

        let maxValue = 0;
        for (let i = 0; i < data.length; i++) {
            const v = data[i][2];
            if (typeof v === 'number' && v > maxValue) maxValue = v;
        }

        const option = {
            tooltip: {
                position: 'top',
                formatter: function (p) {
                    const x = xLabels[p.data[0]];
                    const y = yLabels[p.data[1]];
                    const v = p.data[2];
                    return (
                        'X: ' + x + '<br/>' +
                        'Y: ' + y + '<br/>' +
                        '人数: ' + v
                    );
                }
            },
            grid: { top: 60, left: 90, right: 30, bottom: 90 },
            xAxis: {
                type: 'category',
                data: xLabels,
                splitArea: { show: true },
                axisLabel: { rotate: 45 }
            },
            yAxis: {
                type: 'category',
                data: yLabels,
                splitArea: { show: true }
            },
            visualMap: {
                min: 0,
                max: maxValue,
                calculable: true,
                orient: 'horizontal',
                left: 'center',
                bottom: 15
            },
            series: [{
                name: 'count',
                type: 'heatmap',
                data: data,
                label: { show: false },
                emphasis: {
                    itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.25)' }
                }
            }]
        };

        chart.setOption(option);
    }

    function toNumber(id) {
        const v = $(id).val();
        const n = Number(v);
        if (Number.isNaN(n)) throw new Error('参数不是数字：' + id);
        return n;
    }

    function applyRange() {
        let params;
        try {
            params = {
                xField: $('#xField').val(),
                yField: $('#yField').val(),
                xStart: toNumber('#xStart'),
                xEnd: toNumber('#xEnd'),
                xStep: toNumber('#xStep'),
                yStart: toNumber('#yStart'),
                yEnd: toNumber('#yEnd'),
                yStep: toNumber('#yStep'),
                title: $('#xField').val() + ' × ' + $('#yField').val() + '（range 分桶）'
            };
        } catch (e) {
            showError(e.message || String(e));
            return;
        }

        showLoading();
        $.ajax({
            url: '/sleep/api/chart/heatmap/range',
            method: 'GET',
            dataType: 'json',
            data: params,
            success: function (res) {
                hideLoading();
                $('#heatmapTitle').text(res.title || 'Range 热力图');
                render(res);
            },
            error: function (xhr) {
                hideLoading();
                const msg = 'HTTP ' + xhr.status + '：' + (xhr.responseText || '请求失败');
                console.error(msg);
                showError(msg);
            }
        });
    }

    // 预设：只负责“填参数”，不再走后端 type 接口
    function setPreset(p) {
        $('#xField').val(p.xField);
        $('#yField').val(p.yField);
        $('#xStart').val(p.xStart);
        $('#xEnd').val(p.xEnd);
        $('#xStep').val(p.xStep);
        $('#yStart').val(p.yStart);
        $('#yEnd').val(p.yEnd);
        $('#yStep').val(p.yStep);
        $('#heatmapTitle').text(p.title || '热力图');
    }

    // 绑定预设按钮
    $('#preset_duration_quality').on('click', function () {
        setPreset({
            title: '睡眠时长(0.5h) × 睡眠质量(1分)',
            xField: 'sleep_duration_hrs', xStart: 3, xEnd: 11, xStep: 0.5,
            yField: 'sleep_quality_score', yStart: 1, yEnd: 11, yStep: 1
        });
        applyRange();
    });

    $('#preset_stress_quality').on('click', function () {
        setPreset({
            title: '压力(0.5分) × 睡眠质量(0.5分)',
            xField: 'stress_score', xStart: 1, xEnd: 11, xStep: 0.5,
            yField: 'sleep_quality_score', yStart: 1, yEnd: 11, yStep: 0.5
        });
        applyRange();
    });

    $('#preset_latency_wake').on('click', function () {
        setPreset({
            title: '入睡潜伏(5min) × 夜醒次数(1次)',
            xField: 'sleep_latency_mins', xStart: 0, xEnd: 60, xStep: 5,
            yField: 'wake_episodes_per_night', yStart: 0, yEnd: 10, yStep: 1
        });
        applyRange();
    });

    $('#preset_temp_quality').on('click', function () {
        setPreset({
            title: '温度(0.5℃) × 睡眠质量(1分)',
            xField: 'room_temperature_celsius', xStart: 16, xEnd: 28, xStep: 0.5,
            yField: 'sleep_quality_score', yStart: 1, yEnd: 11, yStep: 1
        });
        applyRange();
    });

    $('#preset_weekenddiff_quality').on('click', function () {
        setPreset({
            title: '周末差异(0.5h) × 睡眠质量(1分)',
            xField: 'weekend_sleep_diff_hrs', xStart: -3, xEnd: 5, xStep: 0.5,
            yField: 'sleep_quality_score', yStart: 1, yEnd: 11, yStep: 1
        });
        applyRange();
    });

    // 绑定“应用”按钮
    $('#applyRange').on('click', function () {
        applyRange();
    });

    // 默认：填一组常用参数并加载一次
    setPreset({
        title: '睡眠时长(0.5h) × 睡眠质量(1分)',
        xField: 'sleep_duration_hrs', xStart: 3, xEnd: 11, xStep: 0.5,
        yField: 'sleep_quality_score', yStart: 1, yEnd: 11, yStep: 1
    });
    applyRange();

    $(window).on('resize', function () {
        chart.resize();
    });
});