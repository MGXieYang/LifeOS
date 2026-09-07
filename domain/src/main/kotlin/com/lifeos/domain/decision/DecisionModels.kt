package com.lifeos.domain.decision

import java.time.LocalDate
import java.time.LocalDateTime

enum class DecisionPreference { YES, NO, UNDECIDED, CUSTOM }
enum class DecisionCategory { CAREER, CONSUMPTION, FINANCE, RESIDENCE, RELATIONSHIP, LEARNING, TRAVEL, OTHER }
enum class DecisionStatus { THINKING, DECIDED, WAITING, REVIEW_DUE, REVIEWED }
enum class ResultRating { BETTER, AS_EXPECTED, WORSE, UNCLEAR }
enum class ChooseAgain { YES, NO, UNSURE }

data class Decision(
    val id: Long = 0,
    val title: String,
    val category: DecisionCategory? = null,
    val background: String = "",
    val initialPreference: DecisionPreference = DecisionPreference.UNDECIDED,
    val customPreference: String = "",
    val confidence: Int? = null,
    val mainReason: String = "",
    val biggestRisk: String = "",
    val expectedResult: String = "",
    val status: DecisionStatus = DecisionStatus.THINKING,
    val finalDecision: String = "",
    val createdAt: LocalDateTime,
    val decidedAt: LocalDateTime? = null,
    val reviewDate: LocalDate? = null,
) {
    init {
        require(title.isNotBlank()) { "请输入决策标题" }
        require(confidence == null || confidence in 0..100 && confidence % 5 == 0) { "信心程度应为 0～100%，步长 5%" }
        if (initialPreference == DecisionPreference.CUSTOM) require(customPreference.isNotBlank()) { "请输入自定义倾向" }
    }
    fun effectiveStatus(today: LocalDate): DecisionStatus = when {
        status == DecisionStatus.REVIEWED -> DecisionStatus.REVIEWED
        decidedAt != null && reviewDate != null && !today.isBefore(reviewDate) -> DecisionStatus.REVIEW_DUE
        else -> status
    }
}

data class DecisionReview(
    val id: Long = 0,
    val decisionId: Long,
    val resultRating: ResultRating,
    val wouldChooseAgain: ChooseAgain,
    val satisfactionScore: Int? = null,
    val correctJudgment: String = "",
    val wrongJudgment: String = "",
    val reflection: String = "",
    val reviewedAt: LocalDateTime,
) {
    init { require(satisfactionScore == null || satisfactionScore in 1..10) { "满意度应为 1～10 分" } }
}

data class DecisionStatistics(val total: Int, val reviewed: Int, val averageSatisfaction: Double?, val regretCount: Int)
object DecisionStatisticsCalculator {
    fun calculate(decisions: List<Decision>, reviews: List<DecisionReview>): DecisionStatistics {
        val scores = reviews.mapNotNull { it.satisfactionScore }
        return DecisionStatistics(decisions.size, reviews.size,
            scores.takeIf { it.isNotEmpty() }?.average(), reviews.count { it.wouldChooseAgain == ChooseAgain.NO })
    }
}
