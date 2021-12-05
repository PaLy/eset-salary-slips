package io.paly.esetsalaryslipparser.regex

import io.paly.esetsalaryslipparser.Main
import org.intellij.lang.annotations.Language


fun String.findRegex(regex: Regex): List<String> {
    return regex
        .find(this)
        ?.let { it.groupValues.subList(1, it.groupValues.size) }
        ?: run {
            val message = if (Main.debug) {
                "Cannot find \"$regex\" in following text:\n\n$this"
            } else {
                "Cannot find \"$regex\"."
            }
            throw RuntimeException(message)
        }
}


@Language("RegExp")
const val ALPHABET = "A-zÀ-ÖØ-öø-įĴ-őŔ-žǍ-ǰǴ-ǵǸ-țȞ-ȟȤ-ȳɃɆ-ɏḀ-ẞƀ-ƓƗ-ƚƝ-ơƤ-ƥƫ-ưƲ-ƶẠ-ỿ"

@Language("RegExp")
const val NAME = "[$ALPHABET,.]+[$ALPHABET,. ]*[$ALPHABET.]+"

@Language("RegExp")
const val INT = "[0-9]+"

@Language("RegExp")
const val DOUBLE = "$INT(?:(?:.|,)$INT)?"

@Language("RegExp")
const val PERCENT = "$DOUBLE%"
