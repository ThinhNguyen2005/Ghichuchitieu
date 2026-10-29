package com.notepay.ui.component

import com.notepay.ui.theme.AppTheme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category

/** Icon có thể chọn khi tạo danh mục tuỳ chỉnh. ID được lưu cùng danh mục, không phụ thuộc tên. */
data class CategoryIconOption(
    val id: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

val customCategoryIconOptions = listOf(
    CategoryIconOption("food", R.string.icon_label_food, Icons.Rounded.Restaurant),
    CategoryIconOption("coffee", R.string.icon_label_coffee, Icons.Rounded.Coffee),
    CategoryIconOption("shopping", R.string.icon_label_shopping, Icons.Rounded.ShoppingCart),
    CategoryIconOption("transport", R.string.icon_label_transport, Icons.Rounded.DirectionsBus),
    CategoryIconOption("gas", R.string.icon_label_gas, Icons.Rounded.LocalGasStation),
    CategoryIconOption("home", R.string.icon_label_home, Icons.Rounded.Home),
    CategoryIconOption("bill", R.string.icon_label_bill, Icons.Rounded.Payments),
    CategoryIconOption("entertainment", R.string.icon_label_entertainment, Icons.Rounded.Movie),
    CategoryIconOption("health", R.string.icon_label_health, Icons.Rounded.LocalHospital),
    CategoryIconOption("education", R.string.icon_label_education, Icons.Rounded.School),
    CategoryIconOption("pet", R.string.icon_label_pet, Icons.Rounded.Pets),
    CategoryIconOption("travel", R.string.icon_label_travel, Icons.Rounded.Flight),
    CategoryIconOption("family", R.string.icon_label_family, Icons.Rounded.People),
    CategoryIconOption("savings", R.string.icon_label_savings, Icons.Rounded.Savings),
    CategoryIconOption("investment", R.string.icon_label_investment, Icons.AutoMirrored.Rounded.TrendingUp),
    CategoryIconOption("income", R.string.icon_label_income, Icons.Rounded.AttachMoney),
    CategoryIconOption("gift", R.string.icon_label_gift, Icons.Rounded.Favorite),
    CategoryIconOption("insurance", R.string.icon_label_insurance, Icons.Rounded.Shield),
    CategoryIconOption("other", R.string.icon_label_other, Icons.Rounded.LocalMall),
)

/**
 * Mapping Category -> Material Icons (Round).
 * Domain layer không phụ thuộc Compose, nên mapping ở UI layer.
 */
fun categoryIcon(category: Category): ImageVector {
    if (category.isCustom) {
        return customCategoryIconOptions
            .firstOrNull { it.id == category.iconId }
            ?.icon
            ?: Icons.Rounded.LocalMall
    }

    return when (category.id) {
        "FOOD" -> Icons.Rounded.Restaurant
        "TRANSPORT" -> Icons.Rounded.DirectionsBus
        "SHOPPING" -> Icons.Rounded.ShoppingBag
        "BILL" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "ENTERTAINMENT" -> Icons.Rounded.Movie
        "HEALTH" -> Icons.Rounded.MonitorHeart
        "EDUCATION" -> Icons.Rounded.School
        "COFFEE" -> Icons.Rounded.LocalCafe
        "BEAUTY" -> Icons.Rounded.Face
        "PETS" -> Icons.Rounded.Pets
        "SPORTS" -> Icons.Rounded.FitnessCenter
        "FAMILY" -> Icons.Rounded.FamilyRestroom
        "TRAVEL" -> Icons.Rounded.Flight
        "CLOTHES" -> Icons.Rounded.Checkroom
        "HOME" -> Icons.Rounded.Home
        "GAS" -> Icons.Rounded.LocalGasStation
        "REPAIR" -> Icons.Rounded.Build
        "ELECTRICITY" -> Icons.Rounded.ElectricBolt
        "WATER" -> Icons.Rounded.WaterDrop
        "INTERNET" -> Icons.Rounded.Wifi
        "CHILDREN" -> Icons.Rounded.ChildCare
        "CHARITY" -> Icons.Rounded.VolunteerActivism
        "SAVINGS" -> Icons.Rounded.Savings
        "DEBT_LOAN" -> Icons.Rounded.Handshake
        "INSURANCE" -> Icons.Rounded.Shield
        "TAX" -> Icons.Rounded.AccountBalance
        "OTHER", "INCOME_OTHER" -> Icons.Rounded.MoreHoriz
        "SALARY" -> Icons.Rounded.Payments
        "GIFT" -> Icons.Rounded.CardGiftcard
        "INVESTMENT" -> Icons.AutoMirrored.Rounded.TrendingUp
        "BONUS" -> Icons.Rounded.EmojiEvents
        else -> Icons.Rounded.Category
    }
}


private fun Color.preferredContentColor(): Color {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return if (luminance > 0.52f) Color(0xFF1B1B1F) else Color.White
}
@Composable
fun CategoryAvatar(
    category: Category,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(AppTheme.shapes.circle)
            .background(Color(category.colorArgb)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = Color(category.colorArgb).preferredContentColor(),
            modifier = Modifier.size(iconSize),
        )
    }
}
