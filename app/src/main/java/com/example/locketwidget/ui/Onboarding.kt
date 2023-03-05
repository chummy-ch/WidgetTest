package com.example.locketwidget.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.locketwidget.R
import com.example.locketwidget.ui.theme.Orange250
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlin.math.abs

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ViewPagerIndicators(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    color: Color = Orange250,
) {
    val indicatorWidth = dimensionResource(R.dimen.onboarding_dot_size)
    val indicatorWidthPx = LocalDensity.current.run { indicatorWidth.roundToPx() }
    val spacingPx = LocalDensity.current.run { dimensionResource(R.dimen.onboarding_indicators_padding).roundToPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.onboarding_indicators_padding)),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            repeat(pagerState.pageCount + 1) {
                IndicatorDot(modifier = Modifier.size(dimensionResource(R.dimen.onboarding_dot_size)), color = color)
            }
        }

        IndicatorRectangle(
            Modifier
                .offset {
                    val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffset)
                        .coerceIn(
                            0f,
                            (pagerState.pageCount - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                        )
                    IntOffset(
                        x = ((spacingPx + indicatorWidthPx) * scrollPosition).toInt(),
                        y = 0
                    )
                }
                .width(dimensionResource(R.dimen.onboarding_selected_indicator_width))
                .height(dimensionResource(R.dimen.onboarding_dot_size))
                .background(
                    color = color,
                    shape = RoundedCornerShape(20.dp),
                ),
            color = color
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ViewPagerIndicatorsSpring(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    color: Color = Orange250,
) {
    val dotSize = dimensionResource(R.dimen.onboarding_dot_size)
    val selectedSize = dimensionResource(R.dimen.onboarding_selected_indicator_width)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.onboarding_indicators_padding)),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            repeat(pagerState.pageCount) { index ->
                IndicatorDot(
                    modifier = Modifier
                        .height(dimensionResource(R.dimen.onboarding_dot_size))
                        .width(
                            animateDpAsState(
                                targetValue = when {
                                    pagerState.currentPageOffset == 0f -> {
                                        if (index == pagerState.currentPage) {
                                            selectedSize
                                        } else {
                                            dotSize
                                        }
                                    }
                                    pagerState.currentPageOffset > 0f -> {
                                        when (index) {
                                            (pagerState.currentPage + 1) % pagerState.pageCount -> {
                                                (selectedSize * pagerState.currentPageOffset)
                                                    .coerceIn(
                                                        dotSize,
                                                        selectedSize
                                                    )
                                            }
                                            pagerState.currentPage -> {
                                                (selectedSize * (1 - pagerState.currentPageOffset))
                                                    .coerceIn(
                                                        dotSize,
                                                        selectedSize
                                                    )
                                            }
                                            else -> dotSize
                                        }
                                    }
                                    pagerState.currentPageOffset < 0f -> {
                                        if (pagerState.currentPage == pagerState.pageCount - 1) {
                                            dotSize
                                        } else {
                                            when (index) {
                                                pagerState.currentPage -> {
                                                    (selectedSize * (1 - abs(pagerState.currentPageOffset)))
                                                        .coerceIn(
                                                            dotSize,
                                                            selectedSize
                                                        )
                                                }
                                                pagerState.currentPage - 1 -> {
                                                    (selectedSize * abs(pagerState.currentPageOffset))
                                                        .coerceIn(
                                                            dotSize,
                                                            selectedSize
                                                        )
                                                }
                                                else -> dotSize
                                            }
                                        }
                                    }
                                    else -> throw IllegalAccessException()
                                },
                            ).value
                        ),
                    color = color
                )
            }
        }
    }
}

@Composable
fun OnboardingIndicators(
    modifier: Modifier = Modifier,
    amount: Int,
    color: Color = Orange250,
    selected: Int
) {
    Row(modifier = modifier) {
        repeat(amount) {
            if (it != selected) {
                IndicatorDot(modifier = Modifier.size(dimensionResource(R.dimen.onboarding_dot_size)), color = color)
            } else {
                IndicatorRectangle(
                    modifier = Modifier
                        .width(dimensionResource(R.dimen.onboarding_selected_indicator_width))
                        .height(dimensionResource(R.dimen.onboarding_dot_size)),
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.onboarding_indicators_padding)))
        }
    }
}

@Composable
fun IndicatorDot(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color = color)
            .alpha(0.5f)
    )
}

@Composable
fun IndicatorRectangle(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
    )
}

@Preview
@Composable
private fun IndicatorsPreview() {
    OnboardingIndicators(amount = 5, selected = 2, color = Color.White)
}