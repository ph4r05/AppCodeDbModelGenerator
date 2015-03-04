package net.phonex.utils.intellij.ios.dbmodel.builders;

import com.google.common.base.CaseFormat;
import com.intellij.psi.PsiFile;
import com.jetbrains.objc.psi.*;
import com.jetbrains.objc.types.OCType;
import com.jetbrains.objc.util.OCElementFactory;

import java.util.ArrayList;
import java.util.List;

public class DbModelBuilder {
    private String prefix;

    public String generateCreateTable(OCClassDeclaration psiClass, List<OCProperty> fields){
        StringBuilder builder = new StringBuilder("+(NSString *) getCreateTable {\n" +
                "    NSString *createTable = [[NSString alloc] initWithFormat:\n" +
                "            @\"CREATE TABLE IF NOT EXISTS %@ (\"\n" +
                "                    \"  %@  INTEGER PRIMARY KEY AUTOINCREMENT, \"//  \t\t\t\t "+this.prefix+"_FIELD_ID\n");
        StringBuilder bVars = new StringBuilder(this.prefix + "_TABLE, \n "+this.prefix+"_FIELD_ID, \n");

        final String idFieldName = this.prefix + "_FIELD_ID";
        int cnEntries = 0;
        int nmEntries = 0;
        for (OCProperty field : fields) {
            for (OCDeclarator decl : field.getDeclaration().getDeclarators()) {
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, decl.getName());
                String fieldName = this.prefix + "_FIELD_" + upname;
                if (idFieldName.equals(fieldName))
                nmEntries += 1;
            }
        }

        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){
                String typeStr = decl.getResolvedType().getBestNameInContext(psiClass);
                String cannonType = decl.getResolvedType().getCanonicalName();
                String name = decl.getName();
                cnEntries += 1;

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;
                if (idFieldName.equalsIgnoreCase(fieldName)){
                    continue;
                }

                String sqlType = "TEXT";
                if ("NSNumber *".equalsIgnoreCase(cannonType)) {
                    sqlType = "INTEGER";
                } else if ("NSDate *".equalsIgnoreCase(cannonType)) {
                    sqlType = "INTEGER";
                } else if ("BOOL".equalsIgnoreCase(cannonType)) {
                    sqlType = "INTEGER";
                } else if ("int".equalsIgnoreCase(cannonType)) {
                    sqlType = "INTEGER";
                } else if ("double".equalsIgnoreCase(cannonType)) {
                    sqlType = "INTEGER";
                }

                String comma = cnEntries == nmEntries ? "" : ",";
                builder.append("                    \"  %@  ").append(sqlType).append(comma).append(" \"//  \t\t\t\t ").append(fieldName).append("\n");

                // Variables.
                bVars.append(fieldName).append(comma).append(" \n");
            }
        }

        builder.append("\" );\",\n");
        builder.append(bVars.toString());
        builder.append("];\n" +
                "    return createTable;\n" +
                "}");
        return builder.toString();
    }

    public String generateCreateFromCursorMethod(OCClassDeclaration psiClass, List<OCProperty> fields) {
        StringBuilder builder = new StringBuilder("/**\n" +
                "* Create wrapper with content values pairs.\n" +
                "*\n" +
                "* @param args the content value to unpack.\n" +
                "*/\n" +
                "-(void) createFromCursor: (PEXDbCursor *) c {\n" +
                "    int colCount = [c getColumnCount];\n" +
                "    for(int i=0; i<colCount; i++) {\n" +
                "        NSString *colname = [c getColumnName:i];\n");

        int cnEntries = 0;
        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){
                String typeStr = decl.getResolvedType().getBestNameInContext(psiClass);
                String cannonType = decl.getResolvedType().getCanonicalName();
                String name = decl.getName();

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;

                if (cnEntries == 0){
                    builder.append("if ");
                } else {
                    builder.append(" else if ");
                }

                String method = getMethodForType(decl, decl.getResolvedType());
                builder.append(" ([" + fieldName + " isEqualToString: colname]){\n");
                builder.append(" _"+name+" = "+method+"\n");
                builder.append("}");

                cnEntries +=1 ;
            }
        }
        if (cnEntries > 0){
            builder.append(" else {\n" +
                    "            DDLogWarn(@\"Unknown column name %@\", colname);\n" +
                    "        }");
        }

        builder.append("    }\n" +
                "}");
        return builder.toString();
    }

    public String getMethodForType(OCDeclarator decl, OCType typeObj) {
        String type = typeObj.getCanonicalName();
        if ("NSString *".equalsIgnoreCase(type) || "NSMutableString *".equalsIgnoreCase(type)) {
            return "[c getString:i];";
        } else if ("NSNumber *".equalsIgnoreCase(type)) {
            if ("id".equalsIgnoreCase(decl.getName())){
                return "[c getInt64:i];";
            } else {
                return "[c getInt:i];";
            }
        } else if ("NSDate *".equalsIgnoreCase(type)) {
            return "[PEXDbModelBase getDateFromCursor:c idx:i];";
        } else if ("NSData *".equalsIgnoreCase(type) || "NSMutableData *".equalsIgnoreCase(type)) {
            return "[[NSData alloc] initWithBase64EncodedData:[c getString:i] options:0];";
        } else if ("BOOL".equalsIgnoreCase(type)) {
            return "[[c getInt:i] boolValue];";
        } else if ("int".equalsIgnoreCase(type)) {
            return "[[c getInt:i] integerValue];";
        } else if ("double".equalsIgnoreCase(type)) {
            return "[[c getDouble:i] doubleValue];";
        } else {
            return "[c getString:i]; //TODO:verify, type=" + type;
        }
    }

    public String generateGetDbContentValuesMethod(OCClassDeclaration psiClass, List<OCProperty> fields){
        StringBuilder builder = new StringBuilder("/**\n" +
                "* Pack the object content value to store\n" +
                "*\n" +
                "* @return The content value representing the message\n" +
                "*/\n" +
                "-(PEXDbContentValues *) getDbContentValues {\n" +
                "    PEXDbContentValues * cv = [[PEXDbContentValues alloc] init];\n");

        int cnEntries = 0;
        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){
                String typeStr = decl.getResolvedType().getBestNameInContext(psiClass);
                String cannonType = decl.getResolvedType().getCanonicalName();
                String name = decl.getName();

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;

                // Special case - identifier.
                if ("id".equalsIgnoreCase(name)) {
                    builder.append("if (_id != nil && [_id longLongValue] != -1ll) {\n" +
                            "    [cv put: "+fieldName+" NSNumberAsLongLong: _id];\n" +
                            "}\n");
                    continue;
                }

                String method = getPutMethodForType(decl, decl.getResolvedType());
                builder.append("if (_"+name+" != nil)\n" +
                        "    [cv  put: "+fieldName+" "+(method == null ? " string " : method)+" : _"+name+" ]; ");
                if (method == null){
                    builder.append(" // TODO:verify type: ").append(cannonType);
                }

                builder.append("\n");
                cnEntries +=1 ;
            }
        }

        builder.append("\nreturn cv; \n}\n");
        return builder.toString();
    }

    public String getPutMethodForType(OCDeclarator decl, OCType typeObj) {
        String type = typeObj.getCanonicalName();
        if ("NSNumber *".equalsIgnoreCase(type)) {
            return "number";
        } else if ("NSDate *".equalsIgnoreCase(type)) {
            return "date";
        } else if ("NSData *".equalsIgnoreCase(type) || "NSMutableData *".equalsIgnoreCase(type)) {
            return "data";
        } else if ("BOOL".equalsIgnoreCase(type)) {
            return "integer";
        } else if ("int".equalsIgnoreCase(type)) {
            return "integer";
        } else if ("double".equalsIgnoreCase(type)) {
            return "double";
        } else if ("NSString *".equalsIgnoreCase(type) || "NSMutableString *".equalsIgnoreCase(type) || typeObj.isPointerToStringCompatible()){
            return "string";
        } else {
            return null;
        }
    }

    public List<OCDeclaration> generateFieldDeclaration(final PsiFile psiFile, OCClassDeclaration psiClass, List<OCProperty> fields){
        StringBuilder builderInt = new StringBuilder("");
        List<OCDeclaration> decls = new ArrayList<OCDeclaration>();

        final String table = "extern NSString * " + this.prefix + "_TABLE;\n";
        decls.add(OCElementFactory.declarationFromText(table, psiFile));

        int cnEntries = 0;
        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){

                String typeStr = decl.getResolvedType().getBestNameInContext(psiClass);
                String cannonType = decl.getResolvedType().getCanonicalName();
                String name = decl.getName();

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;

                String curDecl = "extern NSString * " + fieldName + "; \n";
                builderInt.append(curDecl);

                decls.add(OCElementFactory.declarationFromText(curDecl, psiFile));
            }
        }

        return decls;
    }

    public List<OCDeclaration> generateFieldDefinition(final PsiFile psiFile, OCClassDeclaration psiClass, List<OCProperty> fields){
        StringBuilder builderImp = new StringBuilder("");
        List<OCDeclaration> decls = new ArrayList<OCDeclaration>();

        final String table = "NSString * " + this.prefix + "_TABLE = @\"FIXME\"; //TODO: FIXME: give propper table name\n";
        decls.add(OCElementFactory.declarationFromText(table, psiFile));

        int cnEntries = 0;
        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){

                String typeStr = decl.getResolvedType().getBestNameInContext(psiClass);
                String cannonType = decl.getResolvedType().getCanonicalName();
                String name = decl.getName();

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;

                builderImp.append("NSString * ").append(fieldName).append(" = @\"").append(name).append("\";\n");

                String curDef = builderImp.toString();
                decls.add(OCElementFactory.declarationFromText(curDef, psiFile));

                builderImp = new StringBuilder();
            }
        }

        return decls;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}