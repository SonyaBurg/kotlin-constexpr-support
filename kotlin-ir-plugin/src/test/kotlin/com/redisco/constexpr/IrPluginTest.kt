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

@file:OptIn(ExperimentalCompilerApi::class)

package com.redisco.constexpr

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals

class IrPluginTest {
  @Test
  fun `Test constexpr`() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "main.kt", """
fun evalAdd(a: Int, b: Int): Int {
  var c = 0
  while (c < 10) {
    c += a
    if (c > b) break
    c += 1
  }
  return c
}

fun main() {
  val a = 5
  println(evalAdd(a, 1))
}
"""
      )
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
  }
}

fun compile(
  sourceFiles: List<SourceFile>,
  plugin: CompilerPluginRegistrar = TemplateCompilerRegistrar(),
): JvmCompilationResult {
  return KotlinCompilation().apply {
    sources = sourceFiles
    compilerPluginRegistrars = listOf(plugin)
    inheritClassPath = true
  }.compile()
}

fun compile(
  sourceFile: SourceFile,
  plugin: CompilerPluginRegistrar = TemplateCompilerRegistrar(),
): JvmCompilationResult {
  return compile(listOf(sourceFile), plugin).also {
    it.generatedFiles
  }
}
