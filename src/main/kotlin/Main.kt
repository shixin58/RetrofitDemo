import com.bride.kotlin.annotations.eg.GitHubApi
import com.bride.kotlin.annotations.eg.RetrofitApi

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val usersApi = RetrofitApi.create<GitHubApi.Users>()
    println(usersApi.get("bennyhuo"))
    println(usersApi.followers("bennyhuo").map { it.login })
}