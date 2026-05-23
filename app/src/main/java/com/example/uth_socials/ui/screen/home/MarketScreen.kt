package com.example.uth_socials.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.uth_socials.ui.screen.market.CategoryChips
import com.example.uth_socials.ui.screen.market.ProductItem
import com.example.uth_socials.ui.screen.market.SearchBar
import com.example.uth_socials.ui.viewmodel.MarketViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MarketScreen(
    navController: NavHostController,
    viewModel: MarketViewModel = viewModel(),
    onProductClick: (String) -> Unit,
) {
    val listUiState by viewModel.listUiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val showTitle by rememberCollapsingHeader(gridState)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MarketTitleHeader()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SearchBar(
                    query = listUiState.searchQuery,
                    hint = "Tìm sách, đồ điện tử…",
                    onQueryChange = viewModel::updateSearchQuery,
                    onClear = viewModel::clearSearch
                )
            }

            CategoryChips(
                types = listUiState.availableTypes,
                selected = listUiState.selectedType,
                onSelect = viewModel::selectType,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            ResultInfoRow(
                totalCount = listUiState.products.size,
                showFilter = listUiState.searchQuery.isNotEmpty() || listUiState.selectedType.isNotEmpty(),
                resultCount = listUiState.filteredProducts.size,
                query = listUiState.searchQuery,
                type = listUiState.selectedType
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    listUiState.isLoading -> LoadingState()
                    listUiState.error != null -> ErrorState(message = listUiState.error!!)
                    listUiState.filteredProducts.isEmpty() -> EmptyState(
                        hasQuery = listUiState.searchQuery.isNotEmpty() ||
                                listUiState.selectedType.isNotEmpty()
                    )
                    else -> ProductGrid(
                        state = gridState,
                        products = listUiState.filteredProducts,
                        onProductClick = onProductClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketTitleHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Chợ UTH",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Mua thì hời, bán thì lời",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rememberCollapsingHeader(gridState: LazyGridState) = produceState(
    initialValue = true,
    key1 = gridState
) {
    var prevIndex = 0
    var prevOffset = 0
    kotlinx.coroutines.flow.combine(
        kotlinx.coroutines.flow.flowOf(Unit),
        androidx.compose.runtime.snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.distinctUntilChanged()
    ) { _, pair -> pair }
        .collect { (idx, off) ->
            value = when {
                idx == 0 && off < 80 -> true
                idx < prevIndex -> true
                idx == prevIndex && off < prevOffset - 8 -> true
                idx > prevIndex -> false
                idx == prevIndex && off > prevOffset + 8 -> false
                else -> value
            }
            prevIndex = idx
            prevOffset = off
        }
}

@Composable
private fun ResultInfoRow(
    totalCount: Int,
    showFilter: Boolean,
    resultCount: Int,
    query: String,
    type: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (showFilter) "Tìm thấy $resultCount sản phẩm"
            else "$totalCount sản phẩm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showFilter) {
            val tag = when {
                type.isNotBlank() && query.isNotBlank() -> "$type · \"$query\""
                type.isNotBlank() -> type
                query.toDoubleOrNull() != null -> "Theo giá"
                else -> "Theo tên"
            }
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProductGrid(
    state: LazyGridState,
    products: List<com.example.uth_socials.data.market.Product>,
    onProductClick: (String) -> Unit
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = products,
            key = { it.id.ifBlank { it.hashCode().toString() } }
        ) { product ->
            ProductItem(
                product = product,
                onClick = {
                    val id = product.id
                    if (id.isNotBlank()) onProductClick(id)
                }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Không tải được dữ liệu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasQuery) Icons.Outlined.SearchOff else Icons.Outlined.Storefront,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasQuery) "Không tìm thấy sản phẩm" else "Chưa có sản phẩm nào",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasQuery) "Thử từ khoá khác hoặc bỏ bộ lọc"
            else "Hãy là người đầu tiên đăng bán nhé!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
