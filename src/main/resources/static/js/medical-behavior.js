$(document).ready(function() {
    var chart = echarts.init(document.getElementById('medicalChart'));
    chart.showLoading();

    $.get('/sleep/api/chart/behavior', function(data) {
        chart.hideLoading();

        var scatterData = [];
        var maxPeople = Math.max(...data.map(d => d.peopleCount));

        data.forEach(function(item) {
            scatterData.push([
                item.avgScreen,
                item.avgLatency,
                item.peopleCount,
                item.avgCaffeine,
                item.groupName
            ]);
        });

        var option = {
            title: {
                text: '各职业分析：睡前屏幕时间与入睡潜伏期关联',
                subtext: '气泡大小=群体人数，气泡颜色越暖代表平均摄入咖啡因越高',
                left: 'center'
            },
            tooltip: {
                formatter: function (params) {
                    var val = params.value;
                    return `
                        <b style="font-size:16px;">职业群体：${val[4]}</b><br/>
                        <hr style="margin:5px 0;">
                        群样别人数: <b>${val[2]}</b> 人<br/>
                        均看屏时间: ${val[0]} 分钟<br/>
                        均入睡潜伏: ${val[1]} 分钟<br/>
                        均摄入咖啡因: ${val[3]} mg<br/>
                    `;
                }
            },
            xAxis: {
                name: '平均睡前屏幕时间 (分钟)',
                type: 'value',
                splitLine: { show: false }
            },
            yAxis: {
                name: '平均入睡潜伏期 (分钟)',
                type: 'value',
                splitLine: { show: true, lineStyle:{ type: 'dashed', opacity: 0.3 } },
                scale: true
            },
            visualMap: {
                type: 'continuous',
                dimension: 3,
                right: 10,
                min: 35,
                max: 45,
                top: 'center',
                text: ['高咖啡因', '低咖啡因'],
                calculable: true,
                inRange: {
                    color: ['#50a3ba', '#eac763', '#d94e5d']
                }
            },
            series: [{
                name: '职业影响分析',
                type: 'scatter',
                data: scatterData,
                symbolSize: function (val) {
                    var size = (val[2] / maxPeople) * 80;
                    return Math.max(size, 15); // 最小15像素
                },
                label: {
                    show: true,
                    formatter: function (params) {
                        return params.value[4];
                    },
                    position: 'top',
                    color: '#333'
                },
                itemStyle: {
                    shadowBlur: 10,
                    shadowColor: 'rgba(0, 0, 0, 0.5)',
                    opacity: 0.85,
                    borderColor: '#fff',
                    borderWidth: 1
                }
            }]
        };

        chart.setOption(option);
    });

    $(window).resize(function() {
        chart.resize();
    });
});
