package com.bnorm.template.operators

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.toIrConst


interface Operator {
  operator fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>?
}

abstract class ArithmeticUnaryOperator<T> : Operator {
  override fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>? {
    if (arguments.size != 1) return null
    val arg = arguments[0].value as? Int ?: return null
    val result = invokeImpl(arg) ?: return null
    return result.toIrConst(type)
  }

  protected abstract fun invokeImpl(arg: Int): T?
}

object UnaryMinus : ArithmeticUnaryOperator<Int>() {
  override fun invokeImpl(arg: Int) = -arg
}

object Decrement : ArithmeticUnaryOperator<Int>() {
  override fun invokeImpl(arg: Int) = arg.dec()
}

object Increment : ArithmeticUnaryOperator<Int>() {
  override fun invokeImpl(arg: Int) = arg.inc()
}

object IntToString : ArithmeticUnaryOperator<String>() {
  override fun invokeImpl(arg: Int) = arg.toString()
}

abstract class ArithmeticBinaryOperator<T> : Operator {
  override fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>? {
    if (arguments.size != 2) return null
    val arg1 = arguments[0].value as? Int ?: return null
    val arg2 = arguments[1].value as? Int ?: return null
    val result = invokeImpl(arg1, arg2) ?: return null
    return result.toIrConst(type)
  }
  protected abstract fun invokeImpl(arg1: Int, arg2: Int): T?
}

object StringPlus : Operator {
  override fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>? {
    if (arguments.size != 2) return null
    val arg1 = arguments[0].value as? String ?: return null
    val arg2 = arguments[1].value as? String ?: return null
    return (arg1 + arg2).toIrConst(type)
  }
}

object GetLength : Operator {
  override fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>? {
    if (arguments.size != 1) return null
    val arg1 = arguments[0].value as? String ?: return null
    return (arg1.length).toIrConst(type)
  }
}

object Plus : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int) = arg1 + arg2
}

object Minus : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int) = arg1 - arg2
}

object Multiply : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int) = arg1 * arg2
}

object Divide : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int? {
    if (arg2 == 0) return null
    return arg1 / arg2
  }
}

object Remainder: ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int? {
    if (arg2 == 0) return null
    return arg1 % arg2
  }
}

object LogicalOr : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int = arg2.or(arg2)
}

object LogicalAnd : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int = arg2.and(arg2)
}

object LogicalXor : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int = arg2.xor(arg2)
}

object Comparison : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int = arg1.compareTo(arg2)
}

object Less : ArithmeticBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Boolean = arg1.compareTo(arg2) == -1
}

object Greater : ArithmeticBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Boolean =  arg1.compareTo(arg2) == 1
}

object ShiftLeft : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int = arg1.shl(arg2)
}

object ShiftRight : ArithmeticBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Int, arg2: Int): Int = arg1.shr(arg2)
}

object IntEquals : ArithmeticBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Int, arg2: Int) = arg1 == arg2
}

abstract class BooleanBinaryOperator<T> : Operator {
  override fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>? {
    if (arguments.size != 2) return null
    val arg1 = arguments[0].value as? Boolean ?: return null
    val arg2 = arguments[1].value as? Boolean ?: return null
    val result = invokeImpl(arg1, arg2) ?: return null
    return result.toIrConst(type)
  }

  protected abstract fun invokeImpl(arg1: Boolean, arg2: Boolean): T?
}

object BooleanAnd : BooleanBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Boolean, arg2: Boolean): Boolean = arg1.and(arg2)
}

object BooleanXor : BooleanBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Boolean, arg2: Boolean): Boolean = arg1.xor(arg2)
}

object BooleanOr : BooleanBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Boolean, arg2: Boolean): Boolean = arg1.or(arg2)
}

abstract class BooleanUnaryOperator : Operator {
  override fun invoke(type: IrType, vararg arguments: IrConst<*>): IrConst<*>? {
    if (arguments.size != 1) return null
    val arg1 = arguments[0].value as? Boolean ?: return null
    return invokeImpl(arg1).toIrConst(type)
  }

  protected abstract fun invokeImpl(arg1: Boolean): Boolean
}

object BooleanComparison : BooleanBinaryOperator<Int>() {
  override fun invokeImpl(arg1: Boolean, arg2: Boolean): Int = arg1.compareTo(arg2)
}

object BooleanEquals : BooleanBinaryOperator<Boolean>() {
  override fun invokeImpl(arg1: Boolean, arg2: Boolean) = arg1 == arg2
}

object BooleanNot : BooleanUnaryOperator() {
  override fun invokeImpl(arg1: Boolean): Boolean = arg1.not()
}
