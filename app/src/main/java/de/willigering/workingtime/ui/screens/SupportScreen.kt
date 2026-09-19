package de.willigering.workingtime.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.willigering.workingtime.R
import de.willigering.workingtime.core.AppInfo
import de.willigering.workingtime.ui.components.GlassCard
import de.willigering.workingtime.ui.theme.AppColors

private data class CryptoOption(
    val id: String,
    val displayName: String,
    val symbol: String,
    val address: String,
    val networkNote: String? = null,
    val accent: Color,
)

/**
 * Professional "Support the Project" section — not a classic donation plea.
 * Embedded under Stats / About.
 */
@Composable
fun SupportProjectSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val options = remember {
        listOf(
            CryptoOption("BTC", "Bitcoin", "₿", AppInfo.DONATE_BTC, accent = AppColors.CoinBtc),
            CryptoOption("ETH", "Ethereum", "Ξ", AppInfo.DONATE_ETH, accent = AppColors.CoinEth),
            CryptoOption(
                "SOL",
                "Solana",
                "◎",
                AppInfo.DONATE_SOL,
                accent = AppColors.CoinSol,
            ),
            CryptoOption("XRP", "XRP", "✕", AppInfo.DONATE_XRP, accent = AppColors.CoinXrp),
            CryptoOption(
                "USDT",
                "USDT",
                "₮",
                AppInfo.DONATE_USDT_ERC20,
                networkNote = "ERC-20",
                accent = AppColors.CoinUsdt,
            ),
            CryptoOption(
                "LTC",
                "Litecoin",
                "Ł",
                AppInfo.DONATE_LTC,
                accent = AppColors.CoinLtc,
            ),
            CryptoOption(
                "XMR",
                "Monero",
                "ɱ",
                AppInfo.DONATE_XMR,
                accent = AppColors.CoinXmr,
            ),
        ).filter { it.address.isNotBlank() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            accentColor = AppColors.Accent,
            padding = 20.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(8.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(AppColors.Accent.copy(alpha = 0.18f))
                        .border(1.dp, AppColors.Accent.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.support_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.support_free_line),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Accent,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.support_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            stringResource(R.string.support_paypal_heading),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        PayPalCard(
            isPlaceholder = AppInfo.DONATE_PAYPAL_PLACEHOLDER ||
                AppInfo.DONATE_PAYPAL_URL.isBlank(),
            onClick = {
                if (AppInfo.DONATE_PAYPAL_PLACEHOLDER || AppInfo.DONATE_PAYPAL_URL.isBlank()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.support_paypal_placeholder),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    openUrl(context, AppInfo.DONATE_PAYPAL_URL)
                }
            },
        )
        Spacer(Modifier.height(18.dp))

        Text(
            stringResource(R.string.support_payment_methods),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.support_payment_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(12.dp))

        options.forEach { option ->
            CryptoSupportCard(
                option = option,
                modifier = Modifier.fillMaxWidth(),
                onCopy = { copyAddress(context, option.displayName, option.address) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = AppColors.AccentBlue) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.VolunteerActivism,
                    contentDescription = null,
                    tint = AppColors.AccentBlue,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.support_why_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.support_why_body),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = AppColors.Accent) {
            Text(
                stringResource(R.string.support_thanks),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PayPalCard(
    isPlaceholder: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "paypalScale")
    val buttonLabel = if (isPlaceholder) {
        stringResource(R.string.support_paypal_cta_soon)
    } else {
        stringResource(R.string.support_paypal_cta)
    }
    val subtitle = if (isPlaceholder) {
        stringResource(R.string.support_paypal_subtitle_placeholder)
    } else {
        stringResource(R.string.support_paypal_subtitle)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        accentColor = AppColors.PayPalBlue,
        padding = 20.dp,
    ) {
        Text(
            "PayPal",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.PayPalBlue.copy(alpha = if (isPlaceholder) 0.72f else 1f),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        ) {
            Text(
                buttonLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CryptoSupportCard(
    option: CryptoOption,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cryptoScale")

    GlassCard(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onCopy),
        accentColor = option.accent,
        padding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(option.accent.copy(alpha = 0.22f))
                    .border(1.dp, option.accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.symbol,
                    color = option.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        option.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (option.networkNote != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            option.networkNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    shortenAddress(option.address),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(option.accent.copy(alpha = 0.16f))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.donate_copy),
                    tint = option.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.donate_copy),
                    style = MaterialTheme.typography.labelMedium,
                    color = option.accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun shortenAddress(address: String): String {
    if (address.length <= 20) return address
    return "${address.take(10)}…${address.takeLast(6)}"
}

private fun copyAddress(context: Context, label: String, address: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, address))
    Toast.makeText(context, context.getString(R.string.donate_copied, label), Toast.LENGTH_SHORT).show()
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.support_open_failed), Toast.LENGTH_SHORT).show()
    }
}
