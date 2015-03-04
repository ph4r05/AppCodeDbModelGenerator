package net.phonex.utils.intellij.ios.dbmodel.generators;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.jetbrains.objc.psi.*;
import net.phonex.utils.intellij.ios.dbmodel.utilities.PsiUtility;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class GenerateDialog extends DialogWrapper {

    private final CollectionListModel<OCProperty> myFields;
    private final JComponent myComponent;
    private final JTextField myPrefix;
    private static final Logger LOG = Logger.getInstance(GenerateDialog.class);

    @SuppressWarnings("unchecked")
    public GenerateDialog(PsiFile psiFile, OCClassDeclaration psiClass, String title, String labelText) {
        super(psiClass.getProject());
        setTitle(title);

        final String cName = psiClass.getName();
        OCProperty[] ocProperties = fieldsFrom(psiClass);
        LOG.info(String.format("ocProperties len=%d", ocProperties.length));
        if (ocProperties.length == 0 && psiClass instanceof OCImplementation){
            OCFile cntFile = psiClass.getContainingOCFile();
            OCFile assocFile = cntFile.getAssociatedFile();

            LOG.info(String.format("Null properties. cName=%s, CnfFile=%s, assocFile: %s", cName, cntFile.getName(), assocFile.getName()));
            OCInterface ocx = PsiUtility.findInterfaceInElement(assocFile, cName);

            // Read properties from the @interface part.
            if (ocx != null){
                ocProperties = fieldsFrom(ocx);
            }
        }

        myFields = new CollectionListModel<OCProperty>(ocProperties);

        JList fieldList = new JBList(myFields);
        fieldList.setCellRenderer(new DefaultPsiElementCellRenderer());

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(fieldList);
        decorator.disableAddAction();
        JPanel panel = decorator.createPanel();
        LabeledComponent<JPanel> jPanelLabeledComponent = LabeledComponent.create(panel, labelText);

        myPrefix = new JBTextField("PEX_DBFT");
        LabeledComponent<JTextField> jTextPrefix = LabeledComponent.create(myPrefix, "Prefix");

        JBPanel jbPanel = new JBPanel(new BorderLayout());
        jbPanel.add(jPanelLabeledComponent, BorderLayout.CENTER);
        jbPanel.add(jTextPrefix, BorderLayout.PAGE_END);

        myComponent = jbPanel;
        init();
    }

    private OCProperty[] fieldsFrom(final OCClassDeclaration psiClass) {
        final List<OCProperty> filteredFields = new ArrayList<OCProperty>();
        List<OCProperty> propList = psiClass.getProperties();
        for (OCProperty psiField : propList) {
            filteredFields.add(psiField);
        }

        return filteredFields.toArray(new OCProperty[filteredFields.size()]);
    }

//
//    private boolean isNotSerialVersionUIDField(final PsiField psiField) {
//        return !("serialVersionUID".equals(psiField.getName())
//                && hasModifier(psiField, "static")
//                && hasModifier(psiField, "final")
//                && PsiType.LONG.equals(psiField.getType()));
//    }
//
//    private boolean hasModifier(final PsiField field, final String modifier) {
//        final PsiModifierList modifierList = field.getModifierList();
//        return modifierList != null && modifierList.hasModifierProperty(modifier);
//    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myComponent;
    }

    public List<OCProperty> getFields() {
        return myFields.getItems();
    }

    public String getPrefix(){
        return myPrefix.getText();
    }

}

