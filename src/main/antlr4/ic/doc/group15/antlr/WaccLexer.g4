lexer grammar WaccLexer;

//whitespace
WS: [ \n\t\r]+ -> skip;

//comment
COMMENT: '#' ~('\n')* '\n' -> skip;

//types
T_INT: 'int';
T_BOOL: 'bool';
T_CHAR: 'char';
T_STRING: 'string';
T_VOID: 'void';

//boolean values
TRUE: 'true';
FALSE: 'false';

//unary operators
BANG: '!';
LEN: 'len';
ORD: 'ord';
CHR: 'chr';

//function prototyping
IS: 'is';

//functions
READ: 'read';
RETURN: 'return';
EXIT: 'exit';
PRINT: 'print';
PRINTLN: 'println';

//flow control operators
IF: 'if';
FI: 'fi';
THEN: 'then';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
INRANGE: 'in' (' ')+ 'range';
DO: 'do';
DONE: 'done';
SKIP_STAT: 'skip';
END_STAT: ';';
BEGIN: 'begin';
END: 'end';
CONTINUE: 'continue loop';
BREAK: 'break loop';

//heap
NEWPAIR: 'newpair';
CALL: 'call';
NULL: 'null';
SIZEOF: 'sizeof';
POINTER: 'pointer';
DEREF: '$';
REF: '&';
ALLOC: 'alloc';
FREE: 'free';

//pair
PAIR: 'pair';
FST: 'fst';
SND: 'snd';

//binary operators
ASSIGN: '=';
MULT: '*';
DIV: '/';
MOD: '%';
PLUS: '+';
MINUS: '-';
GT: '>';
GTE: '>=';
LT: '<';
LTE: '<=';
EQUALS: '==';
NOT_EQUALS: '!=';
AND: '&&';
OR: '||';

//brackets
OPEN_PAREN: '(';
OPEN_BRACKETS: '[';
COMMA: ',';
CLOSE_PAREN: ')';
CLOSE_BRACKETS: ']';

//numbers
fragment DIGIT: [0-9];
fragment ALPHABET: [a-zA-Z];
fragment ALPHANUMERIC: DIGIT | ALPHABET;

fragment MAX_POSITIVE_INTEGER: [1-2][0-1][0-4][0-7][0-4][0-8][0-3][0-6][0-4][0-7];
fragment MAX_NEGATIVE_INTEGER: [1-2][0-1][0-4][0-7][0-4][0-8][0-3][0-6][0-4][0-8];
//fragment SMALL_INTEGER: DIGIT+ {getText().length() < 10}?;
fragment SMALL_INTEGER: DIGIT DIGIT? DIGIT? DIGIT? DIGIT? DIGIT? DIGIT?
DIGIT? DIGIT?;

POSITIVE_OR_NEGATIVE_INTEGER: [0]* (SMALL_INTEGER | MAX_POSITIVE_INTEGER);
NEGATIVE_INTEGER: [0]* MAX_NEGATIVE_INTEGER;

//identifiers
IDENT: (UNDERSCORE | ALPHABET) (UNDERSCORE | ALPHANUMERIC)*;

//characters
BACKSLASH: '\\';
APOSTROPHE: '\'';
DOUBLE_QUOTE: '"';
UNDERSCORE: '_';
ESC_CHAR: '0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\';
CHAR: ~('\\' | '\'' | '"');
CHAR_LITER_TOKEN:
  APOSTROPHE CHAR APOSTROPHE;
ESC_CHAR_LITER:
  APOSTROPHE BACKSLASH ESC_CHAR APOSTROPHE;
STRING_LITER_TOKEN:
  DOUBLE_QUOTE (CHAR | BACKSLASH ESC_CHAR)* DOUBLE_QUOTE;
