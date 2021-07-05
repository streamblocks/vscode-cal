/*
 * ----------------------------------------------------------------------------
 *  ____  _                            ____  _            _
 * / ___|| |_ _ __ ___  __ _ _ __ ___ | __ )| | ___   ___| | _____
 * \___ \| __| '__/ _ \/ _` | '_ ` _ \|  _ \| |/ _ \ / __| |/ / __|
 *  ___) | |_| | |  __/ (_| | | | | | | |_) | | (_) | (__|   <\__ \
 * |____/ \__|_|  \___|\__,_|_| |_| |_|____/|_|\___/ \___|_|\_\___/
 * ----------------------------------------------------------------------------
 * Copyright (c) 2020, Streamgenomics sarl
 * All rights reserved.
 * ----------------------------------------------------------------------------
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Streamgenomics sarl nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package streamblocks.cal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.nodemodel.INode;

import streamblocks.cal.cal.AstProcedure;
import streamblocks.cal.cal.AstState;
import streamblocks.cal.cal.AstType;
import streamblocks.cal.cal.AstTypeUser;
import streamblocks.cal.cal.CalFactory;
import streamblocks.cal.cal.CalPackage;
import streamblocks.cal.util.Util;

public class CalLinkingService extends DefaultLinkingService {

	public static String strUndef = "undef";

	private Resource stubsResource = null;

	private Map<String, AstProcedure> procedures;
	
	
	public CalLinkingService() {
		procedures = new HashMap<String, AstProcedure>();

		addProcedure("print");
		addProcedure("println");
	}

	
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) {

		List<EObject> result = null;
		try {
			result = super.getLinkedObjects(context, ref, node);
		} catch (Exception x) {
			System.out.println(x.getMessage());
		}
		if (result != null && !result.isEmpty()) {
			return result;
		}

		final EClass requiredType = ref.getEReferenceType();
		final String s = getCrossRefNodeAsString(node);
		if (requiredType != null && s != null) {
			if (CalPackage.Literals.AST_STATE.isSuperTypeOf(requiredType)) {
				return getState(context, ref, s);
			}
			if (CalPackage.Literals.AST_PROCEDURE.isSuperTypeOf(requiredType)) {
				return builtinProcedure(context, s);
			}
		}

		return Collections.emptyList();
	}

	private List<EObject> getState(EObject context, EReference reference, String name) {
		EObject cter = Util.getTopLevelContainer(context);

		// Create the model element instance using the factory
		AstState state = CalFactory.eINSTANCE.createAstState();
		state.setName(name);

		// Attach the stub to the resource that's being parsed
		Resource res = makeResource(cter.eResource());
		res.getContents().add(state);

		return Collections.singletonList((EObject) state);
	}

	
	/**
	 * Returns a singleton if <code>name</code> is a builtin procedure, and an
	 * empty list otherwise.
	 * 
	 * @param context
	 *            the context in which a procedure is referenced.
	 * @param name
	 *            procedure name
	 * @return a list
	 */
	private List<EObject> builtinProcedure(EObject context, String name) {
		AstProcedure procedure = procedures.get(name);
		if (procedure != null) {
			EObject cter = EcoreUtil2.getRootContainer(context);

			// Attach the stub to the resource that's being parsed
			Resource res = makeResource(cter.eResource());
			res.getContents().add(procedure);

			return Collections.singletonList((EObject) procedure);
		}

		return Collections.emptyList();
	}
	
	/**
	 * Use a temporary 'child' resource to hold created stubs. The real resource URI
	 * is used to generate a 'temporary' resource to be the container for stub
	 * EObjects.
	 * 
	 * @param source the real resource that is being parsed
	 * @return the cached reference to a resource named by the real resource with
	 *         the added extension 'xmi'
	 */
	private Resource makeResource(Resource source) {
		if (null != stubsResource)
			return stubsResource;
		URI stubURI = source.getURI().appendFileExtension("xmi");

		stubsResource = source.getResourceSet().getResource(stubURI, false);
		if (null == stubsResource) {
			// TODO find out if this should be cleaned up so as not to clutter
			// the project.
			stubsResource = source.getResourceSet().createResource(stubURI);
		}

		return stubsResource;
	}

	public static AstType createAstTypeUndef() {
		AstType type = CalFactory.eINSTANCE.createAstType();
		AstTypeUser typeUser = CalFactory.eINSTANCE.createAstTypeUser();
		typeUser.setName(strUndef);
		type.setName(typeUser);
		return type;
	}
	
	/**
	 * Adds a new procedure to the built-in procedures map with the given
	 * parameters types .
	 * 
	 * @param name
	 *            function name
	 * @param parameters
	 *            types of function parameters
	 */
	private void addProcedure(String name, AstType... parameters) {
		AstProcedure procedure;
		procedure = CalFactory.eINSTANCE.createAstProcedure();
		procedure.setName(name);
		procedures.put(name, procedure);
	}
	
}
