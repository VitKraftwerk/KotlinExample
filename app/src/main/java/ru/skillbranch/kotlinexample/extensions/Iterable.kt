package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    val res = mutableListOf<T>()
    for(i in this) {
        if (!predicate (i)) res.add(i)
        else break
    }
    return res
}

fun String.checkPhone(): String? {
    val checked = !this.isNullOrBlank()
            && this[0] == '+'
            && this?.replace("[^+\\d]".toRegex(), "").length == 12
            && this.replace("[\\W\\d]".toRegex(), "").isEmpty()

    if (!checked) throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

    return this?.replace("[^+\\d]".toRegex(), "")

}