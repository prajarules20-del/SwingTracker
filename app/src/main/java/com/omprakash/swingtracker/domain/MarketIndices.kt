package com.omprakash.swingtracker.domain

/** A market index tracked on the Home tab. Yahoo symbols for indices start with ^. */
data class MarketIndexInfo(val symbol: String, val displayName: String)

object MarketIndices {
    val all = listOf(
        MarketIndexInfo("^NSEI", "Nifty 50"),
        MarketIndexInfo("^BSESN", "Sensex"),
        MarketIndexInfo("^NSEBANK", "Bank Nifty"),
        MarketIndexInfo("^CNXIT", "Nifty IT")
    )
}
