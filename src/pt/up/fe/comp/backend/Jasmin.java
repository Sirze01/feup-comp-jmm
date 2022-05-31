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

    int operatorLabel;
    int comparisonLabel;

    public String build(ClassUnit ollirClass) throws OllirErrorException {
        this.ollirClass = ollirClass;
        this.operatorLabel = 0;
        this.comparisonLabel = 0;

        this.ollirClass.checkMethodLabels();
        this.ollirClass.buildCFGs();
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
            // Are Arrays of Objects Supported?
            String className = type instanceof ClassType ?
                    ((ClassType) type).getName() : this.ollirClass.getClassName();

            String fullyQualifiedClassName = this.fullyQualifiedNames.get(className);
            String finalClassName = fullyQualifiedClassName != null
                    ? this.fullyQualifiedNames.get(className) : className;

            typeCode.append("L").append(finalClassName).append(";");
            return typeCode.toString();
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

        HashMap<String, Instruction> labels = method.getLabels();
        for(Instruction instruction : method.getInstructions()) {
            for(String label : labels.keySet())
               if(labels.get(label) == instruction) this.jasminCodeBuilder.append(label).append(":\n");

            this.jasminCodeBuilder.append(buildMethodInstructions(instruction));

            if(instruction.getInstType() == InstructionType.CALL)
                if(((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID)
                    this.jasminCodeBuilder.append("\tpop\n");
        }

        if (method.isConstructMethod() || method.getReturnType().getTypeOfElement() == ElementType.VOID)
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
        StringBuilder binaryOpInstruction = new StringBuilder();
        String labelTrue, labelContinue;
        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();

        switch(instruction.getOperation().getOpType()){
            case ANDB: case ANDI32:
                binaryOpInstruction.append(pushElement(leftOperand))
                                   .append(pushElement(rightOperand))
                                   .append("\n\tiand\n");
                break;
            case ORB: case ORI32:
                binaryOpInstruction.append(pushElement(leftOperand))
                                   .append(pushElement(rightOperand))
                                   .append("\n\tior\n");
                break;
            case NOT: case NOTB:
                labelTrue = "True_" + this.operatorLabel;
                labelContinue = "Continue_" + this.operatorLabel++;

                binaryOpInstruction.append(pushElement(leftOperand))
                                   .append("\tifgt ").append(labelTrue)
                                   .append("\n\ticonst_1\n")
                                   .append("\tgoto ").append(labelContinue).append("\n")
                                   .append(labelTrue).append(":\n")
                                   .append("\ticonst_0\n")
                                   .append(labelContinue).append(":\n");
                break;
            case EQ: case EQI32:
                labelTrue = "True_" + this.operatorLabel;
                labelContinue = "Continue_" + this.operatorLabel++;

                binaryOpInstruction.append(pushElement(leftOperand))
                                   .append(pushElement(rightOperand))
                                   .append("\tif_icmpeq ").append(labelTrue)
                                   .append("\n\ticonst_0\n")
                                   .append("\tgoto ").append(labelContinue).append("\n")
                                   .append(labelTrue).append(":\n")
                                   .append("\ticonst_1\n")
                                   .append(labelContinue).append(":\n");
                break;
            case LTH: case LTHI32:
                labelTrue = "True_" + this.operatorLabel;
                labelContinue = "Continue_" + this.operatorLabel++;

                binaryOpInstruction.append(pushElement(leftOperand))
                        .append(pushElement(rightOperand))
                        .append("\tif_icmplt ").append(labelTrue)
                        .append("\n\ticonst_0\n")
                        .append("\tgoto ").append(labelContinue).append("\n")
                        .append(labelTrue).append(":\n")
                        .append("\ticonst_1\n")
                        .append(labelContinue).append(":\n");
                break;
            case GTE: case GTEI32:
                labelTrue = "True_" + this.operatorLabel;
                labelContinue = "Continue_" + this.operatorLabel++;

                binaryOpInstruction.append(pushElement(leftOperand))
                        .append(pushElement(rightOperand))
                        .append("\tif_icmpge ").append(labelTrue)
                        .append("\n\ticonst_0\n")
                        .append("\tgoto ").append(labelContinue).append("\n")
                        .append(labelTrue).append(":\n")
                        .append("\ticonst_1\n")
                        .append(labelContinue).append(":\n");
                break;
            case ADD: case ADDI32:
                binaryOpInstruction.append(pushElement(leftOperand))
                        .append(pushElement(rightOperand))
                        .append("\tiadd\n");
                break;
            case MUL: case MULI32:
                binaryOpInstruction.append(pushElement(leftOperand))
                        .append(pushElement(rightOperand))
                        .append("\timul\n");
                break;
            case DIV: case DIVI32:
                binaryOpInstruction.append(pushElement(leftOperand))
                        .append(pushElement(rightOperand))
                        .append("\tidiv\n");
                break;
            case SUB: case SUBI32:
                binaryOpInstruction.append(pushElement(leftOperand))
                        .append(pushElement(rightOperand))
                        .append("\tisub\n");
                break;
            default:
                throw new NotImplementedException(instruction.getOperation().getOpType());
        }

        return binaryOpInstruction.toString();
    }

    private String buildReturnInstruction(ReturnInstruction instruction) {
        StringBuilder returnInstruction = new StringBuilder();

        if (!instruction.hasReturnValue()) return "";

        Element operand = instruction.getOperand();
        String returnType = (operand.getType().getTypeOfElement() == ElementType.INT32
                || operand.getType().getTypeOfElement() == ElementType.BOOLEAN) ? "i" : "a";

        returnInstruction.append(pushElement(operand)).append("\t").append(returnType).append("return\n");

        return returnInstruction.toString();

    }

    private String buildPutFieldInstruction(PutFieldInstruction instruction) {
        StringBuilder putFieldInstruction = new StringBuilder();

        Operand field = ((Operand) instruction.getSecondOperand());

        String fieldClass = ((Operand)instruction.getFirstOperand()).getName();
        String fieldName =  Objects.equals(fieldClass, "this")
                ? this.ollirClass.getClassName() : fieldClass;

        putFieldInstruction.append(pushElement(instruction.getFirstOperand()))
                           .append(pushElement(instruction.getThirdOperand()))
                           .append("\tputfield ")
                           .append(fieldName).append("/")
                           .append(field.getName()).append(" ")
                           .append(buildTypes(field.getType())).append("\n");

        return putFieldInstruction.toString();
    }


    private String buildGetFieldInstruction(GetFieldInstruction instruction) {
        StringBuilder getFieldInstruction = new StringBuilder();

        Operand field = ((Operand) instruction.getSecondOperand());

        String fieldClass = ((Operand)instruction.getFirstOperand()).getName();
        String fieldName =  Objects.equals(fieldClass, "this")
                ? this.ollirClass.getClassName() : fieldClass;

        getFieldInstruction.append(pushElement(instruction.getFirstOperand()))
                .append("\tgetfield ")
                .append(fieldName).append("/")
                .append(field.getName()).append(" ")
                .append(buildTypes(field.getType())).append("\n");

        return getFieldInstruction.toString();
    }


    private String buildBranchInstruction(CondBranchInstruction instruction) {
        StringBuilder condBranchInstruction = new StringBuilder();

        BinaryOpInstruction condition= ((BinaryOpInstruction)instruction.getCondition());

        Element leftOperand = condition.getLeftOperand();
        Element rightOperand = condition.getRightOperand();

        if(condition.getOperation().getOpType() == OperationType.ANDB){
            String labelComparison = "Condition_" + this.comparisonLabel++;

            condBranchInstruction.append(pushElement(leftOperand))
                    .append("\tifeq ").append(labelComparison).append("\n")
                    .append(pushElement(rightOperand))
                    .append("\tifeq ").append(labelComparison).append("\n")
                    .append("\tgoto ").append(instruction.getLabel()).append("\n")
                    .append(labelComparison).append(":\n");

            return condBranchInstruction.toString();
        }

        if(condition.getOperation().getOpType() == OperationType.ORB){
            String labelComparison = "Condition_" + this.comparisonLabel++;

            condBranchInstruction.append(pushElement(leftOperand))
                    .append("\tifgt ").append(instruction.getLabel()).append("\n")
                    .append(pushElement(rightOperand))
                    .append("\tifeq ").append(instruction.getLabel()).append("\n")
                    .append("\tgoto ").append(labelComparison).append("\n")
                    .append(labelComparison).append(":\n");

            return condBranchInstruction.toString();
        }

        condBranchInstruction.append(pushElement(leftOperand))
                .append(pushElement(rightOperand))
                .append("\t");

        switch (condition.getOperation().getOpType()){
            case GTE: case GTEI32:
                condBranchInstruction.append("if_icmpge ");
                break;
            case LTH: case LTHI32:
                condBranchInstruction.append("if_icmplt ");
                break;
            case EQ: case EQI32:
                condBranchInstruction.append("if_icmpeq ");
                break;
            case NOTB: case NEQ: case NEQI32:
                condBranchInstruction.append("if_icmpne ");
                break;
            default:
                throw new NotImplementedException("Condition Operation Not Implemented: "
                        + condition.getOperation().getOpType());
        }

        condBranchInstruction.append(instruction.getLabel())
                             .append("\n");

        return condBranchInstruction.toString();

    }

    private String buildGoToInstruction(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String buildAssignInstruction(AssignInstruction instruction) {
        StringBuilder assignInstruction = new StringBuilder();

        Operand operand = (Operand) instruction.getDest();
        Type destType = operand.getType();
        Descriptor destVariable = this.variableTable.get(operand.getName());

        // Increment Assignment
       if(instruction.getRhs().getInstType() == InstructionType.BINARYOPER){
            BinaryOpInstruction binaryOperation = (BinaryOpInstruction) instruction.getRhs();

            if(binaryOperation.getOperation().getOpType() == OperationType.ADD
                || binaryOperation.getOperation().getOpType() == OperationType.SUB){

                Operand leftOperand = binaryOperation.getLeftOperand().isLiteral() ?
                        null : (Operand) binaryOperation.getLeftOperand();
                Operand rightOperand = binaryOperation.getRightOperand().isLiteral() ?
                        null : (Operand) binaryOperation.getRightOperand();

                String operationSign = binaryOperation.getOperation().getOpType() == OperationType.ADD ? "" : "-";

                if(leftOperand != null && leftOperand.getName().equals(operand.getName()) && rightOperand == null){
                    String leftValue = operationSign +
                            ((LiteralElement) binaryOperation.getRightOperand()).getLiteral();

                    if(Integer.parseInt(leftValue) >= -128 && Integer.parseInt(leftValue) <= 127){
                        assignInstruction.append("\tiinc ")
                                .append(destVariable.getVirtualReg()).append(" ")
                                .append(leftValue).append("\n");
                        return assignInstruction.toString();
                    }

                }

                else if(rightOperand != null &&  rightOperand.getName().equals(operand.getName()) && leftOperand == null){
                    String rightValue = operationSign +
                            ((LiteralElement) binaryOperation.getLeftOperand()).getLiteral();

                    if(Integer.parseInt(rightValue) >= -128 && Integer.parseInt(rightValue) <= 127) {
                        assignInstruction.append("\tiinc ")
                                .append(destVariable.getVirtualReg()).append(" ")
                                .append(rightValue).append("\n");
                        return assignInstruction.toString();
                    }
                }
            }

        }

        // Array Assignment (Needs Testing - Next Checkpoint)
        if(destVariable.getVarType().getTypeOfElement() == ElementType.ARRAYREF
                && destType.getTypeOfElement() != ElementType.ARRAYREF){
            Element index = ((ArrayOperand) operand).getIndexOperands().get(0);

            assignInstruction.append(pushElementDescriptor(destVariable))
                    .append(pushElement(index));

            // Arrays with other dest types are stored as "astore"
            if(destType.getTypeOfElement() == ElementType.INT32 || destType.getTypeOfElement() == ElementType.BOOLEAN){
                assignInstruction.append(buildMethodInstructions(instruction.getRhs()))
                        .append("\tiastore\n");
                return assignInstruction.toString();
            }
        }
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
                break;
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
                    for(Element operand: instruction.getListOfOperands()) {
                        callInstruction.append(pushElement(operand));
                    }

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

        switch (element.getType().getTypeOfElement()){
            case INT32: case BOOLEAN:
                int literal = Integer.parseInt(element.getLiteral());
                if(literal == -1)
                    literalElement.append("iconst_m1\n");
                else if (literal >= 0 && literal <= 5)
                    literalElement.append("iconst_").append(literal).append("\n");
                else if (literal >= -128 && literal <= 127)
                    literalElement.append("bipush ").append(literal).append("\n");
                else if (literal >= -32768 && literal <= 32767)
                    literalElement.append("sipush ").append(literal).append("\n");
                else {
                    literalElement.append("ldc ").append(literal).append("\n");
                }
                break;
            default:
                literalElement.append("ldc ").append(element.getLiteral()).append("\n");
        }
        return literalElement.toString();
    }
}