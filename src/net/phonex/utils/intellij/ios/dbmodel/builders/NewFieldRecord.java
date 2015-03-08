package net.phonex.utils.intellij.ios.dbmodel.builders;

/**
 * Holder class for new globally defined fields to add.
 */
public class NewFieldRecord {
    public String decl;
    public String impl;
    public final String name;
    public final String value;
    public String comment;

    public NewFieldRecord(String name, String value) {
        this.name = name.trim();
        this.value = value.trim();
        this.setupDefs();
    }

    public NewFieldRecord(String name, String value, String comment) {
        this.name = name.trim();
        this.value = value.trim();
        this.comment = comment.trim();
        this.setupDefs();
    }

    public void addComment(String comment){
        if (this.comment == null){
            this.comment = comment;
        } else {
            this.comment = this.comment + "; " + comment;
        }

        setupDefs();
    }

    public void setupDefs(){
        this.decl = "extern NSString * " + name + "; \n";
        if (comment == null) {
            this.impl = "NSString *" + name + " = @\"" + value + "\";\n";
        } else {
            this.impl = "NSString *" + name + " = @\"" + value + "\"; //" + comment + "\n";
        }
    }

    @Override
    public String toString() {
        return "NewFieldRecord{" +
                "declaration='" + decl + '\'' +
                ", impl='" + impl + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
