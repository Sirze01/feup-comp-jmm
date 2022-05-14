package pt.up.fe.comp.optimization;

import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.ast.AstUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;
import java.util.Map;

public class OllirExpressionGenerator extends AJmmVisitor<Boolean, String> {
    private final List<Report> reports;
    private final SymbolTable symbolTable;
    private final String methodSignature;
    private final Map<String, String> config;

    private int indent;
    private int[] tempCount;
    private int stmtDepth = 0;

    private final StringBuilder code = new StringBuilder();

    private int getNumSpaces(int indent) {
        return indent * OllirGeneratorUtils.getNumSpaces(config);
    }

    OllirExpressionGenerator(List<Report> reports, SymbolTable symbolTable, int indent, int[] tempCount, String methodSignature) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        this.indent = indent;
        this.tempCount = tempCount;
        this.methodSignature = methodSignature;
        this.config = null;

        addVisits();
    }

    OllirExpressionGenerator(Map<String, String> config, List<Report> reports, SymbolTable symbolTable, int indent, int[] tempCount, String methodSignature) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        this.indent = indent;
        this.tempCount = tempCount;
        this.methodSignature = methodSignature;
        this.config = config;

        addVisits();
    }

    private void addVisits() {
        addVisit("Statement", this::statementVisit);
        addVisit("ReturnExpression", this::returnExpressionVisit);
        addVisit("IDAssignment", this::idAssignmentVisit);
        addVisit("ArrayAssignment", this::arrayAssignmentVisit);

        addVisit("BinOp", this::binOpVisit);
        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("ArrayExpression", this::arrayExpressionVisit);
        addVisit("Literal", this::literalVisit);
        addVisit("ID", this::idVisit);
        addVisit("_New", this::newVisit);

        setDefaultVisit(this::defaultVisit);
    }

    private String generateTmp(String type) {
        return "tmp" + tempCount[0]++ + "." + OllirGeneratorUtils.toOllirType(type);
    }

    public String getCode() {
        return code.toString();
    }

    private String defaultVisit(JmmNode node, Boolean dummy) {
        return "";
    }

    private String returnExpressionVisit(JmmNode returnNode, Boolean dummy) {
        StringBuilder returnExpression = new StringBuilder();

        returnExpression.append(" ".repeat(getNumSpaces(indent)));
        returnExpression.append("ret.").append(OllirGeneratorUtils.toOllirType(symbolTable.getReturnType(methodSignature))).append(" ");

        returnExpression.append(visit(returnNode.getJmmChild(0)));

        returnExpression.append(";\n");
        code.append(returnExpression);
        return returnExpression.toString();
    }

    private String statementVisit(JmmNode statementNode, Boolean dummy) {
        stmtDepth++;
        StringBuilder statement = new StringBuilder();

        statement.append(visit(statementNode.getJmmChild(0)));

        stmtDepth--;
        if (stmtDepth == 0) {
            code.append(statement);
        }

        return statement.toString();
    }

    private String idAssignmentVisit(JmmNode assignmentNode, Boolean dummy) {
        StringBuilder assignmentStmt = new StringBuilder();

        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, assignmentNode.getJmmChild(0).get("name"));

        assignmentStmt.append(" ".repeat(getNumSpaces(indent)));
        assignmentStmt.append(s.getName() + "." + OllirGeneratorUtils.toOllirType(s.getType()));
        assignmentStmt.append(" :=." + OllirGeneratorUtils.toOllirType(s.getType()) + " ");
        assignmentStmt.append(visit(assignmentNode.getJmmChild(1)));
        assignmentStmt.append(";\n");

        return assignmentStmt.toString();
    }

    private String arrayAssignmentVisit(JmmNode assignmentNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder assignmentStmt = new StringBuilder();

        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, assignmentNode.getJmmChild(0).get("name"));
        String idx = "tmp" + tempCount[0]++ + ".i32";
        before.append(" ".repeat(getNumSpaces(indent)));
        before.append(idx + " :=.i32 " + visit(assignmentNode.getJmmChild(1).getJmmChild(0)) + ";\n");

        assignmentStmt.append(" ".repeat(getNumSpaces(indent)));
        assignmentStmt.append(s.getName() + "[" + idx + "].");
        assignmentStmt.append(OllirGeneratorUtils.toOllirType(s.getType().getName()));
        assignmentStmt.append(" :=." + OllirGeneratorUtils.toOllirType(s.getType().getName()) + " ");
        assignmentStmt.append(visit(assignmentNode.getJmmChild(2)));
        assignmentStmt.append(";\n");

        code.append(before);
        return assignmentStmt.toString();
    }


    private String binOpVisit(JmmNode binOpNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder binOpStmt = new StringBuilder();

        String[] op = {"", ""};
        for (int i = 0; i < 2; i++) {
            if (!(binOpNode.getJmmChild(i).getKind().equals("BinOp") ||
                    binOpNode.getJmmChild(i).getKind().equals("ArrayExpression") ||
                    binOpNode.getJmmChild(i).getKind().equals("AccessExpression") ||
                    binOpNode.getJmmChild(i).getKind().equals("CallExpression") ||
                    binOpNode.getJmmChild(i).getKind().equals("UnaryOp"))) {
                op[i] = visit(binOpNode.getJmmChild(i));
                continue;
            }


            JmmNode child = AstUtils.getFirstDescendantOfKind(binOpNode.getJmmChild(i), "Literal");
            String type = "";
            if (child != null) {
                type = OllirGeneratorUtils.toOllirType(child.get("type"));
            } else {
                child = AstUtils.getFirstDescendantOfKind(binOpNode.getJmmChild(i), "ID");
                Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, child.get("name"));
                if (s == null) {
                    s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, child.get("name"));
                }
                type = OllirGeneratorUtils.toOllirType(s.getType().getName());
            }
            op[i] = generateTmp(type);


            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(op[i] + " :=." + type + " " + visit(binOpNode.getJmmChild(i)) + ";\n");
        }


        String opType = OllirGeneratorUtils.getTypeFromOllirVar(op[0]);
        binOpStmt.append(op[0]);
        switch (binOpNode.get("op")) {
            case "And" -> binOpStmt.append(" &&.")
                    .append(opType);
            case "Less" -> binOpStmt.append(" <.")
                    .append(opType);
            case "Add" -> binOpStmt.append(" +.")
                    .append(opType);
            case "Sub" -> binOpStmt.append(" -.")
                    .append(opType);
            case "Mult" -> binOpStmt.append(" *.")
                    .append(opType);
            case "Div" -> binOpStmt.append(" /.")
                    .append(opType);
            default -> {
            }
        }
        binOpStmt.append(" " + op[1]);

        code.append(before);
        return binOpStmt.toString();
    }

    private String unaryOpVisit(JmmNode unaryNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder unaryOpStmt = new StringBuilder();

        String op;

        if (!(unaryNode.getJmmChild(0).getKind().equals("BinOp") ||
                unaryNode.getJmmChild(0).getKind().equals("ArrayExpression") ||
                unaryNode.getJmmChild(0).getKind().equals("AccessExpression") ||
                unaryNode.getJmmChild(0).getKind().equals("CallExpression"))) {
            op = visit(unaryNode.getJmmChild(0));
        } else {
            JmmNode child = AstUtils.getFirstDescendantOfKind(unaryNode, "Literal");
            String type = "";
            if (child != null) {
                type = OllirGeneratorUtils.toOllirType(child.get("type"));
            } else {
                child = AstUtils.getFirstDescendantOfKind(unaryNode.getJmmChild(0), "ID");
                Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, child.get("name"));
                if (s == null) {
                    s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, child.get("name"));
                }
                type = OllirGeneratorUtils.toOllirType(s.getType());
            }
            op = generateTmp(type);


            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(op + " :=." + type + " " + visit(unaryNode.getJmmChild(0)) + ";\n");
        }

        String opType = OllirGeneratorUtils.getTypeFromOllirVar(op);
        switch (unaryNode.get("op")) {
            case "Not" -> unaryOpStmt.append("!.")
                    .append(opType);
            default -> {
            }
        }
        unaryOpStmt.append(" " + op);

        code.append(before);
        return unaryOpStmt.toString();
    }

    private String arrayExpressionVisit(JmmNode arrayNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder arrayStmt = new StringBuilder();

        String expResult;

        if (!(arrayNode.getJmmChild(1).getKind().equals("BinOp") ||
                arrayNode.getJmmChild(1).getKind().equals("ArrayExpression") ||
                arrayNode.getJmmChild(1).getKind().equals("AccessExpression") ||
                arrayNode.getJmmChild(1).getKind().equals("CallExpression"))) {
            expResult = visit(arrayNode.getJmmChild(1));
        } else {
            JmmNode child = AstUtils.getFirstDescendantOfKind(arrayNode.getJmmChild(1), "Literal");
            String type;
            if (child != null) {
                type = OllirGeneratorUtils.toOllirType(child.get("type"));
            } else {
                child = AstUtils.getFirstDescendantOfKind(arrayNode.getJmmChild(1), "ID");
                Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, child.get("name"));
                if (s == null) {
                    s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, child.get("name"));
                }
                type = OllirGeneratorUtils.toOllirType(s.getType());
            }
            expResult = generateTmp(type);


            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(expResult + " :=." + type + " " + visit(arrayNode.getJmmChild(1)) + ";\n");
        }

        String opType = OllirGeneratorUtils.getTypeFromOllirVar(expResult);

        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, arrayNode.getJmmChild(0).get("name"));
        if (s == null) {
            s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, arrayNode.getJmmChild(0).get("name"));
        }
        arrayStmt.append(s.getName() + "[")
                .append(expResult)
                .append("].")
                .append(OllirGeneratorUtils.toOllirType(s.getType().getName()));

        code.append(before);
        return arrayStmt.toString();
    }

    private String literalVisit(JmmNode literalNode, Boolean dummy) {
        return OllirGeneratorUtils.getCodeLiteral(symbolTable, literalNode);
    }

    private String newVisit(JmmNode newNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder newStmt = new StringBuilder();

        if (newNode.get("type").equals("IntArray")) {
            String tmp = generateTmp(newNode.getJmmChild(0).get("type"));

            newStmt.append(tmp);

            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(tmp)
                    .append(" :=.")
                    .append(OllirGeneratorUtils.toOllirType(newNode.get("type")));
            before.append(" new(");
            before.append("array,");
            before.append(visit(newNode.getJmmChild(0)));
            before.append(").array.i32");
            before.append(";\n");
        } else {
            String tmp = generateTmp(newNode.getJmmChild(0).get("type"));

            newStmt.append(tmp);

            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(tmp)
                    .append(" :=.")
                    .append(OllirGeneratorUtils.toOllirType(newNode.getJmmChild(0).get("type")));
            before.append(" new(");
            before.append(OllirGeneratorUtils.toOllirType(newNode.getJmmChild(0).get("type")));
            before.append(").");
            before.append(OllirGeneratorUtils.toOllirType(newNode.getJmmChild(0).get("type")));
            before.append(";\n");

            before.append(" ".repeat(getNumSpaces(indent)));
            before.append("invokespecial(")
                    .append(newStmt)
                    .append(", \"<init>\").V;\n");
        }

        code.append(before);
        return newStmt.toString();
    }

    private String idVisit(JmmNode idNode, Boolean dummy) {
        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, idNode.get("name"));
        if (s == null) {
            s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, idNode.get("name"));
            assert s != null;
            int idx = symbolTable.getParameters(methodSignature).indexOf(s);

            if (JmmSymbolTable.isMain(methodSignature)) {
                return "$" + idx + idNode.get("name") + "." + OllirGeneratorUtils.toOllirType(s.getType());
            }

            return "$" + ++idx + idNode.get("name") + "." + OllirGeneratorUtils.toOllirType(s.getType());
        }

        return idNode.get("name") + "." + OllirGeneratorUtils.toOllirType(s.getType());
    }
}
