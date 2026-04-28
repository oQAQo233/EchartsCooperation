// ========== 记录列表页面 - DataTables 服务端模式 ==========

$(document).ready(function() {
    $('#recordsTable').DataTable({
        processing: true,
        serverSide: true,
        paging: true,
        searching: true,
        ordering: true,
        lengthMenu: [10, 25, 50, 100],
        pageLength: 10,
        language: {
            lengthMenu: "每页 _MENU_ 条记录",
            zeroRecords: "没有找到记录",
            info: "第 _PAGE_ 页 (共 _PAGES_ 页)",
            infoEmpty: "无记录",
            search: "搜索:",
            paginate: {
                first: "首页",
                last: "末页",
                next: "下一页",
                previous: "上一页"
            },
            processing: "加载中..."
        },
        ajax: {
            url: '/sleep/api/records',
            type: 'GET',
            data: function(d) {
                if (d.order && d.order.length > 0) {
                    d['order[0][column]'] = d.order[0].column;
                    d['order[0][dir]'] = d.order[0].dir;
                }
            },
            dataSrc: function(json) {
                return json.data;
            }
        },
        columns: [
            { data: 'personId' },
            { data: 'age' },
            { data: 'gender' },
            { data: 'occupation' },
            { data: 'sleepDurationHrs' },
            { data: 'sleepQualityScore' },
            { data: 'stressScore' },
            { data: 'chronotype' },
            { data: 'sleepDisorderRisk' },
            { data: 'country' }
        ],
        columnDefs: [
            {
                render: function(data, type, row) {
                    if (row.sleepDisorderRisk) {
                        var riskClass = getRiskClass(row.sleepDisorderRisk);
                        return '<span class="badge-risk ' + riskClass + '">' + row.sleepDisorderRisk + '</span>';
                    }
                    return '-';
                },
                targets: 8
            },
            {
                render: function(data, type, row) {
                    if (data) {
                        return data.toFixed(1) + ' 小时';
                    }
                    return '-';
                },
                targets: 4
            },
            {
                render: function(data, type, row) {
                    if (data) {
                        return data.toFixed(1) + ' 分';
                    }
                    return '-';
                },
                targets: 5
            }
        ]
    });
});

function getRiskClass(risk) {
    if (!risk) return '';
    risk = risk.toLowerCase();
    if (risk.indexOf('low') !== -1 || risk.indexOf('低') !== -1) return 'low';
    if (risk.indexOf('high') !== -1 || risk.indexOf('高') !== -1) return 'high';
    return 'medium';
}
