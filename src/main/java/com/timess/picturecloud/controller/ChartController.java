package com.timess.picturecloud.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.google.gson.Gson;
//import com.timess.smart_strat.annotation.AuthCheck;
//import com.timess.smart_strat.bizmq.MQMessageProducer;
//import com.timess.smart_strat.common.BaseResponse;
//import com.timess.smart_strat.common.DeleteRequest;
//import com.timess.smart_strat.common.ErrorCode;
//import com.timess.smart_strat.common.ResultUtils;
//import com.timess.smart_strat.constant.CommonConstant;
//import com.timess.smart_strat.constant.UserConstant;
//import com.timess.smart_strat.exception.BusinessException;
//import com.timess.smart_strat.exception.ThrowUtils;
//import com.timess.smart_strat.manager.AiManager;
//import com.timess.smart_strat.manager.RedissonLimitManager;
//import com.timess.smart_strat.model.dto.chart.*;
//import com.timess.smart_strat.model.entity.Chart;
//import com.timess.smart_strat.model.entity.User;
//import com.timess.smart_strat.model.vo.AiResponse;
//import com.timess.smart_strat.service.ChartService;
//import com.timess.smart_strat.service.UserService;
//import com.timess.smart_strat.utils.ExcelUtils;
//import com.timess.smart_strat.utils.SqlUtils;
import com.timess.picturecloud.annotation.AuthCheck;
import com.timess.picturecloud.bizmq.MQMessageProducer;
import com.timess.picturecloud.common.BaseResponse;
import com.timess.picturecloud.common.DeleteRequest;
import com.timess.picturecloud.common.ResultUtils;
import com.timess.picturecloud.constant.CommonConstant;
import com.timess.picturecloud.constant.UserConstant;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.manager.AiManager;
import com.timess.picturecloud.manager.RedissonLimitManager;
import com.timess.picturecloud.model.domain.Chart;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.chart.*;
import com.timess.picturecloud.model.vo.AiResponse;
import com.timess.picturecloud.service.ChartService;
import com.timess.picturecloud.service.UserService;
import com.timess.picturecloud.utils.ExcelUtils;
import com.timess.picturecloud.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *图表接口
 * @author xing10
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonLimitManager redissonLimitManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private MQMessageProducer mqMessageProducer;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 文件智能分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<AiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验
        //如果分析目标为空，抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"分析目标为空");
        //如果名称不为空，且长度>100,抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(name) || name.length() > 100, ErrorCode.PARAMS_ERROR,"图表名称过长");

        /**
         * 文件格式和大小校验
         *
         */
        //获取文件大小
        long size = multipartFile.getSize();

        //取得原始文件名
        String originalFilename = multipartFile.getOriginalFilename();

        /**
         * 校验文件大小
         * 定义一个常量等于1MB = 1024 * 1024 byte
         */
        final long ONE_MB = 1024 * 1024L;
        //如果文件大于1MB,则抛出异常，并提示
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过1M");

        /**
         * 校验文件后缀
         * 一般文件名为： xxx.文件后缀
         * 利用FileUtil工具类中的getSuffix方法获取文件后缀名
         */
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg","xlsx");
        //如果suffix不在validFileSuffixList有效文件后缀内，抛出异常，并提示"文件后缀非法"
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //调用用户
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redissonLimitManager.doRateLimit("aiInvoke_"  + loginUser.getId());

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");

        //拼接分析目标
        String userGoal = goal;
        //如果图表类型不为空
        if(StringUtils.isNotBlank(chartType)){
            //就将分析目标拼接上"请使用" + 图表类型
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据： ").append("\n");

        //将传入的excel数据压缩csv格式
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //调用接口拿到返回结果
        // TODO:替换调用方式
        String result = aiManager.invoke(userInput.toString());

        //对返回结果进行拆分，按照五个中括号进行拆分
        String [] splits = result.split("【【【【【");
        //对拆分部分进行校验
        if(splits.length < 3){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"ai生成错误");
        }

        //提取图表代码
        String genChart = splits[1].trim();
        //提取分析结论信息
        String genResult = splits[2].trim();

        //插入数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        //保存图表信息
        boolean saveResult = chartService.save(chart);
        //保存不成功，报错
        ThrowUtils.throwIf(!saveResult,ErrorCode.PARAMS_ERROR, "图表保存失败");

        //返回响应数据
        AiResponse aiResponse = new AiResponse();
        aiResponse.setGenChart(genChart);
        aiResponse.setGenResult(genResult);
        aiResponse.setChartId(chart.getId());
        return ResultUtils.success(aiResponse);
    }

    /**
     * 文件智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<AiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验
        //如果分析目标为空，抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"分析目标为空");
        //如果名称不为空，且长度>100,抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(name) || name.length() > 100, ErrorCode.PARAMS_ERROR,"图表名称过长");

        /**
         * 文件格式和大小校验
         *
         */
        //获取文件大小
        long size = multipartFile.getSize();

        //取得原始文件名
        String originalFilename = multipartFile.getOriginalFilename();

        /**
         * 校验文件大小
         * 定义一个常量等于1MB = 1024 * 1024 byte
         */
        final long ONE_MB = 1024 * 1024L;
        //如果文件大于1MB,则抛出异常，并提示
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过1M");

        /**
         * 校验文件后缀
         * 一般文件名为： xxx.文件后缀
         * 利用FileUtil工具类中的getSuffix方法获取文件后缀名
         */
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffixList = Arrays.asList("xls","xlsx");
        //如果suffix不在validFileSuffixList有效文件后缀内，抛出异常，并提示"文件后缀非法"
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //调用用户
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redissonLimitManager.doRateLimit("aiInvoke_"  + loginUser.getId());

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");

        //拼接分析目标
        String userGoal = goal;
        //如果图表类型不为空
        if(StringUtils.isNotBlank(chartType)){
            //就将分析目标拼接上"请使用" + 图表类型
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据： ").append("\n");

        //将传入的excel数据压缩csv格式
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //插入数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        //保存图表信息
        boolean saveResult = chartService.save(chart);
        //保存不成功，报错
        ThrowUtils.throwIf(!saveResult,ErrorCode.PARAMS_ERROR, "图表保存失败");

        //由原来的直接调用接口，变成了提交任务
        CompletableFuture.runAsync(()->{
            //为了减少重复提交，优先将图表状态该为执行中
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if(!b){
                handleChartUpdateError(updateChart.getId(), "更新图表执行中状态失败");
                return;
            }
            //调用AI接口
            // TODO:AI接口失效后更改
            String result = aiManager.invoke(userInput.toString());
            //对返回结果进行拆分，按照五个中括号进行拆分
            String [] splits = result.split("【【【【【");
            //对拆分部分进行校验
            if(splits.length < 3){
                handleChartUpdateError(chart.getId(), "AI生成错误");
            }
            //提取图表代码
            String genChart = splits[1].trim();
            //提取分析结论信息
            String genResult = splits[2].trim();
            //得到分析结果后，再次更新chart
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            //更新
            boolean updateResult = chartService.updateById(updateChartResult);
            if(! updateResult){
                handleChartUpdateError(chart.getId(), "更新图表状态为“succeed” 失败");
            }

        }, threadPoolExecutor);

        //返回响应数据
        AiResponse aiResponse = new AiResponse();
        aiResponse.setChartId(chart.getId());
        return ResultUtils.success(aiResponse);
    }

    /**
     * 文件智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<AiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验
        //如果分析目标为空，抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"分析目标为空");
        //如果名称不为空，且长度>100,抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(name) || name.length() > 100, ErrorCode.PARAMS_ERROR,"图表名称过长");

        /**
         * 文件格式和大小校验
         *
         */
        //获取文件大小
        long size = multipartFile.getSize();

        //取得原始文件名
        String originalFilename = multipartFile.getOriginalFilename();

        /**
         * 校验文件大小
         * 定义一个常量等于1MB = 1024 * 1024 byte
         */
        final long ONE_MB = 1024 * 1024L;
        //如果文件大于1MB,则抛出异常，并提示
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过1M");

        /**
         * 校验文件后缀
         * 一般文件名为： xxx.文件后缀
         * 利用FileUtil工具类中的getSuffix方法获取文件后缀名
         */
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffixList = Arrays.asList("xls","xlsx");
        //如果suffix不在validFileSuffixList有效文件后缀内，抛出异常，并提示"文件后缀非法"
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //调用用户
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redissonLimitManager.doRateLimit("aiInvoke_"  + loginUser.getId());

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");

        //拼接分析目标
        String userGoal = goal;
        //如果图表类型不为空
        if(StringUtils.isNotBlank(chartType)){
            //就将分析目标拼接上"请使用" + 图表类型
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据： ").append("\n");

        //将传入的excel数据压缩csv格式
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //插入数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        //保存图表信息
        boolean saveResult = chartService.save(chart);
        //保存不成功，报错
        ThrowUtils.throwIf(!saveResult,ErrorCode.PARAMS_ERROR, "图表保存失败");

        mqMessageProducer.sendMessage(String.valueOf(chart.getId()));
        //返回响应数据
        AiResponse aiResponse = new AiResponse();
        aiResponse.setChartId(chart.getId());
        return ResultUtils.success(aiResponse);
    }


    /**
     * 异常工具类
     */
    private void handleChartUpdateError(long charId, String execMessage){
        Chart updateChart = new Chart();
        updateChart.setId(charId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChart);
        if(!updateResult){
            log.error("更新图表状态为“failed” 失败" + charId  + "," + execMessage);
        }
    }
}
