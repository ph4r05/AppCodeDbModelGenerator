package net.phonex.utils.intellij.ios.dbmodel.utilities;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.objc.psi.*;
import com.jetbrains.objc.resolve.OCResolveUtil;
import com.jetbrains.objc.search.OCSearchUtil;

public class PsiUtility {

    public OCClassDeclaration getOCClassDeclaration(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAt = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(elementAt, OCClassDeclaration.class);
    }

    public static OCInterface findInterfaceInElement(PsiElement element, String cName) {
        OCInterface ocx = null;
        for (OCInterface ocInterface : PsiTreeUtil.findChildrenOfType(element, OCInterface.class)) {
            String name = ocInterface.getName();

            if (cName.equals(name)) {
                ocx = ocInterface;
                break;
            }
        }

        return ocx;
    }

    public static OCInterface getInterfaceFor(OCClassDeclaration cls){
        OCFile contFile = cls.getContainingOCFile();
        if (contFile == null){
            return null;
        }

        OCFile assocFile = contFile.getAssociatedFile();
        if (assocFile == null){
            return null;
        }

        return findInterfaceInElement(assocFile, cls.getCanonicalName());
    }

    public static OCImplementation findImplementationInElement(PsiElement element, String cName) {
        OCImplementation ocx = null;
        for (OCImplementation ocImpl : PsiTreeUtil.findChildrenOfType(element, OCImplementation.class)) {
            String name = ocImpl.getName();

            if (cName.equals(name)) {
                ocx = ocImpl;
                break;
            }
        }

        return ocx;
    }

    public static OCImplementation getImplementationFor(OCClassDeclaration cls){
        OCFile contFile = cls.getContainingOCFile();
        if (contFile == null){
            return null;
        }

        OCFile assocFile = contFile.getAssociatedFile();
        if (assocFile == null){
            return null;
        }

        return findImplementationInElement(assocFile, cls.getCanonicalName());
    }



}