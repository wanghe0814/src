import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a Mini program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode {
	// every subclass must provide an unparse operation
	abstract public void unparse(PrintWriter p, int indent);

	abstract public void nameAnalyze(SymTable symT);

	// this method can be used by the unparse methods to do indenting
	protected void doIndent(PrintWriter p, int indent) {
		for (int k = 0; k < indent; k++)
			p.print(" ");
	}

	protected void addScope(SymTable symT) {
		symT.addScope();
	}

	protected void removeScope(SymTable symT) {
		try {
			symT.removeScope();
		} catch (EmptySymTableException ex) {
			System.err.println("remove scope empty exception");
		}
	}

	protected void addDecl(SymTable symT, String name, SemSym sym) {
		try {
			symT.addDecl(name, sym);
		} catch (DuplicateSymException e1) {
			System.err.println("addDecl failed:" + e1);
		} catch (EmptySymTableException e2) {
			System.err.println("addDecl failed:" + e2);
		} catch (NullPointerException e3) {
			System.err.println("addDecl failed:" + e3);
		}
	}
}

// **********************************************************************
// ProgramNode, DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
	public ProgramNode(DeclListNode L) {
		myDeclList = L;
	}

	public void nameAnalyze(SymTable symT) {
		addScope(symT);
		myDeclList.nameAnalyze(symT);
		removeScope(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		myDeclList.unparse(p, indent);
	}

	// 1 kid
	private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
	public DeclListNode(List<DeclNode> S) {
		myDecls = S;
	}

	public void nameAnalyze(SymTable symT) {
		Iterator it = myDecls.iterator();
		try {
			while (it.hasNext()) {
				((DeclNode) it.next()).nameAnalyze(symT);
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in DeclListNode.print");
			System.exit(-1);
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator it = myDecls.iterator();
		try {
			while (it.hasNext()) {
				((DeclNode) it.next()).unparse(p, indent);
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in DeclListNode.print");
			System.exit(-1);
		}
	}

	public HashMap<String, SemSym> getStField() {
		Iterator it = myDecls.iterator();
		HashMap<String, SemSym> stField = new HashMap<String, SemSym>();
		while (it.hasNext()) {
			DeclNode tmpNode = (DeclNode) it.next();
			if (tmpNode.getStField() == null)
				stField.put(tmpNode.getId(), new SemSym(tmpNode.getType()));
			else {
				// System.out.println("****not st null");
				stField.put(tmpNode.getId(), new SemSym(tmpNode.getType(), tmpNode.getStField()));
			}
		}
		return stField;
	}

	// list of kids (DeclNodes)
	private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
	public FormalsListNode(List<FormalDeclNode> S) {
		myFormals = S;
	}

	public void nameAnalyze(SymTable symT) {
		Iterator<FormalDeclNode> it = myFormals.iterator();
		if (it.hasNext()) { // if there is at least one element
			it.next().nameAnalyze(symT);
			while (it.hasNext()) { // print the rest of the list
				it.next().nameAnalyze(symT);
			}
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator<FormalDeclNode> it = myFormals.iterator();
		if (it.hasNext()) { // if there is at least one element
			it.next().unparse(p, indent);
			while (it.hasNext()) { // print the rest of the list
				p.print(", ");
				it.next().unparse(p, indent);
			}
		}
	}

	public List<String> getFormalsListTypes() {
		Iterator<FormalDeclNode> it = myFormals.iterator();
		ArrayList<String> formalsListTypes = new ArrayList<String>();
		if (it.hasNext()) { // if there is at least one element
			formalsListTypes.add(it.next().getType());
			while (it.hasNext()) { // print the rest of the list
				formalsListTypes.add(it.next().getType());
			}
		}
		return formalsListTypes;
	}

	// list of kids (FormalDeclNodes)
	private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
	public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
		myDeclList = declList;
		myStmtList = stmtList;
	}

	public void nameAnalyze(SymTable symT) {
		myDeclList.nameAnalyze(symT);
		myStmtList.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		myDeclList.unparse(p, indent);
		myStmtList.unparse(p, indent);
	}

	// 2 kids
	private DeclListNode myDeclList;
	private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
	public StmtListNode(List<StmtNode> S) {
		myStmts = S;
	}

	public void nameAnalyze(SymTable symT) {
		Iterator<StmtNode> it = myStmts.iterator();
		while (it.hasNext()) {
			it.next().nameAnalyze(symT);
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator<StmtNode> it = myStmts.iterator();
		while (it.hasNext()) {
			it.next().unparse(p, indent);
		}
	}

	// list of kids (StmtNodes)
	private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
	public ExpListNode(List<ExpNode> S) {
		myExps = S;
	}

	public void nameAnalyze(SymTable symT) {
		Iterator<ExpNode> it = myExps.iterator();
		if (it.hasNext()) { // if there is at least one element
			it.next().nameAnalyze(symT);
			while (it.hasNext()) { // print the rest of the list
				it.next().nameAnalyze(symT);
			}
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator<ExpNode> it = myExps.iterator();
		if (it.hasNext()) { // if there is at least one element
			it.next().unparse(p, indent);
			while (it.hasNext()) { // print the rest of the list
				p.print(", ");
				it.next().unparse(p, indent);
			}
		}
	}

	// list of kids (ExpNodes)
	private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
	abstract public String getId();

	abstract public String getType();

	abstract public HashMap<String, SemSym> getStField();
}

class VarDeclNode extends DeclNode {
	public VarDeclNode(TypeNode type, IdNode id, int size) {
		myType = type;
		myId = id;
		mySize = size;
	}

	public void nameAnalyze(SymTable symT) {
		myType.nameAnalyze(symT);
		String type = myType.getId();
		if (type == "int" || type == "bool" || type == "void")
			// not struct id
			myId.nameAnalyze_declId(symT, type);
		else // struct id
			myId.nameAnalyze_declStVar(symT, myType.getStField(), type);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myType.unparse(p, 0);
		p.print(" ");
		myId.unparse(p, 0);
		p.println(";");
	}

	public String getId() {
		return myId.getId();
	}

	public String getType() {
		return myType.getId();
	}

	public HashMap<String, SemSym> getStField() {
		return myId.getStField();
	}

	// 3 kids
	private TypeNode myType;
	private IdNode myId;
	private int mySize; // use value NOT_STRUCT if this is not a struct type

	public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
	public FnDeclNode(TypeNode type, IdNode id, FormalsListNode formalList, FnBodyNode body) {
		myType = type;
		myId = id;
		myFormalsList = formalList;
		myBody = body;
	}

	public void nameAnalyze(SymTable symT) {
		myType.nameAnalyze(symT);
		myId.nameAnalyze_declFn(symT, myFormalsList.getFormalsListTypes(), myType.getId());
		addScope(symT);
		myFormalsList.nameAnalyze(symT);
		myBody.nameAnalyze(symT);
		removeScope(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myType.unparse(p, 0);
		p.print(" ");
		myId.unparse(p, 0);
		p.print("(");
		myFormalsList.unparse(p, 0);
		p.println(") {");
		myBody.unparse(p, indent + 4);
		p.println("}\n");
	}

	public String getId() {
		return myId.getId();
	}

	public String getType() {
		return myType.getId();
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	// 4 kids
	private TypeNode myType;
	private IdNode myId;
	private FormalsListNode myFormalsList;
	private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
	public FormalDeclNode(TypeNode type, IdNode id) {
		myType = type;
		myId = id;
	}

	public void nameAnalyze(SymTable symT) {
		myType.nameAnalyze(symT);
		myId.nameAnalyze_declId(symT, myType.getId());
	}

	public void unparse(PrintWriter p, int indent) {
		myType.unparse(p, 0);
		p.print(" ");
		myId.unparse(p, 0);
	}

	public String getId() {
		return myId.getId();
	}

	public String getType() {
		return myType.getId();
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	// 2 kids
	private TypeNode myType;
	private IdNode myId;
}

class StructDeclNode extends DeclNode {
	public StructDeclNode(IdNode id, DeclListNode declList) {
		myId = id;
		myDeclList = declList;
	}

	public void nameAnalyze(SymTable symT) {
		addScope(symT);
		myDeclList.nameAnalyze(symT);
		HashMap<String, SemSym> tmpStField = myDeclList.getStField();
		removeScope(symT);
		myId.nameAnalyze_declSt(symT, tmpStField);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("struct ");
		myId.unparse(p, 0);
		p.println("{");
		myDeclList.unparse(p, indent + 4);
		doIndent(p, indent);
		p.println("};\n");

	}

	public String getType() {
		return "struct";
	}

	public String getId() {
		return myId.getId();
	}

	public HashMap<String, SemSym> getStField() {
		return myId.getStField();
	}

	// 2 kids
	private IdNode myId;
	private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
	abstract public String getId();

	abstract public HashMap<String, SemSym> getStField();
}

class IntNode extends TypeNode {
	public IntNode() {
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("int");
	}

	public String getId() {
		return "int";
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}
}

class BoolNode extends TypeNode {
	public BoolNode() {
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("bool");
	}

	public String getId() {
		return "bool";
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}
}

class VoidNode extends TypeNode {
	public VoidNode() {
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("void");
	}

	public String getId() {
		return "void";
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}
}

class StructNode extends TypeNode {
	public StructNode(IdNode id) {
		myId = id;
	}

	public void nameAnalyze(SymTable symT) {
		myId.nameAnalyze_stNode(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("struct ");
		myId.unparse(p, 0);
	}

	public String getId() {
		return myId.getId();
	}

	public HashMap<String, SemSym> getStField() {
		if (myId == null) {
			System.err.println("*stNode:myId null");
			System.exit(-1);
		}
		return myId.getStField();
	}

	// 1 kid
	private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
}

class AssignStmtNode extends StmtNode {
	public AssignStmtNode(AssignNode assign) {
		myAssign = assign;
	}

	public void nameAnalyze(SymTable symT) {
		myAssign.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myAssign.unparse(p, -1); // no parentheses
		p.println(";");
	}

	// 1 kid
	private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
	public PostIncStmtNode(ExpNode exp) {
		myExp = exp;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myExp.unparse(p, 0);
		p.println("++;");
	}

	// 1 kid
	private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
	public PostDecStmtNode(ExpNode exp) {
		myExp = exp;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myExp.unparse(p, 0);
		p.println("--;");
	}

	// 1 kid
	private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
	public ReadStmtNode(ExpNode e) {
		myExp = e;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("cin >> ");
		myExp.unparse(p, 0);
		p.println(";");
	}

	// 1 kid (actually can only be an IdNode or an ArrayExpNode)
	private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
	public WriteStmtNode(ExpNode exp) {
		myExp = exp;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("cout << ");
		myExp.unparse(p, 0);
		p.println(";");
	}

	// 1 kid
	private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
	public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
		myDeclList = dlist;
		myExp = exp;
		myStmtList = slist;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
		addScope(symT);
		myDeclList.nameAnalyze(symT);
		myStmtList.nameAnalyze(symT);
		removeScope(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("if (");
		myExp.unparse(p, 0);
		p.println(") {");
		myDeclList.unparse(p, indent + 4);
		myStmtList.unparse(p, indent + 4);
		doIndent(p, indent);
		p.println("}");
	}

	// e kids
	private ExpNode myExp;
	private DeclListNode myDeclList;
	private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
	public IfElseStmtNode(ExpNode exp, DeclListNode dlist1, StmtListNode slist1, DeclListNode dlist2,
			StmtListNode slist2) {
		myExp = exp;
		myThenDeclList = dlist1;
		myThenStmtList = slist1;
		myElseDeclList = dlist2;
		myElseStmtList = slist2;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
		addScope(symT);
		myThenDeclList.nameAnalyze(symT);
		myThenStmtList.nameAnalyze(symT);
		removeScope(symT);
		addScope(symT);
		myElseDeclList.nameAnalyze(symT);
		myElseStmtList.nameAnalyze(symT);
		removeScope(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("if (");
		myExp.unparse(p, 0);
		p.println(") {");
		myThenDeclList.unparse(p, indent + 4);
		myThenStmtList.unparse(p, indent + 4);
		doIndent(p, indent);
		p.println("}");
		doIndent(p, indent);
		p.println("else {");
		myElseDeclList.unparse(p, indent + 4);
		myElseStmtList.unparse(p, indent + 4);
		doIndent(p, indent);
		p.println("}");
	}

	// 5 kids
	private ExpNode myExp;
	private DeclListNode myThenDeclList;
	private StmtListNode myThenStmtList;
	private StmtListNode myElseStmtList;
	private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
	public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
		myExp = exp;
		myDeclList = dlist;
		myStmtList = slist;
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
		addScope(symT);
		myDeclList.nameAnalyze(symT);
		myStmtList.nameAnalyze(symT);
		removeScope(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("while (");
		myExp.unparse(p, 0);
		p.println(") {");
		myDeclList.unparse(p, indent + 4);
		myStmtList.unparse(p, indent + 4);
		doIndent(p, indent);
		p.println("}");
	}

	// 3 kids
	private ExpNode myExp;
	private DeclListNode myDeclList;
	private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
	public CallStmtNode(CallExpNode call) {
		myCall = call;
	}

	public void nameAnalyze(SymTable symT) {
		myCall.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myCall.unparse(p, indent);
		p.println(";");
	}

	// 1 kid
	private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
	public ReturnStmtNode(ExpNode exp) {
		myExp = exp;
	}

	public void nameAnalyze(SymTable symT) {
		if (myExp != null)
			myExp.nameAnalyze(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("return");
		if (myExp != null) {
			p.print(" ");
			myExp.unparse(p, 0);
		}
		p.println(";");
	}

	// 1 kid
	private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
	abstract public HashMap<String, SemSym> getStField();

	abstract public String getId();

	abstract public void nameAnalyze_LHS(SymTable symT);

	abstract public void nameAnalyze(SymTable symT);

	abstract public boolean isLoc();
}

class IntLitNode extends ExpNode {
	public IntLitNode(int lineNum, int charNum, int intVal) {
		myLineNum = lineNum;
		myCharNum = charNum;
		myIntVal = intVal;
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print(myIntVal);
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}

	private int myLineNum;
	private int myCharNum;
	private int myIntVal;
}

class StringLitNode extends ExpNode {
	public StringLitNode(int lineNum, int charNum, String strVal) {
		myLineNum = lineNum;
		myCharNum = charNum;
		myStrVal = strVal;
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print(myStrVal);
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return myStrVal;
	}

	public boolean isLoc() {
		return false;
	}

	private int myLineNum;
	private int myCharNum;
	private String myStrVal;
}

class TrueNode extends ExpNode {
	public TrueNode(int lineNum, int charNum) {
		myLineNum = lineNum;
		myCharNum = charNum;
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("true");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return "true";
	}

	public boolean isLoc() {
		return false;
	}

	private int myLineNum;
	private int myCharNum;
}

class FalseNode extends ExpNode {
	public FalseNode(int lineNum, int charNum) {
		myLineNum = lineNum;
		myCharNum = charNum;
	}

	public void nameAnalyze(SymTable symT) {
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("false");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return "false";
	}

	public boolean isLoc() {
		return false;
	}

	private int myLineNum;
	private int myCharNum;
}

class IdNode extends ExpNode {
	public IdNode(int lineNum, int charNum, String strVal) {
		myLineNum = lineNum;
		myCharNum = charNum;
		myStrVal = strVal;
	}

	public void nameAnalyze(SymTable symT) {
		mySym = symT.lookupGlobal(myStrVal);
		if (mySym == null) {
			ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
			ErrMsg.isError = true;
		} else {
			myIdType = mySym.getType();
		}
	}

	public void nameAnalyze_declId(SymTable symT, String type) {
		myIdType = type;
		mySym = new SemSym(myIdType);
		if (myIdType == "void") {
			ErrMsg.fatal(myLineNum, myCharNum, "Non-function declared void");
			ErrMsg.isError = true;
		}
		if (symT.lookupLocal(myStrVal) == null) {
			addDecl(symT, myStrVal, mySym);
		} else {
			ErrMsg.fatal(myLineNum, myCharNum, "Multiply declared identifier");
			ErrMsg.isError = true;
		}
	}

	public void nameAnalyze_declFn(SymTable symT, List<String> formalsListTypes, String fnType) {
		myIdType = fnType;
		mySym = new SemSym(myIdType, formalsListTypes);
		if (symT.lookupLocal(myStrVal) == null) {
			addDecl(symT, myStrVal, mySym);
		} else {
			ErrMsg.fatal(myLineNum, myCharNum, "Multiply declared identifier");
			ErrMsg.isError = true;
		}
	}

	public void nameAnalyze_declSt(SymTable symT, HashMap<String, SemSym> stField) {
		myIdType = "struct";
		mySym = new SemSym(myIdType, stField);
		if (symT.lookupLocal(myStrVal) == null) {
			addDecl(symT, myStrVal, mySym);
		} else {
			ErrMsg.fatal(myLineNum, myCharNum, "Multiply declared identifier");
			ErrMsg.isError = true;
		}
	}

	public void nameAnalyze_declStVar(SymTable symT, HashMap<String, SemSym> stField, String type) {
		if (stField == null) {
			return;
		}
		myIdType = type;
		myStField = stField;
		mySym = new SemSym(type, stField);
		if (symT.lookupLocal(myStrVal) == null) {
			addDecl(symT, myStrVal, mySym);
		} else {
			ErrMsg.fatal(myLineNum, myCharNum, "Multiply declared identifier");
			ErrMsg.isError = true;
		}
	}

	public void nameAnalyze_stNode(SymTable symT) {
		mySym = symT.lookupGlobal(myStrVal);
		if (mySym != null) {
			myStField = mySym.getStField();
		} else {
			ErrMsg.fatal(myLineNum, myCharNum, "Invalid name of struct type");
			ErrMsg.isError = true;
		}
	}

	public void nameAnalyze_dotRHS(SymTable symT, String LHSId) {
		mySym = symT.lookupGlobal(LHSId);
		if (mySym != null) {
			myStField = mySym.getStField();
			if (myStField == null) {
				ErrMsg.fatal(myLineNum, myCharNum, "Invalid struct field name");
				ErrMsg.isError = true;
			} else {
				if (myStField.containsKey(myStrVal) == true) {
					myIdType = myStField.get(myStrVal).getType();
				} else {
					ErrMsg.fatal(myLineNum, myCharNum, "Invalid struct field name");
					ErrMsg.isError = true;
				}
			}
		} else {
		}
	}

	public void nameAnalyze_dotRHS(HashMap<String, SemSym> stField, String LHSId) {
		if (stField == null) {
			ErrMsg.fatal(myLineNum, myCharNum, "Dot-access of non-struct type");
			ErrMsg.isError = true;
			return;
		}
		mySym = stField.get(LHSId);
		if (mySym != null) {
			myStField = mySym.getStField();
			if (myStField == null) {
				ErrMsg.fatal(myLineNum, myCharNum, "Dot-access of non-struct type");
				ErrMsg.isError = true;
			} else {
				if (myStField.containsKey(myStrVal) == true) {
					myIdType = myStField.get(myStrVal).getType();
				} else {
					ErrMsg.fatal(myLineNum, myCharNum, "Invalid struct field name");
					ErrMsg.isError = true;
				}
			}
		} else {
		}
	}

	public void nameAnalyze_LHS(SymTable symT) {
		mySym = symT.lookupGlobal(myStrVal);
		if (mySym != null) {
			myIdType = mySym.getType();
			if (symT.lookupGlobal(mySym.getType()) == null) {
				ErrMsg.fatal(myLineNum, myCharNum, "Dot-access of non-struct type");
				ErrMsg.isError = true;
			}

		} else {
			ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
			ErrMsg.isError = true;
		}
	}

	public void nameAnalyze_fnCall(SymTable symT) {
		mySym = symT.lookupGlobal(myStrVal);
		if (mySym == null) {
			ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
			ErrMsg.isError = true;
		} else {
			myIdType = mySym.getType();
			myFormalsListTypes = mySym.getFormalsListTypes();
		}
	}

	public void unparse(PrintWriter p, int indent) {
		p.print(myStrVal);
		p.print("(" + myIdType + ")");
	}

	public void unparse_decl(PrintWriter p, int indent) {
		p.print(myStrVal);
	}

	public void unparse_fnCall(PrintWriter p, int indent) {
		p.print(myStrVal);
		p.print("(");
		if (myFormalsListTypes != null) {
			Iterator it = myFormalsListTypes.iterator();
			try {
				if (it.hasNext())
					p.print(it.next());
				while (it.hasNext()) {
					p.print(", " + it.next());
				}
			} catch (NoSuchElementException ex) {
				System.err.println("unexpected NoSuchElementException in DeclListNode.print");
				System.exit(-1);
			}
		}
		p.print("->" + myIdType + ")");
	}

	public void setIdType(String IdType) {
		myIdType = IdType;
	}

	public String getId() {
		return myStrVal;
	}

	public HashMap<String, SemSym> getStField() {
		if (mySym == null) {
			// System.out.println("****IdNode: mySym null");
			return null;
		}
		return myStField;
	}

	public boolean isLoc() {
		return false;
	}

	private int myLineNum;
	private int myCharNum;
	private String myStrVal;
	private String myIdType;
	private SemSym mySym;
	private List<String> myFormalsListTypes;
	private HashMap<String, SemSym> myStField;
}

class DotAccessExpNode extends ExpNode {
	public DotAccessExpNode(ExpNode loc, IdNode id) {
		myLoc = loc;
		myId = id;
	}

	public void nameAnalyze(SymTable symT) {
		if (myLoc.isLoc() == false) {
			myLoc.nameAnalyze_LHS(symT);
			myId.nameAnalyze_dotRHS(symT, myLoc.getId());
		} else {
			myLoc.nameAnalyze(symT);
			myId.nameAnalyze_dotRHS(myLoc.getStField(), myLoc.getId());
		}
	}

	public void nameAnalyze_LHS(SymTable symT) {
		myLoc.nameAnalyze_LHS(symT);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myLoc.unparse(p, 0);
		p.print(").");
		myId.unparse(p, 0);
	}

	public HashMap<String, SemSym> getStField() {
		return myId.getStField();
	}

	public String getId() {
		return myId.getId();
	}

	public boolean isLoc() {
		return true;
	}

	// 2 kids
	private ExpNode myLoc;
	private IdNode myId;
	private boolean flag;
}

class AssignNode extends ExpNode {
	public AssignNode(ExpNode lhs, ExpNode exp) {
		myLhs = lhs;
		myExp = exp;
	}

	public void nameAnalyze(SymTable symT) {
		myLhs.nameAnalyze(symT);
		myExp.nameAnalyze(symT);
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		if (indent != -1)
			p.print("(");
		myLhs.unparse(p, 0);
		p.print(" = ");
		myExp.unparse(p, 0);
		if (indent != -1)
			p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}

	// 2 kids
	private ExpNode myLhs;
	private ExpNode myExp;
}

class CallExpNode extends ExpNode {
	public CallExpNode(IdNode name, ExpListNode elist) {
		myId = name;
		myExpList = elist;
	}

	public CallExpNode(IdNode name) {
		myId = name;
		myExpList = new ExpListNode(new LinkedList<ExpNode>());
	}

	public void nameAnalyze(SymTable symT) {
		myId.nameAnalyze_fnCall(symT);
		if (myExpList != null)
			myExpList.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	// ** unparse **
	public void unparse(PrintWriter p, int indent) {
		myId.unparse(p, 0);
		p.print("(");
		if (myExpList != null) {
			myExpList.unparse(p, 0);
		}
		p.print(")");
	}

	public boolean isLoc() {
		return false;
	}

	// 2 kids
	private IdNode myId;
	private ExpListNode myExpList; // possibly null
}

abstract class UnaryExpNode extends ExpNode {
	public UnaryExpNode(ExpNode exp) {
		myExp = exp;
	}

	// one child
	protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
	public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
		myExp1 = exp1;
		myExp2 = exp2;
	}

	// two kids
	protected ExpNode myExp1;
	protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
	public UnaryMinusNode(ExpNode exp) {
		super(exp);
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public boolean isLoc() {
		return false;
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(-");
		myExp.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}
}

class NotNode extends UnaryExpNode {
	public NotNode(ExpNode exp) {
		super(exp);
	}

	public void nameAnalyze(SymTable symT) {
		myExp.nameAnalyze(symT);
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(!");
		myExp.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
	public PlusNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);
	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" + ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class MinusNode extends BinaryExpNode {
	public MinusNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" - ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class TimesNode extends BinaryExpNode {
	public TimesNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" * ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public String getId() {
		return null;
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class DivideNode extends BinaryExpNode {
	public DivideNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" / ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class AndNode extends BinaryExpNode {
	public AndNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" && ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class OrNode extends BinaryExpNode {
	public OrNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" || ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class EqualsNode extends BinaryExpNode {
	public EqualsNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" == ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class NotEqualsNode extends BinaryExpNode {
	public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" != ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class LessNode extends BinaryExpNode {
	public LessNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" < ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class GreaterNode extends BinaryExpNode {
	public GreaterNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" > ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class LessEqNode extends BinaryExpNode {
	public LessEqNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" <= ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}

class GreaterEqNode extends BinaryExpNode {
	public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void nameAnalyze(SymTable symT) {
		myExp1.nameAnalyze(symT);
		myExp2.nameAnalyze(symT);

	}

	public void nameAnalyze_LHS(SymTable symT) {
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" >= ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public HashMap<String, SemSym> getStField() {
		return null;
	}

	public String getId() {
		return null;
	}

	public boolean isLoc() {
		return false;
	}
}
