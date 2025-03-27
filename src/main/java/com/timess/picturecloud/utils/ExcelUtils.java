package com.timess.picturecloud.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xing10
 *  Excel相关工具类
 *  使用easy excel 库对excel文件进行处理，并转换成csv文件
 */
@Slf4j
public class ExcelUtils {
    public static String excelToCsv(MultipartFile multipartFile) {

        //读取数据
        List<Map<Integer,String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
           log.error("表格处理错误", e);
        }
        if(CollUtil.isEmpty(list)){
            return "";
        }
        //转换为csv格式
        //读取表头，以列的方式读取数据
        StringBuilder stringBuilder = new StringBuilder();

        //将读取的数据转换成有序的LinkedHashMap
        LinkedHashMap<Integer, String> headerHashMap = (LinkedHashMap) list.get(0);
        //过滤掉表头为null的，而不是为空的，为空的有时候也是有用的
        List<String> headerList = headerHashMap.values().stream().
                filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList,",")).append("\n");

        //读取数据
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap) list.get(i);
            //过滤掉表头为null的，而不是为空的，为空的有时候也是有用的
            List<String> dataList = dataMap.values().stream().
                    filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList,",")).append("\n");
        }
        return stringBuilder.toString();
    }
}
