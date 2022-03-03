package ic.doc.group15.codegen.assembly

import ic.doc.group15.ast.ASTNode
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class TranslatorMethod(val nodeType: KClass<out ASTNode>)
