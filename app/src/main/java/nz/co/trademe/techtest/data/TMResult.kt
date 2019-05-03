package nz.co.trademe.techtest.data

/**
 * Data class which represents a potentially nullable result
 * This class primarily exists to support RxJava's lack of support for null emissions
 */
data class TMResult<T>(
    val value: T?
)