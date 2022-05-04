package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {
    String className = null;
    String superName = null;
    List<String> imports = new ArrayList<>();
    Map<String, Symbol> fields = new HashMap<>();
    List<String> methods = new ArrayList<>();
    Map<String, Type> methodReturnTypes =  new HashMap<>();
    Map<String, List<Symbol>> methodParameters = new HashMap<>();
    Map<String, List<Symbol>> localVars = new HashMap<>();


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

    public void addMethod(String methodSignature, Type methodReturnType, List<Symbol> methodParameters) {
        this.methods.add(methodSignature);
        this.methodReturnTypes.put(methodSignature, methodReturnType);
        this.methodParameters.put(methodSignature, methodParameters);
    }

    public void addLocalVars(String methodSignature, List<Symbol> localVars) {
        this.localVars.put(methodSignature, localVars);
    }

    public Map<String, Symbol> getFieldsMap(){
        return fields;
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
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methodReturnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methodParameters.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return localVars.get(methodSignature);
    }
}
