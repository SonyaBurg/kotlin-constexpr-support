/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redisco.constexpr

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.collections.MutableMap
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toMap
import kotlin.collections.toMutableMap
import kotlin.collections.zip

class TemplateIrGenerationExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    for (file in moduleFragment.files) {
      val data = Container()
      AccessorCallTransformer(pluginContext, data).runOnFileInOrder(file)
    }
  }
}

data class Container(
  val valueMap: MutableMap<String, IrConst<*>> = mutableMapOf(),
  var fromEval: Boolean = false,
  val parentTransformer: AccessorCallTransformer? = null
)

class AccessorCallTransformer(
  val context: IrPluginContext, private val dataContainer: Container
) : IrElementTransformer<Container>, FileLoweringPass {

  override fun lower(irFile: IrFile) {
    irFile.transformChildren(this, dataContainer)
  }

  override fun visitVariable(declaration: IrVariable, data: Container): IrStatement {
    val initializer = declaration.initializer?.transform(this, data) ?: super.visitVariable(declaration, data)
    if (initializer is IrConst<*>) {
      data.valueMap[declaration.name.asString()] = initializer
    }
    return super.visitVariable(declaration, data)
  }

  override fun visitGetValue(expression: IrGetValue, data: Container): IrExpression {
    if (data.fromEval && data.valueMap.containsKey(expression.symbol.owner.name.asString())) {
      return data.valueMap[expression.symbol.owner.name.asString()] ?: super.visitGetValue(expression, data)
    }
    return super.visitGetValue(expression, data)
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Container): IrElement {
    if (expression.symbol.owner.name.asString().startsWith("eval")) {
      val evalTransformer = EvalTransformer()
      data.fromEval = true
      val transformedValueArguments = expression.valueArguments.mapNotNull { it?.transform(this, data) as? IrConst<*> }
      if (transformedValueArguments.size == expression.valueArguments.size) {
        val keys = expression.symbol.owner.valueParameters.map { it.name.asString() }
        val container = Container(valueMap = keys.zip(transformedValueArguments).toMap().toMutableMap(), true, this)
        val result = try {
          expression.symbol.owner.body?.accept(evalTransformer, container)
        } catch (e: StackOverflowError) {
          null
        }
        return result ?: super.visitFunctionAccess(expression, data)
      }
    }
    return super.visitFunctionAccess(expression, data)
  }
}

fun FileLoweringPass.runOnFileInOrder(irFile: IrFile) {
  irFile.acceptVoid(object : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
      element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
      lower(declaration)
      declaration.acceptChildrenVoid(this)
    }
  })
}
