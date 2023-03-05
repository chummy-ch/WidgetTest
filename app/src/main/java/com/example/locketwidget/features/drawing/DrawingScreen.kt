package com.example.locketwidget.features.drawing

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.locketwidget.R
import com.example.locketwidget.core.LocalCameraSize
import com.example.locketwidget.core.LocalPhotoScope
import com.example.locketwidget.data.ScreenItem
import com.example.locketwidget.screens.main.CameraOutput
import com.example.locketwidget.screens.preview.PreviewViewModel
import com.example.locketwidget.ui.*
import com.example.locketwidget.ui.theme.ColorPicker
import com.example.locketwidget.ui.theme.Orange200
import kotlin.math.roundToInt

@Composable
fun DrawingScreen(navController: NavController) {
    val previewViewModel: PreviewViewModel = mavericksViewModel(scope = LocalPhotoScope.current)
    val output = previewViewModel.collectAsState { it.cameraOutput }.value.invoke()
    val photoPath = (output as? CameraOutput.Photo)?.path
    DrawingFeature(
        photoPath = photoPath,
        saveAction = { paths, size ->
            previewViewModel.draw(paths, size)
            navController.popBackStack()
        },
        popBackStack = navController::popBackStack,
        lifeScope = LocalPhotoScope.current
    )
}

@Composable
fun DrawingMomentScreen(navController: NavController) {
    val viewModel: DrawingMomentViewModel = mavericksViewModel()
    val updatePhotoPath = viewModel.collectAsState { it.savedImagePath }.value
    DrawingFeature(
        photoPath = null,
        saveAction = viewModel::save,
        popBackStack = navController::popBackStack,
        lifeScope = LocalLifecycleOwner.current
    )

    LaunchedEffect(updatePhotoPath) {
        if (updatePhotoPath is Success) {
            val path = updatePhotoPath.invoke()
            val route = "${ScreenItem.PhotoPreview.route}?${ScreenItem.PREVIEW_ARG}=$path," +
                    "${ScreenItem.PREVIEW_TYPE_ARG}=${true}," +
                    "${ScreenItem.EMOJIS_ARG}=${null}"
            navController.navigate(route) {
                popUpTo(ScreenItem.Locket.route)
            }
        }
    }
}

@Composable
private fun DrawingFeature(
    photoPath: String?,
    saveAction: (List<Pair<Path, PathProperties>>, Int) -> Unit,
    popBackStack: () -> Unit,
    lifeScope: LifecycleOwner
) {
    val viewModel: DrawingViewModel = mavericksViewModel(scope = lifeScope)
    val paths = viewModel.collectAsState { it.paths }.value
    val pathsUndone = viewModel.collectAsState { it.pathsUndone }.value
    val currentPath = viewModel.collectAsState { it.currentPath }.value

    var motionEvent by remember { mutableStateOf(MotionEvent.Idle) }
    var currentPosition by remember { mutableStateOf(Offset.Unspecified) }
    var previousPosition by remember { mutableStateOf(Offset.Unspecified) }
    var drawMode by remember { mutableStateOf(DrawMode.Draw) }
    var currentPathProperty by remember { mutableStateOf(PathProperties()) }

    var firstPosition by remember { mutableStateOf(Offset.Unspecified) }

    var drawingOffsetYRange by remember { mutableStateOf(IntRange(0, 0)) }
    val photoSize = LocalDensity.current.run { LocalCameraSize.current.size.toPx() }
    val drawingOffsetXRange by remember {
        derivedStateOf {
            IntRange(
                0,
                photoSize.roundToInt()
            )
        }
    }

    LocketScreen(
        topBar = {
            TopBar(currentScreenItem = ScreenItem.Drawing) {
                popBackStack()
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.photo_drawing_corner_radius)))
                    .size(LocalCameraSize.current.size)
                    .onSizeChanged { size ->
                        drawingOffsetYRange = IntRange(0, size.height)
                    }
            ) {

                if (photoPath != null) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = rememberAsyncImagePainter(photoPath),
                        contentDescription = null
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .dragMotionEvent(
                            onDragStart = { pointerInputChange ->
                                motionEvent = MotionEvent.Down
                                currentPosition = pointerInputChange.position
                                firstPosition = currentPosition
                                if (pointerInputChange.pressed != pointerInputChange.previousPressed) pointerInputChange.consume()
                            },
                            onDrag = { pointerInputChange ->

                                if (drawingOffsetYRange.contains(pointerInputChange.position.y.roundToInt()) &&
                                    drawingOffsetXRange.contains(pointerInputChange.position.x.roundToInt())
                                ) {
                                    motionEvent = MotionEvent.Move
                                    currentPosition = pointerInputChange.position

                                    if (drawMode == DrawMode.Touch) {
                                        val change = pointerInputChange.positionChange()
                                        viewModel.translatePaths(change)
                                    }
                                    if (pointerInputChange.positionChange() != Offset.Zero) pointerInputChange.consume()
                                }
                            },
                            onDragEnd = { pointerInputChange ->
                                motionEvent = MotionEvent.Up
                                if (pointerInputChange.pressed != pointerInputChange.previousPressed) pointerInputChange.consume()
                            }
                        ),
                    onDraw = {
                        when (motionEvent) {
                            MotionEvent.Down -> {
                                onDown(
                                    drawMode = drawMode,
                                    currentPath = currentPath,
                                    currentPosition = currentPosition,
                                    setCurrentPath = viewModel::setCurrentPath
                                )
                                currentPathProperty = currentPathProperty.copy(eraseMode = drawMode == DrawMode.Erase)
                                previousPosition = currentPosition
                            }
                            MotionEvent.Move -> {
                                onMove(
                                    drawMode = drawMode,
                                    firstPosition = firstPosition,
                                    previousPosition = previousPosition,
                                    currentPosition = currentPosition,
                                    setCurrentPath = viewModel::setCurrentPath,
                                    currentPath = currentPath
                                )
                                previousPosition = currentPosition
                            }
                            MotionEvent.Up -> {
                                onUp(
                                    drawMode = drawMode,
                                    currentPath = currentPath,
                                    currentPosition = currentPosition,
                                    currentPathProperty = currentPathProperty,
                                    setCurrentPath = viewModel::setCurrentPath,
                                    addPath = viewModel::addPath,
                                    restoreCurrentPathProperties = {
                                        currentPathProperty = PathProperties(
                                            strokeWidth = currentPathProperty.strokeWidth,
                                            color = currentPathProperty.color,
                                            strokeCap = currentPathProperty.strokeCap,
                                            strokeJoin = currentPathProperty.strokeJoin,
                                            eraseMode = currentPathProperty.eraseMode
                                        )
                                    },
                                    restoreParams = {
                                        currentPosition = Offset.Unspecified
                                        previousPosition = currentPosition
                                        firstPosition = Offset.Unspecified
                                        motionEvent = MotionEvent.Idle
                                    },
                                    clearUndo = viewModel::clearUndo
                                )
                            }
                            else -> Unit
                        }

                        drawPaths(
                            currentPath = currentPath,
                            motionEvent = motionEvent,
                            currentPathProperty = currentPathProperty,
                            paths = paths,
                            drawContext = drawContext
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isBrushMenuShown = remember { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            val position = remember { mutableStateOf(IntOffset.Zero) }
            val hue = remember {
                mutableStateOf(0f)
            }

            if (isBrushMenuShown.value) {
                BrushSettingsDialog(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = interactionSource) { isBrushMenuShown.value = false },
                    position = position.value,
                    onDismiss = { isBrushMenuShown.value = false },
                    selectHue = {
                        if (hue.value + it in 0.0..360.0) {
                            hue.value += it
                            val color = Color.hsv(hue = hue.value, saturation = 1f, value = 1f)
                            currentPathProperty = currentPathProperty.copy(color = color)
                        }
                    },
                    hue = hue.value,
                    changeBrushSize = {
                        currentPathProperty = currentPathProperty.copy(strokeWidth = it)
                    },
                    pathProperties = currentPathProperty
                )
            }

            DrawingMenu(
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = dimensionResource(R.dimen.screen_elements_padding))
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        val positionInRoot = layoutCoordinates.positionInRoot()
                        position.value = IntOffset(0, positionInRoot.y.roundToInt())
                    },
                onRedo = {
                    if (pathsUndone.isNotEmpty()) {
                        viewModel.undo()
                    }
                },
                onUndo = {
                    if (paths.isNotEmpty()) {
                        viewModel.redo()
                    }
                },
                changeDrawMode = {
                    motionEvent = MotionEvent.Idle
                    drawMode = it
                    currentPathProperty = currentPathProperty.copy(eraseMode = drawMode == DrawMode.Erase)
                },
                currentDrawMode = drawMode,
                brushSettings = { isBrushMenuShown.value = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                text = stringResource(R.string.photo_editor_save_button),
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.screen_elements_padding))
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_size))
            ) {
                saveAction(paths, drawingOffsetYRange.last)
            }
        }
    }
}

private fun onDown(
    drawMode: DrawMode,
    currentPath: Path,
    currentPosition: Offset,
    setCurrentPath: (Path) -> Unit
) {
    if (drawMode != DrawMode.Touch) {
        setCurrentPath(
            currentPath.apply {
                moveTo(currentPosition.x, currentPosition.y)
            }
        )
    }
}

private fun onMove(
    drawMode: DrawMode,
    setCurrentPath: (Path) -> Unit,
    firstPosition: Offset,
    currentPosition: Offset,
    currentPath: Path,
    previousPosition: Offset
) {
    if (drawMode == DrawMode.Line) {
        setCurrentPath(
            Path().apply {
                moveTo(firstPosition.x, firstPosition.y)
                lineTo(currentPosition.x, currentPosition.y)
            }
        )

    } else if (drawMode != DrawMode.Touch) {
        setCurrentPath(
            currentPath.apply {
                quadraticBezierTo(
                    previousPosition.x,
                    previousPosition.y,
                    (previousPosition.x + currentPosition.x) / 2,
                    (previousPosition.y + currentPosition.y) / 2

                )
            }
        )
    }
}

private fun onUp(
    drawMode: DrawMode,
    currentPath: Path,
    currentPathProperty: PathProperties,
    currentPosition: Offset,
    setCurrentPath: (Path) -> Unit,
    addPath: (Pair<Path, PathProperties>) -> Unit,
    restoreCurrentPathProperties: () -> Unit,
    clearUndo: () -> Unit,
    restoreParams: () -> Unit
) {
    if (drawMode != DrawMode.Touch) {
        setCurrentPath(
            currentPath.apply {
                lineTo(currentPosition.x, currentPosition.y)
            }
        )

        // Pointer is up save current path
        addPath(Pair(currentPath, currentPathProperty))
        setCurrentPath(Path())

        // Create new instance of path properties to have new path and properties
        // only for the one currently being drawn
        restoreCurrentPathProperties()
    }

    if (drawMode != DrawMode.Touch) {
        clearUndo()
    }

    restoreParams()
}

private fun DrawScope.drawPaths(
    currentPath: Path,
    motionEvent: MotionEvent,
    currentPathProperty: PathProperties,
    paths: List<Pair<Path, PathProperties>>,
    drawContext: DrawContext
) {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)

        paths.forEach {

            val path = it.first
            val property = it.second

            if (!property.eraseMode) {
                drawPath(
                    color = property.color,
                    path = path,
                    style = Stroke(
                        width = property.strokeWidth,
                        cap = property.strokeCap,
                        join = property.strokeJoin
                    )
                )
            } else {
                drawPath(
                    color = Color.Transparent,
                    path = path,
                    style = Stroke(
                        width = currentPathProperty.strokeWidth,
                        cap = currentPathProperty.strokeCap,
                        join = currentPathProperty.strokeJoin
                    ),
                    blendMode = BlendMode.Clear
                )
            }
        }

        if (motionEvent != MotionEvent.Idle) {

            if (!currentPathProperty.eraseMode) {
                drawPath(
                    color = currentPathProperty.color,
                    path = currentPath,
                    style = Stroke(
                        width = currentPathProperty.strokeWidth,
                        cap = currentPathProperty.strokeCap,
                        join = currentPathProperty.strokeJoin
                    )
                )
            } else {
                drawPath(
                    color = Color.Transparent,
                    path = currentPath,
                    style = Stroke(
                        width = currentPathProperty.strokeWidth,
                        cap = currentPathProperty.strokeCap,
                        join = currentPathProperty.strokeJoin
                    ),
                    blendMode = BlendMode.Clear
                )
            }
        }
        restoreToCount(checkPoint)
    }
}

@Composable
private fun BrushSettingsDialog(
    modifier: Modifier = Modifier,
    position: IntOffset,
    onDismiss: () -> Unit,
    pathProperties: PathProperties,
    hue: Float,
    selectHue: (Float) -> Unit,
    changeBrushSize: (Float) -> Unit
) {
    var menuHight by remember {
        mutableStateOf(0)
    }
    val dialogBottomPadding = LocalDensity.current.run { dimensionResource(R.dimen.brush_settings_menu_horizontal_padding).toPx() }

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = modifier) {
            FloatingActionMenu(
                modifier = Modifier
                    .onSizeChanged { menuHight = it.height }
                    .offset {
                        val bottomPadding = position.y - menuHight - dialogBottomPadding
                        position.copy(y = bottomPadding.roundToInt())
                    }
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    ColorSwitcher(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(),
                        hue = hue,
                        selectHue = selectHue
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(30.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .size(pathProperties.strokeWidth.dp)
                                    .background(pathProperties.color)
                            )
                        }
                        Slider(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .fillMaxWidth(),
                            value = pathProperties.strokeWidth,
                            onValueChange = changeBrushSize,
                            valueRange = 4f..30f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwitcher(
    modifier: Modifier = Modifier,
    hue: Float,
    selectHue: (Float) -> Unit
) {
    val cameraSize = LocalCameraSize.current.size
    val switcherWidth = remember { mutableStateOf(cameraSize.value.roundToInt()) }
    val animatedOffsetX = animateIntAsState(
        targetValue = hue.roundToInt() * switcherWidth.value / 360,
    )
    val density = LocalDensity.current.density

    Box {
        Box(
            modifier = modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .onSizeChanged { switcherWidth.value = it.width }
                .background(brush = Brush.horizontalGradient(colors = ColorPicker.colors))
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        animatedOffsetX.value - 10 * density.roundToInt(),
                        0
                    )
                }
                .clip(CircleShape)
                .size(20.dp)
                .background(color = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { x ->
                            val currentHue = x * 360 / switcherWidth.value
                            val newValue = hue + currentHue
                            if (newValue in 0.0..360.0) {
                                selectHue(currentHue)
                            }
                        }
                    )
                    .clip(CircleShape)
                    .size(16.dp)
                    .background(Orange200)
            )
        }
    }
}

@Composable
private fun DrawingMenu(
    modifier: Modifier = Modifier,
    currentDrawMode: DrawMode,
    changeDrawMode: (DrawMode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    brushSettings: () -> Unit
) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween
    ) {

        RoundedCornerButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = if (currentDrawMode == DrawMode.Touch) R.drawable.ic_move_gradient else R.drawable.ic_move,
        ) {
            changeDrawMode(DrawMode.Touch)
        }

        RoundedCornerButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = if (currentDrawMode == DrawMode.Line) R.drawable.ic_line_gradient else R.drawable.ic_line
        ) {
            changeDrawMode(DrawMode.Line)
            brushSettings()
        }

        RoundedCornerButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = if (currentDrawMode == DrawMode.Draw) R.drawable.ic_editor_gradient else R.drawable.ic_edit
        ) {
            changeDrawMode(DrawMode.Draw)
            brushSettings()
        }

        RoundedCornerButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_undo,
            action = onUndo
        )

        RoundedCornerButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.button_size)),
            drawableRes = R.drawable.ic_redo,
            action = onRedo
        )
    }
}


fun Modifier.dragMotionEvent(
    onDragStart: (PointerInputChange) -> Unit = {},
    onDrag: (PointerInputChange) -> Unit = {},
    onDragEnd: (PointerInputChange) -> Unit = {}
) = this.then(
    Modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                awaitDragMotionEvent(onDragStart, onDrag, onDragEnd)
            }
        }
    }
)

suspend fun AwaitPointerEventScope.awaitDragMotionEvent(
    onDragStart: (PointerInputChange) -> Unit = {},
    onDrag: (PointerInputChange) -> Unit = {},
    onDragEnd: (PointerInputChange) -> Unit = {}
) {
    // Wait for at least one pointer to press down, and set first contact position
    val down: PointerInputChange = awaitFirstDown()
    onDragStart(down)

    var pointer = down

    // 🔥 Waits for drag threshold to be passed by pointer
    // or it returns null if up event is triggered
    val change: PointerInputChange? =
        awaitTouchSlopOrCancellation(down.id) { change: PointerInputChange, over: Offset ->
            // 🔥🔥 If consumePositionChange() is not consumed drag does not
            // functfun Modifier.dragMotionEvent(
            //    onDragStart: (PointerInputChange) -> Unit = {},
            //    onDrag: (PointerInputChange) -> Unit = {},
            //    onDragEnd: (PointerInputChange) -> Unit = {}
            //) = this.then(
            //    Modifier.pointerInput(Unit) {
            //        forEachGesture {
            //            awaitPointerEventScope {
            //                awaitDragMotionEvent(onDragStart, onDrag, onDragEnd)
            //            }
            //        }
            //    }
            //)ion properly.
            // Consuming position change causes change.positionChanged() to return false.
            if (change.positionChange() != Offset.Zero) change.consume()
        }

    if (change != null) {
        // 🔥 Calls  awaitDragOrCancellation(pointer) in a while loop
        drag(change.id) { pointerInputChange: PointerInputChange ->
            pointer = pointerInputChange
            onDrag(pointer)
        }

        // All of the pointers are up
        onDragEnd(pointer)
    } else {
        // Drag threshold is not passed and last pointer is up
        onDragEnd(pointer)
    }
}