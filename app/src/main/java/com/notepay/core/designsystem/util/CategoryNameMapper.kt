package com.notepay.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.notepay.R
import com.notepay.domain.model.Category

@StringRes
fun categoryNameRes(id: String): Int? = when (id) {
    "FOOD" -> R.string.category_food
    "TRANSPORT" -> R.string.category_transport
    "SHOPPING" -> R.string.category_shopping
    "BILL" -> R.string.category_bill
    "ENTERTAINMENT" -> R.string.category_entertainment
    "HEALTH" -> R.string.category_health
    "EDUCATION" -> R.string.category_education
    "COFFEE" -> R.string.category_coffee
    "BEAUTY" -> R.string.category_beauty
    "PETS" -> R.string.category_pets
    "SPORTS" -> R.string.category_sports
    "FAMILY" -> R.string.category_family
    "TRAVEL" -> R.string.category_travel
    "CLOTHES" -> R.string.category_clothes
    "HOME" -> R.string.category_home
    "GAS" -> R.string.category_gas
    "REPAIR" -> R.string.category_repair
    "ELECTRICITY" -> R.string.category_electricity
    "WATER" -> R.string.category_water
    "INTERNET" -> R.string.category_internet
    "CHILDREN" -> R.string.category_children
    "CHARITY" -> R.string.category_charity
    "SAVINGS" -> R.string.category_savings
    "DEBT_LOAN" -> R.string.category_debt_loan
    "INSURANCE" -> R.string.category_insurance
    "TAX" -> R.string.category_tax
    "OTHER" -> R.string.category_other
    "SALARY" -> R.string.category_salary
    "GIFT" -> R.string.category_gift
    "INVESTMENT" -> R.string.category_investment
    "BONUS" -> R.string.category_bonus
    "INCOME_OTHER" -> R.string.category_income_other
    else -> null // danh mục người dùng tự tạo
}

/** Dùng trong Composable. */
@Composable
fun Category.localizedName(): String =
    categoryNameRes(id)?.let { stringResource(it) } ?: displayName

/** Dùng ở ViewModel, mapper, backup... */
fun Category.localizedName(context: Context): String =
    categoryNameRes(id)?.let(context::getString) ?: displayName
