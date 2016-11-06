###
# This Makefile can be used to make a parser for the harambe language
# (parser.class) and to make a program (P4.class) that tests the parser and
# the unparse methods in ast.java.
#
# make clean removes all generated files.
#
###

JC = javac
CP = ~cs536-1/public/tools/deps_src/java-cup-11b.jar:~cs536-1/public/tools/deps_src/java-cup-11b-runtime.jar:~cs536-1/public/tools/deps:.
CP2 = ~cs536-1/public/tools/deps:.

P4.class: P4.java parser.class Yylex.class ASTnode.class
	$(JC)    P4.java

parser.class: parser.java ASTnode.class Yylex.class ErrMsg.class
	$(JC)      parser.java

parser.java: harambe.cup
	java   java_cup.Main < harambe.cup

Yylex.class: harambe.jlex.java sym.class ErrMsg.class
	$(JC)   harambe.jlex.java

ASTnode.class: ast.java
	$(JC)  -g ast.java

harambe.jlex.java: harambe.jlex sym.class
	java    JLex.Main harambe.jlex

sym.class: sym.java
	$(JC)  -g sym.java

sym.java: harambe.cup
	java    java_cup.Main < harambe.cup

ErrMsg.class: ErrMsg.java
	$(JC) ErrMsg.java

##test
test: 
	
	java   P4 test.cf test.out
	
	java   P4 nameErrors.cf nameErrors.out
	


###
# clean
###
clean:
	rm -f *~ *.class parser.java harambe.jlex.java sym.java
