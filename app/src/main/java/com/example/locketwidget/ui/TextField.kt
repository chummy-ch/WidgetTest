package com.example.locketwidget.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300

@Composable
fun EditNameTextField(
    modifier: Modifier = Modifier,
    hint: String,
    name: String,
    changeName: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = name,
        onValueChange = {
            changeName(it)
        },
        textStyle = MaterialTheme.typography.h2,
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.White,
            focusedLabelColor = Orange300
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        label = {
            Text(
                text = hint,
                style = TextStyle(color = Orange200, fontSize = 10.sp)
            )
        },
        shape = RoundedCornerShape(16.dp)
    )
}