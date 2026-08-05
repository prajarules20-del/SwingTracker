package com.omprakash.swingtracker.domain

import android.content.Context
import org.json.JSONArray

data class StockSymbolInfo(val symbol: String, val name: String)

/**
 * Loads the bundled NSE symbol list (assets/nse_symbols.json) once and lets
 * you search it offline as the user types. This list is a curated set of
 * commonly traded NSE stocks - it won't cover every listed company, but
 * covers the ones people usually watch. Add more entries to the JSON file
 * if you need a specific stock that's missing.
 */
object StockSymbols {

    @Volatile private var cache: List<StockSymbolInfo>? = null

    /** All bundled symbols - exposed for the database validation check. */
    fun loadAll(context: Context): List<StockSymbolInfo> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val json = context.assets.open("nse_symbols.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<StockSymbolInfo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(StockSymbolInfo(obj.getString("symbol"), obj.getString("name")))
            }
            cache = list
            return list
        }
    }

    /**
     * Returns up to [limit] matches for [query], matching against either the
     * symbol or the company name (case-insensitive, "contains" match with a
     * preference for matches at the start of the symbol). Symbols in
     * [excluded] (found broken by a database check) are skipped.
     */
    fun search(context: Context, query: String, limit: Int = 5, excluded: Set<String> = emptySet()): List<StockSymbolInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val q = trimmed.uppercase()

        val all = loadAll(context).filter { it.symbol !in excluded }
        val startsWithSymbol = all.filter { it.symbol.startsWith(q) }
        val otherMatches = all.filter {
            !it.symbol.startsWith(q) &&
                (it.symbol.contains(q) || it.name.uppercase().contains(q))
        }
        return (startsWithSymbol + otherMatches).take(limit)
    }
}
