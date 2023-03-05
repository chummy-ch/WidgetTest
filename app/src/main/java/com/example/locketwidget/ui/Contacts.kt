package com.example.locketwidget.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ContactsBody(
    @StringRes firstTitle: Int,
    @StringRes secondTitle: Int,
    pagerState: PagerState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        ContactsTitleText(
            modifier = Modifier.weight(1f),
            text = firstTitle,
            color = MaterialTheme.colors.onPrimary.copy(if (pagerState.currentPage == 1) 0.2f else 1f)
        ) { scope.launch { pagerState.animateScrollToPage(0) } }
        ContactsTitleText(
            modifier = Modifier.weight(1f),
            text = secondTitle,
            color = MaterialTheme.colors.onPrimary.copy(if (pagerState.currentPage == 0) 0.2f else 1f)
        ) { scope.launch { pagerState.animateScrollToPage(1) } }
    }
    content()
}

@Composable
fun ContactsTitleText(
    modifier: Modifier = Modifier,
    @StringRes text: Int,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        modifier = modifier.noRippleClickable { onClick() },
        textAlign = TextAlign.Center,
        text = stringResource(text),
        style = MaterialTheme.typography.h1.copy(color = color)
    )
}

inline fun Modifier.noRippleClickable(crossinline onClick: () -> Unit): Modifier = composed {
    clickable(indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}