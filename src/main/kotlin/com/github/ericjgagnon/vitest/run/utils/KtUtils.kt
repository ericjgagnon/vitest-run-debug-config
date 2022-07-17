package com.github.ericjgagnon.vitest.run.utils

inline fun <E: Any, T: Collection<E>> T?.withNotNullNorEmpty(func: T.() -> Unit): T? {
    if (this != null && this.isNotEmpty()) {
        with (this) { func() }
    }
    return this
}