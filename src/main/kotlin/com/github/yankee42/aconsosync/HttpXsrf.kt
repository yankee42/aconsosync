import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.cookies.cookies
import io.ktor.client.features.cookies.get
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.http.clone
import io.ktor.util.AttributeKey

class HttpXsrf(private val cookieName: String, private val headerName: String) {
    class Config {
        var cookieName = "XSRF-TOKEN"
        var headerName = "X-XSRF-TOKEN"

        internal fun build() = HttpXsrf(cookieName, headerName)
    }

    companion object : HttpClientFeature<Config, HttpXsrf> {
        override val key: AttributeKey<HttpXsrf> = AttributeKey("HttpXsrf")

        override fun prepare(block: Config.() -> Unit): HttpXsrf = Config().apply(block).build()

        override fun install(feature: HttpXsrf, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                if (context.method == HttpMethod.Post) {
                    scope.cookies(context.url.clone().build())[feature.cookieName]?.let {
                        context.header(feature.headerName, it.value)
                    }
                }
            }
        }
    }
}
