package com.sample.newsapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sample.newsapplication.ui.theme.NewsApplicationTheme


@Composable
fun CircularLoader() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(8.dp)
                .size(50.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoaderPreview() {
    NewsApplicationTheme {
        CircularLoader()
    }
}

@Composable
fun ErrorItem(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(100.dp),
            painter = painterResource(android.R.drawable.stat_notify_error),
            contentDescription = null
        )
        Text(
            text = if (message.isNullOrEmpty()) "An error has occurred while processing your request. Please try again" else message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorItemPreview() {
    NewsApplicationTheme {
        ErrorItem()
    }
}


@Composable
fun SearchTextField(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(15.dp),
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = "Search for topics")
        },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = {
                    onClear.invoke()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close, contentDescription = null
                    )
                }
            }
        })
}

@Preview(showBackground = true)
@Composable
private fun SearchTextFieldPreview() {
    NewsApplicationTheme {
        SearchTextField(value = "", onValueChange = {}, onClear = {})
    }
}


@Composable
fun NoItems(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(100.dp),
            imageVector = Icons.Default.Warning,
//            painter = painterResource(android.R.drawable.stat_notify_sync),
            contentDescription = null
        )
        Text(
            text = "No items found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoItemsPreview() {
    NewsApplicationTheme {
        NoItems()
    }
}