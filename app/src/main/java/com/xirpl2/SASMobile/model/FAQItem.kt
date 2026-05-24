package com.xirpl2.SASMobile.model

data class FAQItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)
