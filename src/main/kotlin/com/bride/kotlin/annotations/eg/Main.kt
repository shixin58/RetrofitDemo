package com.bride.kotlin.annotations.eg

fun main(args: Array<String>) {
    log("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    log("Program arguments: ${args.joinToString()}")

    val usersApi = RetrofitApi.create<GitHubApi.Users>()
    log(usersApi.get("bennyhuo"))
    log(usersApi.followers("bennyhuo").map { it.login })
}