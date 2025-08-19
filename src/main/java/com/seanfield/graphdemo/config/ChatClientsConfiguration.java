package com.seanfield.graphdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天客户端配置类
 */
@Configuration
public class ChatClientsConfiguration {

	// DeepSeek（OpenAI 协议）作为默认的聊天客户端构建器

	// Qwen（DashScope）保留，用于图工作流或需要通义千问的场景
	// 直接使用主要的聊天模型，不依赖特定的限定符
	@Bean(name = "qwenChatClientBuilder")
	public ChatClient.Builder qwenChatClientBuilder(ChatModel chatModel) {
		return ChatClient.builder(chatModel);
	}
}


