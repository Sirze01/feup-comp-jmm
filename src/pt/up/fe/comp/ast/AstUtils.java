package pt.up.fe.comp.ast;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Objects;

public abstract class AstUtils {
    public static Type getNodeType(JmmNode node){
        Type type;

        if (Objects.equals(node.get("type"), "IntArray")) {
            type = new Type("Int", true);
        } else {
            type = new Type(node.get("type"), false);
        }

        return type;
    }

    public static JmmNode getFirstDescendantOfKind(JmmNode node, String kind){
        for(JmmNode child : node.getChildren()){
            if(child.getKind().equals(kind)){
                return child;
            }
            return getFirstDescendantOfKind(child, kind);
        }

        return null;
    }
}
