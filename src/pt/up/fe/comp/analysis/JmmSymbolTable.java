package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {
    List<String> imports = new ArrayList<>();

    String className = null;
    String superName = null;
    Map<String, Symbol> fields = new HashMap<>();


    Map<String, JmmMethod> methods = new HashMap<>();


    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }

    public void addImport(String pkg) {
        this.imports.add(pkg);
    }

    public void addField(Symbol field) {
        this.fields.put(field.getName(), field);
    }

    public JmmMethod addMethod(JmmMethod method) {
        return this.methods.putIfAbsent(method.toString(), method);
    }

    public Map<String, Symbol> getFieldsMap() {
        return fields;
    }

    public static boolean isMain(String methodSignature){
        return methodSignature.equals(new JmmMethod("main", new Type("void", false), List.of(new Symbol(new Type("String", true), "any"))).toString());
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(fields.values());
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods.keySet());
    }

    public JmmMethod getMethodObject(String methodSignature) {
        return methods.get(methodSignature);
    }

    public void printLocalVars() {
        for (JmmMethod method : methods.values()) {
            System.out.println(method.getName());
            for (Symbol s : method.getVars()) {
                System.out.println(s);
            }
        }
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methods.get(methodSignature).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methods.get(methodSignature).getParameters();
    }

    public Symbol getParameter(String methodSignature, String parameterName) {
        for (Symbol s : methods.get(methodSignature).getParameters()) {
            if (parameterName.equals(s.getName())) {
                return s;
            }
        }

        return null;
    }


    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return methods.get(methodSignature).getVars();
    }

    public Symbol getLocalVar(String methodSignature, String varName) {
        for (Symbol s : methods.get(methodSignature).getVars()) {
            if (varName.equals(s.getName())) {
                return s;

            }
        }

        return null;
    }
}
