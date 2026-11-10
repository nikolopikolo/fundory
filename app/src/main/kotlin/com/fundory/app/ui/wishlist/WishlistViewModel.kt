package com.fundory.app.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.data.repository.GoalRepository
import com.fundory.app.data.repository.ProductRepository
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

enum class WishlistSort {
    DATE_DESC,
    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC
}

data class WishlistItemUi(
    val product: Product,
    val convertedPriceGel: BigDecimal,
    val formattedPrice: String,
    val formattedGelPrice: String
)

data class WishlistUiState(
    val items: List<WishlistItemUi> = emptyList(),
    val availableGoals: List<Goal> = emptyList(),
    val selectedMarketplaceFilter: String? = null,
    val availableMarketplaces: List<String> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: WishlistSort = WishlistSort.DATE_DESC,
    val totalWishlistGel: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false
)

private data class WishlistFilterParams(
    val query: String,
    val marketplace: String?,
    val sort: WishlistSort
)

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val goalRepository: GoalRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedMarketplace = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(WishlistSort.DATE_DESC)

    private val _filters = combine(
        _searchQuery,
        _selectedMarketplace,
        _sortOrder
    ) { query, marketplace, sort ->
        WishlistFilterParams(query, marketplace, sort)
    }

    val uiState: StateFlow<WishlistUiState> = combine(
        productRepository.getWishlistProductsFlow(),
        goalRepository.activeGoalsWithProductsFlow,
        currencyRepository.ratesToGelFlow,
        _filters
    ) { products, goals, ratesToGel, filters ->
        val activeGoals = goals.filter { it.status == GoalStatus.ACTIVE }
        val marketplaces = products.mapNotNull { it.marketplace }.distinct().sorted()

        var filtered = products
        if (filters.query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(filters.query, ignoreCase = true) ||
                it.description.contains(filters.query, ignoreCase = true) ||
                (it.marketplace?.contains(filters.query, ignoreCase = true) == true)
            }
        }

        if (filters.marketplace != null) {
            filtered = filtered.filter { it.marketplace == filters.marketplace }
        }

        val itemsUi = filtered.map { prod ->
            val gel = prod.calculateTotalGel(ratesToGel)
            WishlistItemUi(
                product = prod,
                convertedPriceGel = gel,
                formattedPrice = Currencies.format(prod.originalPrice, prod.currency),
                formattedGelPrice = Currencies.format(gel, "GEL")
            )
        }

        val sorted = when (filters.sort) {
            WishlistSort.DATE_DESC -> itemsUi.sortedByDescending { it.product.dateAddedEpochMillis }
            WishlistSort.PRICE_ASC -> itemsUi.sortedBy { it.convertedPriceGel }
            WishlistSort.PRICE_DESC -> itemsUi.sortedByDescending { it.convertedPriceGel }
            WishlistSort.NAME_ASC -> itemsUi.sortedBy { it.product.name.lowercase() }
        }

        val totalGel = itemsUi.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.convertedPriceGel) }

        WishlistUiState(
            items = sorted,
            availableGoals = activeGoals,
            selectedMarketplaceFilter = filters.marketplace,
            availableMarketplaces = marketplaces,
            searchQuery = filters.query,
            sortOrder = filters.sort,
            totalWishlistGel = totalGel,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WishlistUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onMarketplaceFilterSelected(marketplace: String?) {
        _selectedMarketplace.value = marketplace
    }

    fun onSortOrderChanged(sort: WishlistSort) {
        _sortOrder.value = sort
    }

    fun moveProductToGoal(productId: String, targetGoalId: String) {
        viewModelScope.launch {
            productRepository.moveProductToGoal(productId, targetGoalId)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }
}
