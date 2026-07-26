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
    @StringRes val labelRes: Int,
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
    // --- 1. ĂN UỐNG & GIẢI TRÍ ---
    Category.FOOD.id -> Icons.Rounded.Restaurant
    Category.COFFEE.id -> Icons.Rounded.Coffee
    Category.ENTERTAINMENT.id -> Icons.Rounded.Movie
    Category.SPORTS.id -> Icons.Rounded.FitnessCenter

    // --- 2. MUA SẮM & LÀM ĐẸP ---
    Category.SHOPPING.id -> Icons.Rounded.ShoppingCart
    Category.CLOTHES.id -> Icons.Rounded.Checkroom       // Quần áo, thời trang
    Category.BEAUTY.id -> Icons.Rounded.Spa

    // --- 3. DI CHUYỂN & XE CỘ ---
    Category.TRANSPORT.id -> Icons.Rounded.DirectionsBus // Xe công cộng, taxi
    Category.GAS.id -> Icons.Rounded.LocalGasStation     // Xăng xe, dầu nhớt
    Category.REPAIR.id -> Icons.Rounded.Build            // Sửa chữa, bảo dưỡng xe/đồ đạc

    // --- 4. NHÀ CỬA & HÓA ĐƠN TIỆN ÍCH ---
    Category.HOME.id -> Icons.Rounded.Home               // Tiền thuê nhà, sửa nhà
    Category.BILL.id -> Icons.Rounded.Payments           // Hóa đơn chung
    Category.ELECTRICITY.id -> Icons.Rounded.Lightbulb   // Tiền điện
    Category.WATER.id -> Icons.Rounded.WaterDrop         // Tiền nước
    Category.INTERNET.id -> Icons.Rounded.Wifi           // Internet, truyền hình, 4G

    // --- 5. Y TẾ & GIÁO DỤC ---
    Category.HEALTH.id -> Icons.Rounded.LocalHospital
    Category.EDUCATION.id -> Icons.Rounded.School

    // --- 6. GIA ĐÌNH, CON CÁI & THÚ CƯNG ---
    Category.FAMILY.id -> Icons.Rounded.People
    Category.CHILDREN.id -> Icons.Rounded.ChildCare       // Bỉm, sữa, đồ chơi cho con
    Category.PETS.id -> Icons.Rounded.Pets

    // --- 7. ĐỜI SỐNG & MỐI QUAN HỆ ---
    Category.TRAVEL.id -> Icons.Rounded.Flight
    Category.GIFT.id -> Icons.Rounded.Favorite           // Quà biếu, đám đình, hiếu hỷ
    Category.CHARITY.id -> Icons.Rounded.VolunteerActivism // Từ thiện, quyên góp

    // --- 8. THU NHẬP, ĐẦU TƯ & TÍCH LŨY ---
    Category.SALARY.id -> Icons.Rounded.AttachMoney      // Lương cố định
    Category.BONUS.id -> Icons.Rounded.Redeem            // Thưởng, phụ cấp, quà tặng tiền mặt
    Category.INVESTMENT.id -> Icons.AutoMirrored.Rounded.TrendingUp // Chứng khoán, coin, bất động sản
    Category.SAVINGS.id -> Icons.Rounded.Savings         // Tiền gửi tiết kiệm, heo đất
    Category.DEBT_LOAN.id -> Icons.Rounded.AccountBalance // Trả nợ, vay mượn, ngân hàng
    Category.INSURANCE.id -> Icons.Rounded.Shield        // Bảo hiểm (nhân thọ, y tế, xe)
    Category.TAX.id -> Icons.AutoMirrored.Rounded.ReceiptLong         // Thuế, phí cầu đường, phạt hành chính

    // --- CÁC KHOẢN KHÁC ---
        else -> Icons.Rounded.LocalMall
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
