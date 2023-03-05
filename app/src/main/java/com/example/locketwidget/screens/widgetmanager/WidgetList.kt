package com.example.locketwidget.screens.widgetmanager

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.data.*
import com.example.locketwidget.ui.GradientButton
import com.example.locketwidget.ui.LocketScreen
import com.example.locketwidget.ui.TopBar
import com.example.locketwidget.ui.gradientBorder
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300
import com.example.locketwidget.widget.LocketWidgetProvider
import com.example.locketwidget.widget.WidgetTransformation

private const val SPAN_COUNT = 2

@Composable
fun WidgetManager(
    navController: NavController,
) {
    val viewModel: WidgetManagerViewModel = mavericksViewModel()
    val widgets = viewModel.collectAsState { it.widgets }.value
    val context = LocalContext.current
    val event = viewModel.collectAsState { it.event }.value

    LocketScreen(
        topBar = { TopBar(currentScreenItem = ScreenItem.WidgetList) { navController.popBackStack() } },
        message = event?.mapToErrorMessage(),
        disposeEvent = viewModel::clearEvent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = dimensionResource(R.dimen.screen_elements_padding),
                        end = dimensionResource(R.dimen.screen_elements_padding),
                        top = 16.dp
                    ),
                columns = GridCells.Fixed(SPAN_COUNT)
            ) {
                if (widgets is Success) {
                    items(widgets.invoke()) {
                        WidgetItem(
                            widget = it.first,
                            photo = it.second,
                            action = {
                                navigateToWidgetDetails(
                                    it.first,
                                    it.second.invoke(),
                                    navController
                                )
                            },
                            remove = viewModel::remove
                        )
                    }
                    items(SPAN_COUNT - widgets.invoke().size % SPAN_COUNT) {
                        Box {}
                    }
                }
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    GradientButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = dimensionResource(R.dimen.screen_elements_padding),
                                bottom = dimensionResource(R.dimen.screen_elements_padding)
                            ),
                        text = stringResource(R.string.create_widget_button)
                    ) {
                        viewModel.setWidgetAddedEvent(addWidget(context))
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetItem(
    modifier: Modifier = Modifier,
    widget: LocketWidgetModel,
    photo: Async<String>,
    action: () -> Unit,
    remove: (Int) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .padding(top = 20.dp)
            .clickable { action() }
    ) {

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.CenterVertically),
                text = widget.name,
                style = MaterialTheme.typography.h3
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically)
                    .background(brush = Brush.linearGradient(listOf(Orange200, Orange300)))
                    .clickable { remove(widget.id) }
                    .padding(4.dp),
                painter = painterResource(R.drawable.ic_remove),
                tint = Color.White,
                contentDescription = null
            )
        }

        val imageModifier = modifier
            .size(140.dp)
            .padding(top = 8.dp)
        when (widget.style) {
            is WidgetShape -> {
                AsyncImage(
                    modifier = imageModifier
                        .clip(RoundedCornerShape(16.dp))
                        .gradientBorder(widget.style.stroke, 140.dp, 16.dp, 6.dp),
                    model = ImageRequest.Builder(context)
                        .data(photo.invoke())
                        .transformations(WidgetTransformation(widget.style.shape))
                        .build(),
                    contentDescription = null,
                    placeholder = painterResource(widget.style.shape.backgroundRes),
                    error = painterResource(widget.style.shape.backgroundRes)
                )
            }
            is WidgetForeground -> {
                Box(
                    modifier = imageModifier
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .padding(widget.style.padding.dp)
                            .fillMaxSize(),
                        model = photo.invoke(),
                        contentDescription = null,
                        placeholder = painterResource(R.drawable.default_avatar),
                        error = painterResource(R.drawable.default_avatar)
                    )
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = rememberAsyncImagePainter(
                            model = widget.style.preview,
                            imageLoader = remember {
                                ImageLoader.Builder(context)
                                    .components {
                                        if (Build.VERSION.SDK_INT >= 28) {
                                            add(ImageDecoderDecoder.Factory())
                                        } else {
                                            add(GifDecoder.Factory())
                                        }
                                    }
                                    .build()
                            }
                        ),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

private fun navigateToWidgetDetails(widget: LocketWidgetModel, photo: String?, navController: NavController) {
    val json = widget.toJson()
    var path = ScreenItem.WidgetDetails.route + "/$json"
    if (photo != null) path += "?${ScreenItem.WIDGET_PHOTO_ARG}=$photo"
    navController.navigate(path)
}

fun addWidget(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val widgetManager = context.getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(context, LocketWidgetProvider::class.java)
        val intent = Intent(context, LocketWidgetProvider::class.java)
        val callback = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        widgetManager.requestPinAppWidget(provider, Bundle(), callback)
        true
    } else {
        false
    }
}