parser grammar WaccParser;

options {
  tokenVocab=WaccLexer;
}

// EOF indicates that the program must consume to the end of the input.
program: BEGIN (func)* stat_sequence END EOF;

func: return_type ident OPEN_PAREN (param (COMMA param)*)? CLOSE_PAREN IS
          stat_sequence
      END;

param: type ident;

decl: type ident ASSIGN assign_rhs;

stat_sequence: stat (END_STAT stat_sequence)?;

for_stat: FOR OPEN_PAREN decl END_STAT expr END_STAT stat CLOSE_PAREN DO stat_sequence DONE;

for_range_stat: FOR ident INRANGE (int_liter COMMA int_liter) DO stat_sequence DONE;

stat: SKIP_STAT                                                     #skipStat
    | decl                                                          #declarationStat
    | assign_lhs ASSIGN assign_rhs                                  #assignmentStat
    | call                                                          #callStat
    | READ assign_lhs                                               #readStat
    | FREE expr                                                     #freeStat
    | EXIT expr                                                     #exitStat
    | CONTINUE                                                      #continueStat
    | BREAK                                                         #breakStat
    | PRINT expr                                                    #printStat
    | PRINTLN expr                                                  #printlnStat
    | IF expr THEN stat_sequence ELSE stat_sequence FI              #ifStat
    | WHILE expr DO stat_sequence DONE                              #whileStat
    | for_stat                                                      #forStat
    | for_range_stat                                                #forInRangeStat
    | BEGIN stat_sequence END                                       #beginEndStat
    | RETURN expr?                                                  #returnStat
;

assign_lhs: pointer_deref                                           #derefAssignLhs
          | ident                                                   #identAssignLhs
          | array_elem                                              #arrayElemAssignLhs
          | pair_elem                                               #pairElemAssignLhs
;

assign_rhs: expr                                                    #exprAssignRhs
          | array_liter                                             #arrayLiterAssignRhs
          | NEWPAIR OPEN_PAREN expr COMMA expr CLOSE_PAREN          #newPairAssignRhs
          | pair_elem                                               #pairElemAssignRhs
          | call                                                    #callAssignRhs
          | ALLOC OPEN_PAREN expr CLOSE_PAREN                       #allocAssignRhs
;

arg_list: expr (COMMA expr)*;

call: CALL ident OPEN_PAREN arg_list? CLOSE_PAREN;

pair_elem: FST expr                                                 #fstPair
         | SND expr                                                 #sndPair
;

return_type: type
           | T_VOID
;

type: base_type
    | array_type
    | pair_type
    | pointer_type
;

base_type: T_INT | T_BOOL | T_CHAR | T_STRING;

array_type: (base_type | pair_type) (array_brackets)+;

array_brackets: OPEN_BRACKETS CLOSE_BRACKETS;

pair_type: PAIR OPEN_PAREN pair_elem_type COMMA pair_elem_type CLOSE_PAREN;

pair_elem_type: base_type
              | array_type
              | PAIR
;

pointer_type: POINTER OPEN_PAREN type CLOSE_PAREN;

expr: OPEN_PAREN expr CLOSE_PAREN                                   #bracketExpr
    | SIZEOF type                                                   #sizeofExpr
    | REF assign_lhs                                                #referenceExpr
    | pointer_deref                                                 #derefExpr
    | int_liter                                                     #singleElemExpr
    | bool_liter                                                    #singleElemExpr
    | char_liter                                                    #singleElemExpr
    | str_liter                                                     #singleElemExpr
    | pair_liter                                                    #singleElemExpr
    | array_elem                                                    #singleElemExpr
    | ident                                                         #identExpr
    | BANG expr                                                     #unaryExpr
    | MINUS expr                                                    #unaryExpr
    | LEN expr                                                      #unaryExpr
    | ORD expr                                                      #unaryExpr
    | CHR expr                                                      #unaryExpr
    | expr MULT expr                                                #binaryExpr
    | expr DIV expr                                                 #binaryExpr
    | expr MOD expr                                                 #binaryExpr
    | expr PLUS expr                                                #binaryExpr
    | expr MINUS expr                                               #binaryExpr
    | expr GT expr                                                  #binaryExpr
    | expr GTE expr                                                 #binaryExpr
    | expr LT expr                                                  #binaryExpr
    | expr LTE expr                                                 #binaryExpr
    | expr EQUALS expr                                              #binaryExpr
    | expr NOT_EQUALS expr                                          #binaryExpr
    | expr AND expr                                                 #binaryExpr
    | expr OR expr                                                  #binaryExpr
;

ident: IDENT;

pointer_deref: (DEREF)+ expr;

array_elem: ident (OPEN_BRACKETS expr CLOSE_BRACKETS)+;

int_liter: int_liter_negative | int_liter_positive;

int_liter_negative: MINUS (POSITIVE_OR_NEGATIVE_INTEGER | NEGATIVE_INTEGER);
int_liter_positive: PLUS? POSITIVE_OR_NEGATIVE_INTEGER;

bool_liter: TRUE                                                    #tBool
          | FALSE                                                   #fBool
;

char_liter: CHAR_LITER_TOKEN | ESC_CHAR_LITER;

str_liter: STRING_LITER_TOKEN;

array_liter: OPEN_BRACKETS (expr (COMMA expr)*)? CLOSE_BRACKETS;

pair_liter: NULL;
