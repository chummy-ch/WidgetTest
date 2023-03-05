package com.example.locketwidget.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.example.locketwidget.ActivityState
import com.example.locketwidget.ActivityViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.FirestoreUserResponse
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.screens.POLICY_LINK
import com.example.locketwidget.screens.premium.PremiumViewModel
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.TopBar
import com.google.firebase.auth.FirebaseAuth

const val APP_PACKAGE_NAME = "com.smartfoxlab.locket.widget"
const val WHATS_NEW_LINK = "https://www.craft.do/s/vhSBaeNggmn3Gh"
const val SEND_FEEDBACK_LINK = "https://besties-widget.canny.io/feature-requests"


@Composable
fun SettingsScreen(
    navController: NavController,
    logOut: () -> Unit
) {
    val viewModel: ActivityViewModel = mavericksActivityViewModel()
    val premiumViewModel: PremiumViewModel = mavericksActivityViewModel()
    val user = viewModel.collectAsState(ActivityState::user).value.invoke() ?: return
    val isPremium = premiumViewModel.collectAsState { it.isPremium }.value
    val activityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    LocketScreen(
        topBar = { TopBar(currentScreenItem = ScreenItem.Settings) { navController.popBackStack() } }
    ) { paddingValues ->
        LazyColumn(
            Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            item {
                Column {
                    Spacer(modifier = Modifier.weight(1f))

                    Profile(modifier = Modifier.align(Alignment.CenterHorizontally), user = user)

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(color = Color.White)
                    )
                    {
                        SettingItem(
                            icon = R.drawable.ic_profile,
                            title = stringResource(R.string.settings_profile)
                        ) {
                            navController.navigate(ScreenItem.ProfileSettings.route)
                        }
                        /*  SettingItem(icon = R.drawable.ic_paint, title = stringResource(R.string.settings_scheme)) {
                              // TODO: Provide action
                          }*/
                        SettingItem(
                            icon = R.drawable.ic_book,
                            title = stringResource(R.string.settings_widget_info)
                        ) {
                            navController.navigate(ScreenItem.WidgetSettingsGuide.route)
                        }
                        SettingItem(
                            icon = R.drawable.ic_menu,
                            title = stringResource(R.string.settings_manage_widget)
                        ) {
                            navController.navigate(ScreenItem.WidgetList.route)
                        }
                        if (isPremium is Success && !isPremium.invoke()) {
                            SettingItem(
                                icon = R.drawable.ic_buy_premium,
                                title = stringResource(R.string.settings_buy_premium)
                            ) {
                                navController.navigate(ScreenItem.Premium.route)
                            }
                        }
                        SettingItem(
                            icon = R.drawable.ic_star,
                            title = stringResource(R.string.settings_review_app)
                        ) {
                            try {
                                activityLauncher.launch(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$APP_PACKAGE_NAME")
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
                                activityLauncher.launch(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=$APP_PACKAGE_NAME")
                                    )
                                )
                            }
                            LocketAnalytics.logReviewApp()
                        }
                        SettingItem(
                            icon = R.drawable.ic_feedback,
                            title = stringResource(R.string.settings_send_feedback)
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SEND_FEEDBACK_LINK))
                            activityLauncher.launch(intent)
                        }
                        SettingItem(
                            icon = R.drawable.ic_book,
                            title = stringResource(R.string.settings_privacy)
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(POLICY_LINK))
                            activityLauncher.launch(intent)
                        }
                        SettingItem(
                            icon = R.drawable.ic_device,
                            title = stringResource(R.string.settings_whats_new)
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WHATS_NEW_LINK))
                            activityLauncher.launch(intent)
                        }
                        SettingItem(
                            icon = R.drawable.ic_logout,
                            title = stringResource(R.string.log_out)
                        ) {
                            logOut()
                        }
                        /* SettingItem(
                             icon = R.drawable.ic_user_remove,
                             title = stringResource(R.string.settings_remove_account)
                         ) {
                             // TODO: Provide action
                         }*/
                    }
                }
            }
        }
    }
}

@Composable
fun Profile(
    modifier: Modifier = Modifier,
    user: FirestoreUserResponse
) {
    Column(modifier = modifier) {
        Image(
            modifier = Modifier
                .size(dimensionResource(R.dimen.user_profile_photo_size))
                .clip(CircleShape)
                .align(Alignment.CenterHorizontally),
            painter = rememberAsyncImagePainter(
                model = user.photoLink,
                error = painterResource(R.drawable.default_avatar)
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
            text = user.name,
            style = MaterialTheme.typography.caption
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
            text = FirebaseAuth.getInstance().currentUser?.email ?: "",
            style = MaterialTheme.typography.h2
        )
    }
}

@Composable
fun SettingsItemWithSubtitle(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    action: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { action() }
            .padding(start = 24.dp, top = 20.dp, bottom = 20.dp)) {

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(icon),
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .padding(start = 20.dp)
                .align(Alignment.CenterVertically),
            text = title,
            style = MaterialTheme.typography.h3
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.padding(end = 20.dp),
            text = subtitle,
            style = MaterialTheme.typography.h2.copy(color = Color.LightGray)
        )
    }
}

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    title: String,
    action: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { action() }
            .padding(start = 24.dp, top = 20.dp, bottom = 20.dp)) {

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(icon),
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .padding(start = 20.dp)
                .align(Alignment.CenterVertically),
            text = title,
            style = MaterialTheme.typography.h3.copy(
                lineHeight = 22.sp,
                fontWeight = FontWeight.W400
            )
        )
    }
}
