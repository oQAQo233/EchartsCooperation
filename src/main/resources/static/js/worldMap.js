// ========== 全球地图页面 ==========
let worldMapChart;

$(document).ready(function() {
    initWorldMap();
    loadMapData();
});

function initWorldMap() {
    worldMapChart = echarts.init(document.getElementById('worldMapContainer'), 'sleepTheme');

    $(window).resize(function() {
        worldMapChart.resize();
    });
}

function loadMapData() {
    $.ajax({
        url: '/sleep/map/api/country-data',
        method: 'GET',
        success: function(data) {
            renderWorldMap(data);
        },
        error: function() {
            $('#worldMapContainer').html('<div class="loading">数据加载失败</div>');
        }
    });
}

function renderWorldMap(data) {
    // 显示统计信息
    let totalCountries = data.length;
    let totalRecords = 0;
    for (let i = 0; i < data.length; i++) {
        totalRecords += data[i].value;
    }

    $('#countryCount').text(totalCountries);
    $('#totalRecords').text(totalRecords);

    // 渲染地图
    $.get('/json/world.json', function(worldJson) {
        echarts.registerMap('world', worldJson);

        // 转换数据为地图格式
        var mapData = {};
        for (var i = 0; i < data.length; i++) {
            var name = data[i].name;
            // 标准化国家名称（ECharts地图数据使用的名称）
            name = normalizeCountryName(name);
            mapData[name] = {
                value: data[i].value,
                avgDuration: data[i].avgDuration,
                avgQuality: data[i].avgQuality
            };
        }

        var option = {
            tooltip: {
                trigger: 'item',
                formatter: function(params) {
                    var data = mapData[params.name];
                    if (data) {
                        return params.name + '<br>' +
                               '记录数: ' + data.value + '<br>' +
                               '平均睡眠时长: ' + (data.avgDuration || 0).toFixed(2) + '小时<br>' +
                               '平均睡眠质量: ' + (data.avgQuality || 0).toFixed(2) + '分';
                    }
                    return params.name + ': 无数据';
                }
            },
            visualMap: {
                min: 0,
                max: Math.max.apply(null, data.map(function(d) { return d.value; })),
                left: 'left',
                top: 'bottom',
                text: ['高', '低'],
                calculable: true,
                inRange: {
                    color: ['#e0e7ff', '#6366f1', '#4338ca', '#312e81']
                }
            },
            series: [{
                name: '记录数',
                type: 'map',
                map: 'world',
                roam: true,
                scaleLimit: {
                    min: 1,
                    max: 5
                },
                emphasis: {
                    label: {
                        show: true,
                        color: '#fff'
                    },
                    itemStyle: {
                        areaColor: '#8b5cf6'
                    }
                },
                data: Object.keys(mapData).map(function(key) {
                    return {
                        name: key,
                        value: mapData[key].value
                    };
                })
            }]
        };

        worldMapChart.setOption(option);
    }).fail(function() {
        // 如果地图数据加载失败，显示错误信息
        $('#worldMapContainer').html(
            '<div style="display: flex; align-items: center; justify-content: center; height: 100%; color: #64748b;">' +
            '地图数据加载失败，请检查 world.json 文件<br>' +
            '可以从 <a href="https://datav.jiaminghu.com/" target="_blank">datav.jiaminghu.com</a> 下载' +
            '</div>'
        );
    });
}

// 标准化国家名称（ECharts地图使用的名称）
function normalizeCountryName(name) {
    var nameMap = {
        'United States': 'United States',
        'USA': 'United States',
        'United Kingdom': 'United Kingdom',
        'UK': 'United Kingdom',
        'China': 'China',
        'Japan': 'Japan',
        'Germany': 'Germany',
        'France': 'France',
        'Australia': 'Australia',
        'Canada': 'Canada',
        'Brazil': 'Brazil',
        'India': 'India',
        'Russia': 'Russia',
        'South Korea': 'South Korea',
        'Mexico': 'Mexico',
        'Indonesia': 'Indonesia',
        'Netherlands': 'Netherlands',
        'Saudi Arabia': 'Saudi Arabia',
        'Turkey': 'Turkey',
        'Switzerland': 'Switzerland',
        'Sweden': 'Sweden',
        'Belgium': 'Belgium',
        'Argentina': 'Argentina',
        'Austria': 'Austria',
        'Norway': 'Norway',
        'Ireland': 'Ireland',
        'Denmark': 'Denmark',
        'Finland': 'Finland',
        'Portugal': 'Portugal',
        'Vietnam': 'Vietnam',
        'Philippines': 'Philippines',
        'Poland': 'Poland',
        'Chile': 'Chile',
        'Colombia': 'Colombia',
        'Czech Republic': 'Czech Republic',
        'New Zealand': 'New Zealand',
        'Egypt': 'Egypt',
        'Pakistan': 'Pakistan',
        'Bangladesh': 'Bangladesh',
        'Malaysia': 'Malaysia',
        'Nigeria': 'Nigeria',
        'Thailand': 'Thailand',
        'South Africa': 'South Africa',
        'United Arab Emirates': 'United Arab Emirates',
        'Singapore': 'Singapore',
        'Hong Kong': 'Hong Kong',
        'Taiwan': 'Taiwan'
    };

    return nameMap[name] || name;
}
