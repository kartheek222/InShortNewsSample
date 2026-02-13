package com.sample.newsapplication.ui.screens.home

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.sample.newsapplication.data.entity.ArticleModel
import com.sample.newsapplication.data.entity.NewsResponseModel
import com.sample.newsapplication.data.entity.SourceModel
import com.sample.newsapplication.ui.theme.NewsApplicationTheme
import com.sample.newsapplication.utilities.shimmerable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(modifier: Modifier = Modifier, responseModel: NewsResponseModel) {
    val pagerState =
        rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = {
                responseModel.articles.size
            }
        )
    VerticalPager(
        modifier = Modifier
            .fillMaxSize()
//            .padding(innerPadding)
        ,
        state = pagerState,
        pageSize = PageSize.Fill,
        pageSpacing = 6.dp

    ) { page ->
        val config = LocalConfiguration.current
        val orientation = config.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            NewsArticleLandscape(article = responseModel.articles[page])
        } else {
            NewsArticle(article = responseModel.articles[page])
        }
    }
}


@Composable
fun NewsArticle(article: ArticleModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        AsyncImage(
            model = article.urlToImage,
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .height(250.dp),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            error = painterResource(android.R.drawable.ic_menu_report_image),
            placeholder = painterResource(android.R.drawable.ic_menu_report_image)
        )

        Spacer(modifier = Modifier.size(6.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth(),
            text = article.title ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth(),
            text = article.description ?: "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 10.dp, horizontal = 14.dp)
                .clickable {
                    article.url?.let {
                        val intent = Intent(Intent.ACTION_VIEW, article.url.toUri())
                        context.startActivity(intent)
                    }
                }
        ) {
            Text(
                text = article.author ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = article.source?.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
        }
    }
}


@Composable
fun NewsArticleLoader() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AsyncImage(
            model = "",
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .height(250.dp)
                .shimmerable(true),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            error = painterResource(android.R.drawable.ic_menu_report_image),
            placeholder = painterResource(android.R.drawable.ic_menu_report_image)
        )

        Spacer(modifier = Modifier.size(6.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .shimmerable(true),
            text = "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            minLines = 2
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .shimmerable(true),
            text = "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default,
            minLines = 6
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 14.dp)
                .shimmerable(true)
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArticlePreview() {
    NewsApplicationTheme {
        NewsArticle(
            ArticleModel(
                author = "author",
                content = "content adsfl;kasj falskdfj a lkj asl;dfkj asldkf jas;ldfkkj as;lkdf jasd;lkfj asl;kf",
                description = "description asl;dfkj alskdjf a;lkfdj aslk;fdj aslkjf a;lskjf ;alskfj asl;kjf alskjf alskjf alskjf alksjf ",
                publishedAt = "publishedAt",
                source = SourceModel(id = "id", name = "name"),
                title = "title",
                url = "https://www.shutterstock.com/image-photo/sun-sets-behind-mountain-ranges-600nw-2479236003.jpg",
                urlToImage = "https://www.shutterstock.com/image-photo/sun-sets-behind-mountain-ranges-600nw-2479236003.jpg"
            )
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.ORIENTATION_LANDSCAPE
)
@Composable
fun ArticleLandscapePreview() {
    NewsApplicationTheme {
        NewsArticleLandscape(
            ArticleModel(
                author = "author",
                content = "content adsfl;kasj falskdfj a lkj asl;dfkj asldkf jas;ldfkkj as;lkdf jasd;lkfj asl;kf",
                description = "description asl;dfkj alskdjf a;lkfdj aslk;fdj aslkjf a;lskjf ;alskfj asl;kjf alskjf alskjf alskjf alksjf ",
                publishedAt = "publishedAt",
                source = SourceModel(id = "id", name = "name"),
                title = "title",
                url = "https://www.shutterstock.com/image-photo/sun-sets-behind-mountain-ranges-600nw-2479236003.jpg",
                urlToImage = "https://www.shutterstock.com/image-photo/sun-sets-behind-mountain-ranges-600nw-2479236003.jpg"
            )
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ArticleLoaderPreview() {
    NewsApplicationTheme {
        NewsArticleLoader()
    }
}


@Composable
fun NewsArticleLandscape(article: ArticleModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp)
                .weight(1f)
                .fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = article.urlToImage,
                modifier = Modifier
                    .weight(0.4f)
                    .height(250.dp)
                    .align(alignment = Alignment.CenterVertically),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                error = painterResource(android.R.drawable.ic_menu_report_image),
                placeholder = painterResource(android.R.drawable.ic_menu_report_image)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Column(
                modifier = Modifier
                    .padding(start = 18.dp)
                    .weight(0.6f),
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth(),
                    text = article.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth(),
                    text = article.description ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Default
                )
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 10.dp, horizontal = 14.dp)
                .clickable {
                    article.url?.let {
                        val intent = Intent(Intent.ACTION_VIEW, article.url.toUri())
                        context.startActivity(intent)
                    }
                }
        ) {
            Text(
                text = article.author ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = article.source?.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
        }
    }
}