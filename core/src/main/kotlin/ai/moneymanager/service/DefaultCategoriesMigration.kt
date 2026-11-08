package ai.moneymanager.service

import ai.moneymanager.domain.model.CURRENT_DEFAULTS_VERSION
import ai.moneymanager.domain.model.DefaultCategory
import ai.moneymanager.domain.model.ORIGINAL_DEFAULTS_VERSION
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.entity.CategoryEntity
import ai.moneymanager.repository.entity.MoneyGroupEntity
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class DefaultCategoriesMigration(
    private val groupRepository: MoneyGroupRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryService: CategoryService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun migrate() {
        val groups = groupRepository.findByDefaultCategoriesVersionNot(CURRENT_DEFAULTS_VERSION)
        val added = groups.sumOf { migrate(it) }
        if (groups.isNotEmpty()) log.info("Default categories brought to version {} in {} groups, {} categories added", CURRENT_DEFAULTS_VERSION, groups.size, added)
    }

    fun migrate(group: MoneyGroupEntity): Int {
        val groupId = checkNotNull(group.id)
        val existing = categoryRepository.findByGroupId(groupId)
            .sortedWith(compareBy<CategoryEntity> { it.position ?: Int.MAX_VALUE }.thenBy { it.auditInfo.createdAt })
            .map { it.copy(seedKey = it.seedKey ?: seedOf(it)?.name) }
        val language = languageOf(group)
        val known = existing.mapNotNull { DefaultCategory.of(it.seedKey) }.toSet()
        val missing = DefaultCategory.entries.filter { it.since > ORIGINAL_DEFAULTS_VERSION && it !in known }
            .map { categoryService.defaultCategoryEntity(it, groupId, language, 0) }
            .filter { new -> existing.none { it.type == new.type && it.name.equals(new.name, ignoreCase = true) } }
        val ordered = missing.fold(existing) { list, new -> insertBeforeOther(list, new) }
        categoryRepository.saveAll(ordered.mapIndexed { position, category -> category.copy(position = position) })
        groupRepository.save(group.copy(defaultCategoriesVersion = CURRENT_DEFAULTS_VERSION))
        return missing.size
    }

    private fun seedOf(category: CategoryEntity): DefaultCategory? =
        DefaultCategory.entries.firstOrNull { seed ->
            seed.type == category.type && LocalizationService.SUPPORTED_LANGUAGES.any { localizationService.t(seed.nameKey, it).equals(category.name, ignoreCase = true) }
        }

    private fun insertBeforeOther(list: List<CategoryEntity>, new: CategoryEntity): List<CategoryEntity> {
        val other = list.indexOfFirst { it.type == new.type && DefaultCategory.of(it.seedKey)?.isOther == true }
        return if (other < 0) list + new else list.subList(0, other) + new + list.subList(other, list.size)
    }

    private fun languageOf(group: MoneyGroupEntity): String {
        val owner = userInfoService.getUserInfoByTelegramId(group.ownerId)
        return localizationService.resolveLanguage(owner?.language, owner?.languageCode)
    }
}
