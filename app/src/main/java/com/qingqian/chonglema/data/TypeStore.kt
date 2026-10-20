package com.qingqian.chonglema.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * 打卡类型词条：用户可以自己增删，持久化到 SharedPreferences。
 * 删除词条不会动数据库里已经记录下来的类型字符串。
 */
class TypeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("chonglema_settings", Context.MODE_PRIVATE)

    private val _types = MutableStateFlow(load())
    val types: StateFlow<List<String>> = _types.asStateFlow()

    /** 添加一个词条；返回 null 表示成功，否则返回给用户看的错误文案。 */
    fun add(raw: String): String? {
        val name = raw.trim()
        if (name.isEmpty()) return "词条不能为空"
        if (name.length > MAX_NAME_LENGTH) return "词条最多 " + MAX_NAME_LENGTH + " 个字"
        if (_types.value.any { it.equals(name, ignoreCase = true) }) return "「" + name + "」已经有了"
        if (_types.value.size >= MAX_TYPES) return "最多 " + MAX_TYPES + " 个词条"
        save(_types.value + name)
        return null
    }

    /** 删除一个词条；返回 null 表示成功，否则返回给用户看的错误文案。 */
    fun remove(name: String): String? {
        if (_types.value.size <= 1) return "至少要保留一个词条"
        val next = _types.value - name
        if (next.size == _types.value.size) return null
        save(next)
        return null
    }

    /** 拖动排序：把 from 位置的词条移到 to 位置。 */
    fun move(from: Int, to: Int) {
        val list = _types.value.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        save(list)
    }

    /** 恢复默认的四个词条。 */
    fun reset() = save(DEFAULT_TYPES)

    private fun save(list: List<String>) {
        val json = JSONArray()
        list.forEach { json.put(it) }
        prefs.edit().putString(KEY_TYPES, json.toString()).apply()
        _types.value = list
    }

    private fun load(): List<String> {
        val raw = prefs.getString(KEY_TYPES, null) ?: return DEFAULT_TYPES
        val parsed = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).trim().takeIf { it.isNotEmpty() }
            }
        }.getOrNull()
        // 兼容早期用控制字符拼接的写法
        val fallback = raw.split('\u0001').map { it.trim() }.filter { it.isNotEmpty() }
        val list = (parsed ?: fallback).distinct()
        return list.ifEmpty { DEFAULT_TYPES }
    }

    companion object {
        const val MAX_NAME_LENGTH = 6
        const val MAX_TYPES = 20
        val DEFAULT_TYPES = listOf("自慰", "梦遗", "性行为", "其它")
        private const val KEY_TYPES = "check_in_types"
    }
}
