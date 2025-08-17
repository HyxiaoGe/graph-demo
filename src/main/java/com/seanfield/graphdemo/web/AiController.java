package com.seanfield.graphdemo.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

	@Autowired
	private ChatClient.Builder chatClientBuilder;

	@GetMapping("/ai/hello")
	public String hello(@RequestParam(name = "q", defaultValue = "用一句话介绍你自己") String question) {
		return chatClientBuilder.build()
			.prompt()
			.user(question)
			.call()
			.content();
	}
}


