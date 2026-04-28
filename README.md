# Spring Boot 框架实现 Echarts 前端设计

## 文件结构

```
EchartsCooperation/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── edu/jlu/
│       │       ├── FrontendApplication.java （主启动类）
│       │       ├── controllers/ （控制类，数据传输）
│       │       │   ├── HomeController.java （待完善）
│       │       │   ├── SleepHealthController.java
│       │       │   ├── MapController.java
│       │       │   └── XgboostController.java
│       │       ├── services/ （服务类，功能实现）
│       │       │   ├── SleepHealthService.java
│       │       │   ├── MapService.java
│       │       │   └── XgboostService.java
│       │       └── models/ （数据结构）
│       │           └── SleepHealthRecord.java
│       └── resources/
│           ├── application.properties （配置）
│           ├── templates/pages/ （前端界面）
│           │   ├── dashboard.html
│           │   ├── records.html
│           │   ├── world-map.html
│           │   └── xgboost.html
│           └── static/ （静态数据，包括css、js、json文件等）
```

## 功能开发

| 标签 | 功能 | 状态 | 负责人 |
|------|------|------|------|
| 数据仪表盘 | 简单数据统计 | 还可以加 | 所有人 |
| 数据记录表 | 全部数据的表格 | 正在做 | xjx |
| 全球分布图 | 数据在地图上的可视化显示 | 还可以加 | xjx |
| 模型预测 | 输入信息实现预测功能 | 正在做 | ls |
| 睡眠障碍统计 | 对特定目标列（睡眠障碍风险评估）的可视化 | 正在做 | jk |

