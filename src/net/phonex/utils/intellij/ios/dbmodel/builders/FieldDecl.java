package net.phonex.utils.intellij.ios.dbmodel.builders;

import com.jetbrains.objc.psi.OCDeclaration;
import com.jetbrains.objc.psi.OCDeclarator;

/**
 * Holder class for globally defined field.
 */
public class FieldDecl {
    public String name;
    public String initializer;
    public OCDeclaration declaration;
    public OCDeclarator declarator;

    public FieldDecl(String name, String initializer, OCDeclaration declaration, OCDeclarator declarator) {
        this.name = name;
        this.initializer = initializer;
        this.declaration = declaration;
        this.declarator = declarator;
    }

    @Override
    public String toString() {
        return "FieldDecl{" +
                "name='" + name + '\'' +
                ", initializer='" + initializer + '\'' +
                ", declaration=" + declaration +
                ", declarator=" + declarator +
                '}';
    }
}
