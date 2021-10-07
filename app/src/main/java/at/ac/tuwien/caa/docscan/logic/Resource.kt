package at.ac.tuwien.caa.docscan.logic

sealed class Resource<T>
data class Success<T>(val data: T) : Resource<T>()
data class Failure<T>(val exception: Throwable) : Resource<T>()
