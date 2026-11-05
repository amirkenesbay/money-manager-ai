package ai.moneymanager.web.ask

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.AskSuggestions
import ai.moneymanager.service.CategoryRuleService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.ai.AiRequestGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.badRequest
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val MAX_QUESTION_LENGTH = 300

private const val INVALID_QUESTION_KEY = "${API_ERROR_KEY_PREFIX}invalid_question"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/ask")
class AskController(
    private val groupAccessGuard: GroupAccessGuard,
    private val categoryService: CategoryService,
    private val categoryRuleService: CategoryRuleService,
    private val commandParserService: CommandParserService,
    private val aiRequestGuard: AiRequestGuard,
    private val askAnswers: AskAnswers,
) {
    @PostMapping
    fun ask(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: AskRequest): AnswerCard {
        val group = groupAccessGuard.requireMember(user, groupId)
        val text = request.text?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_QUESTION_LENGTH } ?: throw badRequest(INVALID_QUESTION_KEY, MAX_QUESTION_LENGTH)
        aiRequestGuard.requireQuota(user)
        val context = aiRequestGuard.groupContext(user, group, categoryService.getCategoriesByGroup(groupId), categoryRuleService.rules(groupId))
        val commands = commandParserService.parseCommands(text, context)
        aiRequestGuard.requireParsed(commands)
        return askAnswers.answerOf(user, group, commands) ?: askAnswers.notUnderstood(user, group)
    }

    @GetMapping("/suggestions")
    fun suggestions(user: UserInfo, @PathVariable groupId: ObjectId): AskSuggestions =
        askAnswers.suggestions(user, groupAccessGuard.requireMember(user, groupId))
}
