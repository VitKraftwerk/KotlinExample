package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    val res = mutableListOf<T>()
    for(i in this) {
        if (!predicate (i)) res.add(i)
        else break
    }
    return res
}