package com.redisco.constexpr

import com.redisco.constexpr.operators.*
import org.jetbrains.kotlin.backend.jvm.ir.receiverAndArgs
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.removeAnnotations
import org.jetbrains.kotlin.ir.util.isConstantLike
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class EvalTransformer : IrElementVisitor<IrConst<*>?, Container> {
  private val loopMap = mutableMapOf<IrLoop, Boolean>()

  override fun visitGetValue(expression: IrGetValue, data: Container): IrConst<*>? {
    if (data.valueMap.containsKey(expression.symbol.owner.name.asString())) {
      return data.valueMap[expression.symbol.owner.name.asString()] ?: super.visitGetValue(expression, data)
    }
    return super.visitGetValue(expression, data)
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Container): IrConst<*>? {
    if (expression.symbol.owner.name.asString().startsWith("eval")) {
      return data.parentTransformer?.let { expression.transform(it, data) as? IrConst<*>? }
    }
    val args = expression.receiverAndArgs().map { it.accept(this, data) }
    if (args.any { it !is IrConst<*> }) {
      return super.visitFunctionAccess(expression, data)
    }
    val constArgs = args.map { it as IrConst<*> }
    val name = expression.symbol.owner.name.asString()
    val operator = when {
      constArgs.all {
        it.type.makeNotNull().removeAnnotations().getPrimitiveType() == PrimitiveType.INT
      } -> getIntOperatorIfPossible(name)

      constArgs.all {
        it.type.makeNotNull().removeAnnotations().getPrimitiveType() == PrimitiveType.BOOLEAN
      } -> getBooleanOperatorIfPossible(name)

      constArgs.all { it.type.isString() } -> getStringOperatorIfPossible(name)
        ?: return super.visitFunctionAccess(expression, data)

      else -> null
    }
    return operator?.invoke(expression.type, *constArgs.map { it }.toTypedArray()) ?: super.visitFunctionAccess(
      expression,
      data
    )
  }

  private fun getIntOperatorIfPossible(name: String) = when (name) {
    "plus" -> Plus
    "minus" -> Minus
    "times" -> Multiply
    "div" -> Divide
    "shl" -> ShiftLeft
    "shr" -> ShiftRight
    "rem" -> Remainder
    "and" -> LogicalAnd
    "compareTo" -> Comparison
    "less" -> Less
    "greater" -> Greater
    "equals" -> IntEquals
    "or" -> LogicalOr
    "xor" -> LogicalXor
    "unaryMinus" -> UnaryMinus
    "dec" -> Decrement
    "inc" -> Increment
    "toString" -> IntToString
    else -> null
  }

  private fun getStringOperatorIfPossible(name: String) = when (name) {
    "plus" -> StringPlus
    "<get-length>" -> GetLength
    else -> null
  }

  private fun getBooleanOperatorIfPossible(name: String) = when (name) {
    "and" -> BooleanAnd
    "or" -> BooleanOr
    "xor" -> BooleanXor
    "not" -> BooleanNot
    "equals" -> BooleanEquals
    "compareTo" -> BooleanComparison
    else -> null
  }

  override fun visitReturn(expression: IrReturn, data: Container): IrConst<*>? {
    if (!data.fromEval) {
      return super.visitReturn(expression, data)
    }
    val transformed = expression.value.accept(this, data) as? IrExpression
    if (transformed?.isConstantLike == true) {
      return transformed as? IrConst<*>?
    }
    return super.visitReturn(expression, data)
  }

  override fun visitBlock(expression: IrBlock, data: Container): IrConst<*>? {
    return expression.statements.map { it.accept(this, data) }.last()
  }

  override fun visitBody(body: IrBody, data: Container): IrConst<*>? {
    return body.statements.map { it.accept(this, data) }.last()
  }

  override fun visitVariable(declaration: IrVariable, data: Container): IrConst<*>? {
    val initializer = declaration.initializer?.accept(this, data) ?: return null
    data.valueMap[declaration.name.asString()] = initializer
    return initializer
  }

  override fun visitWhen(expression: IrWhen, data: Container): IrConst<*>? {
    for (branch in expression.branches) {
      val condition = branch.condition.accept(this, data) ?: return null
      if (condition.value as? Boolean == true) {
        return branch.result.accept(this, data)
      }
    }
    return null
  }

  override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Container): IrConst<*>? {
    return expression.argument.accept(this, data)
  }

  override fun visitSetValue(expression: IrSetValue, data: Container): IrConst<*>? {
    val result = expression.value.accept(this, data) ?: return null
    data.valueMap[expression.symbol.owner.name.asString()] = result
    return result
  }

  override fun visitConst(expression: IrConst<*>, data: Container): IrConst<*> = expression

  override fun visitBreakContinue(jump: IrBreakContinue, data: Container): IrConst<*>? {
    if (jump is IrBreak) {
      loopMap[jump.loop] = true
    } else if (jump is IrContinue) {
      loopMap[jump.loop] = false
    }
    return null
  }

  override fun visitWhileLoop(loop: IrWhileLoop, data: Container): IrConst<*>? {
    var condition = loop.condition.accept(this, data) ?: return null
    var result: IrConst<*>? = null
    outerLoop@ while (condition.value as? Boolean == true) {
      val body = (loop.body as? IrBlock) ?: return null
      for (statement in body.statements) {
        result = statement.accept(this, data)
        val shouldBreak = loopMap.getOrDefault(loop, null) ?: continue
        if (shouldBreak) {
          break@outerLoop
        } else {
          break
        }
      }
      condition = loop.condition.accept(this, data) ?: return null
    }
    return result
  }

  override fun visitElement(element: IrElement, data: Container): IrConst<*>? = null
}
