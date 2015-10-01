package net.phonex.utils.intellij.ios.dbmodel.builders;

import com.google.common.base.CaseFormat;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.jetbrains.cidr.*;
import com.jetbrains.cidr.lang.psi.*;
import com.jetbrains.cidr.lang.types.OCType;
import com.jetbrains.cidr.lang.util.OCElementFactory;
import net.phonex.utils.intellij.ios.dbmodel.utilities.PsiUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbModelBuilder {
    private static final Logger LOG = Logger.getInstance(DbModelBuilder.class);

    private String prefix;
    private PsiFile psiFile;
    private OCClassDeclaration ocClassDecl;
    private List<OCProperty> fields;

    CodeStyleManager codeStyleManager;

    // Class implementation & its containing file, regardless to the current selection.
    private OCImplementation clsImpl;
    private PsiFile fileImpl;

    // Class interface & its containing file, regardless to the current selection.
    private OCInterface clsDecl;
    private PsiFile fileDecl;

    public void setContext(final PsiFile psiFile, final OCClassDeclaration classDecl, final List<OCProperty> fields){
        this.psiFile = psiFile;
        this.ocClassDecl = classDecl;
        this.fields = fields;
        codeStyleManager = CodeStyleManager.getInstance(ocClassDecl.getProject());

        // Get class implementation & its containing file, regardless to the current selection.
        try {
            clsImpl = PsiUtility.getImplementationFor(ocClassDecl);
            fileImpl = clsImpl == null ? null : clsImpl.getContainingFile();
        } catch(Exception ex){

        }

        // Get class interface & its containing file, regardless to the current selection.
        try {
            clsDecl = PsiUtility.getInterfaceFor(ocClassDecl);
            fileDecl = clsDecl == null ? null : clsDecl.getContainingFile();
        } catch(Exception e){

        }
    }

    public String generateCreateTableString(OCClassDeclaration psiClass, List<OCProperty> fields){
        StringBuilder builder = new StringBuilder("+(NSString *) getCreateTable {\n" +
                "    NSString *createTable = [[NSString alloc] initWithFormat:\n" +
                "            @\"CREATE TABLE IF NOT EXISTS %@ (\"\n" +
                "                    \"  %@  INTEGER PRIMARY KEY AUTOINCREMENT, \"//  \t\t\t\t "+this.prefix+"_FIELD_ID\n");
        StringBuilder bVars = new StringBuilder(this.prefix + "_TABLE_NAME, \n "+this.prefix+"_FIELD_ID, \n");

        final String idFieldName = this.prefix + "_FIELD_ID";
        int cnEntries = 0;
        int nmEntries = 0;
        for (OCProperty field : fields) {
            for (OCDeclarator decl : field.getDeclaration().getDeclarators()) {
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, decl.getName());
                String fieldName = this.prefix + "_FIELD_" + upname;
                if (idFieldName.equals(fieldName)){
                    continue;
                }

                nmEntries += 1;
            }
        }

        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){
                String typeStr = decl.getResolvedType().getBestNameInContext(psiClass);
                String cannonType = decl.getResolvedType().getCanonicalName();
                String name = decl.getName();

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;
                if (idFieldName.equalsIgnoreCase(fieldName)){
                    continue;
                }

                cnEntries += 1;
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

    public List<OCDeclaration> generateFieldDeclaration(){
        StringBuilder builderInt = new StringBuilder("");
        List<OCDeclaration> decls = new ArrayList<OCDeclaration>();

        final String table = "extern NSString * " + this.prefix + "_TABLE;\n";
        decls.add(OCElementFactory.declarationFromText(table, psiFile));

        int cnEntries = 0;
        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){

                String typeStr = decl.getResolvedType().getBestNameInContext(ocClassDecl);
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

    public List<OCDeclaration> generateFieldDefinition(){
        StringBuilder builderImp = new StringBuilder("");
        List<OCDeclaration> decls = new ArrayList<OCDeclaration>();

        final String table = "NSString * " + this.prefix + "_TABLE = @\"FIXME\"; //TODO: FIXME: give propper table name\n";
        decls.add(OCElementFactory.declarationFromText(table, psiFile));

        int cnEntries = 0;
        for (OCProperty field : fields) {
            for(OCDeclarator decl : field.getDeclaration().getDeclarators()){

                String typeStr = decl.getResolvedType().getBestNameInContext(ocClassDecl);
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

    public void generateCreateTable() {
        // Exists in declaration file?
        OCMethod declMethod = findMethod(clsDecl, "getCreateTable");
        if (declMethod == null){
            final String declMethodString = "+(NSString *) getCreateTable;\n";
            declMethod = OCElementFactory.methodFromText(declMethodString, clsDecl, true);
            PsiElement createTableElement = clsDecl.addBefore(declMethod, clsDecl.getLastChild());
            codeStyleManager.reformat(createTableElement);
        }

        final String createTableMethodAsString = generateCreateTableString(clsImpl, fields);
        OCMethod createTableMethod = OCElementFactory.methodFromText(createTableMethodAsString, clsImpl, true);

        OCMethod implMethod = findMethod(clsImpl, "getCreateTable");
        PsiElement createTableElement = implMethod == null ?
                  clsImpl.addBefore(createTableMethod, clsImpl.getLastChild())
                : clsImpl.addAfter(createTableMethod, implMethod);
        codeStyleManager.reformat(createTableElement);
    }

    public void generateCreateFromCursor(){
        // Exists in declaration file?
        OCMethod declMethod = findMethod(clsDecl, "createFromCursor:");
        if (declMethod == null){
            final String declMethodString = "- (void)createFromCursor:(PEXDbCursor *)c;\n";
            declMethod = OCElementFactory.methodFromText(declMethodString, clsDecl, true);
            PsiElement elem = clsDecl.addBefore(declMethod, clsDecl.getLastChild());
            codeStyleManager.reformat(elem);
        }

        final String methodString = generateCreateFromCursorMethod(clsImpl, fields);
        OCMethod method = OCElementFactory.methodFromText(methodString, clsImpl, true);

        OCMethod implMethod = findMethod(clsImpl, "createFromCursor:");
        PsiElement elem = implMethod == null ?
                clsImpl.addBefore(method, clsImpl.getLastChild())
                : clsImpl.addAfter(method, implMethod);
        codeStyleManager.reformat(elem);
    }

    public void generateGetContentValues(){
        // Exists in declaration file?
        OCMethod declMethod = findMethod(clsDecl, "getDbContentValues");
        if (declMethod == null){
            final String declMethodString = "- (PEXDbContentValues *)getDbContentValues;\n";
            declMethod = OCElementFactory.methodFromText(declMethodString, clsDecl, true);
            PsiElement elem = clsDecl.addBefore(declMethod, clsDecl.getLastChild());
            codeStyleManager.reformat(elem);
        }

        final String methodString = generateGetDbContentValuesMethod(clsImpl, fields);
        OCMethod method = OCElementFactory.methodFromText(methodString, clsImpl, true);

        OCMethod implMethod = findMethod(clsImpl, "getDbContentValues");
        PsiElement elem = implMethod == null ?
                clsImpl.addBefore(method, clsImpl.getLastChild())
                : clsImpl.addAfter(method, implMethod);
        codeStyleManager.reformat(elem);
    }

    /**
     * Generates globally defined constants for DB fields.
     */
    public void generateDbFields() {
        List<NewFieldRecord> newFields = new ArrayList<NewFieldRecord>();

        // Add table
        newFields.add(new NewFieldRecord(prefix + "_TABLE_NAME", clsDecl.getName(), "TODO: FIXME: give a proper table name"));
        for (OCProperty field : fields) {
            for (OCDeclarator decl : field.getDeclaration().getDeclarators()) {
                String name = decl.getName();

                // Convert camel case to snake case.
                String upname = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
                String fieldName = this.prefix + "_FIELD_" + upname;

                // Add new field definition to array.
                newFields.add(new NewFieldRecord(fieldName, name));
            }
        }

        // Iterate over collected new fields.
        PsiElement prevDecl = null;
        PsiElement prevImpl = null;
        for (NewFieldRecord newField : newFields) {
            // Find if field is already present in declaration file.
            FieldDecl prevFieldDecl = findField(fileDecl, newField.name);
            if (prevFieldDecl == null){
                OCDeclaration fieldDecl = OCElementFactory.declarationFromText(newField.decl, fileDecl);
                PsiElement fieldElem = prevDecl == null ? fileDecl.addBefore(fieldDecl, clsDecl) : fileDecl.addAfter(fieldDecl, prevDecl);
                prevDecl = codeStyleManager.reformat(fieldElem);
            } else {
                prevDecl = prevFieldDecl.declaration;
            }

            // Find if field is already present in implementation file.
            FieldDecl prevFieldImpl = findField(fileImpl, newField.name);
            if (prevFieldImpl == null){
                OCDeclaration fieldImpl = OCElementFactory.declarationFromText(newField.impl, fileImpl);
                PsiElement fieldElem = prevImpl == null ? fileImpl.addBefore(fieldImpl, clsImpl) : fileImpl.addAfter(fieldImpl, prevImpl);
                prevImpl = codeStyleManager.reformat(fieldElem);
                continue;
            } else {
                prevImpl = prevFieldImpl.declaration;
            }

            // Present, do we have exact match of the values?
            final String valueInitializer = "\"" + newField.value + "\"";
            if (newField.value.equals(prevFieldImpl.initializer) || valueInitializer.equals(prevFieldImpl.initializer)){
                continue;
            }

            // Present and value differs, add anyway, but under previous field.
            newField.addComment("TODO: verify");
            OCDeclaration fieldImpl = OCElementFactory.declarationFromText(newField.impl, fileImpl);
            PsiElement fieldElem = fileImpl.addAfter(fieldImpl, prevFieldImpl.declaration);
            codeStyleManager.reformat(fieldElem);
        }
    }

    private PsiElement addAsLast(PsiElement elem){
        return ocClassDecl.addBefore(elem, ocClassDecl.getLastChild());
    }

    /**
     * Locates all globally defined NSString variables, returns map with variable name as a key.
     * @param fileImpl
     * @return
     */
    public Map<String, FieldDecl> getAllGlobalFields(PsiFile fileImpl){
        HashMap<String, FieldDecl> declMap = new HashMap<String, FieldDecl>();
        if (fileImpl == null){
            return declMap;
        }

        // Scan for defined declarations.
        PsiElement[] children = fileImpl.getChildren();
        for (PsiElement child : children) {
            if (!(child instanceof OCDeclaration)) {
                continue;
            }

            final OCDeclaration decl = (OCDeclaration) child;
            final String cannonType = decl.getResolvedType().getCanonicalName();
            if (!"NSString *".equalsIgnoreCase(cannonType) && !"NSString".equalsIgnoreCase(cannonType)){
                continue;
            }

            for (OCDeclarator ocDeclarator : decl.getDeclarators()) {
                final String name = ocDeclarator.getName();
                final OCExpression initializer = ocDeclarator.getInitializer();
                if (initializer != null) {
                    if (!(initializer instanceof OCLiteralExpression)) {
                        continue;
                    }

                    final OCLiteralExpression ocLit = (OCLiteralExpression) initializer;
                    declMap.put(name, new FieldDecl(name, ocLit.getRawLiteralText(), decl, ocDeclarator));
                } else {
                    declMap.put(name, new FieldDecl(name, null, decl, ocDeclarator));
                }
            }
        }

        return declMap;
    }

    /**
     * Tries to look up method by its name.
     * @param cls
     * @param methodName
     * @return
     */
    private static OCMethod findMethod(OCClassDeclaration cls, String methodName) {
        List<OCMethod> methods = cls.getMethods();
        for (OCMethod method : methods) {
            if (!methodName.equals(method.getName())){
                continue;
            }

            return method;
        }

        return null;
    }

    /**
     * Tries to find field declaration/definition in a given file.
     * @param file
     * @param varName
     * @return
     */
    public static FieldDecl findField(PsiFile file, String varName) {
        if (file == null){
            return null;
        }

        // Scan for defined declarations.
        PsiElement[] children = file.getChildren();
        for (PsiElement child : children) {
            if (!(child instanceof OCDeclaration)) {
                continue;
            }

            final OCDeclaration decl = (OCDeclaration) child;
            final String cannonType = decl.getResolvedType().getCanonicalName();
            if (!"NSString *".equalsIgnoreCase(cannonType) && !"NSString".equalsIgnoreCase(cannonType)){
                continue;
            }

            for (OCDeclarator ocDeclarator : decl.getDeclarators()) {
                final String name = ocDeclarator.getName();
                if (!varName.equals(name)){
                    continue;
                }

                final OCExpression initializer = ocDeclarator.getInitializer();
                if (initializer != null) {
                    if (!(initializer instanceof OCLiteralExpression)) {
                        continue;
                    }

                    final OCLiteralExpression ocLit = (OCLiteralExpression) initializer;
                    return new FieldDecl(name, ocLit.getRawLiteralText(), decl, ocDeclarator);
                } else {
                    return new FieldDecl(name, null, decl, ocDeclarator);
                }
            }
        }

        return null;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

}