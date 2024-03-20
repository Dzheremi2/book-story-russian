package ua.acclorite.book_story.presentation.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.components.GoBackButton
import ua.acclorite.book_story.presentation.data.Navigator
import ua.acclorite.book_story.presentation.data.Screen
import ua.acclorite.book_story.presentation.screens.settings.components.SettingsCategoryItem
import ua.acclorite.book_story.presentation.ui.elevation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigator: Navigator
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        canScroll = {
            listState.canScrollForward || listState.canScrollBackward
        }
    )

    Scaffold(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(id = R.string.settings_screen))
                },
                navigationIcon = {
                    GoBackButton(navigator = navigator)
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.elevation()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            state = listState
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsCategoryItem(
                    icon = Icons.Default.DisplaySettings,
                    text = stringResource(id = R.string.general_settings),
                    description = stringResource(id = R.string.general_settings_desc)
                ) {
                    navigator.navigate(Screen.GENERAL_SETTINGS, false)
                }
            }

            item {
                SettingsCategoryItem(
                    icon = Icons.Default.Palette,
                    text = stringResource(id = R.string.appearance_settings),
                    description = stringResource(id = R.string.appearance_settings_desc)
                ) {
                    navigator.navigate(Screen.APPEARANCE_SETTINGS, false)
                }
            }

            item {
                SettingsCategoryItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    text = stringResource(id = R.string.reader_settings),
                    description = stringResource(id = R.string.reader_settings_desc)
                ) {
                    navigator.navigate(Screen.READER_SETTINGS, false)
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}