package com.timess.picturecloud.manager;

import com.timess.picturecloud.common.AiDescription;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author xing10
 * 用于对接AI平台
 */
@Service
public class AiManager {
    /**
     * AI对话
     */
    @Value("${deepseek.sk}")
    private String sk;

    public String invoke(String message) {
        ClientV4 client = new ClientV4.Builder(sk)
                .networkConfig(30, 30, 60, 60, TimeUnit.SECONDS).build();

        List<ChatMessage> messages = getChatMessages(message);
        final String requestIdTemplate = "req-%s";
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4Flash)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        return (String) invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent();
    }

    @NotNull
    private static List<ChatMessage> getChatMessages(String message) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), message +"\n"+ AiDescription.str + "\n"+ AiDescription.example);
        messages.add(chatMessage);
        return messages;
    }


//    public String invoke(String message){
//        // 1. 构建请求头
//        String bearerToken = "Bearer " + sk;
//
//        // 2. 使用JSONObject构建请求体
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("model", "deepseek-ai/DeepSeek-R1-Distill-Qwen-14B");
//        requestBody.put("stream", false);
//        requestBody.put("max_tokens", 8192);
//        requestBody.put("temperature", 0.7);
//        requestBody.put("top_p", 0.7);
//        requestBody.put("top_k", 50);
//        requestBody.put("frequency_penalty", 0.5);
//        requestBody.put("n", 1);
//        // 空数组替代null
//        requestBody.put("stop", new JSONArray());
//
//        // 构建messages数组
//        JSONArray messages = new JSONArray();
//        JSONObject userMessage = new JSONObject();
//        userMessage.put("role", "user");
//        // 自动处理特殊字符转义
//        userMessage.put("content", message);
//        messages.put(userMessage);
//        requestBody.put("messages", messages);
//        try {
//            // 3. 发送请求
//            HttpResponse<String> response = Unirest.post("https://api.siliconflow.cn/v1/chat/completions")
//                    .header("Authorization", bearerToken)
//                    .header("Content-Type", "application/json")
//                    // 自动转换为合法JSON字符串
//                    .body(requestBody.toString())
//                    .asString();
//
//            if (response.isSuccess()) {
//                JSONObject responseJson = new JSONObject(response.getBody());
//                String result = responseJson.getJSONArray("choices")
//                        .getJSONObject(0)
//                        .getJSONObject("message")
//                        .getString("reasoning_content");
//                System.out.println("响应内容：" + result);
//                return result;
//            } else {
//                System.out.println("响应异常" + response);
//                return "API Error: " + response.getStatus();
//            }
//        } catch (Exception e) {
//            System.out.println("出现异常" + e.getMessage());
//            return "Request Failed: " + e.getMessage();
//        }
//    }
}
