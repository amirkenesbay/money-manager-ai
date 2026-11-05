package ai.moneymanager.testing

import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.LoanRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.service.FinanceOperationService
import org.mockito.Mockito.mock

fun testFinanceOperationService(
    repository: FinanceOperationRepository,
    groupRepository: MoneyGroupRepository = mock(MoneyGroupRepository::class.java),
    loanRepository: LoanRepository = mock(LoanRepository::class.java),
    onEvent: (Any) -> Unit = {},
): FinanceOperationService = FinanceOperationService(repository, groupRepository, loanRepository) { onEvent(it) }
