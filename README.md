# Compile-time evaluation for constant expression

This plugin allows a behaviour similar to `constexpr` in C++: constant values as well as functions with names starting from `eval` prefix will be computed at compile-time.

The plugin currently supports constants, variables, for/while loops, if/when expressions, basic mathematical and logical operations, string concatenation, and string length retrieval.

### Usage
* Publish the plugin to mavenLocal and include the plugin into your `build.gradle.kts` file
* To see the decompiled code, use `javap -c <YourMain.class>`

### Example
```kotlin
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
```
In the example above the invocation of `evalAdd(a, 1)` would return 5, hence, the .class file compiled using this plugin would decompile to
```assembly
public final class MainKt {
  public static final int evalAdd(int, int);
    Code:
       0: iconst_0
       1: istore_2
       2: iload_2
       3: bipush        10
       5: if_icmpge     23
       8: iload_2
       9: iload_0
      10: iadd
      11: istore_2
      12: iload_2
      13: iload_1
      14: if_icmpgt     23
      17: iinc          2, 1
      20: goto          2
      23: iload_2
      24: ireturn

  public static final void main();
    Code:
       0: iconst_5
       1: istore_0
       2: iconst_5
       3: istore_1
       4: getstatic     #18                 // Field java/lang/System.out:Ljava/io/PrintStream;
       7: iload_1
       8: invokevirtual #24                 // Method java/io/PrintStream.println:(I)V
      11: return

  public static void main(java.lang.String[]);
    Code:
       0: invokestatic  #27                 // Method main:()V
       3: return
}
```
The function call is omitted and its result is already calculated.
