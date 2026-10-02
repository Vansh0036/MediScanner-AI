package com.example.mediscannerai.presentation.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders a small, common subset of Markdown (#/##/### headers, **bold**,
 * *italic*, "* " bullet lists, and "---" dividers) as styled Compose text.
 * Gemini's responses use this formatting; Compose's Text() has no built-in
 * Markdown support, so this keeps the app dependency-free rather than
 * pulling in a full Markdown-rendering library for a handful of symbols.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        for (rawLine in markdown.lines()) {
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> Spacer(modifier = Modifier.height(8.dp))

                line == "---" -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                line.startsWith("### ") -> Text(
                    text = inlineFormatted(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                line.startsWith("## ") -> Text(
                    text = inlineFormatted(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                line.startsWith("# ") -> Text(
                    text = inlineFormatted(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Regex("^\\*\\s+").containsMatchIn(line) -> {
                    val content = line.replaceFirst(Regex("^\\*\\s+"), "")
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = inlineFormatted(content))
                    }
                }

                else -> Text(
                    text = inlineFormatted(line),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Converts **bold** and *italic* markers within a single line into an
 * AnnotatedString by walking the text character by character and toggling
 * bold/italic state whenever a marker is seen, rather than using regex —
 * this handles overlapping/nested markers correctly and is easy to follow.
 */
private fun inlineFormatted(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    var bold = false
    var italic = false
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                bold = !bold
                i += 2
            }
            text[i] == '*' -> {
                italic = !italic
                i += 1
            }
            else -> {
                withStyle(
                    SpanStyle(
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
                    )
                ) {
                    append(text[i])
                }
                i += 1
            }
        }
    }
}

