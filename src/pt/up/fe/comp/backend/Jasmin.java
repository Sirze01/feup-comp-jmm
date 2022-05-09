package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;

public class Jasmin {
    private ClassUnit ollirClass;
    private StringBuilder jasminCodeBuilder;

    public String build(ClassUnit ollirClass) {
        this.ollirClass = ollirClass;
        this.ollirClass.buildVarTables();

       this.jasminCodeBuilder = new StringBuilder();

        buildClassDeclaration();
        buildClassMethod();

        return jasminCodeBuilder.toString();
    }

    private void buildClassDeclaration(){
        // Class Name Definition
        this.jasminCodeBuilder.append(".class public ")
                .append(this.ollirClass.getClassName())
                .append("\n");

        // Class Super Definition
        String superClass = this.ollirClass.getSuperClass() == null ?
               "java/lang/Object" : this.ollirClass.getSuperClass();
        this.jasminCodeBuilder.append(".super ")
               .append(superClass)
               .append("\n");

        // Class Fields Definition
        for(int i = 0; i < this.ollirClass.getNumFields(); i++) {
            Field field = this.ollirClass.getField(i);
            this.jasminCodeBuilder.append(buildClassField(field));
        }
        this.jasminCodeBuilder.append("\n");

        // Class Initialization Definition

    }

    private String buildClassField(Field field) {
        return ".field " +
                accessScope(field.getFieldAccessModifier()) +
                " " + field.getFieldName() + " " +
                buildTypes(field.getFieldType().getTypeOfElement()) + "\n";
    }

    private String buildTypes(ElementType type) {
        if (type == ElementType.ARRAYREF)
            return "[I";
        else if (type == ElementType.INT32)
            return "I";
        else if (type == ElementType.BOOLEAN)
            return "Z";
        else if (type == ElementType.STRING)
            return "Ljava/lang/String;";
        else if (type == ElementType.VOID)
            return "V";
        else if (type == ElementType.OBJECTREF || type == ElementType.CLASS )
            return "L" + type.getClass().getName() + ";";
        else
            throw new IllegalStateException("Unexpected value: " + type);
    }

    private String accessScope(AccessModifiers accessModifier) {
        return String.valueOf(accessModifier.equals(AccessModifiers.DEFAULT) ? AccessModifiers.PUBLIC : accessModifier).toLowerCase();
    }


    private void buildClassMethod() {
    }
}
