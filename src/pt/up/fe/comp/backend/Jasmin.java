package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Jasmin {
    private ClassUnit ollirClass;
    private Map<String, String> fullyQualifiedNames;
    private StringBuilder jasminCodeBuilder;

    public String build(ClassUnit ollirClass) {
        this.ollirClass = ollirClass;
        //this.ollirClass.buildVarTables();

        this.jasminCodeBuilder = new StringBuilder();
        setImportNames();

        buildClassDeclaration();

        for(Method method : this.ollirClass.getMethods())
            buildClassMethod(method);

        return jasminCodeBuilder.toString();
    }

    private void setImportNames(){
        this.fullyQualifiedNames = new HashMap<>();

        for(var importString : this.ollirClass.getImports()){
            var splittedImport = importString.split("\\.");
            var lastName = splittedImport.length == 0 ? importString : splittedImport[splittedImport.length - 1];

            this.fullyQualifiedNames.put(lastName, importString.replace('.', '/'));
        }
    }

    private void buildClassDeclaration(){
        // Class Name Definition
        this.jasminCodeBuilder.append(".class public ")
                .append(this.ollirClass.getClassName())
                .append("\n");

        // Class Super Definition
        String superClassName = this.ollirClass.getSuperClass() != null ?
                this.fullyQualifiedNames.get(this.ollirClass.getSuperClass()) : "java/lang/Object";
        this.jasminCodeBuilder.append(".super ")
               .append(superClassName)
               .append("\n\n");

        // Class Fields Definition
        for(Field field : this.ollirClass.getFields())
            this.jasminCodeBuilder.append(buildClassField(field));

        this.jasminCodeBuilder.append("\n");

        // Class Initialization Definition
        this.jasminCodeBuilder.append(".method public <init>()V\n")
                              .append("\taload_0\n")
                              .append("\tinvokenonvirtual ")
                              .append(superClassName)
                              .append("/<init>()V\n")
                              .append("\treturn\n")
                              .append(".end method\n\n");
    }

    private String buildClassField(Field field) {
        StringBuilder classField = new StringBuilder();
        classField.append(".field ")
                  .append(accessScope(field.getFieldAccessModifier()));

        if(field.isStaticField()) classField.append("static ");
        if(field.isFinalField()) classField.append("final ");

        classField.append(field.getFieldName())
                  .append(" ")
                  .append(buildTypes(field.getFieldType()))
                  .append("\n");

        if(field.isInitialized())
            classField.append(" = ").append(field.getInitialValue());

        classField.append("\n");
        return classField.toString();
    }

    private String buildTypes(Type type) {
        switch(type.getTypeOfElement()){
            case ARRAYREF:
                return "[" + buildTypes(((ArrayType) type).getTypeOfElements());
            case OBJECTREF: case CLASS:
                String className = ((ClassType) type).getName();
                return "L" + this.fullyQualifiedNames.get(className) + ";";
            default:
                return buildTypes(type.getTypeOfElement());
        }
    }

    private String buildTypes(ElementType elementType) {
        switch (elementType) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            default:
                throw new NotImplementedException(elementType);
        }
    }


    private String accessScope(AccessModifiers accessModifier) {
        return (accessModifier.equals(AccessModifiers.DEFAULT) ? "" : accessModifier + " ").toLowerCase();
    }

    private void buildClassMethod(Method method) {
        // Method Signature Definition
        this.jasminCodeBuilder.append(".method ")
                              .append(accessScope(method.getMethodAccessModifier()))
                              .append(method.isStaticMethod() ? "static " : "")
                              .append(method.isFinalMethod() ? "final " : "")
                              .append(method.isConstructMethod() ? "<init>" : method.getMethodName())
                              .append("(");

        // Method Parameters
        String methodParamTypes = method.getParams().stream()
                                                        .map(element -> buildTypes(element.getType()))
                                                        .collect(Collectors.joining());
        this.jasminCodeBuilder.append(methodParamTypes).append(")")
                              .append(buildTypes(method.getReturnType()))
                              .append("\n");

        // Limit Declarations
        this.jasminCodeBuilder.append(".limit stack 99\n");
        this.jasminCodeBuilder.append(".limit locals 99\n");

        // Method Instructions
        for(Instruction instruction : method.getInstructions())
            buildMethodInstructions(instruction);

        this.jasminCodeBuilder.append(".end method\n\n");

    }

    private String buildMethodInstructions(Instruction instruction){
       switch(instruction.getInstType()){
           case ASSIGN:
               return buildAssignInstruction((AssignInstruction) instruction);
           case CALL:
               return buildCallInstruction((CallInstruction) instruction);
           case GOTO:
               return buildGoToInstruction((GotoInstruction) instruction);
           case BRANCH:
               return buildBranchInstruction((CondBranchInstruction) instruction);
           case RETURN:
               return buildReturnInstruction((ReturnInstruction) instruction);
           case PUTFIELD:
               return buildPutFieldInstruction((PutFieldInstruction) instruction);
           case GETFIELD:
               return buildGetFieldInstruction((GetFieldInstruction) instruction);
           case UNARYOPER:
               return buildUnaryOperatorInstruction((UnaryOpInstruction) instruction);
           case BINARYOPER:
               return buildBinaryOperatorInstruction((BinaryOpInstruction) instruction);
           case NOPER:
               return buildNOperInstruction((SingleOpInstruction) instruction);
           default:
               throw new NotImplementedException(instruction.getInstType());
       }

    }

    private String buildNOperInstruction(SingleOpInstruction instruction) {
        return "";
    }

    private String buildBinaryOperatorInstruction(BinaryOpInstruction instruction) {
        return "";
    }

    private String buildUnaryOperatorInstruction(UnaryOpInstruction instruction) {
        return "";
    }

    private String buildGetFieldInstruction(GetFieldInstruction instruction) {
        return "";
    }

    private String buildReturnInstruction(ReturnInstruction instruction) {
        return "";
    }

    private String buildPutFieldInstruction(PutFieldInstruction instruction) {
        return "";
    }

    private String buildBranchInstruction(CondBranchInstruction instruction) {
        return "";
    }

    private String buildGoToInstruction(GotoInstruction instruction) {
        return "";
    }

    private String buildAssignInstruction(AssignInstruction instruction) {
        return "";
    }

    private String buildCallInstruction(CallInstruction call){
        StringBuilder callInstruction = new StringBuilder();

        switch(call.getInvocationType()){
            case invokevirtual:
                break;
            case invokeinterface:
                break;
            case invokespecial:
                break;
            case invokestatic:
                callInstruction.append("invokestatic ");
            case NEW:
                break;
            case arraylength:
                break;
            case ldc:
                break;
            default:
                throw new NotImplementedException(call.getInvocationType());
        }

        return callInstruction.toString();
    }

    private String buildCallInvokeStaticInstruction(CallInstruction call) {
        return "";
    }
}
