package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Jasmin {
    private ClassUnit ollirClass;
    private Map<String, String> fullyQualifiedNames;
    private HashMap<String, Descriptor> variableTable;
    private StringBuilder jasminCodeBuilder;
    private String superClassName;

    public String build(ClassUnit ollirClass) {
        this.ollirClass = ollirClass;
        this.ollirClass.buildVarTables();

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
        this.superClassName = this.ollirClass.getSuperClass() != null ?
                this.fullyQualifiedNames.get(this.ollirClass.getSuperClass()) : "java/lang/Object";
        this.jasminCodeBuilder.append(".super ")
                .append(superClassName)
                .append("\n\n");

        // Class Fields Definition
        for(Field field : this.ollirClass.getFields())
            this.jasminCodeBuilder.append(buildClassField(field));

        this.jasminCodeBuilder.append("\n");
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
        StringBuilder typeCode = new StringBuilder();
        ElementType elementType = type.getTypeOfElement();

        if(elementType == ElementType.ARRAYREF){
            elementType = ((ArrayType) type).getArrayType();
            typeCode.append("[");
        }

        if((elementType == ElementType.OBJECTREF || elementType == ElementType.CLASS)){
            String className = elementType.getClass().getName();
            String fullyQualifiedClassName = this.fullyQualifiedNames.get(className);
            String finalClassName = fullyQualifiedClassName != null
                    ? this.fullyQualifiedNames.get(className) : className;
            return "L" + finalClassName + ";";
        }

        typeCode.append(buildTypes(elementType));

        return typeCode.toString();
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
        this.variableTable = method.getVarTable();
        // Method Signature Definition
        this.jasminCodeBuilder.append(".method ")
                .append(accessScope(method.getMethodAccessModifier()))
                .append(method.isStaticMethod() ? "static " : "")
                .append(method.isFinalMethod() ? "final " : "")
                .append(method.isConstructMethod() ? "public <init>" : method.getMethodName())
                .append("(");

        // Method Parameters
        String methodParamTypes = method.getParams().stream()
                .map(element -> buildTypes(element.getType()))
                .collect(Collectors.joining());
        this.jasminCodeBuilder.append(methodParamTypes).append(")")
                .append(buildTypes(method.getReturnType()))
                .append("\n");

        // Limit Declarations
        if(!method.isConstructMethod()) {
            this.jasminCodeBuilder.append("\t.limit stack 99\n");
            this.jasminCodeBuilder.append("\t.limit locals 99\n\n");
        }

        // Method Instructions TODO Are Labels Needed?
        for(Instruction instruction : method.getInstructions())
            this.jasminCodeBuilder.append(buildMethodInstructions(instruction));

        if (method.isConstructMethod())
            this.jasminCodeBuilder.append("\treturn\n");

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
            case BINARYOPER:
                return buildBinaryOperatorInstruction((BinaryOpInstruction) instruction);
            case NOPER:
                return buildNOperInstruction((SingleOpInstruction) instruction);
            default:
                throw new NotImplementedException(instruction.getInstType());
        }

    }

    private String buildNOperInstruction(SingleOpInstruction instruction) {
        return pushElement(instruction.getSingleOperand());
    }

    private String buildBinaryOperatorInstruction(BinaryOpInstruction instruction) {
        return "";
    }

    private String buildGetFieldInstruction(GetFieldInstruction instruction) {
        return "";
    }

    private String buildReturnInstruction(ReturnInstruction instruction) {
        StringBuilder returnInstruction = new StringBuilder();
        if (!instruction.hasReturnValue())
            return "\treturn\n";

        Element operand = instruction.getOperand();
        String returnType = (operand.getType().getTypeOfElement() == ElementType.INT32
                || operand.getType().getTypeOfElement() == ElementType.BOOLEAN) ? "i" : "a";

        returnInstruction.append(pushElement(operand)).append("\t").append(returnType).append("return\n");

        return returnInstruction.toString();

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
        StringBuilder assignInstruction = new StringBuilder();
        System.out.println("Started Assignment");
        Operand operand = (Operand) instruction.getDest();
        Type destType = operand.getType();
        Descriptor destVariable = this.variableTable.get(operand.getName());

        // Increment Assignment TODO TEST
       /*if(instruction.getRhs().getInstType() == InstructionType.BINARYOPER){
            BinaryOpInstruction binaryOperation = (BinaryOpInstruction) instruction.getRhs();
            Element leftOperand = binaryOperation.getLeftOperand();
            Element rightOperand = binaryOperation.getRightOperand();

            if(binaryOperation.getOperation().getOpType() == OperationType.ADD
                || binaryOperation.getOperation().getOpType() == OperationType.SUB){
                String operationSign = binaryOperation.getOperation().getOpType() == OperationType.ADD ? "" : "-";

                if(!leftOperand.isLiteral() && ((Operand) leftOperand).getName().equals(operand.getName())
                        && rightOperand.isLiteral()){
                    String leftValue = operationSign +
                            ((LiteralElement) binaryOperation.getLeftOperand()).getLiteral();

                    if(Integer.parseInt(leftValue) >= -128 && Integer.parseInt(leftValue) <= 127){
                        assignInstruction.append("\tiinc ")
                                .append(destVariable.getVirtualReg()).append(" ")
                                .append(leftValue)
                                .append(((LiteralElement) leftOperand).getLiteral()).append("\n");
                        return assignInstruction.toString();
                    }

                }

                else if(!rightOperand.isLiteral() && ((Operand) rightOperand).getName().equals(operand.getName())
                        && leftOperand.isLiteral()){
                    String rightValue = operationSign +
                            ((LiteralElement) binaryOperation.getLeftOperand()).getLiteral();

                    if(Integer.parseInt(rightValue) >= -128 && Integer.parseInt(rightValue) <= 127) {
                        assignInstruction.append("\tiinc ")
                                .append(destVariable.getVirtualReg()).append(" ")
                                .append(rightValue)
                                .append(((LiteralElement) leftOperand).getLiteral()).append("\n");
                        return assignInstruction.toString();
                    }
                }
            }

        }*/

        // Array Assignment TODO TEST
        if(destVariable.getVarType().getTypeOfElement() == ElementType.ARRAYREF
                && destType.getTypeOfElement() != ElementType.ARRAYREF){
            System.out.println("In Array Assignment");
            Element index = ((ArrayOperand) operand).getIndexOperands().get(0);

            assignInstruction.append(pushElementDescriptor(destVariable))
                    .append(pushElement(index));

            // Arrays with other dest types are stored as "astore"
            if(destType.getTypeOfElement() == ElementType.INT32 || destType.getTypeOfElement() == ElementType.BOOLEAN){
                assignInstruction.append(buildMethodInstructions(instruction.getRhs()))
                        .append("\tiastore");
                return assignInstruction.toString();
            }
        }
        System.out.println(instruction.getRhs().getInstType());
        assignInstruction.append(buildMethodInstructions(instruction.getRhs()));

        String storeType = (destType.getTypeOfElement() == ElementType.INT32
                || destType.getTypeOfElement() == ElementType.BOOLEAN) ? "i" : "a";
        String hasSM = (destVariable.getVirtualReg() > 0 && destVariable.getVirtualReg() <= 3) ? "_" : " ";

        assignInstruction.append("\t")
                .append(storeType).append("store").append(hasSM).append(destVariable.getVirtualReg())
                .append("\n");

        return assignInstruction.toString();
    }

    private String buildCallInstruction(CallInstruction instruction){
        StringBuilder callInstruction = new StringBuilder();

        switch(instruction.getInvocationType()){
            case invokevirtual:
                callInstruction.append(pushElement(instruction.getFirstArg()));

                for(Element operand: instruction.getListOfOperands())
                    callInstruction.append(pushElement(operand));

                callInstruction.append("\tinvokevirtual ");

                String virtualClass = ((ClassType) instruction.getFirstArg().getType()).getName();
                String virtualClassCallName = Objects.equals(virtualClass, "this")
                        ? this.ollirClass.getClassName() : virtualClass;


                callInstruction.append(virtualClassCallName)
                        .append("/")
                        .append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""))
                        .append("(");

                for(Element operand : instruction.getListOfOperands())
                    callInstruction.append(buildTypes(operand.getType()));

                callInstruction.append(")")
                        .append(buildTypes(instruction.getReturnType()))
                        .append("\n");

            case invokespecial:
                callInstruction.append(pushElement(instruction.getFirstArg()));

                String initClassName = instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS
                        ? this.superClassName : this.ollirClass.getClassName();
                callInstruction.append("\tinvokespecial ").append(initClassName).append("/<init>(");

                for(Element operand : instruction.getListOfOperands())
                    callInstruction.append(buildTypes(operand.getType()));

                callInstruction.append(")")
                        .append(buildTypes(instruction.getReturnType()))
                        .append("\n");
                break;
            case invokestatic:
                for(Element operand: instruction.getListOfOperands())
                    callInstruction.append(pushElement(operand));

                callInstruction.append("\tinvokestatic ");

                String staticClass = ((Operand) instruction.getFirstArg()).getName();
                String staticClassCallName = Objects.equals(staticClass, "this")
                        ? this.ollirClass.getClassName() : staticClass;

                callInstruction.append(staticClassCallName)
                        .append("/")
                        .append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""))
                        .append("(");

                for(Element operand : instruction.getListOfOperands())
                    callInstruction.append(buildTypes(operand.getType()));

                callInstruction.append(")")
                        .append(buildTypes(instruction.getReturnType()))
                        .append("\n");
                break;
            case NEW:
                if(instruction.getReturnType().getTypeOfElement() == ElementType.OBJECTREF){
                    for(Element operand: instruction.getListOfOperands())
                        callInstruction.append(pushElement(operand));

                    callInstruction.append("\tnew ")
                            .append(((Operand) instruction.getFirstArg()).getName())
                            .append("\n\tdup\n");
                }
                else if (instruction.getReturnType().getTypeOfElement() == ElementType.ARRAYREF){
                    for(Element operand: instruction.getListOfOperands())
                        callInstruction.append(pushElement(operand));

                    if(instruction.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32)
                        callInstruction.append("\tnewarray int\n");
                    else
                        throw new NotImplementedException("New Array with Type"
                                + instruction.getListOfOperands().get(0).getType().getTypeOfElement());
                    // Other Array Types are not supported
                }
                else
                    throw new NotImplementedException("New with type "
                            + instruction.getFirstArg().getType().getTypeOfElement());
                // Other new types are not supported
                break;
            case arraylength:
                callInstruction.append(pushElement(instruction.getFirstArg()))
                        .append("\tarraylength\n");
                break;
            case ldc:
                callInstruction.append(pushElement(instruction.getFirstArg()));
                break;
            default:
                throw new NotImplementedException(instruction.getInvocationType());
        }

        /*if(instruction.getReturnType().getTypeOfElement() != ElementType.VOID)
            callInstruction.append("\tpop\n");*/

        return callInstruction.toString();
    }

    private String pushElement(Element element) {
        // Literal Element
        if(element.isLiteral())
            return pushLiteral((LiteralElement) element);

        String operandName = ((Operand) element).getName();

        // Array Element
        try{
            if(this.variableTable.get(operandName).getVarType().getTypeOfElement() == ElementType.ARRAYREF
                    && element.getType().getTypeOfElement() != ElementType.ARRAYREF){
                ArrayOperand arrayOperand = (ArrayOperand) element;
                return pushElementDescriptor(this.variableTable.get(operandName))
                        + pushElement(arrayOperand.getIndexOperands().get(0))
                        + "\tiaload\n";
            }
        }
        catch(ClassCastException ignored){} // Element is not an ArrayOperand

        return pushElementDescriptor(this.variableTable.get(operandName));
    }

    private String pushElementDescriptor(Descriptor descriptor) {
        ElementType type = descriptor.getVarType().getTypeOfElement();
        if(type == ElementType.THIS)
            return "\taload_0\n";

        String pushType = (type == ElementType.INT32 || type == ElementType.BOOLEAN) ? "i" : "a";
        String hasSM = (descriptor.getVirtualReg() > 0 && descriptor.getVirtualReg() <= 3) ? "_" : " ";

        return "\t" + pushType + "load" + hasSM + descriptor.getVirtualReg() + "\n";
    }

    private String pushLiteral(LiteralElement element) {
        StringBuilder literalElement = new StringBuilder("\t");
        int literal = Integer.parseInt(element.getLiteral());

        switch (element.getType().getTypeOfElement()){
            case INT32: case BOOLEAN:
                if(literal == -1)
                    literalElement.append("iconst_m1\n");
                else if (literal >= 0 && literal <= 5)
                    literalElement.append("iconst_").append(literal).append("\n");
                else if (literal >= -128 && literal <= 127)
                    literalElement.append("bipush ").append(literal).append("\n");
                else if (literal > 127)
                    literalElement.append("sipush ").append(literal).append("\n");
                else
                    literalElement.append("ldc ").append(literal).append("\n");
                break;
            default:
                literalElement.append("ldc ").append(literal).append("\n");
        }

        return literalElement.toString();
    }
}