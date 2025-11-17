package com.example.uth_socials.ui.component.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasswordTextField(
    value : String,
    onValueChange : (String) -> Unit,
    label : String="Mật khẩu"
){
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if(passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = icon, contentDescription = null)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color(0xFF06635A),
            unfocusedIndicatorColor = Color(0xFFB0BEC5),
            focusedContainerColor = Color(0xFFF1F4FF),
            unfocusedContainerColor = Color(0xFFF1F4FF),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )

    )
}

@Composable
fun InputTextField(
    value : String,
    onValueChange : (String) -> Unit,
    label : String="Email"
){
    OutlinedTextField(
        value=value,
        onValueChange=onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color(0xFF06635A),
            unfocusedIndicatorColor = Color(0xFFB0BEC5),
            focusedContainerColor = Color(0xFFF1F4FF),
            unfocusedContainerColor = Color(0xFFF1F4FF),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}