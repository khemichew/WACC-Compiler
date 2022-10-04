### WACC-Compiler

This group project is developed for Imperial College London's Year 2 Compilers module, which converts WACC code to ARM assembly code using Kotlin and ANTLR4.

## WACC

WACC (pronounced "whack") is a simple variant on the While family of languages encountered in many program reasoning/verification courses (in particular in the Models of Computation course taught to our 2nd year undergraduates). It features all the common language constructs you would expect of a no-ops. It also features a rich set of extra constructs, such as simple types, functions, arrays and basic tuple creation on the heap.

The WACC language is intended to help unify the material taught in our more theoretical courses (such as Models of Computation) with the material taught in our more practical courses (such as Compilers). The core of the language should be simple enough to reason about. 

You could look at the full language specification at wacc_examples/WACCLangSpec.pdf.

## Workflow 

1) ANTLR4 is used to parse the code lexically, producing tokens which is then applied syntactic analysis to check for syntax validity.
2) The compiler performs semantic analysis to produce resulting intermediate representation (more on this later).
3) Optimsations are performed on the intermediate representation to reduce code generated. 
4) Finally, the compiler generates code in assembly that targets the ARM instruction set.
