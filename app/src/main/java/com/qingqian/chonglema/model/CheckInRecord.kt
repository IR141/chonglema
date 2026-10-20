package com.qingqian.chonglema.model

/** 记录结果：真的冲了。 */
const val OUTCOME_DONE = "DONE"

/** 记录结果：欲望来了但忍住了。 */
const val OUTCOME_RESIST = "RESIST"

/**
 * 一条打卡记录：既可以是「冲了」，也可以是「忍住了」。
 */
data class CheckInRecord(
    val id: Long = 0L,
    /** 日期，格式 yyyy-MM-dd */
    val date: String,
    /** 时间，格式 HH:mm */
    val time: String,
    /** 类型：自慰 / 梦遗 / 性行为 / 其它 / 忍住 */
    val type: String,
    /** 备注（选填） */
    val note: String = "",
    /** 毫秒时间戳，用于排序与间隔统计 */
    val timestamp: Long,
    /** 结果：OUTCOME_DONE 或 OUTCOME_RESIST */
    val outcome: String = OUTCOME_DONE,
) {
    /** 是否是一条「忍住了」的记录。 */
    val isResist: Boolean get() = outcome == OUTCOME_RESIST
}
