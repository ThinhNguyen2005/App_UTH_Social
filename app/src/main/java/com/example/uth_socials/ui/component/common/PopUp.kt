package com.example.uth_socials.ui.component.common

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.uth_socials.data.util.MenuItemData

@Composable
fun ReusablePopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    menuItems: List<MenuItemData>,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        menuItems.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.text) },
                onClick = {
                    item.onClick()
                    onDismissRequest()
                },
                leadingIcon = {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.text
                        )
                    }
                }
            )
        }
    }
}

//cách sử dụng
// 1. Tạo state để kiểm soát việc hiển thị menu
//var menuExpanded by remember { mutableStateOf(false) }
//
//// 2. Định nghĩa các mục sẽ có trong menu này
//val menuItems = listOf(
//    MenuItemData(
//        text = "Lưu bài viết",
//        icon = Icons.Default.Bookmark,
//        onClick = { /* TODO: Xử lý logic lưu */ }
//    ),
//    MenuItemData(
//        text = "Chỉnh sửa",
//        icon = Icons.Default.Edit,
//        onClick = { /* TODO: Xử lý logic chỉnh sửa */ }
//    ),
//    MenuItemData(
//        text = "Xóa",
//        icon = Icons.Default.Delete,
//        onClick = { /* TODO: Xử lý logic xóa */ }
//    )
//)
//Box {
//    // 3. "Mỏ neo" - Nút 3 chấm để mở menu
//    IconButton(onClick = { menuExpanded = true }) {
//        Icon(
//            imageVector = Icons.Default.MoreVert,
//            contentDescription = "Tùy chọn khác"
//        )
//    }
//
//    // 4. Gọi Composable tái sử dụng của chúng ta
//    ReusablePopupMenu(
//        expanded = menuExpanded,
//        onDismissRequest = { menuExpanded = false },
//        menuItems = menuItems
//    )
//}