// ========== 记录列表页面 ==========
let recordsTable;

$(document).ready(function() {
    loadRecords();
});

function loadRecords() {
    $.ajax({
        url: '/sleep/api/records',
        method: 'GET',
        success: function(data) {
            renderTable(data);
        },
        error: function() {
            $('#recordsBody').html('<tr><td colspan="10" class="text-center text-muted">数据加载失败</td></tr>');
        }
    });
}

function renderTable(records) {
    let html = '';
    for (let i = 0; i < records.length; i++) {
        const r = records[i];
        const riskClass = getRiskClass(r.sleepDisorderRisk);
        const riskLabel = r.sleepDisorderRisk || '未知';

        html += '<tr>';
        html += '<td>' + (r.personId || '-') + '</td>';
        html += '<td>' + (r.age || '-') + '</td>';
        html += '<td>' + (r.gender || '-') + '</td>';
        html += '<td>' + (r.occupation || '-') + '</td>';
        html += '<td>' + (r.sleepDurationHrs ? r.sleepDurationHrs.toFixed(1) : '-') + ' 小时</td>';
        html += '<td>' + (r.sleepQualityScore ? r.sleepQualityScore.toFixed(1) : '-') + ' 分</td>';
        html += '<td>' + (r.stressScore ? r.stressScore.toFixed(1) : '-') + '</td>';
        html += '<td>' + (r.chronotype || '-') + '</td>';
        html += '<td><span class="badge-risk ' + riskClass + '">' + riskLabel + '</span></td>';
        html += '<td>' + (r.country || '-') + '</td>';
        html += '</tr>';
    }

    if (records.length === 0) {
        html = '<tr><td colspan="10" class="text-center text-muted">暂无数据</td></tr>';
    }

    $('#recordsBody').html(html);
}

function getRiskClass(risk) {
    if (!risk) return '';
    risk = risk.toLowerCase();
    if (risk.indexOf('low') !== -1 || risk.indexOf('低') !== -1) return 'low';
    if (risk.indexOf('high') !== -1 || risk.indexOf('高') !== -1) return 'high';
    return 'medium';
}
