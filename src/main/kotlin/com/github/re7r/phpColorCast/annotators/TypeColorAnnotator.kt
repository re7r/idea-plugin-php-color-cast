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
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocVariable
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import java.awt.Color
import java.awt.Font
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TypeColorAnnotator : Annotator, DumbAware {
    private data class TypeColor(val type: String, val color: String)

    private val typeCache = WeakHashMap<Field, TypeColor?>()
    private val transparent = JBColor.lazy {
        val bg = EditorColorsManager.getInstance().globalScheme.defaultBackground
        @Suppress("UseJBColor")
        Color(bg.red, bg.green, bg.blue, 0)
    }

    private lateinit var state: State

    class SharedState {
        companion object {
            var isInitialized = false
            var cachedArrayType: StateItem? = null
            val colorCache = ConcurrentHashMap<String, TextAttributesKey>()

            fun clear() {
                isInitialized = false
                cachedArrayType = null
                colorCache.clear()
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

        if (!SharedState.isInitialized) {
            SharedState.isInitialized = true
            SharedState.cachedArrayType = state.types.find {
                normalizeTypePath(it.path) == "\\array"
            }
        }

        when {
            state.references && element is FieldReference -> {
                val name = element.nameNode ?: return
                val field = element.resolveLocal().filterIsInstance<Field>().firstOrNull() ?: return
                val typeColor = cachedCheckType(field, state.types)
                if (typeColor != null) {
                    colorize(holder, name.textRange, typeColor)
                }
            }

            state.properties && element is Field -> {
                val name = element.nameNode ?: return
                val typeColor = cachedCheckType(element, state.types)
                if (typeColor != null) {
                    colorize(holder, name.textRange, typeColor)
                }
            }

            state.variables && when (element) {
                is Variable -> {
                    val name = element.nameNode ?: return
                    if (name.text == "\$this") return
                    val typeColor = checkType(element, state.types)
                    if (typeColor != null) colorize(holder, name.textRange, typeColor)
                    true
                }

                is Parameter -> {
                    if (!state.properties || !element.isPromotedField) {
                        val name = element.nameNode ?: return
                        val typeColor = checkType(element, state.types)
                        if (typeColor != null) colorize(holder, name.textRange, typeColor)
                    }
                    true
                }

                is PhpDocVariable -> {
                    val nameNode = element.firstChild?.node ?: return
                    val typeColor = checkType(element, state.types)
                    if (typeColor != null) colorize(holder, nameNode.textRange, typeColor)
                    true
                }

                else -> false
            } -> Unit
        }
    }

    private fun colorize(holder: AnnotationHolder, range: TextRange, typeColor: TypeColor) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(getColorKey(typeColor.type, typeColor.color))
            .create()
    }

    private fun getColorKey(type: String, hex: String): TextAttributesKey {
        return SharedState.colorCache.computeIfAbsent(type) {
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

    private fun checkType(field: PsiElement, types: List<StateItem>): TypeColor? {
        val phpType: PhpType = when (field) {
            is Field -> field.type
            is Variable -> (field as? PhpTypedElement)?.type ?: PhpType.EMPTY
            is Parameter -> (field as? PhpTypedElement)?.type ?: PhpType.EMPTY
            is PhpDocVariable -> (field.parent as? PhpDocTag)?.type ?: PhpType.EMPTY
            else -> PhpType.EMPTY
        }

        if (phpType.isEmpty) return null

        val index = PhpIndex.getInstance(field.project)

        if (SharedState.cachedArrayType != null && PhpType.isArray(phpType) || phpType.types.any { it.endsWith("[]") }) {
            return TypeColor(SharedState.cachedArrayType!!.path, SharedState.cachedArrayType!!.color)
        }

        val matchingType = types.find { stateType ->
            val targetFqn = normalizeTypePath(stateType.path)

            phpType.types.any { typeName ->
                typeName == targetFqn ||
                        index.getAnyByFQN(typeName).any { it is PhpClass && isInstanceOf(it, targetFqn) }
            }
        }

        return matchingType?.let { TypeColor(it.path, it.color) }
    }

    private fun isInstanceOf(phpClass: PhpClass, targetFqn: String): Boolean {
        return phpClass.fqn == targetFqn || phpClass.supers.any { isInstanceOf(it, targetFqn) }
    }

    private fun normalizeTypePath(path: String): String {
        return if (path.startsWith('\\')) path else "\\$path"
    }
}
