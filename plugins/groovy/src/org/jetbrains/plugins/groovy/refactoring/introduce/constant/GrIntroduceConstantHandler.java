/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.refactoring.introduce.constant;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.refactoring.GrRefactoringError;
import org.jetbrains.plugins.groovy.refactoring.GroovyRefactoringBundle;
import org.jetbrains.plugins.groovy.refactoring.introduce.GrIntroduceContext;
import org.jetbrains.plugins.groovy.refactoring.introduce.GrIntroduceDialog;
import org.jetbrains.plugins.groovy.refactoring.introduce.GrIntroduceHandlerBase;
import org.jetbrains.plugins.groovy.refactoring.introduce.StringPartInfo;

/**
 * @author Maxim.Medvedev
 */
public class GrIntroduceConstantHandler extends GrIntroduceHandlerBase<GrIntroduceConstantSettings> {
  public static final String REFACTORING_NAME = "Introduce Constant";

  @NotNull
  @Override
  protected String getRefactoringName() {
    return REFACTORING_NAME;
  }

  @NotNull
  @Override
  protected String getHelpID() {
    return HelpID.INTRODUCE_CONSTANT;
  }

  @NotNull
  @Override
  protected PsiElement findScope(GrExpression expression, GrVariable variable, StringPartInfo stringPart) {
    final PsiElement place = getCurrentPlace(expression, variable, stringPart);
    return place.getContainingFile();
  }

  @Override
  protected void checkExpression(@NotNull GrExpression selectedExpr) {
    selectedExpr.accept(new ConstantChecker(selectedExpr, selectedExpr));
  }

  @Override
  protected void checkVariable(@NotNull GrVariable variable) throws GrRefactoringError {
    final GrExpression initializer = variable.getInitializerGroovy();
    if (initializer == null) {
      throw new GrRefactoringError(RefactoringBundle.message("variable.does.not.have.an.initializer", variable.getName()));
    }
    checkExpression(initializer);
  }

  @Override
  protected void checkStringLiteral(@NotNull StringPartInfo info) throws GrRefactoringError {
    //todo
  }

  @Override
  protected void checkOccurrences(@NotNull PsiElement[] occurrences) {
    if (hasLhs(occurrences)) {
      throw new GrRefactoringError(GroovyRefactoringBundle.message("selected.variable.is.used.for.write"));
    }
  }

  @Nullable
  public static GrTypeDefinition findContainingClass(GrIntroduceContext context) {
    PsiElement place = context.getPlace();
    while (true) {
      final GrTypeDefinition typeDefinition = PsiTreeUtil.getParentOfType(place, GrTypeDefinition.class, true, GroovyFileBase.class);
      if (typeDefinition == null) return null;
      if (!typeDefinition.isAnonymous() &&
          (typeDefinition.hasModifierProperty(PsiModifier.STATIC) || typeDefinition.getContainingClass() == null)) {
        return typeDefinition;
      }
      place = typeDefinition;
    }
  }

  @NotNull
  @Override
  protected GrIntroduceDialog<GrIntroduceConstantSettings> getDialog(@NotNull GrIntroduceContext context) {
    return new GrIntroduceConstantDialog(context, findContainingClass(context));
  }

  @Override
  public GrField runRefactoring(@NotNull GrIntroduceContext context, @NotNull GrIntroduceConstantSettings settings) {
    return new GrIntroduceConstantProcessor(context, settings).run();
  }

  private static class ConstantChecker extends GroovyRecursiveElementVisitor {
    private final PsiElement scope;
    private final GrExpression expr;

    @Override
    public void visitReferenceExpression(GrReferenceExpression referenceExpression) {
      final PsiElement resolved = referenceExpression.resolve();
      if (resolved instanceof PsiVariable) {
        if (!isStaticFinalField((PsiVariable)resolved)) {
          if (expr instanceof GrClosableBlock) {
            if (!PsiTreeUtil.isContextAncestor(scope, resolved, true)) {
              throw new GrRefactoringError(GroovyRefactoringBundle.message("closure.uses.external.variables"));
            }
          }
          else {
            throw new GrRefactoringError(RefactoringBundle.message("selected.expression.cannot.be.a.constant.initializer"));
          }
        }
      }
      else if (resolved instanceof PsiMethod && ((PsiMethod)resolved).getContainingClass() != null) {
        final GrExpression qualifier = referenceExpression.getQualifierExpression();
        if (qualifier == null ||
            (qualifier instanceof GrReferenceExpression && ((GrReferenceExpression)qualifier).resolve() instanceof PsiClass)) {
          if (!((PsiMethod)resolved).hasModifierProperty(PsiModifier.STATIC)) {
            throw new GrRefactoringError(RefactoringBundle.message("selected.expression.cannot.be.a.constant.initializer"));
          }
        }
      }
    }

    private static boolean isStaticFinalField(PsiVariable var) {
      return var instanceof PsiField && var.hasModifierProperty(PsiModifier.FINAL) && var.hasModifierProperty(PsiModifier.STATIC);
    }

    @Override
    public void visitClosure(GrClosableBlock closure) {
      if (closure == expr) {
        super.visitClosure(closure);
      }
      else {
        closure.accept(new ConstantChecker(closure, scope));
      }
    }

    private ConstantChecker(GrExpression expr, PsiElement expressionScope) {
      scope = expressionScope;
      this.expr = expr;
    }
  }
}
