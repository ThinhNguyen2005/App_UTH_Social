package com.example.uth_socials.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import coil.compose.AsyncImage
import com.example.uth_socials.config.AdminConfig
import com.example.uth_socials.data.post.AdminAction
import com.example.uth_socials.data.post.AdminReport
import com.example.uth_socials.data.post.User
import com.example.uth_socials.data.user.AdminUser
import com.example.uth_socials.ui.viewmodel.AdminDashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.uth_socials.ui.viewmodel.CategoryModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToUser: (String) -> Unit = {},
    backStackEntry: NavBackStackEntry? = null
) {
    val viewModel: AdminDashboardViewModel = viewModel()
    val viewModelCategory: CategoryModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Parse initial tab from navigation argument or route
    val initialTab = remember(backStackEntry) {
        val tabArg = backStackEntry?.arguments?.getString("tab")
        val route = backStackEntry?.destination?.route

        when {
            tabArg?.lowercase() == "categories" -> AdminTab.CATEGORIES
            tabArg?.lowercase() == "users" -> AdminTab.USERS
            tabArg?.lowercase() == "admins" -> AdminTab.ADMINS
            route == "categories" -> AdminTab.CATEGORIES // Direct categories route
            else -> AdminTab.REPORTS
        }
    }

    var selectedTab by remember { mutableStateOf(initialTab) }
    var showActionDialog by remember { mutableStateOf<AdminReport?>(null) }
    var showGrantAdminDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf<com.example.uth_socials.data.post.Category?>(null) }
    // 🔸 Ban dialog state
    var showBanDialog by remember { mutableStateOf<User?>(null) }
    // 🔸 Post detail modal state
    var showPostDetail by remember { mutableStateOf<AdminReport?>(null) }
    // 🔸 Delete category dialog state
    var showDeleteCategoryDialog by remember { mutableStateOf<com.example.uth_socials.data.post.Category?>(null) }

    // Check if current user is super admin
    var isSuperAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        isSuperAdmin = currentUserId?.let { AdminConfig.isSuperAdmin(it) } ?: false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Chỉ hiển thị FAB khi đang ở tab Categories
            if (selectedTab == AdminTab.CATEGORIES) {
                FloatingActionButton(
                    onClick = { showAddCategoryDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Tab selector - filter tabs based on permissions
            val availableTabs = if (isSuperAdmin) {
                AdminTab.entries
            } else {
                AdminTab.entries.filter { it != AdminTab.ADMINS }
            }

            val currentTabIndex = availableTabs.indexOf(selectedTab)

            TabRow(selectedTabIndex = currentTabIndex) {
                availableTabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            // If user is not super admin and somehow on ADMINS tab, switch to REPORTS
            LaunchedEffect(isSuperAdmin, selectedTab) {
                if (!isSuperAdmin && selectedTab == AdminTab.ADMINS) {
                    selectedTab = AdminTab.REPORTS
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                AdminTab.REPORTS -> ReportsTab(
                    reports = uiState.pendingReports,
                    isLoading = uiState.isLoadingReports,
                    onReportClick = { showPostDetail = it },
                    onNavigateToUser = onNavigateToUser
                )
                AdminTab.USERS -> UsersTab(
                    bannedUsers = uiState.bannedUsers,
                    isLoading = uiState.isLoadingUsers,
                    onBanUser = { user ->
                        showBanDialog = user
                    },
                    onUnbanUser = { userId ->
                        scope.launch {
                            viewModel.unbanUser(userId)
                        }
                    }
                )
                AdminTab.ADMINS -> AdminsTab(
                    admins = uiState.admins,
                    isLoading = uiState.isLoadingAdmins,
                    isSuperAdmin = isSuperAdmin,
                    onGrantAdmin = { showGrantAdminDialog = true },
                    onRevokeAdmin = { adminUser ->
                        scope.launch {
                            viewModel.revokeAdminRole(adminUser.userId)
                        }
                    }
                )
                AdminTab.CATEGORIES -> CategoriesTab(
                    categories = uiState.categories,
                    isLoading = uiState.isLoadingCategories,
                    onEditCategory = { category -> showEditCategoryDialog = category },
                    onDeleteCategory = { category ->
                        showDeleteCategoryDialog = category
                    }
                )
            }
        }
    }

    // Report Action Dialog
    showActionDialog?.let { adminReport ->
        ReportActionDialog(
            adminReport = adminReport,
            onDismiss = { showActionDialog = null },
            onAction = { action, notes ->
                scope.launch {
                    viewModel.reviewReport(adminReport.report.id, action, notes)
                    showActionDialog = null
                }
            }
        )
    }

    // Grant Admin Dialog
    if (showGrantAdminDialog) {
        GrantAdminDialog(
            onDismiss = { showGrantAdminDialog = false },
            onGrant = { userId, role ->
                scope.launch {
                    viewModel.grantAdminRole(userId, role)
                    showGrantAdminDialog = false
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { categoryName ->
                scope.launch {
                    viewModelCategory.addCategory(categoryName)
                    showAddCategoryDialog = false
                }
            }
        )
    }

    // Edit Category Dialog
    showEditCategoryDialog?.let { category ->
        EditCategoryDialog(
            category = category,
            onDismiss = { showEditCategoryDialog = null },
            onEdit = { categoryId, newName ->
                scope.launch {
                    viewModelCategory.updateCategory(categoryId, newName)
                    showEditCategoryDialog = null
                }
            }
        )
    }

    // Delete Category Dialog
    showDeleteCategoryDialog?.let { category ->
        DeleteCategoryDialog(
            category = category,
            allCategories = uiState.categories,
            onDismiss = { showDeleteCategoryDialog = null },
            onDelete = { migrateToCategoryId ->
                scope.launch {
                    viewModelCategory.deleteCategoryWithConfirmation(category.id, migrateToCategoryId)
                    showDeleteCategoryDialog = null
                }
            }
        )
    }

    // Ban User Dialog
    showBanDialog?.let { user ->
        BanUserDialog(
            user = user,
            onDismiss = { showBanDialog = null },
            onBan = { reason ->
                scope.launch {
                    viewModel.banUser(user.id, reason)
                    showBanDialog = null
                }
            }
        )
    }

    // Post Detail Modal
    showPostDetail?.let { adminReport ->
        PostDetailModal(
            adminReport = adminReport,
            onDismiss = { showPostDetail = null },
            onAction = { action, notes ->
                scope.launch {
                    viewModel.reviewReport(adminReport.report.id, action, notes)
                    showPostDetail = null
                    // Refresh reports after action
                    delay(500)
                }
            },
            onNavigateToUser = { userId ->
                onNavigateToUser(userId)
                showPostDetail = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrantAdminDialog(
    onDismiss: () -> Unit,
    onGrant: (String, String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("admin") }
    var showError by remember { mutableStateOf<String?>(null) }

    val isValid = userId.isNotBlank() && selectedRole.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grant Admin Role") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = {
                        userId = it
                        showError = null
                    },
                    label = { Text("User ID") },
                    placeholder = { Text("Enter user ID") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError != null,
                    supportingText = showError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Text("Select Role:", style = MaterialTheme.typography.titleSmall)
                listOf("admin", "super_admin").forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRole = role }
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Text(role.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userId.isBlank()) {
                        showError = "Please enter a user ID"
                    } else {
                        onGrant(userId.trim(), selectedRole)
                    }
                },
                enabled = isValid
            ) {
                Text("Grant Admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class AdminTab(val title: String) {
    REPORTS("Reports"),
    USERS("Users"),
    ADMINS("Admins"),
    CATEGORIES("Categories")
}

@Composable
private fun ReportsTab(
    reports: List<AdminReport>,
    isLoading: Boolean,
    onReportClick: (AdminReport) -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (reports.isEmpty()) {
        EmptyState("No pending reports", Icons.Default.Report)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(reports) { report ->
                ReportCard(
                    report,
                    onClick = { onReportClick(report) },
                    onNavigateToUser = { onNavigateToUser(report.reportedUser?.id ?: "") }
                )
            }
        }
    }
}

@Composable
private fun UsersTab(
    bannedUsers: List<User>,
    isLoading: Boolean,
    onBanUser: (User) -> Unit,
    onUnbanUser: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (bannedUsers.isEmpty()) {
        EmptyState("No banned users", Icons.Default.Person)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(bannedUsers) { user ->
                BannedUserCard(
                    user,
                    onBan = { onBanUser(user) },
                    onUnban = { onUnbanUser(user.id) }
                )
            }
        }
    }
}


@Composable
private fun ReportCard(
    report: AdminReport,
    onClick: () -> Unit,
    onNavigateToUser: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Report: ${report.report.reason}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNavigateToUser) {
                    Icon(Icons.Default.Person, "View User Profile")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            report.post?.let { post ->
                Text("Post: ${post.textContent.take(50)}...")
                Text("By: ${report.reportedUser?.username ?: "Unknown"}")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Reported by: ${report.reporter?.username ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Reason: ${report.report.description}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BannedUserCard(
    user: User,
    onBan: () -> Unit,
    onUnban: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.username, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Violation and warning counts
                    Row {
                        Text(
                            "Violations: ${user.violationCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (user.violationCount >= 3) Color.Red else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Warnings: ${user.warningCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800) // Material Design Orange
                        )
                    }

                    user.banReason?.let {
                        Text(
                            "Ban Reason: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Ban button (edit ban reason)
                    IconButton(onClick = onBan) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit Ban",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // Unban button
                    IconButton(onClick = onUnban) {
                        Icon(
                            Icons.Default.LockOpen,
                            "Unban User",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AdminsTab(
    admins: List<AdminUser>,
    isLoading: Boolean,
    isSuperAdmin: Boolean,
    onGrantAdmin: () -> Unit,
    onRevokeAdmin: (AdminUser) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Admin Management",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    // Only SUPER_ADMIN can grant admin roles
                    if (isSuperAdmin) {
                        Button(onClick = onGrantAdmin) {
                            Icon(Icons.Default.PersonAdd, "Add Admin")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Admin")
                        }
                    }
                }
            }

            items(admins) { admin ->
                AdminCard(admin, onRevoke = { onRevokeAdmin(admin) })
            }

            if (admins.isEmpty()) {
                item {
                    EmptyState("No admins found")
                }
            }
        }
    }
}

@Composable
private fun CategoriesTab(
    categories: List<com.example.uth_socials.data.post.Category>,
    isLoading: Boolean,
    onEditCategory: (com.example.uth_socials.data.post.Category) -> Unit,
    onDeleteCategory: (com.example.uth_socials.data.post.Category) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Header chỉ có title
                Text(
                    "Category Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(categories) { category ->
                CategoryCard(category, onEdit = { onEditCategory(category) }, onDelete = { onDeleteCategory(category) })
            }

            if (categories.isEmpty()) {
                item {
                    EmptyState("No categories found")
                }
            }
        }
    }
}

@Composable
private fun AdminCard(
    admin: AdminUser,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        admin.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (admin.role) {
                            "super_admin" -> MaterialTheme.colorScheme.primary
                            "admin" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                    if (admin.role == "super_admin") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            "Super Admin",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "User ID: ${admin.userId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    "Granted by: ${admin.grantedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                admin.grantedAt?.let {
                    Text(
                        "Granted: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(it.toDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Only show revoke button for non-super admins
            if (admin.role != "super_admin") {
                IconButton(onClick = onRevoke) {
                    Icon(
                        Icons.Default.Delete,
                        "Revoke Admin",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportActionDialog(
    adminReport: AdminReport,
    onDismiss: () -> Unit,
    onAction: (AdminAction, String?) -> Unit
) {
    var selectedAction by remember { mutableStateOf(AdminAction.NONE) }
    var adminNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Report") },
        text = {
            Column {
                Text("Report: ${adminReport.report.reason}")
                adminReport.post?.let { post ->
                    Text("Post: ${post.textContent.take(100)}...")
                }
                Text("Reported by: ${adminReport.reporter?.username ?: "Unknown"}")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Action:", style = MaterialTheme.typography.titleSmall)
                AdminAction.entries.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAction = action }
                    ) {
                        RadioButton(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action }
                        )
                        Text(action.displayName)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = adminNotes,
                    onValueChange = { adminNotes = it },
                    label = { Text("Admin Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(selectedAction, adminNotes.ifBlank { null }) },
                enabled = selectedAction != AdminAction.NONE
            ) {
                Text("Execute Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CategoryCard(
    category: com.example.uth_socials.data.post.Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "ID: ${category.id} | Order: ${category.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }

                if (!com.example.uth_socials.data.post.Category.DEFAULT_CATEGORIES.any { it.id == category.id }) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    val isValid = categoryName.isNotBlank() && categoryName.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        showError = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("Enter category name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError != null,
                    supportingText = showError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                if (categoryName.isNotBlank()) {
                    Text(
                        "Generated ID: ${categoryName.lowercase().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), "_").take(20)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.trim().length < 2) {
                        showError = "Category name must be at least 2 characters"
                    } else {
                        onAdd(categoryName.trim())
                    }
                },
                enabled = isValid
            ) {
                Text("Add Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryDialog(
    category: com.example.uth_socials.data.post.Category,
    onDismiss: () -> Unit,
    onEdit: (String, String) -> Unit
) {
    var categoryName by remember { mutableStateOf(category.name) }
    var showError by remember { mutableStateOf<String?>(null) }

    val isValid = categoryName.isNotBlank() && categoryName.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        showError = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("Enter category name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError != null,
                    supportingText = showError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Text(
                    "ID: ${category.id} | Order: ${category.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.trim().length < 2) {
                        showError = "Category name must be at least 2 characters"
                    } else {
                        onEdit(category.id, categoryName.trim())
                    }
                },
                enabled = isValid
            ) {
                Text("Update Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailModal(
    adminReport: AdminReport,
    onDismiss: () -> Unit,
    onAction: (AdminAction, String?) -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Report Details",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            "Close",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 📋 Report Info
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Report Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Reason: ${adminReport.report.reason}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Description: ${adminReport.report.description}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Reported by: ${adminReport.reporter?.username ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 👤 Reported User Info
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { adminReport.reportedUser?.id?.let { onNavigateToUser(it) } },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar
                                AsyncImage(
                                    model = adminReport.reportedUser?.avatarUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        adminReport.reportedUser?.username ?: "Unknown User",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "ID: ${adminReport.reportedUser?.id ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    "View Profile",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // 📝 Post Content
                    adminReport.post?.let { post ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Reported Post",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        post.textContent,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // Post images
                                    if (post.imageUrls.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyColumn {
                                            items(post.imageUrls.take(3)) { imageUrl ->
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = "Post Image",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .background(Color.LightGray),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Category: ${post.category?.ifEmpty { "Uncategorized" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ⚡ Action Buttons
                    item {
                        Text(
                            "Admin Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Delete Post Button
                            Button(
                                onClick = { onAction(AdminAction.DELETE_POST, "Post deleted by admin") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)
                                )
                            ) {
                                Icon(Icons.Default.Delete, "Delete Post")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Post")
                            }

                            // Ban User Button
                            Button(
                                onClick = { onAction(AdminAction.BAN_USER, null) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF57C00)
                                )
                            ) {
                                Icon(Icons.Default.Block, "Ban User")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ban User")
                            }

                            // Warn User Button
                            Button(
                                onClick = { onAction(AdminAction.WARN_USER, "Warning issued by admin") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFBC02D)
                                )
                            ) {
                                Icon(Icons.Default.Warning, "Warn User")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Warn User")
                            }

                            // Dismiss Report Button
                            OutlinedButton(
                                onClick = { onAction(AdminAction.DISMISS, "Report dismissed") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, "Dismiss")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dismiss Report")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteCategoryDialog(
    category: com.example.uth_socials.data.post.Category,
    allCategories: List<com.example.uth_socials.data.post.Category>,
    onDismiss: () -> Unit,
    onDelete: (String?) -> Unit
) {
    var selectedMigrationCategory by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Filter out the category being deleted and default categories from migration options
    val migrationOptions = allCategories.filter { it.id != category.id && !com.example.uth_socials.data.post.Category.DEFAULT_CATEGORIES.any { default -> default.id == it.id } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Category: ${category.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "ID: ${category.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "⚠️ Warning: Deleting this category will affect posts using it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (migrationOptions.isNotEmpty()) {
                    Text("Choose where to move existing posts:", style = MaterialTheme.typography.titleSmall)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Option to move to uncategorized
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMigrationCategory = "" }
                        ) {
                            RadioButton(
                                selected = selectedMigrationCategory == "",
                                onClick = { selectedMigrationCategory = "" }
                            )
                            Text("Move to 'Uncategorized'")
                        }

                        // Options to move to other categories
                        migrationOptions.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMigrationCategory = cat.id }
                            ) {
                                RadioButton(
                                    selected = selectedMigrationCategory == cat.id,
                                    onClick = { selectedMigrationCategory = cat.id }
                                )
                                Text("Move to '${cat.name}'")
                            }
                        }
                    }
                } else {
                    Text(
                        "Posts will be moved to 'Uncategorized'",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onDelete(selectedMigrationCategory)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Delete Category")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BanUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onBan: (String) -> Unit
) {
    var banReason by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    val isValid = banReason.isNotBlank() && banReason.trim().length >= 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ban User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Username: ${user.username}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "User ID: ${user.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = banReason,
                    onValueChange = {
                        banReason = it
                        showError = null
                    },
                    label = { Text("Ban Reason") },
                    placeholder = { Text("Enter ban reason (min 5 characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = showError != null,
                    supportingText = showError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (banReason.trim().length < 5) {
                        showError = "Ban reason must be at least 5 characters"
                    } else {
                        onBan(banReason.trim())
                    }
                },
                enabled = isValid
            ) {
                Text("Ban User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Extension properties for display names
val AdminAction.displayName: String
    get() = when (this) {
        AdminAction.NONE -> "No Action"
        AdminAction.DISMISS -> "Dismiss Report"
        AdminAction.WARN_USER -> "Warn User"
        AdminAction.DELETE_POST -> "Delete Post"
        AdminAction.BAN_USER -> "Ban User"
        AdminAction.BAN_REPORTER -> "Ban Reporter"
    }
