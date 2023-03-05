package com.example.locketwidget.screens.premium

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.adapty.models.PaywallModel
import com.adapty.models.ProductModel
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalActivity
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.PremiumUtil
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.data.mapToErrorMessage
import com.example.locketwidget.screens.POLICY_LINK
import com.example.locketwidget.screens.TERMS_LINK
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.ViewPagerIndicatorsSpring
import com.example.locketwidget.ui.theme.LightGray
import com.example.locketwidget.ui.theme.Orange250
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay

private data class PremiumFeatureModel(@DrawableRes val image: Int, @StringRes val text: Int)

private val features = listOf(
    PremiumFeatureModel(R.drawable.premium_feature_1, R.string.premium_feature_1),
    PremiumFeatureModel(R.drawable.premium_feature_2, R.string.premium_feature_2),
    PremiumFeatureModel(R.drawable.premium_feature_3, R.string.premium_feature_3),
    PremiumFeatureModel(R.drawable.premium_feature_4, R.string.premium_feature_4),
    //  PremiumFeatureModel(R.drawable.premium_feature_4, R.string.premium_feature_5) TODO change
)

private const val SCROLL_TIME_MLS = 3000L
private const val NO_SCREEN_LOG = "no_screen"


@OptIn(ExperimentalPagerApi::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    selectedScreen: Int?
) {
    val viewModel: PremiumViewModel = mavericksActivityViewModel()
    val paywalls = viewModel.collectAsState { it.paywall }.value
    val selectedProduct = viewModel.collectAsState { it.selectedProduct }.value
    val currentActivity = LocalActivity.current
    val event = viewModel.collectAsState { it.event }.value
    val featureScrollState = rememberPagerState()
    val isPremium = viewModel.collectAsState { it.isPremium }.value
    val trialDays = paywalls.invoke()?.products?.first()?.freeTrialPeriod?.let { PremiumUtil.getTrialDays(it) }?.toString()
    LaunchedEffect(isPremium) {
        if (isPremium is Success && isPremium.invoke()) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        val screenName = when (selectedScreen) {
            0 -> ScreenItem.WidgetDetails.route
            1 -> ScreenItem.Contacts.route
            2 -> ScreenItem.HistoryList.route
            3 -> ScreenItem.History.route
            //4 -> "Reactions"
            else -> NO_SCREEN_LOG
        }
        LocketAnalytics.logOpenPremium(screenName)
        selectedScreen?.let { featureScrollState.animateScrollToPage(it) }
        viewModel.savePremiumScreenShownTime()
    }

    LocketScreen(
        topBar = { PremiumTopBar(popBackStack = navController::popBackStack) },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(0.25f))
                HorizontalPager(count = features.size, state = featureScrollState) { index ->
                    PremiumFeatureItem(feature = features[index])
                }
                ViewPagerIndicatorsSpring(
                    pagerState = featureScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                )
                Spacer(modifier = Modifier.weight(1f))

                if (paywalls is Success && selectedProduct is Success) {
                    if (paywalls.invoke().products.size > 1) {
                        MultiProductPaywall(
                            paywalls = paywalls.invoke(),
                            selectedProduct = selectedProduct,
                            onClick = viewModel::changeSelectedProduct
                        )
                    } else {
                        SingleProductPaywall(
                            selectedProduct = selectedProduct,
                            trialDays = trialDays,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))
                GradientButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.continue_button)
                ) {
                    viewModel.makePurchase(currentActivity)
                    LocketAnalytics.logClickBuyPremiumButton()
                }
                InfoText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.premium_screen_notation),
                )
                Spacer(modifier = Modifier.weight(0.5f))
                LinksRow(modifier = Modifier.fillMaxWidth(), restorePurchase = viewModel::restorePurchase)
                Spacer(modifier = Modifier.weight(0.25f))
            }

            if (isPremium is Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = LightGray.copy(alpha = 0.5f))
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
    LaunchedEffect(featureScrollState.currentPage) {
        delay(SCROLL_TIME_MLS)
        val pageIndex = (featureScrollState.currentPage + 1) % features.size
        featureScrollState.animateScrollToPage(page = pageIndex)
    }
}

@Composable
fun MultiProductPaywall(paywalls: PaywallModel, selectedProduct: Async<ProductModel>, onClick: (ProductModel) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val products = paywalls.products
        val sortedProducts = products.sortedBy { it.price }
        products.forEach { product ->
            val modifier = Modifier
                .padding(top = 8.dp, end = 5.dp)
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .run {
                    if (product == selectedProduct.invoke()) {
                        border(2.dp, MaterialTheme.colors.primary, RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colors.primary.copy(0.6f))
                    } else {
                        background(Color.White)
                            .clickable { onClick(product) }
                    }
                }
                .padding(20.dp)

            val discountText = if (product == sortedProducts.last()) {
                stringResource(R.string.best_deal)
            } else if (products.size > 2 && product == sortedProducts[products.size - 2]) {
                stringResource(R.string.month_discount)
            } else {
                null
            }

            ProductItem(product = product, columnModifier = modifier, discountText = discountText, boxModifier = Modifier.weight(1f))
            if (product != products.last()) {
                Spacer(modifier = Modifier.size(9.dp))
            }
        }
    }
}

@Composable
fun SingleProductPaywall(selectedProduct: Async<ProductModel>, trialDays: String?, modifier: Modifier) {
    if (trialDays != null) {
        Text(
            modifier = modifier,
            text = stringResource(R.string.premium_screen_trial_text, trialDays),
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            lineHeight = 22.sp
        )
        Text(
            modifier = modifier,
            text = stringResource(
                R.string.premium_screen_trial_price,
                selectedProduct.invoke()!!.localizedPrice ?: ""
            ),
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    } else {
        Text(
            modifier = modifier,
            text = stringResource(
                R.string.premium_screen_price,
                selectedProduct.invoke()!!.localizedPrice ?: ""
            ),
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProductDiscount(text: String) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colors.secondaryVariant,
                        MaterialTheme.colors.onSecondary
                    )
                )
            )
            .rotate(-15f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            color = Color.White,
            fontWeight = FontWeight.W600,
            lineHeight = 9.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProductItem(product: ProductModel, columnModifier: Modifier, discountText: String?, boxModifier: Modifier) {
    Box(contentAlignment = Alignment.TopEnd, modifier = boxModifier) {
        Column(
            modifier = columnModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.localizedPrice ?: "",
                style = MaterialTheme.typography.body1.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (product.subscriptionPeriod?.numberOfUnits == 1) {
                    product.localizedSubscriptionPeriod.toString().substring(2)
                } else {
                    product.localizedSubscriptionPeriod.toString()
                },
                style = MaterialTheme.typography.h2.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    lineHeight = 20.sp
                )
            )
        }
        if (discountText != null) {
            ProductDiscount(text = discountText)
        }
    }
}

@Composable
private fun InfoText(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier
            .alpha(0.5f),
        text = text,
        style = MaterialTheme.typography.body1.copy(fontSize = 10.sp, fontWeight = FontWeight.W400),
        lineHeight = 16.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LinksRow(
    modifier: Modifier = Modifier,
    restorePurchase: () -> Unit
) {
    val activityAction = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        InfoText(
            text = stringResource(R.string.terms_conditions),
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_LINK))
                activityAction.launch(intent)
            }
        )
        InfoText(
            text = stringResource(R.string.privacy_policy),
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(POLICY_LINK))
                activityAction.launch(intent)
            }
        )
        Row {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(2.dp)
                    .background(LightGray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            InfoText(
                text = stringResource(R.string.restore_purchase),
                modifier = Modifier.clickable { restorePurchase() }
            )
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    modifier: Modifier = Modifier,
    feature: PremiumFeatureModel
) {
    Column(modifier = modifier) {
        Image(painter = painterResource(feature.image), contentDescription = null)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(feature.text),
            style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.Bold, color = Orange250),
            lineHeight = 22.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PremiumTopBar(
    popBackStack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
            .padding(top = 10.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = getAnnotatedString(stringResource(R.string.screen_premium_title)),
            style = MaterialTheme.typography.caption,
            lineHeight = 34.sp,
            letterSpacing = 0.5.sp
        )

        Icon(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.CenterEnd)
                .clickable {
                    popBackStack()
                },
            painter = painterResource(R.drawable.ic_remove),
            contentDescription = null
        )
    }
}

private fun getAnnotatedString(text: String) = with(AnnotatedString.Builder()) {
    val textList = text.split(" ")
    pushStyle(SpanStyle(color = Orange250, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    append("${textList.first()} ")
    pop()
    pushStyle(SpanStyle(color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    textList.filterNot { it == textList.first() }.forEach {
        append("$it ")
    }
    pop()
    toAnnotatedString()
}