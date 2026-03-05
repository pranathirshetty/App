package to.kuudere.anisuge.screens.home

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable

@Composable
fun TestPager() {
    val state = rememberPagerState(pageCount = { 10 })
    HorizontalPager(state = state) { }
}
