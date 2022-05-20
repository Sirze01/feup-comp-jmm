package pt.up.fe.comp.analysis.visitors;

import jas.Var;
import org.eclipse.jgit.util.SystemReader;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.analysis.JmmMethod;

import pt.up.fe.comp.jmm.ast.JmmVisitor;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VariablesVisitor extends AJmmVisitor<List<Report>, String> {
    JmmSymbolTable symbolTable;
    List<String> variables;

    public VariablesVisitor(JmmSymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.variables = new ArrayList<>();
        addVisit("ArrayExpression", this::visitArrayExp);
        addVisit("_New", this::visitNew);
        addVisit("AccessExpression", this::visitObjectMethod);
        addVisit("CallExpression", this::visitCallMethod);
        addVisit("MemberArgs", this::visitMemberArgs);

        addVisit("IDAssignment", this::visitIDAssignment);
        addVisit("InheritanceDeclaration", this::visitInheritance);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("MethodHeader", this::visitMethodHeader);
        addVisit("Type", this::visitType);
        addVisit("Literal", this::visitLiteral);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ID", this::visitID);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("BinOp", this::visitBinOp);
        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, List<Report> reports){
        for (JmmNode child : jmmNode.getChildren())
            visit(child, reports);
        return "";
    }

    private String visitInheritance(JmmNode node, List<Report> reports){
        System.out.println("Herança");
        return "";
    }

    private String visitArrayExp(JmmNode node, List<Report> reports){
        for(JmmNode children : node.getChildren()){
            if(children.getKind().equals("Literal")){
                String idxType = visit(children, reports);
                if(!idxType.equals("Int")){
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("column")),
                            "Array indexes must be of type int."
                    ));
                    return "<Invalid>";
                }
            }
        }

        return "";
    }

    private String visitType(JmmNode node, List<Report> reports){
        return node.get("type");
    }

    private String visitLiteral(JmmNode node, List<Report> reports){
        return node.get("type");
    }

    private String visitNew(JmmNode jmmNode, List<Report> reports){
        String newTypeArray = jmmNode.get("type");
        String newType = newTypeArray.substring(0, newTypeArray.length() - 5);

        String childType = visit(jmmNode.getJmmChild(0), reports);

        if (childType.equals(newType)) {
            return newTypeArray + "Expression";
        } else {
            reports.add(new Report(
                    ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("column")),
                    "Array length must be of type int."
            ));
        }
        return childType;
    }

    private String visitVarDeclaration(JmmNode node, List<Report> reports){
        List<String> types = new ArrayList<>(Arrays.asList("Int", "IntArray", "Boolean", symbolTable.getClassName()));

        for (String imp: symbolTable.getImports()){
            String[] impList = imp.split("\\.");
            String type = impList[impList.length-1];
            types.add(type);
        }

        if (!types.contains(node.getJmmChild(0).get("type"))){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC, Integer.parseInt(node.getJmmChild(0).get("line")),
                    Integer.parseInt(node.getJmmChild(0).get("column")),
                    "Type \"" + node.getJmmChild(0).get("type") + "\" is not defined."));
            return "<Invalid>";
        }

        //System.out.println(visitType(node.getJmmChild(0), reports) +  " LILI");
        return visitType(node.getJmmChild(0), reports);
    }

    private String visitIDAssignment(JmmNode jmmNode, List<Report> reports){
        JmmNode id0 = jmmNode.getJmmChild(0);
        //System.out.println("id0: " + id0);
        JmmNode id1 = jmmNode.getJmmChild(1);
        //System.out.println("id1: " + id1);

        if (!id0.getKind().equals("ID")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(id0.get("line")),
                    Integer.parseInt(id0.get("column")),
                    id0.getKind() + " is not an ID"
            ));
        }

        String ret0 = visit(id0, reports);
        //System.out.println("first var:" + ret0);
        String ret1 = visit(id1, reports);
        //System.out.println("second var:" + ret1);

        //  String methodSignature = symbolTable.getParentMethodName(id1).toString();
        //Symbol s =  symbolTable.getLocalVar(methodSignature, id1.get("name"));

        if (!(ret0.equals("IntArray") && ret1.equals("Int")) && !ret0.equals(ret1) && !ret1.equals("import") && !ret1.equals("extends")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(id1.get("line")),
                    Integer.parseInt(id1.get("column")),
                    "Type mismatch in operation. '" + ret0 + "' to '" + ret1 + "'"
            ));
        } else {
            if (id0.getKind().equals("ID"))
                variables.add(id0.get("name"));
            else
                variables.add(id0.getChildren().get(0).get("name"));
        }

        return "";
    }

    private String visitID(JmmNode jmmNode, List<Report> reports){

            Optional<JmmNode> ancestor = jmmNode.getAncestor("MainMethod").isPresent() ? jmmNode.getAncestor("MainMethod") : jmmNode.getAncestor("MethodBody");

            if (ancestor.isEmpty())
                return "";

            JmmMethod method = symbolTable.getParentMethodName(jmmNode);

            if (method == null){
                if (!checkImports(jmmNode.get("name"))) {
                    reports.add(new Report(
                            ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(jmmNode.get("line")),
                            Integer.parseInt(jmmNode.get("column")),
                            "Variable \"" + jmmNode.get("name") + "\" is undefined."
                    ));
                    return "<Invalid>";
                }
                return "";
            }

            Optional<String> name = jmmNode.getOptional("name");
            if(name.isEmpty()){
                return "";
            }

            Symbol s = symbolTable.getLocalVar(method.toString(), name.get());

            checkInitializedVariable(jmmNode, reports, method, s);

            String ret = "<Invalid>";
            if (s != null)
                ret = s.getType().isArray() ? s.getType().getName() + "ArrayExpression" : s.getType().getName();
            return ret;

    }

    private String visitObjectMethod(JmmNode node, List<Report> reports){
       if(node.getJmmChild(0).getKind().equals("Literal") && node.getJmmChild(0).get("value").equals("this") && symbolTable.getSuper() == null){
           if (symbolTable.getMethodByName(node.getJmmChild(1).getJmmChild(0).get("name")) == null){
               reports.add(new Report(ReportType.ERROR,
                       Stage.SEMANTIC,
                       node.getJmmChild(1).getJmmChild(0).get("line") != null ? Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("line")) : 0,
                       Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("column")),
                       "Method " + node.getJmmChild(1).getJmmChild(0).get("name") + "() isn't declared"));
           }
           else if (node.getJmmChild(1).getJmmChild(1).getChildren().size()
               != symbolTable.getMethodByName(node.getJmmChild(1).getJmmChild(0).get("name")).getParameters().size()){
                 reports.add(new Report(ReportType.ERROR,
                         Stage.SEMANTIC,
                         node.getJmmChild(1).getJmmChild(0).get("line") != null ? Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("line")) : 0,
                         Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("column")),
                         "Method " + node.getJmmChild(1).getJmmChild(0).get("name") + "() has the wrong number of arguments"));
           }
       }

       for(JmmNode children: node.getChildren())
           visit(children, reports);

       return "Method";
    }

    private String visitCallMethod(JmmNode node, List<Report> reports){

        for (JmmNode child : node.getChildren())
            visit(child, reports);

        if(symbolTable.getMethodByName(node.getJmmChild(0).get("name")) != null){
            return symbolTable.getMethodByName(node.getJmmChild(0).get("name")).getReturnType().getName();
        }
        else if (!node.getAncestor("AccessExpression").get().getJmmChild(0).getKind().equals("Literal")){
            if (symbolTable.getImports().contains(node.getAncestor("AccessExpression").get().getChildren().get(0).get("name"))){
                return "import";
            }
            else if (symbolTable.getSuper() != null) {
                Optional<JmmNode> ancestor = node.getAncestor("MainMethod").isPresent() ? node.getAncestor("MainMethod") : node.getAncestor("InstanceMethodHeader");
                String varName = node.getAncestor("AccessExpression").get().getChildren().get(0).get("name");
                Symbol symbol = node.getAncestor("MainMethod").isPresent() ? symbolTable.getFieldByName(varName) : symbolTable.getLocalVar(varName, ancestor.get().getJmmChild(1).get("name")) ;
                if (symbol.getType().getName().equals(symbolTable.getClassName())){
                    return "extends";
                }
            }
        }

        return "";
    }

    private String visitMemberArgs(JmmNode node, List<Report> reports){
        JmmMethod ancestor = symbolTable.getParentMethodName(node);
        if(ancestor == null) return "";

        int i = 0;
        for(JmmNode child: node.getChildren()){
            String methodName = node.getJmmParent().getJmmChild(0).get("name");

            if(child.getKind().equals("ID")){
                String symbolName = child.get("name");
                Boolean isMain = ancestor.getName() == "main";

                Symbol symbol = isMain ? symbolTable.getFieldByName(symbolName) : symbolTable.getLocalVar(methodName, symbolName) ;
                if (symbolTable.getMethodByName(methodName) != null && !symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName().equals(symbol.getType().getName())) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            child.get("line") != null ? Integer.parseInt(child.get("line")) : 0,
                            Integer.parseInt(child.get("column")),
                            "Argument " + child.get("name") + " of type " +  symbol.getType() +  " is of wrong type, " +
                                    symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName() + " expected at " +  methodName + " method."));
                }
            }
            else if (i < symbolTable.getMethodByName(methodName).getParameters().size() && !symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName().equals(child.get("type"))) {
                reports.add(new Report(ReportType.ERROR,
                        Stage.SEMANTIC, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0,
                        Integer.parseInt(child.get("column")),
                        "Argument is of wrong type, " + symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName() + " expected."));
            }
            visit(child, reports);
            i++;
        }

        return "";
    }

    private String visitBinOp(JmmNode node, List<Report> reports){



        //System.out.println("popo op: " + node.get("op"));
        return "";
    };

    private String visitUnaryOp(JmmNode node, List<Report> reports){
        //System.out.println(" glulu ");
        return "UnaryOp";
    }
    private String visitAndMethod(JmmNode node, List<Report> reports){

        return "";
    }

    private String visitMethodHeader(JmmNode node, List<Report> reports) {
        JmmNode identifier = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);
        visit(identifier, reports);
        return visit(method, reports);
    }
    private String visitMethodBody(JmmNode node, List<Report> reports){
        variables.clear();
        return defaultVisit(node, reports);
    }


    private void checkInitializedVariable(JmmNode jmmNode, List<Report>  reports, JmmMethod method, Symbol s){
     /*if (jmmNode.getJmmParent().getKind().equals("BinOp")
                    || (jmmNode.getJmmParent().getKind().equals("IDAssignment")
                    && !jmmNode.getJmmParent().getJmmChild(0).equals(jmmNode)))
            {
                if (!jmmNode.getJmmParent().getJmmChild(0).equals(jmmNode)){
                    if (!variables.contains(jmmNode.get("name"))
                            && !symbolTable.getMethodObject(method.toString()).getParameters().contains(s)
                            && !symbolTable.getFields().contains(s)) {
                        reports.add(new Report(
                                ReportType.ERROR,
                                Stage.SEMANTIC,
                                Integer.parseInt(jmmNode.get("line")),
                                Integer.parseInt(jmmNode.get("column")),
                                "Variable \"" + jmmNode.get("name") + "\" has not been initialized."
                        ));
                    }
                }
            }*/
    }

    private boolean checkImports(String variableName){
        List<String> imports = symbolTable.getImports();
        for (String imp : imports) {
            if (imp.equals(variableName)) return true;
        }
        return false;
    }
}
