package com.porarrirr.sumahohikakuku.ui.common

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    data class StringResource(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : UiText
}

fun UiText.resolve(context: Context): String {
    return when (this) {
        is UiText.Dynamic -> value
        is UiText.StringResource -> context.getString(resId, *formatArgs.toTypedArray())
    }
}
