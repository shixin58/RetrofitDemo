package com.bride.kotlin.annotations.eg

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Proxy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

data class User(
    var login: String,
    var location: String,
    var bio: String
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Api(val url: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Path(val url: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Get(val url: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PathVariable(val name: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Query(val name: String = "")

@Api(url = "https://api.github.com")
interface GitHubApi {
    @Api("users")
    interface Users {
        @Get("{name}")
        fun get(@PathVariable("name") name: String): User

        @Get("{name}/followers")
        fun followers(name: String): List<User>
    }

    @Api("repos")
    interface Repos {
        @Get("{owner}/{repo}/forks")
        fun forks(owner: String, repo: String)
    }
}

object RetrofitApi {
    const val PATH_PATTERN = """(\{(\w+)\})"""

    val okHttp = OkHttpClient()
    val gson = Gson()

    val enclosing = {
        cls: Class<*> ->
        var currentCls: Class<*>? = cls
        sequence {
            while (currentCls != null) {
//                yield(currentCls)
//                currentCls = currentCls?.enclosingClass
                currentCls = currentCls?.also { yield(it) }?.enclosingClass
            }
        }
    }

    inline fun <reified T> create(): T {
        val functionMap = T::class.functions.associateBy { it.name }
        val interfaces = enclosing(T::class.java).takeWhile { it.isInterface }.toList()
        val apiPath = interfaces.foldRight(StringBuilder()) {
            clazz, acc ->
            acc.append(clazz.getAnnotation(Api::class.java)
                ?.url?.takeIf { it.isNotEmpty() }?:clazz.name)
                .append("/")
        }.toString()

        // 使用了内联特化
        return Proxy.newProxyInstance(RetrofitApi.javaClass.classLoader, arrayOf(T::class.java)) {
            proxy, method, args ->
            functionMap[method.name]?.takeIf { it.isAbstract }?.let {
                kFunction ->
                val parameterMap = kFunction.valueParameters.associate { parameter ->
                    val key = parameter.findAnnotation<PathVariable>()?.name?.takeIf { it.isNotEmpty() }?:parameter.name
                    key to args[parameter.index - 1]
                }
                val endPoint = kFunction.findAnnotation<Get>()!!.url.takeIf { it.isNotEmpty() }?:kFunction.name
                val compiledEndPoint = Regex(PATH_PATTERN).findAll(endPoint).map {
                    matchResult ->
                    matchResult.groups[1]!!.range to parameterMap[matchResult.groups[2]!!.value]
                }.fold(endPoint) {
                    acc, pair ->
                    acc.replaceRange(pair.first, pair.second.toString())
                }
                val url = apiPath + compiledEndPoint
                val request = Request.Builder().url(url).get().build()
                // 相比let，use可自动关闭
                okHttp.newCall(request).execute().body()?.charStream()?.use {
                    // 反序列化操作。genericReturnType方便拿到泛型实参信息
                    gson.fromJson(JsonReader(it), method.genericReturnType)
                }
            }
        } as T
    }
}