package com.example.locketwidget.screens.contacts

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.core.LocketGson
import com.example.locketwidget.core.PremiumUtil
import com.example.locketwidget.core.provideActionIfPremium
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.data.DataStoreGroupModel
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.screens.premium.PremiumViewModel
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@OptIn(com.google.accompanist.pager.ExperimentalPagerApi::class)
@Composable
fun ContactsScreen(
    navController: NavController
) {
    val viewModel: ContactsViewModel = mavericksViewModel()
    val contactsAsync = viewModel.collectAsState(ContactsState::contacts).value
    val context = LocalContext.current
    val pagerState = rememberPagerState()

    LocketScreen(
        topBar = {
            TopBar(currentScreenItem = ScreenItem.Contacts) { navController.popBackStack() }
        }
    ) { paddingValues ->

        if (contactsAsync is Success) {
            Column(modifier = Modifier.padding(paddingValues)) {
                ContactsBody(
                    firstTitle = R.string.your_friends,
                    secondTitle = R.string.groups,
                    pagerState = pagerState
                ) {
                    HorizontalPager(
                        count = 2,
                        state = pagerState,
                    ) { page ->
                        if (page == 0) {
                            FriendPickerScreen(
                                context = context,
                                viewModel = viewModel,
                                contactsAsync = contactsAsync,
                                navController = navController
                            )
                        } else {
                            GroupPickerScreen(viewModel = viewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    modifier: Modifier = Modifier,
    contact: ContactResponse,
    remove: () -> Unit
) {
    ConstraintLayout(modifier = modifier) {
        val (imageRef, nameRef, buttonRef) = createRefs()

        ContactImageWithBorder(
            modifier = Modifier
                .size(90.dp)
                .constrainAs(imageRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            painter = rememberAsyncImagePainter(
                model = contact.photoUrl,
                error = painterResource(id = R.drawable.default_avatar),
            ),
            borderBrush = Brush.linearGradient(colors = listOf(Orange200, Orange300)),
            borderPadding = 6.dp,
            borderSize = 3.dp
        )

        Image(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { remove() }
                .constrainAs(buttonRef) {
                    end.linkTo(imageRef.end)
                    top.linkTo(imageRef.top)
                }
        )

        Text(
            text = contact.name,
            modifier = Modifier.constrainAs(nameRef) {
                top.linkTo(imageRef.bottom, 10.dp)
                bottom.linkTo(parent.bottom, 10.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            style = MaterialTheme.typography.h2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FriendPickerScreen(
    context: Context,
    viewModel: ContactsViewModel,
    contactsAsync: Async<List<ContactResponse>>,
    navController: NavController
) {
    val premiumViewModel: PremiumViewModel = mavericksActivityViewModel()
    val isPremium = premiumViewModel.collectAsState { it.isPremium }.value
    val shareLinkResult =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.clearDeepLink()
            LocketAnalytics.logFriendLinkCopy()
        }

    val linkAsync = viewModel.collectAsState(ContactsState::link).value

    LaunchedEffect(linkAsync) {
        if (linkAsync is Success) {
            val text = context.getString(R.string.share_link_text, linkAsync.invoke())
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val chooserIntent =
                Intent.createChooser(intent, context.getString(R.string.share_link_exported_title))
            shareLinkResult.launch(chooserIntent)
        }
    }

    Column(
        modifier = Modifier.padding(top = 24.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LazyTriangleLayout(Modifier.padding(horizontal = 24.dp)) {
                contactsAsync.invoke()!!.forEach { contact ->
                    ContactItem(
                        modifier = Modifier.width(90.dp),
                        contact = contact,
                        remove = { viewModel.removeConnection(contact.id) })
                }
            }
        }

        GradientButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.screen_elements_padding),
                    end = dimensionResource(R.dimen.screen_elements_padding)
                )
                .padding(vertical = dimensionResource(R.dimen.screen_top_padding)),
            text = stringResource(R.string.invite_button)
        ) {
            if (contactsAsync.invoke()!!.size < PremiumUtil.MAX_FRIENDS_COUNT) {
                viewModel.createDeepLink()
            } else {
                provideActionIfPremium(
                    isPremium = isPremium.invoke(),
                    action = viewModel::createDeepLink,
                    nonPremiumAction = {
                        navController.navigate("${ScreenItem.Premium.route}?${ScreenItem.PREMIUM_FEATURE_SCROLL_ARG}=1")
                    }
                )
            }
        }
    }
}

@Composable
fun GroupPickerScreen(viewModel: ContactsViewModel, navController: NavController) {
    Column(
        modifier = Modifier.padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val groups = viewModel.collectAsState { it.groups }.value
        if (groups is Success) {
            if (groups.invoke().isEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_empty_groups),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    text = stringResource(R.string.empty_group_text),
                    modifier = Modifier.padding(bottom = 44.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.W500)
                )
            } else {
                NonEmptyGroupContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), groups.invoke()
                ) {
                    navController.navigate("${ScreenItem.NewGroup.route}?${ScreenItem.GROUP_ARG}=${LocketGson.gson.toJson(it)}")
                }
            }
        }
        GradientButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.screen_top_padding))
                .padding(horizontal = 32.dp),
            text = stringResource(R.string.create_new_group)
        ) {
            navController.navigate(ScreenItem.NewGroup.route)
        }
    }
}

@Composable
fun NonEmptyGroupContent(modifier: Modifier, groups: List<DataStoreGroupModel>, onClick: (DataStoreGroupModel) -> Unit) {
    LazyColumn(modifier = modifier) {
        items(groups) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onClick(item) }
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OverlayLayout {
                        if (item.contacts.isNotEmpty())
                            FriendItem(url = item.contacts[0].photoUrl)
                        if (item.contacts.size > 1)
                            FriendItem(url = item.contacts[1].photoUrl)
                    }
                    if (item.contacts.size > 2)
                        Text(
                            text = stringResource(R.string.plus, item.contacts.size - 2),
                            style = MaterialTheme.typography.h2.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.primary
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                }
                Text(text = item.name, style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.W400))
            }
        }
    }
}

@Composable
fun FriendItem(url: String?) {
    AsyncImage(
        model = url,
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .border(4.dp, MaterialTheme.colors.background, CircleShape),
        contentDescription = null
    )
}