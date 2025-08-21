package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库搜索节点：从预定义的FAQ知识库中搜索匹配答案
 */
public class KnowledgeSearchNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchNode.class);

    // 模拟的FAQ知识库
    private static final List<FAQItem> FAQ_DATABASE = Arrays.asList(
            new FAQItem("产品功能", "我们的产品主要功能包括：智能分析、数据可视化、自动化报告生成等。支持多种数据源集成。", Arrays.asList("功能", "产品", "特性", "能力")),
            new FAQItem("价格政策", "我们提供多种套餐：基础版199元/月，专业版499元/月，企业版999元/月。支持年付优惠。", Arrays.asList("价格", "费用", "收费", "套餐", "多少钱")),
            new FAQItem("使用方法", "使用步骤：1.注册账号 2.上传数据 3.选择分析模板 4.生成报告 5.导出结果。详细教程请查看帮助文档。", Arrays.asList("怎么用", "使用", "操作", "教程", "步骤")),
            new FAQItem("技术支持", "技术支持时间：工作日9:00-18:00。联系方式：400-123-4567，support@company.com。", Arrays.asList("支持", "客服", "联系", "帮助", "电话")),
            new FAQItem("数据安全", "我们采用企业级加密技术，通过ISO27001认证，数据存储在国内合规机房，确保数据安全。", Arrays.asList("安全", "加密", "隐私", "保护", "认证")),
            new FAQItem("免费试用", "新用户可免费试用14天专业版功能，无需绑定信用卡。试用期结束后可选择合适套餐。", Arrays.asList("试用", "免费", "体验", "测试"))
    );

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("KnowledgeSearchNode开始执行，当前状态: {}", state.data());

        // 获取用户问题和意图分析结果
        Map<String, Object> input = state.value("input", Map.of());
        String question = String.valueOf(input.getOrDefault("question", ""));
        
        log.info("在知识库中搜索问题: {}", question);

        // 搜索最匹配的FAQ
        FAQSearchResult searchResult = searchFAQ(question);
        
        log.info("知识库搜索完成: 找到匹配项={}, 匹配度={}", 
                searchResult.found, searchResult.matchScore);

        // 构建返回结果
        HashMap<String, Object> knowledgeInfo = new HashMap<>();
        knowledgeInfo.put("search_found", searchResult.found);
        knowledgeInfo.put("match_score", searchResult.matchScore);
        
        if (searchResult.found) {
            knowledgeInfo.put("faq_title", searchResult.faqItem.title);
            knowledgeInfo.put("faq_answer", searchResult.faqItem.answer);
            knowledgeInfo.put("source", "knowledge_base");
        } else {
            knowledgeInfo.put("message", "知识库中未找到匹配的答案");
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("knowledge_search", knowledgeInfo);
        
        return result;
    }

    private FAQSearchResult searchFAQ(String question) {
        String questionLower = question.toLowerCase();
        FAQItem bestMatch = null;
        int bestScore = 0;

        for (FAQItem faq : FAQ_DATABASE) {
            int score = calculateMatchScore(questionLower, faq);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = faq;
            }
        }

        // 设置匹配阈值
        boolean found = bestScore >= 3;
        return new FAQSearchResult(found, bestScore, bestMatch);
    }

    private int calculateMatchScore(String question, FAQItem faq) {
        int score = 0;
        for (String keyword : faq.keywords) {
            if (question.contains(keyword.toLowerCase())) {
                score += keyword.length(); // 关键词越长，权重越高
            }
        }
        return score;
    }

    private static class FAQItem {
        final String title;
        final String answer;
        final List<String> keywords;

        FAQItem(String title, String answer, List<String> keywords) {
            this.title = title;
            this.answer = answer;
            this.keywords = keywords;
        }
    }

    private static class FAQSearchResult {
        final boolean found;
        final int matchScore;
        final FAQItem faqItem;

        FAQSearchResult(boolean found, int matchScore, FAQItem faqItem) {
            this.found = found;
            this.matchScore = matchScore;
            this.faqItem = faqItem;
        }
    }
}
