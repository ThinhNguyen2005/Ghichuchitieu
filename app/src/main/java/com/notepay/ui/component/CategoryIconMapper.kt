package com.notepay.ui.component

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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    Category.COFFEE.id -> Icons.Rounded.Coffee
    Category.BEAUTY.id -> Icons.Rounded.Spa
    Category.PETS.id -> Icons.Rounded.Pets
    Category.SPORTS.id -> Icons.Rounded.FitnessCenter
    Category.INVESTMENT.id -> Icons.AutoMirrored.Rounded.TrendingUp
    Category.FAMILY.id -> Icons.Rounded.People
    Category.TRAVEL.id -> Icons.Rounded.Flight
    else -> Icons.Rounded.LocalMall
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
            .clip(CircleShape)
            .background(Color(category.colorArgb)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}
