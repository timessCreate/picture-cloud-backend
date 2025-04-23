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
                .networkConfig(60, 60, 240, 240, TimeUnit.SECONDS).build();

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
}
