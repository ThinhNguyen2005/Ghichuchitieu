package com.notepay.ui.component

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
import androidx.compose.ui.graphics.vector.ImageVector
import com.notepay.domain.model.Category

/**
 * Mapping Category -> Material Icons (Round).
 * Domain layer không phụ thuộc Compose, nên mapping ở UI layer.
 */
fun categoryIcon(category: Category): ImageVector = when (category.id) {
    Category.FOOD.id -> Icons.Rounded.Restaurant
    Category.TRANSPORT.id -> Icons.Rounded.DirectionsBus
    Category.SHOPPING.id -> Icons.Rounded.ShoppingCart
    Category.BILL.id -> Icons.Rounded.Payments
    Category.ENTERTAINMENT.id -> Icons.Rounded.Movie
    Category.HEALTH.id -> Icons.Rounded.LocalHospital
    Category.EDUCATION.id -> Icons.Rounded.School
    Category.SALARY.id -> Icons.Rounded.AttachMoney
    Category.GIFT.id -> Icons.Rounded.Favorite
    else -> Icons.Rounded.LocalMall
}
