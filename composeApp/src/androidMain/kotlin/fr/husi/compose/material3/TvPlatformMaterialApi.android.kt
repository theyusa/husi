package fr.husi.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.PrimaryScrollableTabRow as MaterialPrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow as MaterialPrimaryTabRow
import androidx.compose.material3.Tab as MaterialTab
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.LocalTextStyle as MaterialLocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.DrawerState as TvDrawerState
import androidx.tv.material3.DrawerValue as TvDrawerValue
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Card as TvCard
import androidx.tv.material3.CardDefaults as TvCardDefaults
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.ButtonDefaults as TvButtonDefaults
import androidx.tv.material3.Checkbox as TvCheckbox
import androidx.tv.material3.IconButton as TvIconButton
import androidx.tv.material3.IconButtonDefaults as TvIconButtonDefaults
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.RadioButton as TvRadioButton
import androidx.tv.material3.NavigationDrawer as TvNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem as TvNavigationDrawerItem
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Surface as TvSurface
import androidx.tv.material3.SurfaceDefaults as TvSurfaceDefaults
import androidx.tv.material3.Switch as TvSwitch
import androidx.tv.material3.rememberDrawerState as rememberTvDrawerState

internal val LocalTvNavigationDrawerScope =
    staticCompositionLocalOf<NavigationDrawerScope?> { null }

private class TVDrawerStateHolder(
    val state: TvDrawerState,
) : DrawerStateHolder {
    override val canCollapse: Boolean = false
    override val isOpen: Boolean
        get() = state.currentValue == TvDrawerValue.Open

    override suspend fun open() {
        state.setValue(TvDrawerValue.Open)
    }

    override suspend fun close() {
        state.setValue(TvDrawerValue.Closed)
    }
}

internal object TvPlatformMaterialApi : PlatformMaterialApi {
    @Composable
    override fun rememberDrawerStateHolder(): DrawerStateHolder {
        val drawerState = rememberTvDrawerState(TvDrawerValue.Open)
        return remember(drawerState) {
            TVDrawerStateHolder(drawerState)
        }
    }

    @Composable
    override fun NavigationDrawer(
        drawerStateHolder: DrawerStateHolder,
        drawerContent: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val tvDrawerStateHolder = drawerStateHolder as TVDrawerStateHolder
        TvNavigationDrawer(
            drawerState = tvDrawerStateHolder.state,
            drawerContent = { drawerValue ->
                CompositionLocalProvider(LocalTvNavigationDrawerScope provides this) {
                    val drawerWidth = when (drawerValue) {
                        TvDrawerValue.Closed -> NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
                        TvDrawerValue.Open -> NavigationDrawerItemDefaults.ExpandedDrawerItemWidth
                    }
                    TvSurface(
                        modifier = Modifier
                            .width(drawerWidth)
                            .fillMaxHeight(),
                    ) {
                        Column {
                            drawerContent()
                        }
                    }
                }
            },
            content = content,
        )
    }

    @Composable
    override fun DrawerItem(
        label: @Composable () -> Unit,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        icon: @Composable (() -> Unit)?,
    ) {
        val tvScope = LocalTvNavigationDrawerScope.current
        if (tvScope != null) with(tvScope) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(selected, hasFocus) {
                if (selected && hasFocus) {
                    focusRequester.requestFocus()
                }
            }
            TvNavigationDrawerItem(
                selected = selected,
                onClick = onClick,
                leadingContent = {
                    CompositionLocalProvider(
                        MaterialLocalContentColor provides androidx.tv.material3.LocalContentColor.current,
                    ) {
                        icon?.invoke()
                    }
                },
                modifier = modifier.focusRequester(focusRequester),
            ) {
                CompositionLocalProvider(
                    MaterialLocalContentColor provides androidx.tv.material3.LocalContentColor.current,
                ) {
                    label()
                }
            }
        } else {
            standardPlatformMaterialApi().DrawerItem(
                label = label,
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                icon = icon,
            )
        }
    }

    @Composable
    override fun Text(
        text: String,
        modifier: Modifier,
        color: Color,
        fontSize: TextUnit,
        fontStyle: FontStyle?,
        fontWeight: FontWeight?,
        fontFamily: FontFamily?,
        letterSpacing: TextUnit,
        textDecoration: TextDecoration?,
        textAlign: TextAlign?,
        lineHeight: TextUnit,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
        minLines: Int,
        onTextLayout: (TextLayoutResult) -> Unit,
        style: TextStyle?,
    ) {
        MaterialText(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style ?: MaterialLocalTextStyle.current,
        )
    }

    @Composable
    override fun Text(
        text: AnnotatedString,
        modifier: Modifier,
        color: Color,
        fontSize: TextUnit,
        fontStyle: FontStyle?,
        fontWeight: FontWeight?,
        fontFamily: FontFamily?,
        letterSpacing: TextUnit,
        textDecoration: TextDecoration?,
        textAlign: TextAlign?,
        lineHeight: TextUnit,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
        minLines: Int,
        onTextLayout: (TextLayoutResult) -> Unit,
        style: TextStyle?,
    ) {
        MaterialText(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style ?: MaterialLocalTextStyle.current,
        )
    }

    @Composable
    override fun Icon(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier,
        tint: Color?,
    ) {
        MaterialIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: MaterialLocalContentColor.current,
        )
    }

    @Composable
    override fun Icon(
        bitmap: ImageBitmap,
        contentDescription: String?,
        modifier: Modifier,
        tint: Color?,
    ) {
        MaterialIcon(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: MaterialLocalContentColor.current,
        )
    }

    @Composable
    override fun Icon(
        painter: Painter,
        contentDescription: String?,
        modifier: Modifier,
        tint: Color?,
    ) {
        MaterialIcon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: MaterialLocalContentColor.current,
        )
    }

    @Composable
    override fun Button(
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        shape: Shape?,
        containerColor: Color,
        contentColor: Color,
        border: BorderStroke?,
        contentPadding: PaddingValues?,
        content: @Composable RowScope.() -> Unit,
    ) {
        val buttonShape = shape ?: TvMaterialTheme.shapes.medium
        TvButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = if (shape == null) {
                TvButtonDefaults.shape()
            } else {
                TvButtonDefaults.shape(shape = buttonShape)
            },
            colors = if (containerColor == Color.Unspecified && contentColor == Color.Unspecified) {
                TvButtonDefaults.colors()
            } else {
                TvButtonDefaults.colors(
                    containerColor = if (containerColor == Color.Unspecified) {
                        TvMaterialTheme.colorScheme.primary
                    } else {
                        containerColor
                    },
                    contentColor = if (contentColor == Color.Unspecified) {
                        TvMaterialTheme.colorScheme.onPrimary
                    } else {
                        contentColor
                    },
                )
            },
            border = if (border == null) {
                TvButtonDefaults.border()
            } else {
                TvButtonDefaults.border(border = border.toTvBorder(buttonShape))
            },
            contentPadding = contentPadding ?: TvButtonDefaults.ContentPadding,
            content = content,
        )
    }

    @Composable
    override fun Checkbox(
        checked: Boolean,
        onCheckedChange: ((Boolean) -> Unit)?,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        TvCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }

    @Composable
    override fun Switch(
        checked: Boolean,
        onCheckedChange: ((Boolean) -> Unit)?,
        modifier: Modifier,
        enabled: Boolean,
        thumbContent: (@Composable () -> Unit)?,
    ) {
        TvSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            thumbContent = thumbContent,
        )
    }

    @Composable
    override fun RadioButton(
        selected: Boolean,
        onClick: (() -> Unit)?,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        TvRadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }

    @Composable
    override fun IconButton(
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        containerColor: Color,
        contentColor: Color,
        content: @Composable () -> Unit,
    ) {
        TvIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = TvIconButtonDefaults.colors(
                containerColor = if (containerColor == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                } else {
                    containerColor
                },
                contentColor = if (contentColor == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.onSurface
                } else {
                    contentColor
                },
            ),
            content = { content() },
        )
    }

    @Composable
    override fun Card(
        modifier: Modifier,
        elevated: Boolean,
        shape: Shape?,
        color: Color,
        contentColor: Color,
        tonalElevation: Dp,
        border: BorderStroke?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val cardShape = shape ?: TvMaterialTheme.shapes.medium
        TvSurface(
            modifier = modifier,
            tonalElevation = tonalElevation.takeIf { it != Dp.Unspecified } ?: if (elevated) 1.dp else 0.dp,
            shape = cardShape,
            colors = TvSurfaceDefaults.colors(
                containerColor = if (color == Color.Unspecified) {
                    if (elevated) TvMaterialTheme.colorScheme.surfaceVariant else TvMaterialTheme.colorScheme.surface
                } else {
                    color
                },
                contentColor = if (contentColor == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.onSurface
                } else {
                    contentColor
                },
            ),
            border = border.toTvBorder(cardShape),
        ) {
            Column(content = content)
        }
    }

    @Composable
    override fun Card(
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        elevated: Boolean,
        shape: Shape?,
        color: Color,
        contentColor: Color,
        tonalElevation: Dp,
        border: BorderStroke?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val cardShape = shape ?: TvMaterialTheme.shapes.medium
        TvCard(
            onClick = onClick,
            modifier = modifier,
            onLongClick = { },
            shape = if (shape == null) {
                TvCardDefaults.shape()
            } else {
                TvCardDefaults.shape(shape = cardShape)
            },
            colors = if (color == Color.Unspecified && contentColor == Color.Unspecified) {
                TvCardDefaults.colors()
            } else {
                TvCardDefaults.colors(
                    containerColor = if (color == Color.Unspecified) {
                        TvMaterialTheme.colorScheme.surface
                    } else {
                        color
                    },
                    contentColor = if (contentColor == Color.Unspecified) {
                        TvMaterialTheme.colorScheme.onSurface
                    } else {
                        contentColor
                    },
                )
            },
            scale = TvCardDefaults.scale(),
            border = if (border == null) {
                TvCardDefaults.border()
            } else {
                TvCardDefaults.border(border = border.toTvBorder(cardShape))
            },
            glow = TvCardDefaults.glow(),
            interactionSource = null,
            content = content,
        )
    }

    @Composable
    override fun Surface(
        modifier: Modifier,
        shape: Shape,
        color: Color,
        contentColor: Color,
        tonalElevation: Dp,
        shadowElevation: Dp,
        border: BorderStroke?,
        content: @Composable () -> Unit,
    ) {
        TvSurface(
            modifier = modifier,
            tonalElevation = tonalElevation.takeIf { it != Dp.Unspecified } ?: 0.dp,
            shape = shape,
            colors = TvSurfaceDefaults.colors(
                containerColor = if (color == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.surface
                } else {
                    color
                },
                contentColor = if (contentColor == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.onSurface
                } else {
                    contentColor
                },
            ),
            border = border.toTvBorder(shape),
        ) {
            content()
        }
    }

    @Composable
    override fun Surface(
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        shape: Shape,
        color: Color,
        contentColor: Color,
        tonalElevation: Dp,
        shadowElevation: Dp,
        border: BorderStroke?,
        content: @Composable () -> Unit,
    ) {
        TvSurface(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            tonalElevation = tonalElevation.takeIf { it != Dp.Unspecified } ?: 0.dp,
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (color == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.surface
                } else {
                    color
                },
                contentColor = if (contentColor == Color.Unspecified) {
                    TvMaterialTheme.colorScheme.onSurface
                } else {
                    contentColor
                },
            ),
            border = ClickableSurfaceDefaults.border(
                border = border.toTvBorder(shape),
            ),
        ) {
            content()
        }
    }

    @Composable
    override fun PrimaryTabRow(
        selectedTabIndex: Int,
        modifier: Modifier,
        containerColor: Color,
        content: @Composable () -> Unit,
    ) {
        MaterialPrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier,
            containerColor = if (containerColor == Color.Unspecified) {
                Color.Transparent
            } else {
                containerColor
            },
        ) {
            CompositionLocalProvider(
                LocalPlatformTabRowScope provides TvPlatformTabRowScope,
            ) {
                content()
            }
        }
    }

    @Composable
    override fun PrimaryScrollableTabRow(
        selectedTabIndex: Int,
        modifier: Modifier,
        containerColor: Color,
        edgePadding: Dp,
        content: @Composable () -> Unit,
    ) {
        MaterialPrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier,
            containerColor = if (containerColor == Color.Unspecified) {
                Color.Transparent
            } else {
                containerColor
            },
            edgePadding = edgePadding.takeIf { it != Dp.Unspecified } ?: 0.dp,
        ) {
            CompositionLocalProvider(
                LocalPlatformTabRowScope provides TvPlatformTabRowScope,
            ) {
                content()
            }
        }
    }

    private object TvPlatformTabRowScope : PlatformTabRowScope {
        @Composable
        override fun Tab(
            selected: Boolean,
            onClick: () -> Unit,
            modifier: Modifier,
            enabled: Boolean,
            text: (@Composable () -> Unit)?,
            icon: (@Composable () -> Unit)?,
        ) {
            MaterialTab(
                selected = selected,
                onClick = onClick,
                modifier = modifier.onFocusChanged {
                    if (it.isFocused) {
                        onClick()
                    }
                },
                enabled = enabled,
                text = text,
                icon = icon,
            )
        }
    }

    private fun BorderStroke?.toTvBorder(shape: Shape): Border {
        return if (this == null) {
            Border.None
        } else {
            Border(border = this, shape = shape)
        }
    }
}
