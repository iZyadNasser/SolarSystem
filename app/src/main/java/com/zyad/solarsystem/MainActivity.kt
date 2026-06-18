package com.zyad.solarsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

        }
    }
}

// Font Family
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

// Model
data class Planet(
    val name: String,
    val nickName: String,
    @param:DrawableRes val thumbnail: Int,
    @param:IntRange(from = 0) val person70KgRelativeWeightKg: Int,
    val dayLength: Double,
    @param:StringRes val dayLengthUnit: Int = R.string.hours,
    val temperatureCelsius: Int,
    @param:IntRange(from = 0, to = 3) val numberOfJackets: Int,
    val additionalInfo: String
)

// Data
val planets = listOf(
    Planet(
        name = "Saturn",
        nickName = "The Ring Master",
        thumbnail = R.drawable.im_saturn,
        person70KgRelativeWeightKg = 74,
        dayLength = 10.7,
        temperatureCelsius = -178,
        numberOfJackets = 1,
        additionalInfo = "Lighter than water"
    ),
    Planet(
        name = "Mars",
        nickName = "The next colony",
        thumbnail = R.drawable.im_mars,
        person70KgRelativeWeightKg = 27,
        dayLength = 24.6,
        temperatureCelsius = -65,
        numberOfJackets = 1,
        additionalInfo = "Red Dust Storms"
    ),
    Planet(
        name = "Mercury",
        nickName = "The Fatest Planet",
        thumbnail = R.drawable.im_mercury,
        person70KgRelativeWeightKg = 26,
        dayLength = 1408.0,
        temperatureCelsius = -167,
        numberOfJackets = 0,
        additionalInfo = "Birthday every 88 days"
    ),
    Planet(
        name = "Venus",
        nickName = "The Toxic Beauty",
        thumbnail = R.drawable.im_venus,
        person70KgRelativeWeightKg = 63,
        dayLength = 243.0,
        dayLengthUnit = R.string.days,
        temperatureCelsius = 465,
        numberOfJackets = 0,
        additionalInfo = "Sun rises from West"
    ),
    Planet(
        name = "Jupiter",
        nickName = "The Heavy Giant",
        thumbnail = R.drawable.im_jupiter,
        person70KgRelativeWeightKg = 177,
        dayLength = 9.9,
        temperatureCelsius = -110,
        numberOfJackets = 1,
        additionalInfo = "Has 95 Moons"
    ),
    Planet(
        name = "Uranus",
        nickName = "The Lazy Iceberg",
        thumbnail = R.drawable.im_uranus,
        person70KgRelativeWeightKg = 62,
        dayLength = 17.0,
        temperatureCelsius = -224,
        numberOfJackets = 3,
        additionalInfo = "diamond Shower"
    ),

    Planet(
        name = "Neptune",
        nickName = "The Windy World",
        thumbnail = R.drawable.im_neptune,
        person70KgRelativeWeightKg = 79,
        dayLength = 16.0,
        temperatureCelsius = -214,
        numberOfJackets = 3,
        additionalInfo = "Wind faster than Sound"
    ),
)