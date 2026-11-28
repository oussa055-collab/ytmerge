package com.example.cosmos.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cosmos.R
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

class Cam(
    @StringRes val name: Int,
    @StringRes val desc: Int,
    val imageUrl: String,   // image shown in the list
    val fullUrl: String,    // bigger image for the zoom view
    val pageUrl: String,    // official page (interactive 360° view where available)
)

private const val ESO = "https://www.hq.eso.org/public"
private const val ESO_PAGE = "$ESO/outreach/webcams/"
private const val LA_SILLA = "https://allsky-dk154.asu.cas.cz"

/** URLs come from ESO's own webcams page (ESO allows embedding these images). */
private val Cams = listOf(
    Cam(
        R.string.cam_vlt, R.string.cam_vlt_desc,
        "$ESO/archives/static/pano/latest/pano_PV_960x.jpg",
        "$ESO/archives/static/pano/latest/POI/preset_100.jpg",
        "$ESO_PAGE#paranal",
    ),
    Cam(
        R.string.cam_night, R.string.cam_night_desc,
        "$ESO/archives/static/allsky/latest/460x/preset_101.jpg",
        "$ESO/archives/static/allsky/latest/preset_101.jpg",
        "$ESO_PAGE#paranal",
    ),
    Cam(
        R.string.cam_alma, R.string.cam_alma_desc,
        "$ESO/archives/static/almapano/latest/pano_PV_960x.jpg",
        "$ESO/archives/static/almapano/latest/POI/preset_100.jpg",
        "$ESO_PAGE#alma",
    ),
    Cam(
        R.string.cam_lasilla, R.string.cam_lasilla_desc,
        "$LA_SILLA/AllSkyCurrentImage.JPG",
        "$LA_SILLA/AllSkyCurrentImage.JPG",
        "$LA_SILLA/",
    ),
    Cam(
        R.string.cam_elt, R.string.cam_elt_desc,
        "$ESO/archive_webcams/armazonespanosouth/public/pano_PV_960x.jpg",
        "$ESO/archive_webcams/armazonespanosouth/public/poi/preset_100.jpg",
        "$ESO_PAGE#armazones",
    ),
    Cam(
        R.string.cam_dome, R.string.cam_dome_desc,
        "$ESO/archive_webcams/armazones_dome_1/public/pano_PV_960x.jpg",
        "$ESO/archive_webcams/armazones_dome_1/public/poi/preset_100.jpg",
        "$ESO_PAGE#armazones",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(onBack: () -> Unit) {
    // The stamp is added to every URL so we always get a fresh picture, never a cached one.
    var stamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var zoomed by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5 * 60_000L)
            stamp = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.live_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { stamp = System.currentTimeMillis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.update))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.refreshed_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(stamp))),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(Cams) { cam -> CamCard(cam, stamp) { zoomed = cam.fullUrl } }
            item {
                Text(
                    stringResource(R.string.live_credits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    zoomed?.let { FullScreenImage("$it?t=$stamp") { zoomed = null } }
}

@Composable
private fun CamCard(cam: Cam, stamp: Long, onZoom: () -> Unit) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("${cam.imageUrl}?t=$stamp")
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(cam.name),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onZoom),
        )
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(cam.name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(cam.desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cam.pageUrl)))
            }) { Text(stringResource(R.string.official_page)) }
        }
    }
}
