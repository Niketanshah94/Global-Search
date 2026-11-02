package com.quickfind.core.matching.instant

import java.util.Locale

/**
 * Offline unit conversion (spec 5.5, Phase 1.5): "5 ft in cm", "30c to f", "2 gb in mb", "10 km".
 * Without a target unit, a sensible default is used (ft→cm, kg→lb, °C→°F, …).
 */
object UnitConverter {
    enum class Category { LENGTH, WEIGHT, TEMPERATURE, DATA, VOLUME, SPEED }

    data class Unit(val symbol: String, val category: Category, val toBase: Double, val names: List<String>)

    data class Conversion(val value: Double, val from: Unit, val result: Double, val to: Unit)

    // Base units: metre, kilogram, (temperature handled separately), byte, litre, metre/second.
    private val UNITS = listOf(
        Unit("mm", Category.LENGTH, 0.001, listOf("mm", "millimeter", "millimeters", "millimetre", "millimetres")),
        Unit("cm", Category.LENGTH, 0.01, listOf("cm", "centimeter", "centimeters", "centimetre", "centimetres")),
        Unit("m", Category.LENGTH, 1.0, listOf("m", "meter", "meters", "metre", "metres")),
        Unit("km", Category.LENGTH, 1000.0, listOf("km", "kms", "kilometer", "kilometers", "kilometre", "kilometres")),
        Unit("in", Category.LENGTH, 0.0254, listOf("in", "inch", "inches", "\"")),
        Unit("ft", Category.LENGTH, 0.3048, listOf("ft", "foot", "feet", "'")),
        Unit("yd", Category.LENGTH, 0.9144, listOf("yd", "yard", "yards")),
        Unit("mi", Category.LENGTH, 1609.344, listOf("mi", "mile", "miles")),

        Unit("mg", Category.WEIGHT, 1e-6, listOf("mg", "milligram", "milligrams")),
        Unit("g", Category.WEIGHT, 0.001, listOf("g", "gm", "gram", "grams", "gms")),
        Unit("kg", Category.WEIGHT, 1.0, listOf("kg", "kgs", "kilo", "kilos", "kilogram", "kilograms")),
        Unit("t", Category.WEIGHT, 1000.0, listOf("t", "tonne", "tonnes", "ton", "tons")),
        Unit("oz", Category.WEIGHT, 0.028349523125, listOf("oz", "ounce", "ounces")),
        Unit("lb", Category.WEIGHT, 0.45359237, listOf("lb", "lbs", "pound", "pounds")),
        Unit("st", Category.WEIGHT, 6.35029318, listOf("st", "stone", "stones")),

        Unit("°C", Category.TEMPERATURE, 1.0, listOf("c", "°c", "celsius", "centigrade")),
        Unit("°F", Category.TEMPERATURE, 1.0, listOf("f", "°f", "fahrenheit")),
        Unit("K", Category.TEMPERATURE, 1.0, listOf("k", "kelvin")),

        // Binary multiples, which is what phones show for storage in practice.
        Unit("B", Category.DATA, 1.0, listOf("b", "byte", "bytes")),
        Unit("KB", Category.DATA, 1024.0, listOf("kb", "kilobyte", "kilobytes")),
        Unit("MB", Category.DATA, 1024.0 * 1024, listOf("mb", "megabyte", "megabytes")),
        Unit("GB", Category.DATA, 1024.0 * 1024 * 1024, listOf("gb", "gigabyte", "gigabytes")),
        Unit("TB", Category.DATA, 1024.0 * 1024 * 1024 * 1024, listOf("tb", "terabyte", "terabytes")),

        Unit("ml", Category.VOLUME, 0.001, listOf("ml", "milliliter", "milliliters", "millilitre", "millilitres")),
        Unit("l", Category.VOLUME, 1.0, listOf("l", "liter", "liters", "litre", "litres", "ltr")),
        Unit("gal", Category.VOLUME, 3.785411784, listOf("gal", "gallon", "gallons")),
        Unit("cup", Category.VOLUME, 0.2365882365, listOf("cup", "cups")),
        Unit("fl oz", Category.VOLUME, 0.0295735295625, listOf("floz", "fl oz", "fluid ounce", "fluid ounces")),

        Unit("km/h", Category.SPEED, 1 / 3.6, listOf("kmh", "km/h", "kph", "kmph")),
        Unit("mph", Category.SPEED, 0.44704, listOf("mph")),
        Unit("m/s", Category.SPEED, 1.0, listOf("m/s", "mps")),
        Unit("kn", Category.SPEED, 0.514444, listOf("knot", "knots", "kn")),
    )

    private val DEFAULT_TARGET = mapOf(
        "mm" to "in", "cm" to "in", "m" to "ft", "km" to "mi", "in" to "cm", "ft" to "cm", "yd" to "m", "mi" to "km",
        "mg" to "g", "g" to "oz", "kg" to "lb", "t" to "lb", "oz" to "g", "lb" to "kg", "st" to "kg",
        "°C" to "°F", "°F" to "°C", "K" to "°C",
        "B" to "KB", "KB" to "MB", "MB" to "GB", "GB" to "MB", "TB" to "GB",
        "ml" to "fl oz", "l" to "gal", "gal" to "l", "cup" to "ml", "fl oz" to "ml",
        "km/h" to "mph", "mph" to "km/h", "m/s" to "km/h", "kn" to "km/h",
    )

    private val byName: Map<String, Unit> = buildMap { UNITS.forEach { u -> u.names.forEach { put(it, u) } } }
    private val bySymbol: Map<String, Unit> = UNITS.associateBy { it.symbol }

    private val NUMBER_THEN_REST = Regex("""^\s*(-?\d+(?:[.,]\d+)?)\s*(.+)$""")
    private val SEPARATOR = Regex("""\s+(?:in|to|into|as)\s+|\s*(?:=|->)\s*""")

    fun parse(query: String): Conversion? {
        val m = NUMBER_THEN_REST.matchEntire(query.lowercase(Locale.ROOT)) ?: return null
        val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        // Split on the first " in " / " to ": "1 in to cm" -> ["in", "cm"], "1 in in cm" -> ["in", "cm"].
        val parts = m.groupValues[2].trim().split(SEPARATOR, limit = 2)
        val from = unitFor(parts[0]) ?: return null
        val to = if (parts.size == 2) {
            unitFor(parts[1]) ?: return null
        } else {
            bySymbol[DEFAULT_TARGET[from.symbol]] ?: return null
        }
        if (from.category != to.category || from == to) return null
        return Conversion(value, from, convert(value, from, to), to)
    }

    private fun unitFor(text: String): Unit? = byName[text.trim()] ?: byName[text.replace(" ", "")]

    private fun convert(value: Double, from: Unit, to: Unit): Double {
        if (from.category != Category.TEMPERATURE) return value * from.toBase / to.toBase
        val celsius = when (from.symbol) {
            "°F" -> (value - 32) * 5 / 9
            "K" -> value - 273.15
            else -> value
        }
        return when (to.symbol) {
            "°F" -> celsius * 9 / 5 + 32
            "K" -> celsius + 273.15
            else -> celsius
        }
    }

    /** "152.4" — up to 4 decimals for small numbers, grouped thousands, no trailing zeros. */
    fun format(value: Double): String {
        val abs = kotlin.math.abs(value)
        val decimals = when {
            abs >= 1000 -> 1
            abs >= 1 -> 2
            else -> 4
        }
        val text = String.format(Locale.US, "%,.${decimals}f", value)
        return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
    }
}
