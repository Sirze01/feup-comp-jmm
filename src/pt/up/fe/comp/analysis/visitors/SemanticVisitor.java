package pt.up.fe.comp.analysis.visitors;

import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.analysis.JmmMethod;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SemanticVisitor extends AJmmVisitor<List<Report>, String> {
    JmmSymbolTable symbolTable;
    List<String> variables;

    public SemanticVisitor(JmmSymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.variables = new ArrayList<>();
        addVisit("ArrayExpression", this::visitArrayExp);
        addVisit("_New", this::visitNew);
        addVisit("AccessExpression", this::visitAccessExpression);
        addVisit("CallExpression", this::visitCallExpression);
        addVisit("MemberArgs", this::visitMemberArgs);
        addVisit("IDAssignment", this::visitIDAssignment);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("MethodHeader", this::visitMethodHeader);
        addVisit("Type", this::visitType);
        addVisit("Literal", this::visitLiteral);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ID", this::visitID);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("BinOp", this::visitBinOp);
        addVisit("IfCondition", this::visitCondition);
        addVisit("WhileCondition", this::visitCondition);
        setDefaultVisit(this::defaultVisit);
    }



    private String defaultVisit(JmmNode jmmNode, List<Report> reports){
        for (JmmNode child : jmmNode.getChildren())
            visit(child, reports);
        return "";
    }

    private String visitCondition(JmmNode jmmNode, List<Report> reports) {
        for (JmmNode child : jmmNode.getChildren()) {
            String type = visit(child);
            if (!type.equals("Boolean")) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(jmmNode.get("line")),
                        Integer.parseInt(jmmNode.get("column")),
                        "Condition is not of type 'boolean'"
                ));
                return "<Invalid>";
            }
        }
        return "";
    }

    private String visitArrayExp(JmmNode node, List<Report> reports){
        JmmMethod method = symbolTable.getParentMethodName(node);
        Symbol s = symbolTable.getLocalVar(method.toString(), node.getJmmChild(0).get("name"));

        if (!s.getType().isArray()){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Variable '" + s.getName() + "' cannot be indexed."
            ));
            return "<Invalid>";
        }

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Literal")) {
                String idxType = visit(child, reports);
                if (!idxType.equals("Int")) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("column")),
                            "Array indexes must be of type int."
                    ));
                    return "<Invalid>";
                }
            } else if (!child.equals(node.getJmmChild(0))) {
                Symbol idx = symbolTable.getLocalVar(method.toString(), child.get("name"));
                if (!idx.getType().getName().equals("Int")){
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

        if(!newTypeArray.equals("Class")){
            if (childType.equals(newType)) {
                return newTypeArray + "Expression";
            } else{
                reports.add(new Report(
                        ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(jmmNode.get("line")),
                        Integer.parseInt(jmmNode.get("column")),
                        "Array length must be of type int."
                ));
            }
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

        return visit(node.getJmmChild(0), reports);
    }

    private String visitIDAssignment(JmmNode jmmNode, List<Report> reports){
        JmmNode id0 = jmmNode.getJmmChild(0);
        JmmNode id1 = jmmNode.getJmmChild(1);

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
        String ret1 = visit(id1, reports);

        if (!(ret0.equals("IntArray") && ret1.equals("Int")) && !ret0.equals(ret1)){

            if (checkExtendsImport(ret1) == null) {

                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(id1.get("line")),
                        Integer.parseInt(id1.get("column")),
                        "Type mismatch in operation. '" + ret0 + "' to '" + ret1 + "'"
                ));
                return "<Invalid>";
            } else if (checkExtendsImport(ret1).equals("extends")) {

            } else if (checkExtendsImport(ret0) == null){
                if (checkExtendsImport(ret1).equals("import")) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(id1.get("line")),
                            Integer.parseInt(id1.get("column")),
                            "Type mismatch in operation. '" + ret0 + "' to '" + ret1 + "'"
                    ));
                }
            }
        } else {
            if (id0.getKind().equals("ID"))
                variables.add(id0.get("name"));
            else
                variables.add(id0.getChildren().get(0).get("name"));
        }

        return ret1;
    }

    private String visitID(JmmNode jmmNode, List<Report> reports){

        JmmMethod method = symbolTable.getParentMethodName(jmmNode);

        Optional<JmmNode> ancestor = jmmNode.getAncestor("MainMethod").isPresent() ? jmmNode.getAncestor("MainMethod") : jmmNode.getAncestor("MethodBody");

        boolean isArg = false;
        Optional<JmmNode> argAncestor = jmmNode.getAncestor("MainMethod").isPresent() ? jmmNode.getAncestor("MainMethodArguments") : jmmNode.getAncestor("InstanceMethodArguments");

        if(jmmNode.getJmmParent().getKind().equals("ReturnExpression")){
            for(JmmNode arg: jmmNode.getJmmParent().getJmmParent().getJmmChild(1).getChildren()){
                if(arg.getKind().equals("ID")){
                    if(arg.get("name").equals(jmmNode.get("name"))){
                        isArg = true;
                    }
                }
            }
        }

        if(argAncestor.isPresent())
            for(JmmNode arg: argAncestor.get().getChildren()){
                if(arg.getKind().equals("ID")){
                    if(arg.get("name").equals(jmmNode.get("name"))){
                        isArg = true;
                    }
                }
            }


        if (method != null) {
            if(jmmNode.getAttributes().contains("name")){
                if (symbolTable.getLocalVar(method.toString(), jmmNode.get("name")) == null && !checkImports(jmmNode.get("name")) && !isArg) {
                    reports.add(new Report(
                            ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(jmmNode.get("line")),
                            Integer.parseInt(jmmNode.get("column")),
                            "Variable \"" + jmmNode.get("name") + "\" is not declared."
                    ));
                }
            }
        }
        if (ancestor.isEmpty()) {
            return "";
        }
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
        if (name.isEmpty()){
            return jmmNode.get("type");
        }

        Symbol s = symbolTable.getLocalVar(method.toString(), name.get());

        checkInitializedVariable(jmmNode, reports, method, s);

        String ret = "<Invalid>";
        if (s != null)
            ret = s.getType().isArray() ? s.getType().getName() + "ArrayExpression" : s.getType().getName();

        if(checkExtendsImport(name.get()) != null){
            return name.get();
        }

        return ret;

    }

    private String checkExtendsImport(String nodeType){
        if(symbolTable.getImports().contains(nodeType)){
            return "import";
        }
        if(symbolTable.getSuper() != null && nodeType.equals(symbolTable.getClassName())){
            return "extends";
        }
        if(nodeType.equals(symbolTable.getSuper())){
            return "extends";
        }
        return null;
    }


    private String visitAccessExpression(JmmNode node, List<Report> reports){

        if(node.getJmmChild(0).getKind().equals("Literal") && node.getJmmChild(0).get("value").equals("this") && symbolTable.getSuper() == null){
            JmmNode expressionNode = node.getJmmChild(1);
            if (symbolTable.getMethodByName(expressionNode.getJmmChild(0).get("name")) == null){
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
                        expressionNode.getJmmChild(0).get("line") != null ? Integer.parseInt(expressionNode.getJmmChild(0).get("line")) : 0,
                        Integer.parseInt(expressionNode.getJmmChild(0).get("column")),
                        "Method " + expressionNode.getJmmChild(0).get("name") + "() has the wrong number of arguments"));
            }
        } else if (node.getJmmChild(0).getKind().equals("ID")){
            String childType = visit(node.getJmmChild(0), reports);
            JmmMethod ancestor = symbolTable.getParentMethodName(node);
            String symbolName = node.getJmmChild(0).get("name");

            Symbol symbol = ancestor.getName().equals("main")? symbolTable.getFieldByName(symbolName) : symbolTable.getLocalVar(ancestor.toString(), symbolName);

            System.out.println("visited child: " + node.getJmmChild(0));
            System.out.println("childType: " + childType);


            if (symbol != null) {
                System.out.println(node.getJmmChild(1).getJmmChild(0).get("name"));
                System.out.println(checkExtendsImport(childType));

                if (checkExtendsImport(childType) == null && !childType.equals("")) {
                    if (symbolTable.getMethodByName(node.getJmmChild(1).getJmmChild(0).get("name")) == null) {
                        reports.add(new Report(ReportType.ERROR,
                                Stage.SEMANTIC,
                                node.getJmmChild(1).getJmmChild(0).get("line") != null ? Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("line")) : 0,
                                Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("column")),
                                "Method " + node.getJmmChild(1).getJmmChild(0).get("name") + "() isn't declared for this type."));
                    }
                }
                return "";

            }
            if (checkExtendsImport(childType) == null) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        node.getJmmChild(1).getJmmChild(0).get("line") != null ? Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("line")) : 0,
                        Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("column")),
                        "Method " + node.getJmmChild(1).getJmmChild(0).get("name") + "() isn't declared"));
            }

        }


        for(JmmNode child: node.getChildren()) {
            if (node.get("type").equals("Call") && checkImports(node.getJmmChild(0).get("name")) && child.getKind().equals("CallExpression"))
                visitCallExpression(child, reports);
            visit(child, reports);
        }
        return "Method";
    }



    private String visitMemberArgs(JmmNode node, List<Report> reports){
        JmmMethod ancestor = symbolTable.getParentMethodName(node);
        if(ancestor == null) return "";

        int i = 0;
        for(JmmNode child: node.getChildren()){
            String methodName = node.getJmmParent().getJmmChild(0).get("name");

            if(child.getKind().equals("ID")){
                String symbolName = child.get("name");
                boolean isMain = ancestor.getName().equals("main");

                Symbol symbol = isMain ? symbolTable.getFieldByName(symbolName) : symbolTable.getLocalVar(ancestor.toString(), symbolName) ;
                if (symbolTable.getMethodByName(methodName) != null && symbol != null
                        && !symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName().equals(symbol.getType().getName())) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            child.get("line") != null ? Integer.parseInt(child.get("line")) : 0,
                            Integer.parseInt(child.get("column")),
                            "Argument " + child.get("name") + " of type " +  symbol.getType() +  " is of wrong type, " +
                                    symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName() + " expected at " +  methodName + " method."));
                }
            }
            else if (child.getAttributes().contains("type") && symbolTable.getMethodByName(methodName) != null  && i < symbolTable.getMethodByName(methodName).getParameters().size() && !symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName().equals(child.get("type"))) {
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

    private String visitCallExpression(JmmNode jmmNode, List<Report> reports) {
        return visit(jmmNode.getJmmChild(1), reports);
    }

    private String visitBinOp(JmmNode node, List<Report> reports){

        JmmNode lhs = node.getChildren().get(0);
        JmmNode rhs = node.getChildren().get(1);

        String lhsType;
        String rhsType;

        boolean lhsIsArray = false;
        boolean rhsIsArray = false;


        if (node.getJmmParent().getKind().equals("ReturnExpression")){
            JmmMethod method = symbolTable.getParentMethodName(node);
            Symbol lhsSymbol;
            Symbol rhsSymbol;
            if (node.getJmmChild(0).getAttributes().contains("name")) {
                lhsSymbol = symbolTable.getLocalVar(method.toString(), node.getJmmChild(0).get("name"));
                if (lhsSymbol == null)
                    lhsType = visit(lhs, reports);
                else {
                    lhsType = lhsSymbol.getType().getName();
                    lhsIsArray = lhsSymbol.getType().isArray();
                }
            } else lhsType = visit(lhs, reports);
            if (node.getJmmChild(1).getAttributes().contains("name")){
                rhsSymbol = symbolTable.getLocalVar(method.toString(), node.getJmmChild(1).get("name"));
                if (rhsSymbol == null)
                    rhsType = visit(rhs, reports);
                else {
                    rhsType = rhsSymbol.getType().getName();
                    rhsIsArray = rhsSymbol.getType().isArray();
                }
            } else rhsType = visit(rhs, reports);
        }
        else {
            lhsType = visit(lhs, reports);
            rhsType = visit(rhs, reports);
        }


        if (!lhsType.equals(rhsType) || (rhsIsArray) || lhsIsArray) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rhs.get("line")),
                    Integer.parseInt(rhs.get("column")),
                    "Bin OP: Type mismatch in operation. <" + lhsType + "> to <" + rhsType + ">"
            ));
        }

        List<String> intOp = new ArrayList<>(Arrays.asList("Mult", "Div", "Sub", "Add", "Less"));

        if(intOp.contains(node.get("op"))){
            if ((!lhsType.equals("Int") && !lhsType.equals("import") && !lhsType.equals("extends"))
                    || (!rhsType.equals("Int") && !rhsType.equals("import") && !rhsType.equals("extends"))) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(rhs.get("line")),
                        Integer.parseInt(rhs.get("column")),
                        "BinOP: Operations must be between integers. Used <" + lhsType + "> and <" + rhsType + ">"
                ));
            }
        }
        else if(node.get("op").equals("And")){
            if ((!lhsType.equals("Boolean") && !lhsType.equals("import") && !lhsType.equals("extends"))
                    || (!rhsType.equals("Boolean") && !rhsType.equals("import") && !rhsType.equals("extends"))) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(rhs.get("line")),
                        Integer.parseInt(rhs.get("column")),
                        "BinOP: Operations must be boolean integers. Used <" + lhsType + "> and <" + rhsType + ">"
                ));
            }
        }


        return lhsType;

    }

    private String visitUnaryOp(JmmNode node, List<Report> reports){
        String expressionType = visit(node.getChildren().get(0), reports);

        if (!expressionType.equals("Boolean")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Not operator can only be applied to boolean expressions."
            ));
        }

        return "Boolean";
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
        if (jmmNode.getJmmParent().getKind().equals("BinOp")
                || (jmmNode.getJmmParent().getKind().equals("IDAssignment")
                && !jmmNode.getJmmParent().getJmmChild(0).equals(jmmNode)))
        {
            if (!jmmNode.getJmmParent().getJmmChild(0).equals(jmmNode)){
                if (!variables.contains(jmmNode.get("name"))) {
                    if (!checkMethodParameter(jmmNode, method)){
                        if (!symbolTable.getFields().contains(s)) {
                            reports.add(new Report(
                                    ReportType.ERROR,
                                    Stage.SEMANTIC,
                                    Integer.parseInt(jmmNode.get("line")),
                                    Integer.parseInt(jmmNode.get("column")),
                                    "Variable \"" + jmmNode.get("name") + "\" has not been initialized."
                            ));
                        }
                    }

                }
            }
        }
    }


    private boolean checkMethodParameter(JmmNode jmmNode,  JmmMethod method){
        for (Symbol parameter : method.getParameters()){
            if (parameter.getName().equals(jmmNode.get("name")))
                return true;
        }
        return false;
    }


    private boolean checkImports(String variableName){
        List<String> imports = symbolTable.getImports();
        for (String imp : imports) {
            if (imp.equals(variableName)) return true;
        }
        return false;
    }


    private void addSemanticErrorReport(){

    }
}
