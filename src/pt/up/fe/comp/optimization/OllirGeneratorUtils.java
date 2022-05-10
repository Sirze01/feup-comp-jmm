package pt.up.fe.comp.optimization;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class OllirGeneratorUtils {
    private static final int numSpaces = 4;

    public static int getNumSpaces(Map<String, String> config) {
        return numSpaces;
    }

    public static String toOllirType(String type, boolean isArray) {
        StringBuilder code = new StringBuilder();

        if (isArray) {
            code.append("array.");
        }

        switch (type) {
            case "void" -> code.append("V");
            case "Int" -> code.append("i32");
            case "Boolean" -> code.append("bool");
            default -> code.append(type);
        }

        return code.toString();
    }

    public static String toOllirType(Type type){
        return toOllirType(type.getName(), type.isArray());
    }

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + OllirGeneratorUtils.toOllirType(symbol.getType());
    }

    public static String getMethodHeader(JmmSymbolTable symbolTable, String methodSignature){
        JmmMethod method = symbolTable.getMethodObject(methodSignature);
        String params = method.getParameters().stream().map(OllirGeneratorUtils::getCode).collect(Collectors.joining(", "));
        return method.getName() + "(" + params + ")." + toOllirType(method.getReturnType());
    }

    public static String getCodeLiteral(JmmNode literalNode){
        return literalNode.get("value") + "." + toOllirType(literalNode.get("type"), false);
    }
}
