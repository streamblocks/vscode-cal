package streamblocks.cal.validation;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.CheckType;

import com.google.inject.Inject;

import streamblocks.cal.cal.AstActor;
import streamblocks.cal.cal.AstProcedure;
import streamblocks.cal.cal.AstStatementCall;
import streamblocks.cal.cal.AstVariable;
import streamblocks.cal.cal.CalPackage;
import streamblocks.cal.visitors.BooleanSwitch;

public class WarningValidator extends AbstractCalValidator {

	@Inject
	XtextResourceSet rs;

	@Check(CheckType.NORMAL)
	public void checkAstVariable(final AstVariable variable) {

		boolean used = new BooleanSwitch() {

			@Override
			public Boolean caseAstStatementCall(AstStatementCall call) {
				if (variable instanceof AstProcedure) {
					if (call.getProcedure().equals((AstProcedure) variable)) {
						return true;
					}
				}
				return false;
			}

		}.doSwitch(EcoreUtil.getRootContainer(variable));

		if (!used && variable.eContainer() instanceof AstActor) {
			warning("The procedure " + variable.getName() + " is never called", variable,
					CalPackage.eINSTANCE.getAstVariable_Name(), -1);
		}

	}

}
