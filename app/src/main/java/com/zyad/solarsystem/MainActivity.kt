package com.zyad.solarsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.CancellationException
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
private val EaseInOut = LinearEasing

private const val COLLAPSE_FRACTION = 0.85f

private const val GLOBE_ART_FILL = 0.79f
private const val GLOBE_INTRO_DIAMETER = 1.53f
private const val GLOBE_INTRO_CENTER_Y = 0.8f
private const val GLOBE_HERO_DIAMETER = 0.6f
private const val GLOBE_HERO_CENTER_Y = 0.2f
private const val GLOBE_HERO_OPACITY = 0.5f

private const val CARD_FIRST_TOP = 1.25f      // first card starts this far down (off the bottom)
private val CARD_STEP =
    32.dp                 // constant vertical gap between consecutive cards while scrolling
private const val CARD_ACTIVE_TOP = 0.4f     // where the focused card settles
private const val CARD_STACK_TOP = 0.4f      // where cards pile up at the top
private val CardStackOffset =
    14.dp           // how far each stacked card peeks below the previous one
private const val CARD_IMAGE_FADED_ALPHA =
    0.32f // settled card's planet image fades to this as the next card scrolls in
private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

@Composable
private fun SolarSystemScreen() {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx() }
        val collapseDistance = heightPx * COLLAPSE_FRACTION
        val cardGapPx = with(density) { CARD_STEP.toPx() }
        val stackOffsetPx = with(density) { CardStackOffset.toPx() }

        val cardHeights =
            remember { mutableStateListOf<Int>().apply { repeat(planets.size) { add(0) } } }
        val lastCardTopPx =
            (0 until planets.lastIndex).sumOf { cardHeights[it] } + planets.lastIndex * cardGapPx
        val maxScrollPx =
            heightPx * (CARD_FIRST_TOP - CARD_ACTIVE_TOP) + lastCardTopPx

        LaunchedEffect(scrollState, collapseDistance) {
            val collapse = collapseDistance.roundToInt()
            snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
                val scroll = scrollState.value
                if (scrolling.not() && scroll in 1 until collapse) {
                    try {
                        scrollState.animateScrollTo(if (scroll * 2 > collapse) collapse else 0)
                    } catch (_: CancellationException) {
                    }
                }
            }
        }

        SolarBackground(scrollState, collapseDistance)
        SolarGlobe(scrollState, heightPx, collapseDistance)
        PlanetStack(scrollState, heightPx, cardGapPx, stackOffsetPx, cardHeights)
        SolarHeaderSlot(scrollState, collapseDistance, Modifier.align(Alignment.TopCenter))
        SwipeHint(scrollState, heightPx, collapseDistance, Modifier.align(Alignment.BottomCenter))


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
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

@Composable
private fun SolarHeaderSlot(scrollState: ScrollState, collapseDistance: Float, modifier: Modifier) {
    val slotPx = with(LocalDensity.current) { HeaderSlotHeight.toPx() }

    fun swap(scroll: Int): Float {
        val collapse = (scroll / collapseDistance).coerceIn(0f, 1f)
        return EaseInOut.transform(((collapse - 0.1f) / 0.9f).coerceIn(0f, 1f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(HeaderSlotHeight)
            .clipToBounds(),
    ) {
        HeaderBlock(
            title = stringResource(R.string.earth),
            titleStyle = EarthTitleStyle,
            subtitle = stringResource(R.string.a_long_blue_world_drifting_through_the_endless_dark),
            topPadding = 56.dp,
            modifier = Modifier.graphicsLayer {
                val swap = swap(scrollState.value)
                translationY = -swap * slotPx
                alpha = 1f - swap
            },
        )

        HeaderBlock(
            title = stringResource(R.string.our_solar_system),
            titleStyle = SolarTitleStyle,
            subtitle = stringResource(R.string.earth_is_only_one_small_part),
            topPadding = 98.dp,
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
            modifier = Modifier.padding(bottom = 4.dp),
        )
        BasicText(text = subtitle, style = SubtitleStyle)
    }
}

@Composable
private fun PlanetStack(
    scrollState: ScrollState,
    heightPx: Float,
    cardGapPx: Float,
    stackOffsetPx: Float,
    cardHeights: SnapshotStateList<Int>,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        planets.forEachIndexed { index, planet ->
            PlanetCard(
                planet = planet,
                imageAlpha = {
                    val precedingPx = (0 until index).sumOf { cardHeights[it] }
                    val naturalTop =
                        heightPx * CARD_FIRST_TOP + precedingPx + index * cardGapPx - scrollState.value
                    val stackTop = heightPx * CARD_STACK_TOP + index * stackOffsetPx
                    val span = (cardHeights[index] + cardGapPx - stackOffsetPx).coerceAtLeast(1f)
                    val progress = ((stackTop - naturalTop) / span).coerceIn(0f, 1f)
                    lerp(1f, CARD_IMAGE_FADED_ALPHA, progress)
                },
                cardAlpha = {
                    val precedingPx = (0 until index).sumOf { cardHeights[it] }
                    val naturalTop =
                        heightPx * CARD_FIRST_TOP + precedingPx + index * cardGapPx - scrollState.value
                    val stackTop = heightPx * CARD_STACK_TOP + index * stackOffsetPx
                    val span = (cardHeights[index] + cardGapPx - stackOffsetPx).coerceAtLeast(1f)
                    // Driven by THIS card's rise toward its own settle (unlike imageAlpha, which tracks
                    // the next card): 0 one step before this card settles, 1 once settled and after.
                    val progress = ((stackTop - naturalTop) / span + 1f).coerceIn(0f, 1f)
                    lerp(CARD_IMAGE_FADED_ALPHA, 1f, progress)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .zIndex(index.toFloat())
                    .graphicsLayer {
                        val precedingPx = (0 until index).sumOf { cardHeights[it] }
                        val naturalTop =
                            heightPx * CARD_FIRST_TOP + precedingPx + index * cardGapPx - scrollState.value
                        val stackTop = heightPx * CARD_STACK_TOP + index * stackOffsetPx
                        translationY = maxOf(naturalTop, stackTop)
                    }
                    .onSizeChanged { cardHeights[index] = it.height },
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
    imageAlpha: () -> Float = { 1f },
    cardAlpha: () -> Float = { 1f },
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
                .drawBehind {
                    drawRect(
                        Color(0xFF0B1223).copy(alpha = cardAlpha())
                    )
                }
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp),
        ) {
            PlanetHeader(
                planet = planet,
                imageAlpha = imageAlpha
            )
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
private fun PlanetHeader(
    planet: Planet,
    modifier: Modifier = Modifier,
    imageAlpha: () -> Float = { 1f },
) {
    Row(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(planet.thumbnail),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(end = 8.dp)
                .dropShadow(
                    shape = CardShape,
                    shadow = Shadow(
                        radius = 100.dp,
                        offset = DpOffset(x = 0.dp, y = (-16).dp),
                        spread = 0.dp,
                        color = planet.color,
                        alpha = 0.3f,
                    )
                )
                .size(112.dp)
                .graphicsLayer {
                    translationY = (-30).dp.toPx()
                    alpha = imageAlpha()
                }
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            modifier = Modifier.padding(bottom = 16.dp),
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
                .padding(end = 16.dp), start
        )
        FactColumnDivider()
        Cell(
            Modifier
                .weight(1f)
                .padding(start = 16.dp), end
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
    icon = ImageVector.vectorResource(R.drawable.ic_weight),
    label = stringResource(R.string.you_would_weigh),
    value = AnnotatedString(
        stringResource(R.string.weight_value, BASE_WEIGHT_KG, planet.person70KgRelativeWeightKg)
    ),
)

@Composable
private fun DayFact(planet: Planet) = Fact(
    icon = ImageVector.vectorResource(R.drawable.ic_sun),
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
    icon = ImageVector.vectorResource(R.drawable.ic_thermo),
    label = stringResource(R.string.temperature),
    value = planet.temperatureValue(),
)

@Composable
private fun AdditionalInfoFact(planet: Planet) = Fact(
    icon = ImageVector.vectorResource(R.drawable.ic_info),
    label = stringResource(R.string.additional_info),
    value = AnnotatedString(stringResource(planet.additionalInfo)),
)

@Composable
private fun Fact(icon: ImageVector, label: String, value: AnnotatedString) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(IconTint),
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    fontSize = 12.sp,
    color = Color.White.copy(alpha = 0.66f),
)
private val ValueStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    color = Color.White.copy(alpha = 0.88f),
)
private val HintSpanStyle = SpanStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    color = Color(0xFF7E8298),
)

private val FactDividerColor = Color.White.copy(alpha = 0.16f)

// endregion

// region Model & data

data class Planet(
    @param:StringRes val name: Int,
    @param:StringRes val nickName: Int,
    @param:DrawableRes val thumbnail: Int,
    val color: Color,
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
        color = Color(0xFFAB4F20),
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
        color = Color(0xFFFF844E),
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
        color = Color(0xFF095B91),
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
        color = Color(0xFFC69E4A),
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
        color = Color(0xFFFF8332),
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
        color = Color(0xFF31CFDB),
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
        color = Color(0xFF2CA6DB),
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