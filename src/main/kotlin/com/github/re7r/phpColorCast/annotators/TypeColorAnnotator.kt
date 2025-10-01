package com.github.re7r.phpColorCast.annotators

import com.github.re7r.phpColorCast.scheme.SchemesManager
import com.github.re7r.phpColorCast.state.State
import com.github.re7r.phpColorCast.state.StateItem
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import java.awt.Color
import java.awt.Font
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TypeColorAnnotator : Annotator, DumbAware {
    private data class TypeColor(val type: String, val color: String)

    private val transparent = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
    private val typeCache = WeakHashMap<Field, TypeColor?>()
    private lateinit var state: State

    class ColorCache {
        companion object {
            val cache = ConcurrentHashMap<String, TextAttributesKey>()

            fun clear() {
                cache.clear()
            }
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!::state.isInitialized) {
            state = element.project
                .getService(SchemesManager::class.java)
                .getScheme()
                .state
        }

        when {
            state.properties && element is Field -> {
                val name = element.nameNode ?: return
                val typeColor = cachedCheckType(element, state.types)
                if (typeColor != null) {
                    colorize(holder, name.textRange, typeColor)
                }
            }

            state.references && element is FieldReference -> {
                val name = element.nameNode ?: return
                val field = element.resolve() as? Field ?: return
                val typeColor = cachedCheckType(field, state.types)
                if (typeColor != null) {
                    colorize(holder, name.textRange, typeColor)
                }
            }

            state.variables && element is Variable -> {
                val name = element.nameNode ?: return
                if (name.text == "\$this") return
                val typeColor = checkType(element, state.types)
                if (typeColor != null) {
                    colorize(holder, name.textRange, typeColor)
                }
            }

            state.variables && element is Parameter -> {
                if (!state.properties || !element.isPromotedField) {
                    val name = element.nameNode ?: return
                    val typeColor = checkType(element, state.types)
                    if (typeColor != null) {
                        colorize(holder, name.textRange, typeColor)
                    }
                }
            }
        }
    }

    private fun colorize(holder: AnnotationHolder, range: TextRange, typeColor: TypeColor) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(getColorKey(typeColor.type, typeColor.color))
            .create()
    }

    private fun getColorKey(type: String, hex: String): TextAttributesKey {
        return ColorCache.cache.computeIfAbsent(type) {
            val colorInt = (hex.removePrefix("#").toLongOrNull(16) ?: 0x000000).toInt()
            val color = JBColor(Color(colorInt, false), Color(colorInt, false))
            val name = type.split("\\").joinToString(".") { part ->
                part.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()
            }

            val key = TextAttributesKey.createTextAttributesKey(
                "php.color.cast.dynamic.$name",
                DefaultLanguageHighlighterColors.IDENTIFIER
            )

            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runWriteAction {
                    EditorColorsManager.getInstance().globalScheme.setAttributes(
                        key,
                        TextAttributes(
                            color,
                            transparent,
                            null,
                            null,
                            Font.PLAIN
                        )
                    )
                }
            }

            key
        }
    }

    private fun cachedCheckType(field: Field, types: List<StateItem>): TypeColor? {
        return typeCache.getOrPut(field) { checkType(field, types) }
    }

    private fun checkType(element: PsiElement, types: List<StateItem>): TypeColor? {
        val phpType: PhpType = when (element) {
            is Field -> element.type
            is Variable -> (element as? PhpTypedElement)?.type ?: PhpType.EMPTY
            is Parameter -> (element as? PhpTypedElement)?.type ?: PhpType.EMPTY
            else -> PhpType.EMPTY
        }
        if (phpType.isEmpty) return null

        val index = PhpIndex.getInstance(element.project)

        fun PhpClass.isInstanceOf(targetFqn: String): Boolean =
            fqn == targetFqn || supers.any { it.isInstanceOf(targetFqn) }

        val matchingType = types.find { stateType ->
            val targetFqn = if (stateType.path.startsWith('\\')) {
                stateType.path
            } else {
                "\\${stateType.path}"
            }

            phpType.types.any { typeName ->
                typeName == targetFqn ||
                        index.getAnyByFQN(typeName).any { it is PhpClass && it.isInstanceOf(targetFqn) }
            }
        }

        return matchingType?.let { TypeColor(it.path, it.color) }
    }
}
