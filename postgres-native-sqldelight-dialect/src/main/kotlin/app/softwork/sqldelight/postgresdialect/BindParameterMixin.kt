package app.softwork.sqldelight.postgresdialect

import app.cash.sqldelight.dialect.grammar.mixins.BindParameterMixin
import com.intellij.lang.*

public abstract class BindParameterMixin(node: ASTNode) : BindParameterMixin(node) {
    override fun replaceWith(isAsync: Boolean, index: Int): String = "$$index"
}
