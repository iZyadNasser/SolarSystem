package com.zyad.solarsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolarSystemScreen()
        }
    }
}

// region Solar System screen

private val HeaderSlotHeight = 300.dp
private val EaseInOut = FastOutSlowInEasing

private const val COLLAPSE_FRACTION = 0.85f

private const val GLOBE_ART_FILL = 0.79f
private const val GLOBE_INTRO_DIAMETER = 1.53f
private const val GLOBE_INTRO_CENTER_Y = 0.8f
private const val GLOBE_HERO_DIAMETER = 0.6f
private const val GLOBE_HERO_CENTER_Y = 0.2f
private const val GLOBE_HERO_OPACITY = 0.5f

private const val CARD_FIRST_TOP = 1.25f      // first card starts this far down (off the bottom)
private const val CARD_STEP =
    0.3f           // vertical gap between consecutive cards while scrolling
private const val CARD_ACTIVE_TOP = 0.4f     // where the focused card settles
private const val CARD_STACK_TOP = 0.4f      // where cards pile up at the top
private val CardStackOffset =
    14.dp           // how far each stacked card peeks below the previous one

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

@Composable
private fun SolarSystemScreen() {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx() }
        val collapseDistance = heightPx * COLLAPSE_FRACTION
        val cardStepPx = heightPx * CARD_STEP
        val stackOffsetPx = with(density) { CardStackOffset.toPx() }
        val maxScrollPx =
            heightPx * (CARD_FIRST_TOP - CARD_ACTIVE_TOP) + (planets.size - 1) * cardStepPx

        LaunchedEffect(scrollState, collapseDistance) {
            val collapse = collapseDistance.roundToInt()
            snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
                val scroll = scrollState.value
                if (scrolling.not() && scroll in 1 until collapse) {
                    scrollState.animateScrollTo(if (scroll * 2 > collapse) collapse else 0)
                }
            }
        }

        SolarBackground(scrollState, collapseDistance)
        SolarGlobe(scrollState, heightPx, collapseDistance)
        PlanetStack(scrollState, heightPx, cardStepPx, stackOffsetPx)
        SolarHeaderSlot(scrollState, collapseDistance, Modifier.align(Alignment.TopCenter))
        SwipeHint(scrollState, heightPx, collapseDistance, Modifier.align(Alignment.BottomCenter))


        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)) {
            Spacer(Modifier.height(with(density) { (heightPx + maxScrollPx).toDp() }))
        }
    }
}

@Composable
private fun SolarBackground(scrollState: ScrollState, collapseDistance: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = SpaceBackgroundGradient)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val collapse = (scrollState.value / collapseDistance).coerceIn(0f, 1f)
                    alpha = EaseInOut.transform(collapse)
                }
                .drawBehind {
                    drawRect(brush = SolarBackgroundGradient)
                }
        )

        Image(
            painter = painterResource(R.drawable.im_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.66f)
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SolarGlobe(scrollState: ScrollState, heightPx: Float, collapseDistance: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.im_earth),
            contentDescription = stringResource(R.string.earth),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .aspectRatio(1f)
                .align(Alignment.Center)
                .graphicsLayer {
                    val scroll = scrollState.value.toFloat()
                    val collapse = EaseInOut.transform((scroll / collapseDistance).coerceIn(0f, 1f))

                    val diameter = lerp(GLOBE_INTRO_DIAMETER, GLOBE_HERO_DIAMETER, collapse)
                    val scale = diameter / GLOBE_ART_FILL
                    scaleX = scale
                    scaleY = scale

                    val centerY =
                        lerp(GLOBE_INTRO_CENTER_Y, GLOBE_HERO_CENTER_Y, collapse) * heightPx
                    translationY = (centerY - heightPx / 2f)
                    alpha = lerp(1f, GLOBE_HERO_OPACITY, collapse)
                },
        )
    }
}

/**
 * The header occupies one fixed slot above the globe. The Earth block and the Solar System block
 * share the slot and swap by sliding in opposite directions: Earth slides up and out while the
 * Solar System block slides down into place from above. The slot clips whatever is off-screen.
 */
@Composable
private fun SolarHeaderSlot(scrollState: ScrollState, collapseDistance: Float, modifier: Modifier) {
    val slotPx = with(LocalDensity.current) { HeaderSlotHeight.toPx() }

    // 0 = Earth shown, 1 = Solar System shown. The swap happens over the last 70% of the collapse
    // so "Earth" lingers, then the two blocks cross.
    fun swap(scroll: Int): Float {
        val collapse = (scroll / collapseDistance).coerceIn(0f, 1f)
        return EaseInOut.transform(((collapse - 0.3f) / 0.7f).coerceIn(0f, 1f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(HeaderSlotHeight)
            .clipToBounds(),
    ) {
        // Earth block: at rest during the intro, slides up and out as we collapse.
        HeaderBlock(
            title = stringResource(R.string.earth),
            titleStyle = EarthTitleStyle,
            subtitle = stringResource(R.string.a_long_blue_world_drifting_through_the_endless_dark),
            topPadding = 30.dp,
            titleBottomPadding = 4.dp,
            modifier = Modifier.graphicsLayer {
                val swap = swap(scrollState.value)
                translationY = -swap * slotPx
                alpha = 1f - swap
            },
        )
        // Solar block: starts above the slot and slides down into place.
        HeaderBlock(
            title = stringResource(R.string.our_solar_system),
            titleStyle = SolarTitleStyle,
            subtitle = stringResource(R.string.earth_is_only_one_small_part),
            topPadding = 20.dp,
            titleBottomPadding = 6.dp,
            modifier = Modifier.graphicsLayer {
                val swap = swap(scrollState.value)
                translationY = (swap - 1f) * slotPx
                alpha = swap
            },
        )
    }
}

@Composable
private fun HeaderBlock(
    title: String,
    titleStyle: TextStyle,
    subtitle: String,
    topPadding: Dp,
    titleBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = title,
            style = titleStyle,
            modifier = Modifier.padding(bottom = titleBottomPadding),
        )
        BasicText(text = subtitle, style = SubtitleStyle)
    }
}

@Composable
private fun PlanetStack(
    scrollState: ScrollState,
    heightPx: Float,
    cardStepPx: Float,
    stackOffsetPx: Float,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        planets.forEachIndexed { index, planet ->
            PlanetCard(
                planet = planet,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .zIndex(index.toFloat())
                    .graphicsLayer {
                        val naturalTop =
                            heightPx * CARD_FIRST_TOP + index * cardStepPx - scrollState.value
                        val stackTop = heightPx * CARD_STACK_TOP + index * stackOffsetPx
                        translationY = maxOf(naturalTop, stackTop)
                    },
            )
        }
    }
}

@Composable
private fun SwipeHint(
    scrollState: ScrollState,
    heightPx: Float,
    collapseDistance: Float,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
            .graphicsLayer {
                // Drift down and fade out early in the collapse.
                val collapse = (scrollState.value / collapseDistance).coerceIn(0f, 1f)
                translationY =
                    EaseInOut.transform((collapse / 0.4f).coerceIn(0f, 1f)) * heightPx * 0.18f
                alpha = 1f - (collapse / 0.3f).coerceIn(0f, 1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChevronUp(
            modifier = Modifier
                .graphicsLayer {
                    translationY = 8.dp.toPx()
                }
        )
        ChevronUp(
            color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier
                .graphicsLayer {
                    translationY = 4.dp.toPx()
                }
        )
        ChevronUp(
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 10.dp)
        )
        BasicText(
            text = stringResource(R.string.swipe_up_to_explore),
            style = SwipeStyle,
            modifier = Modifier
                .dropShadow(
                    shape = RectangleShape,
                    shadow = Shadow(
                        radius = 16.dp,
                        offset = DpOffset(x = 0.dp, y = 4.dp),
                        spread = 0.dp,
                        color = Color.White,
                        alpha = 0.44f
                    )
                )
        )
    }
}

@Composable
private fun ChevronUp(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Image(
        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_up),
        contentDescription = stringResource(R.string.swipe_up_to_explore),
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .size(24.dp)
    )
}

// endregion

// region Planet card

@Composable
fun PlanetCard(
    planet: Planet,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.5.dp,
                    color = Color(0xFF2F2E2E),
                    shape = CardShape
                )
                .background(Color(0xFF0B1223), CardShape)
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp),
        ) {
            PlanetHeader(planet)
            PlanetFacts(
                planet = planet,
                modifier = Modifier.graphicsLayer {
                    translationY = -14.dp.toPx()
                }
            )
        }
    }
}

@Composable
private fun PlanetHeader(planet: Planet, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(planet.thumbnail),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(112.dp)
                .graphicsLayer {
                    translationY = (-30).dp.toPx()
                }
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = stringResource(planet.name),
                style = NameStyle,
                modifier = Modifier
                    .dropShadow(
                        shape = RectangleShape,
                        shadow = Shadow(
                            radius = 12.dp,
                            offset = DpOffset(x = (-4).dp, y = 4.dp),
                            spread = 0.dp,
                            color = Color.White,
                            alpha = 0.08f
                        )
                    )
            )
            BasicText(
                text = stringResource(planet.nickName),
                style = NicknameStyle,
                modifier = Modifier
                    .dropShadow(
                        shape = RectangleShape,
                        shadow = Shadow(
                            radius = 12.dp,
                            offset = DpOffset(x = (-4).dp, y = 4.dp),
                            spread = 0.dp,
                            color = Color.White,
                            alpha = 0.04f
                        )
                    )
            )
        }
    }
}

@Composable
private fun PlanetFacts(planet: Planet, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 6.dp)) {
        FactsRow(
            modifier = Modifier.padding(bottom = 14.dp),
            start = { WeightFact(planet) },
            end = { DayFact(planet) },
        )
        FactDivider()
        FactsRow(
            modifier = Modifier.padding(top = 14.dp),
            start = { TemperatureFact(planet) },
            end = { AdditionalInfoFact(planet) },
        )
    }
}

@Composable
private fun FactsRow(
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        Cell(
            Modifier
                .weight(1f)
                .padding(end = 12.dp), start
        )
        FactColumnDivider()
        Cell(
            Modifier
                .weight(1f)
                .padding(start = 12.dp), end
        )
    }
}

@Composable
private fun Cell(modifier: Modifier, content: @Composable () -> Unit) {
    Row(modifier) { content() }
}

@Composable
private fun FactDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(FactDividerColor)
    )
}

@Composable
private fun FactColumnDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(0.5.dp)
            .background(FactDividerColor)
    )
}

@Composable
private fun WeightFact(planet: Planet) = Fact(
    icon = WeightIcon,
    label = stringResource(R.string.you_would_weigh),
    value = AnnotatedString(
        stringResource(R.string.weight_value, BASE_WEIGHT_KG, planet.person70KgRelativeWeightKg)
    ),
)

@Composable
private fun DayFact(planet: Planet) = Fact(
    icon = SunIcon,
    label = stringResource(R.string.one_day),
    value = AnnotatedString(
        stringResource(
            R.string.day_length_value,
            planet.dayLength.asDayLength(),
            stringResource(planet.dayLengthUnit),
        )
    ),
)

@Composable
private fun TemperatureFact(planet: Planet) = Fact(
    icon = TemperatureIcon,
    label = stringResource(R.string.temperature),
    value = planet.temperatureValue(),
)

@Composable
private fun AdditionalInfoFact(planet: Planet) = Fact(
    icon = InfoIcon,
    label = stringResource(R.string.additional_info),
    value = AnnotatedString(stringResource(planet.additionalInfo)),
)

@Composable
private fun Fact(icon: ImageVector, label: String, value: AnnotatedString) {
    Row(verticalAlignment = Alignment.Top) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(label, style = LabelStyle)
            BasicText(value, style = ValueStyle)
        }
    }
}

// endregion

// region Formatting

private const val BASE_WEIGHT_KG = 70

private fun Double.asDayLength(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

@Composable
private fun Planet.temperatureValue(): AnnotatedString {
    val temperature = stringResource(R.string.temperature_value, temperatureCelsius)
    val jacketHint = if (numberOfJackets > 0) {
        pluralStringResource(R.plurals.jacket_hint, numberOfJackets, numberOfJackets)
    } else {
        null
    }
    return buildAnnotatedString {
        append(temperature)
        if (jacketHint != null) {
            append(",")
            withStyle(HintSpanStyle) { append(" $jacketHint") }
        }
    }
}

// endregion

// region Fonts

val Rubik = FontFamily(
    Font(R.font.rubik_light, FontWeight.Light),
    Font(R.font.rubik_regular, FontWeight.Normal),
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_semi_bold, FontWeight.SemiBold),
    Font(R.font.rubik_bold, FontWeight.Bold),
    Font(R.font.rubik_extra_bold, FontWeight.ExtraBold),
    Font(R.font.rubik_black, FontWeight.Black)
)

val LilyScriptOne = FontFamily(
    Font(R.font.lily_script_one_regular, FontWeight.Normal)
)

// endregion

// region Theme

private val SpaceBackground = Color(0xFF0D0608)
private val SpaceBackgroundGradient = Brush.verticalGradient(
    0.00f to Color(0xFF000000).copy(alpha = 0f),
    0.25f to Color(0xFF060816),
    0.43f to Color(0xFF0F172A),
    1.00f to Color(0xFF020D3C)
)

private val SolarBackgroundGradient = Brush.verticalGradient(
    0.00f to Color(0xFF1B1640),
    0.35f to Color(0xFF121A3A),
    0.70f to Color(0xFF0B1228),
    1.00f to Color(0xFF050A1E)
)

private val EarthTitleStyle = TextStyle(
    fontFamily = Rubik,
    fontSize = 64.sp,
    fontWeight = FontWeight.Bold,
    color = Color.White.copy(alpha = 0.88f),
    textAlign = TextAlign.Center,
)
private val SolarTitleStyle = TextStyle(
    fontFamily = Rubik,
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    color = Color.White.copy(alpha = 0.88f),
    textAlign = TextAlign.Center,
)
private val SubtitleStyle = TextStyle(
    fontFamily = LilyScriptOne,
    fontSize = 16.sp,
    color = Color.White.copy(alpha = 0.88f),
    textAlign = TextAlign.Center,
)
private val SwipeStyle = TextStyle(
    fontFamily = Rubik,
    fontSize = 16.sp,
    color = Color.White,
    textAlign = TextAlign.Center,
)

private val CardShape = RoundedCornerShape(20.dp)
private val IconTint = Color.White.copy(alpha = 0.66f)

private val NameStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    color = Color.White.copy(alpha = 0.88f),
)
private val NicknameStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    color = Color.White.copy(alpha = 0.66f),
)
private val LabelStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    color = Color(0xFF7E8298),
)
private val ValueStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    color = Color(0xFFE6E8F0),
)
private val HintSpanStyle = SpanStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    color = Color(0xFF7E8298),
)

private val FactDividerColor = Color.White.copy(alpha = 0.09f)

// endregion

// region Icons (built in-code to keep everything in this file)

private fun spaceIcon(pathData: String): ImageVector =
    ImageVector.Builder(
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(IconTint),
            pathFillType = PathFillType.EvenOdd,
        )
    }.build()

private val SunIcon = spaceIcon(
    "M9.99626 1.66669C9.53603 1.66669 9.16293 2.03978 9.16293 2.50002C9.16293 2.96026 9.53603 3.33335 9.99626 3.33335H10.0037C10.464 3.33335 10.8371 2.96026 10.8371 2.50002C10.8371 2.03978 10.464 1.66669 10.0037 1.66669H9.99626Z " +
            "M15.299 3.86334C14.8388 3.86334 14.4657 4.23644 14.4657 4.69668C14.4657 5.15691 14.8388 5.53001 15.299 5.53001H15.3065C15.7667 5.53001 16.1398 5.15691 16.1398 4.69668C16.1398 4.23644 15.7667 3.86334 15.3065 3.86334H15.299Z " +
            "M4.69508 3.86375C4.23484 3.86375 3.86175 4.23684 3.86175 4.69708C3.86175 5.15732 4.23484 5.53041 4.69508 5.53041H4.70256C5.1628 5.53041 5.53589 5.15732 5.53589 4.69708C5.53589 4.23684 5.1628 3.86375 4.70256 3.86375H4.69508Z " +
            "M2.50001 9.16717C2.03977 9.16717 1.66667 9.54027 1.66667 10.0005C1.66667 10.4607 2.03977 10.8338 2.50001 10.8338H2.50749C2.96772 10.8338 3.34082 10.4607 3.34082 10.0005C3.34082 9.54027 2.96772 9.16717 2.50749 9.16717H2.50001Z " +
            "M17.4925 9.16717C17.0323 9.16717 16.6592 9.54027 16.6592 10.0005C16.6592 10.4607 17.0323 10.8338 17.4925 10.8338H17.5C17.9602 10.8338 18.3333 10.4607 18.3333 10.0005C18.3333 9.54027 17.9602 9.16717 17.5 9.16717H17.4925Z " +
            "M4.69508 14.47C4.23484 14.47 3.86175 14.8431 3.86175 15.3034C3.86175 15.7636 4.23484 16.1367 4.69508 16.1367H4.70256C5.1628 16.1367 5.53589 15.7636 5.53589 15.3034C5.53589 14.8431 5.1628 14.47 4.70256 14.47H4.69508Z " +
            "M15.2985 14.4704C14.8383 14.4704 14.4652 14.8435 14.4652 15.3038C14.4652 15.764 14.8383 16.1371 15.2985 16.1371H15.306C15.7662 16.1371 16.1393 15.764 16.1393 15.3038C16.1393 14.8435 15.7662 14.4704 15.306 14.4704H15.2985Z " +
            "M9.99675 16.6667C9.53651 16.6667 9.16342 17.0398 9.16342 17.5C9.16342 17.9603 9.53651 18.3334 9.99675 18.3334H10.0042C10.4645 18.3334 10.8376 17.9603 10.8376 17.5C10.8376 17.0398 10.4645 16.6667 10.0042 16.6667H9.99675Z " +
            "M10 5.20835C7.35364 5.20835 5.20834 7.35366 5.20834 10C5.20834 12.6464 7.35364 14.7917 10 14.7917C12.6464 14.7917 14.7917 12.6464 14.7917 10C14.7917 7.35366 12.6464 5.20835 10 5.20835ZM6.45834 10C6.45834 8.04401 8.044 6.45835 10 6.45835C11.956 6.45835 13.5417 8.04401 13.5417 10C13.5417 11.956 11.956 13.5417 10 13.5417C8.044 13.5417 6.45834 11.956 6.45834 10Z"
)

private val TemperatureIcon = spaceIcon(
    "M10.625 6.66669C10.625 6.32151 10.3452 6.04169 10 6.04169C9.65483 6.04169 9.37501 6.32151 9.37501 6.66669V11.9613C8.41313 12.2333 7.70834 13.1177 7.70834 14.1667C7.70834 15.4323 8.73436 16.4584 10 16.4584C11.2657 16.4584 12.2917 15.4323 12.2917 14.1667C12.2917 13.1177 11.5869 12.2333 10.625 11.9613V6.66669ZM8.95834 14.1667C8.95834 13.5914 9.42471 13.125 10 13.125C10.5753 13.125 11.0417 13.5914 11.0417 14.1667C11.0417 14.742 10.5753 15.2084 10 15.2084C9.42471 15.2084 8.95834 14.742 8.95834 14.1667Z " +
            "M10.0211 1.04169H9.97896C9.6084 1.04168 9.29904 1.04168 9.04507 1.05901C8.7807 1.07706 8.53097 1.11597 8.28888 1.2163C7.72768 1.44888 7.2818 1.89477 7.04921 2.45597C6.94888 2.69806 6.90998 2.94778 6.89193 3.21216C6.87459 3.46612 6.8746 3.77547 6.8746 4.14604L6.8746 10.5345C5.85526 11.4125 5.20834 12.7141 5.20834 14.1667C5.20834 16.8131 7.35365 18.9584 10 18.9584C12.6464 18.9584 14.7917 16.8131 14.7917 14.1667C14.7917 12.7141 14.1448 11.4125 13.1254 10.5345V4.14606C13.1254 3.77548 13.1254 3.46613 13.1081 3.21216C13.09 2.94778 13.0511 2.69806 12.9508 2.45597C12.7182 1.89477 12.2723 1.44888 11.7111 1.2163C11.469 1.11597 11.2193 1.07706 10.9549 1.05901C10.701 1.04168 10.3916 1.04168 10.0211 1.04169ZM8.76746 2.37106C8.83178 2.3444 8.9295 2.31981 9.1302 2.30611C9.33652 2.29203 9.60298 2.29169 10 2.29169C10.397 2.29169 10.6635 2.29203 10.8698 2.30611C11.0705 2.31981 11.1682 2.3444 11.2326 2.37106C11.4877 2.47678 11.6903 2.67945 11.796 2.93454C11.8227 2.99886 11.8473 3.09659 11.861 3.29729C11.8751 3.50361 11.8754 3.77006 11.8754 4.16709V10.8334C11.8754 11.0299 11.9679 11.215 12.125 11.333C12.9864 11.9803 13.5417 13.0085 13.5417 14.1667C13.5417 16.1227 11.956 17.7084 10 17.7084C8.044 17.7084 6.45834 16.1227 6.45834 14.1667C6.45834 13.0085 7.01358 11.9803 7.87503 11.333C8.03216 11.215 8.1246 11.0299 8.1246 10.8334V4.16709C8.1246 3.77006 8.12494 3.50361 8.13903 3.29729C8.15273 3.09659 8.17731 2.99886 8.20397 2.93454C8.30969 2.67945 8.51237 2.47677 8.76746 2.37106Z"
)

private val WeightIcon = spaceIcon(
    "M7.8 4a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0Z " +
            "M8.8 4a1.2 1.2 0 1 1 2.4 0a1.2 1.2 0 1 1 -2.4 0Z " +
            "M6.5 6.2L13.5 6.2L15 16L5 16Z"
)

private val InfoIcon = spaceIcon(
    "M1.667 10a8.333 8.333 0 1 0 16.666 0a8.333 8.333 0 1 0 -16.666 0Z " +
            "M2.917 10a7.083 7.083 0 1 1 14.166 0a7.083 7.083 0 1 1 -14.166 0Z " +
            "M9.05 5.83a0.95 0.95 0 1 0 1.9 0a0.95 0.95 0 1 0 -1.9 0Z " +
            "M9.05 8.33h1.9v6.04h-1.9Z"
)

// endregion

// region Model & data

data class Planet(
    @param:StringRes val name: Int,
    @param:StringRes val nickName: Int,
    @param:DrawableRes val thumbnail: Int,
    @param:IntRange(from = 0) val person70KgRelativeWeightKg: Int,
    val dayLength: Double,
    @param:StringRes val dayLengthUnit: Int = R.string.hours,
    val temperatureCelsius: Int,
    @param:IntRange(from = 0, to = 3) val numberOfJackets: Int,
    @param:StringRes val additionalInfo: Int
)

val planets = listOf(
    Planet(
        name = R.string.saturn,
        nickName = R.string.saturn_nickname,
        thumbnail = R.drawable.im_saturn,
        person70KgRelativeWeightKg = 74,
        dayLength = 10.7,
        temperatureCelsius = -178,
        numberOfJackets = 1,
        additionalInfo = R.string.saturn_info
    ),
    Planet(
        name = R.string.mars,
        nickName = R.string.mars_nickname,
        thumbnail = R.drawable.im_mars,
        person70KgRelativeWeightKg = 27,
        dayLength = 24.6,
        temperatureCelsius = -65,
        numberOfJackets = 1,
        additionalInfo = R.string.mars_info
    ),
    Planet(
        name = R.string.mercury,
        nickName = R.string.mercury_nickname,
        thumbnail = R.drawable.im_mercury,
        person70KgRelativeWeightKg = 26,
        dayLength = 1408.0,
        temperatureCelsius = -167,
        numberOfJackets = 0,
        additionalInfo = R.string.mercury_info
    ),
    Planet(
        name = R.string.venus,
        nickName = R.string.venus_nickname,
        thumbnail = R.drawable.im_venus,
        person70KgRelativeWeightKg = 63,
        dayLength = 243.0,
        dayLengthUnit = R.string.days,
        temperatureCelsius = 465,
        numberOfJackets = 0,
        additionalInfo = R.string.venus_info
    ),
    Planet(
        name = R.string.jupiter,
        nickName = R.string.jupiter_nickname,
        thumbnail = R.drawable.im_jupiter,
        person70KgRelativeWeightKg = 177,
        dayLength = 9.9,
        temperatureCelsius = -110,
        numberOfJackets = 1,
        additionalInfo = R.string.jupiter_info
    ),
    Planet(
        name = R.string.uranus,
        nickName = R.string.uranus_nickname,
        thumbnail = R.drawable.im_uranus,
        person70KgRelativeWeightKg = 62,
        dayLength = 17.0,
        temperatureCelsius = -224,
        numberOfJackets = 3,
        additionalInfo = R.string.uranus_info
    ),
    Planet(
        name = R.string.neptune,
        nickName = R.string.neptune_nickname,
        thumbnail = R.drawable.im_neptune,
        person70KgRelativeWeightKg = 79,
        dayLength = 16.0,
        temperatureCelsius = -214,
        numberOfJackets = 3,
        additionalInfo = R.string.neptune_info
    ),
)

// endregion

@Preview(backgroundColor = 0xFF0A0E1F, showBackground = true)
@Composable
private fun PlanetCardPreview() {
    PlanetCard(
        planet = planets.first(),
        modifier = Modifier.padding(20.dp),
    )
}


@Preview(showSystemUi = true)
@Composable
private fun SolarSystemScreenPreview() {
    SolarSystemScreen()
}