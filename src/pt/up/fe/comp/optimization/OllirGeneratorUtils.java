package pt.up.fe.comp.optimization;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class OllirGeneratorUtils {
    private static final int numSpaces = 4;

    public static int getNumSpaces(Map<String, String> config) {
        return numSpaces;
    }

    public static String toOllirType(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) {
            code.append("array.");
        }

        switch (type.getName()) {
            case "void" -> code.append("V");
            case "Int" -> code.append("i32");
            case "Boolean" -> code.append("bool");
            default -> code.append(type.getName());
        }

        return code.toString();
    }

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + OllirGeneratorUtils.toOllirType(symbol.getType());
    }

    public static String getCode(JmmSymbolTable symbolTable, String methodSignature){
        JmmMethod method = symbolTable.getMethodObject(methodSignature);
        String params = method.getParameters().stream().map(OllirGeneratorUtils::getCode).collect(Collectors.joining(", "));
        return method.getName() + "(" + params + ")." + toOllirType(method.getReturnType());
    }
}
