package ai.moneymanager.web.security

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver(
    private val userInfoService: UserInfoService,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == UserInfo::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): UserInfo {
        val principal = webRequest.getAttribute(TelegramPrincipal.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
            as? TelegramPrincipal
            ?: throw ApiException(ApiErrorCode.UNAUTHORIZED)
        val user = userInfoService.getOrCreate(principal.toTelegramProfile())
        webRequest.setAttribute(CURRENT_USER_ATTRIBUTE, user, RequestAttributes.SCOPE_REQUEST)
        return user
    }

    companion object {
        const val CURRENT_USER_ATTRIBUTE = "currentUser"
    }
}
