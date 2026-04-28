package edu.jlu.controllers;

import edu.jlu.services.XgboostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Controller // 将该类声明为 Spring MVC 控制器组件，使其能够处理 HTTP 请求
@RequestMapping("/sleep") // 在类级别指定该控制器处理的基础路径。所有请求映射都以此 /sleep 为前缀。
public class XgboostController { // 定义一个处理睡眠相关 XGBoost 模型操作的控制器类

    @Autowired // 自动将 Spring 容器中的 XgboostService 实例赋值给 xgboostService 字段
    private XgboostService xgboostService;
    // 声明一个私有的 XgboostService 类型的成员变量，用于调用业务逻辑

    /**
     * 返回本页面
     */
    @GetMapping("/xgboost") // 将 HTTP GET 请求 /sleep/xgboost 映射到此方法。该方法会返回视图名称
    public String infoPage(Model model) {
        return "pages/xgboost";
    }

    /**
     * 获取可用模型列表
     */
    @GetMapping("/api/models")
    @ResponseBody // 返回值作为 HTTP 响应体（JSON 格式）
    public List<Map<String, String>> getModels() {
        // 返回 List<Map<String, String>>，每个模型的信息（比如 id、名称等）作为一个 Map
        return xgboostService.getAvailableModels();
        // 调用服务层 getAvailableModels() 方法，返回可用模型的列表
    }

    /**
     * 根据模型ID获取特征元数据（包含名称、类型、分类选项等）
     */
    @GetMapping("/api/model/features")
    @ResponseBody // 返回值作为 HTTP 响应体（JSON 格式）
    public List<Map<String, Object>> getFeatures(@RequestParam("modelId") String modelId) {
        // 根据传入的模型 ID 获取该模型的特征元数据，包括特征名称、数据类型、如果是类别型还会有选项值等
        // 返回 List<Map<String, Object>>，每个特征元数据用一个 Map 表示（可包含不同数据类型的值，所以 Value 类型为 Object）。
        // @RequestParam 表示从请求的查询参数中获取名为 modelId 的值，并绑定到 String modelId 参数上
        try {
            return xgboostService.getFeatureMetas(modelId);
            // 调用服务层的 getFeatureMetas(modelId) 方法获取特征元数据列表
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 执行预测
     */
    @PostMapping("/api/predict")
    @ResponseBody // 返回值作为 HTTP 响应体（JSON 格式）
    public Map<String, Object> predict(@RequestBody Map<String, Object> requestData) {
        try {
            return xgboostService.predict(requestData);
            // 调用服务层的 predict 方法，传入请求数据，返回预测结果（可能包含预测值、概率等）
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}