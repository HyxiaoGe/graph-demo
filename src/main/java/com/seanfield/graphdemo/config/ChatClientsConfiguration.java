package com.seanfield.graphdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class ChatClientsConfiguration {

	// DeepSeek（OpenAI 协议）作为默认 ChatClient.Builder
	@Bean
	@Primary
	public ChatClient.Builder deepseekChatClientBuilder(@Qualifier("openAiChatModel") ChatModel openAiChatModel) {
		return ChatClient.builder(openAiChatModel);
	}

	// Qwen（DashScope）保留，用于 Graph 或需要 qwen 的场景
	@Bean(name = "qwenChatClientBuilder")
	public ChatClient.Builder qwenChatClientBuilder(@Qualifier("dashscopeChatModel") ChatModel dashscopeChatModel) {
		return ChatClient.builder(dashscopeChatModel);
	}
}


