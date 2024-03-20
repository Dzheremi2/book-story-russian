package ua.acclorite.book_story.presentation.screens.history.data

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.GroupedHistory
import ua.acclorite.book_story.domain.model.History
import ua.acclorite.book_story.domain.use_case.DeleteHistory
import ua.acclorite.book_story.domain.use_case.DeleteWholeHistory
import ua.acclorite.book_story.domain.use_case.GetBooksById
import ua.acclorite.book_story.domain.use_case.GetHistory
import ua.acclorite.book_story.domain.use_case.InsertHistory
import ua.acclorite.book_story.domain.util.Resource
import ua.acclorite.book_story.presentation.data.Argument
import ua.acclorite.book_story.presentation.data.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val insertHistory: InsertHistory,
    private val getHistory: GetHistory,
    private val deleteWholeHistory: DeleteWholeHistory,
    private val deleteHistory: DeleteHistory,
    private val getBooksById: GetBooksById
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state = _state.asStateFlow()

    private var job: Job? = null
    private var job2: Job? = null
    private var job3: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    isLoading = true
                )
            }
            getHistoryFromDatabase()
        }
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.OnRefreshList -> {
                job3?.cancel()
                job3 = viewModelScope.launch(Dispatchers.IO) {
                    _state.update {
                        it.copy(
                            isRefreshing = true,
                            showSearch = false,
                            listState = LazyListState(0, 0),
                        )
                    }

                    getHistoryFromDatabase("")
                    delay(500)
                    _state.update {
                        it.copy(
                            isRefreshing = false
                        )
                    }
                }
            }

            is HistoryEvent.OnLoadList -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _state.update {
                        it.copy(
                            isLoading = true,
                            listState = LazyListState(0, 0),
                        )
                    }
                    getHistoryFromDatabase()
                }
            }

            is HistoryEvent.OnDeleteWholeHistory -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            showDeleteWholeHistoryDialog = false,
                            isLoading = true
                        )
                    }

                    deleteWholeHistory.execute()
                    event.refreshList()
                    getHistoryFromDatabase("")
                }

            }

            is HistoryEvent.OnShowHideDeleteWholeHistoryDialog -> {
                _state.update {
                    it.copy(
                        showDeleteWholeHistoryDialog = !it.showDeleteWholeHistoryDialog
                    )
                }
            }

            is HistoryEvent.OnDeleteHistoryElement -> {
                viewModelScope.launch(Dispatchers.IO) {
                    deleteHistory.execute(
                        listOf(event.historyToDelete)
                    )

                    onEvent(HistoryEvent.OnRefreshList)
                    event.refreshList()

                    job2?.cancel()
                    event.snackbarState.currentSnackbarData?.dismiss()

                    job2 = viewModelScope.launch(Dispatchers.IO) {
                        delay(10000)
                        event.snackbarState.currentSnackbarData?.dismiss()
                    }
                    val snackbar = event.snackbarState.showSnackbar(
                        event.context.getString(R.string.history_element_deleted),
                        event.context.getString(R.string.undo)
                    )

                    when (snackbar) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> {
                            insertHistory.execute(
                                listOf(event.historyToDelete)
                            )
                            event.refreshList()
                            onEvent(HistoryEvent.OnRefreshList)
                        }
                    }
                }
            }

            is HistoryEvent.OnSearchShowHide -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val shouldHide = _state.value.showSearch

                    if (shouldHide) {
                        getHistoryFromDatabase("")
                    } else {
                        _state.update {
                            it.copy(
                                searchQuery = "",
                                hasFocused = false
                            )
                        }
                    }
                    _state.update {
                        it.copy(
                            showSearch = !shouldHide
                        )
                    }
                }
            }

            is HistoryEvent.OnSearchQueryChange -> {
                _state.update {
                    it.copy(
                        searchQuery = event.query
                    )
                }
                job?.cancel()
                job = viewModelScope.launch(Dispatchers.IO) {
                    delay(500)
                    getHistoryFromDatabase()
                }
            }

            is HistoryEvent.OnRequestFocus -> {
                if (!_state.value.hasFocused) {
                    event.focusRequester.requestFocus()
                    _state.update {
                        it.copy(
                            hasFocused = true
                        )
                    }
                }
            }

            is HistoryEvent.OnUpdateBook -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val history = _state.value.history.map {
                        it.copy(
                            history = it.history.map { history ->
                                if (history.bookId == event.book.id) {
                                    history.copy(
                                        book = event.book
                                    )
                                } else {
                                    history
                                }
                            }
                        )
                    }

                    _state.update {
                        it.copy(
                            history = history
                        )
                    }
                }
            }

            is HistoryEvent.OnNavigateToReaderScreen -> {
                viewModelScope.launch {
                    event.book?.id?.let {
                        insertHistory.execute(
                            listOf(
                                History(
                                    null,
                                    it,
                                    null,
                                    Date().time
                                )
                            )
                        )
                    }
                    event.navigator.navigate(
                        Screen.READER,
                        false,
                        Argument(
                            "book", event.book
                        )
                    )
                }
            }
        }
    }

    private suspend fun getHistoryFromDatabase(
        query: String = if (_state.value.showSearch) _state.value.searchQuery else ""
    ) {
        fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        fun filterMaxElementsById(elements: List<History>): List<History> {
            val groupedById = elements.groupBy { it.bookId }
            val maxElementsById = groupedById.map { (_, values) ->
                values.maxByOrNull { it.time }
            }
            return maxElementsById.filterNotNull()
        }

        getHistory.execute().collect { result ->
            when (result) {
                is Resource.Success -> {
                    val historyWithoutBook =
                        result.data?.sortedByDescending { it.time } ?: emptyList()

                    if (historyWithoutBook.isEmpty()) {
                        _state.update {
                            it.copy(
                                history = emptyList(),
                                isLoading = false
                            )
                        }
                        return@collect
                    }

                    val books = getBooksById.execute(
                        historyWithoutBook.map { it.bookId }.distinct()
                    )

                    if (books.isEmpty()) {
                        _state.update {
                            it.copy(
                                history = emptyList(),
                                isLoading = false
                            )
                        }
                        return@collect
                    }

                    val history = historyWithoutBook.map {
                        val book = books.find { book -> book.id == it.bookId }!!
                        it.copy(
                            book = book
                        )
                    }

                    val groupedHistory = mutableListOf<GroupedHistory>()

                    history
                        .filter {
                            val book = books.find { book -> book.id == it.bookId }!!
                            book.title.lowercase().trim().contains(query.lowercase().trim())
                        }.groupBy { item ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = item.time
                            }
                            val now = Calendar.getInstance()

                            when {
                                isSameDay(calendar, now) -> "today"
                                isSameDay(
                                    calendar,
                                    now.apply {
                                        add(
                                            Calendar.DAY_OF_YEAR,
                                            -1
                                        )
                                    }) -> "yesterday"

                                else -> SimpleDateFormat(
                                    "dd.MM.yy",
                                    Locale.getDefault()
                                ).format(item.time)
                            }
                        }.forEach { (key, value) ->
                            groupedHistory.add(
                                GroupedHistory(
                                    key,
                                    filterMaxElementsById(value)
                                )
                            )
                        }

                    _state.update {
                        it.copy(
                            history = groupedHistory,
                            isLoading = false
                        )
                    }
                }

                is Resource.Loading -> Unit

                is Resource.Error -> Unit
            }
        }
    }
}

