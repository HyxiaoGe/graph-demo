package com.seanfield.graphdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientsConfiguration {

	// DeepSeek（OpenAI 协议）作为默认 ChatClient.Builder

	// Qwen（DashScope）保留，用于 Graph 或需要 qwen 的场景
	// 直接使用主要的 ChatModel，不依赖特定的 qualifier
	@Bean(name = "qwenChatClientBuilder")
	public ChatClient.Builder qwenChatClientBuilder(ChatModel chatModel) {
		return ChatClient.builder(chatModel);
	}
}


