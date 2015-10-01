package net.phonex.utils.intellij.ios.dbmodel.generators;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.jetbrains.cidr.lang.psi.*;
import com.jetbrains.cidr.lang.types.OCType;
import com.jetbrains.cidr.lang.util.OCElementFactory;
import net.phonex.utils.intellij.ios.dbmodel.utilities.PsiUtility;
import net.phonex.utils.intellij.ios.dbmodel.builders.DbModelBuilder;

import java.util.List;

public class GenerateCreateTableAction  extends AnAction {
    private static final String TITLE = "Select Fields for createTable";
    private static final String LABEL_TEXT = "Fields to include in createTable:";

    private final DbModelBuilder dbModelBuilder = new DbModelBuilder();
    private final PsiUtility psiUtility = new PsiUtility();

    public void actionPerformed(AnActionEvent e) {
        OCClassDeclaration psiClass = psiUtility.getOCClassDeclaration(e);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        GenerateDialog dlg = new GenerateDialog(psiFile, psiClass, TITLE, LABEL_TEXT);
        dlg.show();
        if (dlg.isOK()) {
            generateEqualsHashCode(psiFile, psiClass, dlg.getFields(), dlg.getPrefix());
        }
    }

    public void generateEqualsHashCode(final PsiFile psiFile, final OCClassDeclaration ocClassDecl, final List<OCProperty> fields, final String prefix) {
        new WriteCommandAction.Simple(ocClassDecl.getProject(), ocClassDecl.getContainingFile()) {

            @Override
            protected void run() throws Throwable {
                final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(ocClassDecl.getProject());

                dbModelBuilder.setPrefix(prefix);
                dbModelBuilder.setContext(psiFile, ocClassDecl, fields);
                dbModelBuilder.generateCreateTable();
            }

        }.execute();
    }

    @Override
    public void update(AnActionEvent e) {
        OCClassDeclaration psiClass = psiUtility.getOCClassDeclaration(e);
        e.getPresentation().setEnabled(psiClass != null);
    }
}