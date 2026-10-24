package com.qingqian.chonglema.stats

/** 一个成就徽章。解锁条件全部由现有记录实时算出，不额外存状态。 */
data class Badge(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val unlocked: Boolean,
)

fun computeBadges(
    stats: CheckInStats,
    challengeProgresses: List<ChallengeProgress> = emptyList(),
): List<Badge> {
    val impulses = stats.doneCount + stats.resistCount
    val succeeded = challengeProgresses.count { it.completed }
    val longestChallenge = challengeProgresses.maxOfOrNull { progress ->
        if (progress.failed) progress.cleanDays
        else if (progress.completed) progress.days
        else progress.cleanDays
    } ?: 0
    fun cleared(days: Int) = challengeProgresses.any { it.completed && it.days >= days }

    return listOf(
        Badge("first", "🎉", "初次打卡", "记下第一笔", impulses >= 1),
        Badge("streak3", "🔥", "坚持 3 天", "连续打卡 3 天", stats.bestStreak >= 3),
        Badge("streak7", "🔥", "坚持 7 天", "连续打卡 7 天", stats.bestStreak >= 7),
        Badge("streak14", "🔥", "坚持 14 天", "连续打卡 14 天", stats.bestStreak >= 14),
        Badge("streak30", "🏆", "坚持 30 天", "连续打卡 30 天", stats.bestStreak >= 30),
        Badge("count10", "📌", "记录 10 次", "累计 10 次记录", impulses >= 10),
        Badge("count50", "📌", "记录 50 次", "累计 50 次记录", impulses >= 50),
        Badge("count100", "💯", "记录 100 次", "累计 100 次记录", impulses >= 100),
        Badge("resist1", "🛡️", "第一次忍住", "记录一次「忍住了」", stats.resistCount >= 1),
        Badge("resist10", "🛡️", "忍住 10 次", "累计忍住 10 次", stats.resistCount >= 10),
        Badge("resist50", "🛡️", "忍住 50 次", "累计忍住 50 次", stats.resistCount >= 50),
        Badge("days30", "📅", "记录 30 天", "共有 30 个不同的打卡日", stats.totalDays >= 30),
        Badge("max5", "🚀", "放飞自我", "单日 5 次", stats.maxPerDay >= 5),
        Badge("challengeFirst", "🎯", "挑战成功", "完成第一次挑战", succeeded >= 1),
        Badge("challenge3", "🌱", "干净 3 天", "最长一次守住 3 天", longestChallenge >= 3),
        Badge("challenge7", "🛡️", "守住 7 天", "完成一次 7 天以上的挑战", cleared(7)),
        Badge("challenge14", "🛡️", "守住 14 天", "完成一次 14 天以上的挑战", cleared(14)),
        Badge("challenge30", "👑", "守住 30 天", "完成一次 30 天挑战", cleared(30)),
        Badge("challenge5", "🏅", "挑战达人", "累计挑战成功 5 次", succeeded >= 5),
    )
}
