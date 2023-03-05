package com.example.locketwidget.features.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.withState

data class DrawingState(
    val paths: List<Pair<Path, PathProperties>> = listOf(),
    val pathsUndone: List<Pair<Path, PathProperties>> = listOf(),
    val currentPath: Path = Path()
) : MavericksState

class DrawingViewModel(
    initState: DrawingState
) : MavericksViewModel<DrawingState>(initState) {

    fun addPath(pair: Pair<Path, PathProperties>) {
        val paths = withState(this) { it.paths }.plus(pair)
        setState { copy(paths = paths) }
    }

    fun setCurrentPath(path: Path) {
        setState { copy(currentPath = path) }
    }

    fun clearUndo() {
        setState { copy(pathsUndone = listOf()) }
    }

    fun translatePaths(change: Offset) {
        withState { state ->
            val paths = state.paths.onEach { entry ->
                val path: Path = entry.first
                path.translate(change)
            }
            setState { copy(paths = paths) }
        }
    }

    fun redo() {
        withState { state ->
            val lastItem = state.paths.last()
            val lastPath = lastItem.first
            val lastPathProperty = lastItem.second
            val paths = state.paths.minus(lastItem)
            val pathsUndone = state.pathsUndone.plus(Pair(lastPath, lastPathProperty))
            setState { copy(paths = paths, pathsUndone = pathsUndone) }
        }
    }

    fun undo() {
        withState { state ->
            val lastPath = state.pathsUndone.last().first
            val lastPathProperty = state.pathsUndone.last().second
            val undoPaths = state.pathsUndone.minus(state.pathsUndone.last())
            val paths = state.paths.plus(Pair(lastPath, lastPathProperty))
            setState { copy(pathsUndone = undoPaths, paths = paths) }
        }
    }
}