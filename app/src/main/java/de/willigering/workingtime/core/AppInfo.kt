package de.willigering.workingtime.core

/**
 * App metadata and support / donation endpoints.
 * Crypto addresses with a blank [address] are hidden in the UI.
 * PayPal card is always shown; set [DONATE_PAYPAL_PLACEHOLDER] to false when the
 * real [DONATE_PAYPAL_URL] is ready.
 */
object AppInfo {
    const val VERSION = "2.5.9"
    const val BUILD_NUMBER = "34"
    const val DEVELOPER = "Willi Gering"

    // Crypto wallets (Support the Project)
    const val DONATE_BTC = "bc1qnl5jmr7tscrkwp8xx4qnk6r0pljw8a7g4pl23u"
    const val DONATE_ETH = "0x3BaB47BfbA01a88eE709fa5810Be9025C86D42C0"
    const val DONATE_SOL = "3wDAo4X1Fod2Jn7GwHwiJpmabab5EiKbvzVwRZ7cXH8o"
    const val DONATE_XRP = "rqjsHQbPccjRvgLYeKhZPBeaTceiKQC4y"
    /** Leave blank until a Litecoin address is available. */
    const val DONATE_LTC = ""
    /** Leave blank until a Monero address is available. */
    const val DONATE_XMR = ""
    /** USDT on Ethereum (ERC-20) — same wallet as ETH. */
    const val DONATE_USDT_ERC20 = DONATE_ETH
    /** Leave blank until a TRC-20 (Tron) USDT address is available. */
    const val DONATE_USDT_TRC20 = ""

    /**
     * Real PayPal.me / donate link when ready, e.g. https://paypal.me/yourname
     * While [DONATE_PAYPAL_PLACEHOLDER] is true the UI shows the card but does not open a link.
     */
    const val DONATE_PAYPAL_URL = "https://paypal.me/willigeringDE"
    const val DONATE_PAYPAL_PLACEHOLDER = false
}