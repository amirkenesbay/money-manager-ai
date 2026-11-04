package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.ExportFormat
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.export.ExportService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.HistoryAccessPolicy
import ai.moneymanager.web.time.UserCalendar
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

data class ExportRequest(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val format: ExportFormat,
)

data class ExportResponse(
    val count: Int,
)

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/export")
class ExportController(
    private val groupAccessGuard: GroupAccessGuard,
    private val historyAccessPolicy: HistoryAccessPolicy,
    private val exportService: ExportService,
    private val userCalendar: UserCalendar,
) {
    @PostMapping
    fun export(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: ExportRequest): ExportResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val from = request.from?.also { historyAccessPolicy.requireWithinHistory(user, it) } ?: historyAccessPolicy.earliestAllowedDate(user)
        val to = request.to ?: userCalendar.today(user)
        return ExportResponse(exportService.export(checkNotNull(user.telegramUserId), user.language, group, from, to, request.format))
    }
}
