package com.example.locketwidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.locketwidget.R
import com.example.locketwidget.core.LocketAnalytics
import com.example.locketwidget.data.ContactResponse
import com.example.locketwidget.ui.theme.Orange200
import com.example.locketwidget.ui.theme.Orange300

@Composable
fun FriendsList(
    modifier: Modifier = Modifier,
    friends: List<Pair<ContactResponse, Boolean>>,
    title: Int,
    select: (String, Boolean) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = stringResource(title), style = MaterialTheme.typography.h2)
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(items = friends, itemContent = { item ->
                FriendItem(
                    modifier = Modifier.padding(top = 10.dp),
                    contact = item.first,
                    isSelected = item.second,
                    select = select
                )
            })
        }
    }
}

@Composable
fun FriendItem(
    modifier: Modifier = Modifier,
    contact: ContactResponse,
    isSelected: Boolean,
    select: (String, Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                select(contact.id, !isSelected)
                LocketAnalytics.logWidgetFriendListChange()
            }
    ) {
        val borderColor = if (isSelected) listOf(Orange200, Orange300)
        else listOf(MaterialTheme.colors.background, MaterialTheme.colors.background)

        ContactImageWithBorder(
            modifier = Modifier.size(66.dp),
            painter = rememberAsyncImagePainter(contact.photoUrl),
            borderBrush = Brush.linearGradient(borderColor),
            borderPadding = 4.dp,
            borderSize = 2.dp
        )

        val textColor = if (isSelected) Orange200 else Color.Black
        val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.W400
        Text(
            modifier = Modifier
                .padding(start = 20.dp)
                .align(Alignment.CenterVertically),
            text = contact.name,
            style = MaterialTheme.typography.h2.copy(color = textColor, fontWeight = fontWeight)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isSelected) {
            Image(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically)
                    .background(brush = Brush.linearGradient(listOf(Orange200, Orange300))),
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null
            )
        }
    }
}