package edu.jlu.models;

public class SleepHealthRecord {
    private Integer personId; // ID
    private Integer age; // 年龄
    private String gender; // 性别
    private String occupation; // 职业
    private Double bmi; // BMI指标
    private String country; // 国家

    private Double sleepDurationHrs; // 睡眠时长（小时）
    private Double sleepQualityScore; // 睡眠质量评分（1-10）
    private Double remPercentage; // REM睡眠占比（%）
    private Double deepSleepPercentage; // 深度睡眠占比（%）
    private Integer sleepLatencyMins; // 入睡潜伏期（分钟）
    private Integer wakeEpisodesPerNight; // 夜间觉醒次数
    
    private Integer caffeineMgBeforeBed; // 睡前咖啡因摄入量（毫克）
    private Double alcoholUnitsBeforeBed; // 睡前酒精摄入量（单位）
    private Integer screenTimeBeforeBedMins; // 睡前屏幕使用时间（分钟）
    private Integer exerciseDay; // 当天是否运动（0=否，1=是）
    private Integer stepsThatDay; // 当天步数
    private Integer napDurationMins; // 白天小睡时长（分钟）
    private Double stressScore; // 压力指数（1-10）
    private Double workHoursThatDay; // 当天工作时长（小时）
    private String chronotype; // 生物钟类型（"Morning", "Evening", "Neutral"）
    private String mentalHealthCondition; // 心理健康问题（"Healthy", "Anxiety", "Depression", "Both"）
    private Integer heartRateRestingBpm; // 静息心率（每分钟心跳次数）
    private Integer sleepAidUsed; // 是否使用助眠产品（0=否，1=是）
    private Integer shiftWork; // 是否轮班工作（0=否，1=是）
    private Double roomTemperatureCelsius; // 卧室温度（摄氏度）
    private Double weekendSleepDiffHrs; // 周末与工作日睡眠时长差异（小时）
    private String season; // 季节（"Spring", "Summer", "Autumn", "Winter"）
    private String dayType; // 日期类型（"Weekday", "Weekend"）

    private Double cognitivePerformanceScore; // 认知表现评分（1-10）
    private String sleepDisorderRisk; // 睡眠障碍风险（"Healthy", "Mild", "Moderate"）
    private Integer feltRested; // 是否感觉休息好（0=否，1=是）

    public SleepHealthRecord() {
    }

    public Integer getPersonId() { return personId; }
    public void setPersonId(Integer personId) { this.personId = personId; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Double getSleepDurationHrs() { return sleepDurationHrs; }
    public void setSleepDurationHrs(Double sleepDurationHrs) { this.sleepDurationHrs = sleepDurationHrs; }
    public Double getSleepQualityScore() { return sleepQualityScore; }
    public void setSleepQualityScore(Double sleepQualityScore) { this.sleepQualityScore = sleepQualityScore; }
    public Double getRemPercentage() { return remPercentage; }
    public void setRemPercentage(Double remPercentage) { this.remPercentage = remPercentage; }
    public Double getDeepSleepPercentage() { return deepSleepPercentage; }
    public void setDeepSleepPercentage(Double deepSleepPercentage) { this.deepSleepPercentage = deepSleepPercentage; }
    public Integer getSleepLatencyMins() { return sleepLatencyMins; }
    public void setSleepLatencyMins(Integer sleepLatencyMins) { this.sleepLatencyMins = sleepLatencyMins; }
    public Integer getWakeEpisodesPerNight() { return wakeEpisodesPerNight; }
    public void setWakeEpisodesPerNight(Integer wakeEpisodesPerNight) { this.wakeEpisodesPerNight = wakeEpisodesPerNight; }
    public Integer getCaffeineMgBeforeBed() { return caffeineMgBeforeBed; }
    public void setCaffeineMgBeforeBed(Integer caffeineMgBeforeBed) { this.caffeineMgBeforeBed = caffeineMgBeforeBed; }
    public Double getAlcoholUnitsBeforeBed() { return alcoholUnitsBeforeBed; }
    public void setAlcoholUnitsBeforeBed(Double alcoholUnitsBeforeBed) { this.alcoholUnitsBeforeBed = alcoholUnitsBeforeBed; }
    public Integer getScreenTimeBeforeBedMins() { return screenTimeBeforeBedMins; }
    public void setScreenTimeBeforeBedMins(Integer screenTimeBeforeBedMins) { this.screenTimeBeforeBedMins = screenTimeBeforeBedMins; }
    public Integer getExerciseDay() { return exerciseDay; }
    public void setExerciseDay(Integer exerciseDay) { this.exerciseDay = exerciseDay; }
    public Integer getStepsThatDay() { return stepsThatDay; }
    public void setStepsThatDay(Integer stepsThatDay) { this.stepsThatDay = stepsThatDay; }
    public Integer getNapDurationMins() { return napDurationMins; }
    public void setNapDurationMins(Integer napDurationMins) { this.napDurationMins = napDurationMins; }
    public Double getStressScore() { return stressScore; }
    public void setStressScore(Double stressScore) { this.stressScore = stressScore; }
    public Double getWorkHoursThatDay() { return workHoursThatDay; }
    public void setWorkHoursThatDay(Double workHoursThatDay) { this.workHoursThatDay = workHoursThatDay; }
    public String getChronotype() { return chronotype; }
    public void setChronotype(String chronotype) { this.chronotype = chronotype; }
    public String getMentalHealthCondition() { return mentalHealthCondition; }
    public void setMentalHealthCondition(String mentalHealthCondition) { this.mentalHealthCondition = mentalHealthCondition; }
    public Integer getHeartRateRestingBpm() { return heartRateRestingBpm; }
    public void setHeartRateRestingBpm(Integer heartRateRestingBpm) { this.heartRateRestingBpm = heartRateRestingBpm; }
    public Integer getSleepAidUsed() { return sleepAidUsed; }
    public void setSleepAidUsed(Integer sleepAidUsed) { this.sleepAidUsed = sleepAidUsed; }
    public Integer getShiftWork() { return shiftWork; }
    public void setShiftWork(Integer shiftWork) { this.shiftWork = shiftWork; }
    public Double getRoomTemperatureCelsius() { return roomTemperatureCelsius; }
    public void setRoomTemperatureCelsius(Double roomTemperatureCelsius) { this.roomTemperatureCelsius = roomTemperatureCelsius; }
    public Double getWeekendSleepDiffHrs() { return weekendSleepDiffHrs; }
    public void setWeekendSleepDiffHrs(Double weekendSleepDiffHrs) { this.weekendSleepDiffHrs = weekendSleepDiffHrs; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }
    public Double getCognitivePerformanceScore() { return cognitivePerformanceScore; }
    public void setCognitivePerformanceScore(Double cognitivePerformanceScore) { this.cognitivePerformanceScore = cognitivePerformanceScore; }
    public String getSleepDisorderRisk() { return sleepDisorderRisk; }
    public void setSleepDisorderRisk(String sleepDisorderRisk) { this.sleepDisorderRisk = sleepDisorderRisk; }
    public Integer getFeltRested() { return feltRested; }
    public void setFeltRested(Integer feltRested) { this.feltRested = feltRested; }
}
