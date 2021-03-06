/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.deltacloud.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.deltacloud.core.DeltaCloud;
import org.jboss.tools.internal.deltacloud.ui.utils.WorkbenchUtils;
import org.jboss.tools.internal.deltacloud.ui.wizards.NewInstanceWizard;

/**
 * @author Jeff Johnston
 * @author André Dietisheim
 */
public class CreateInstanceFromAnyImageHandler extends AbstractHandler implements IHandler {

	@Override 
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		DeltaCloud cloud = null;
		cloud = getSelectedCloud(event, selection, cloud);
		if (cloud != null) {
			IWizard wizard = new NewInstanceWizard(cloud);
			WizardDialog dialog =
					new WizardDialog(WorkbenchUtils.getActiveWorkbenchWindow().getShell(), wizard);
			dialog.create();
			dialog.open();
		}
		return Status.OK_STATUS;
	}

	private DeltaCloud getSelectedCloud(ExecutionEvent event, ISelection selection, DeltaCloud cloud) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// try selection
			cloud = WorkbenchUtils.getFirstAdaptedElement(selection, DeltaCloud.class);
		} 
		if (cloud == null) {
			// try active part
			cloud = WorkbenchUtils.adapt(HandlerUtil.getActivePart(event), DeltaCloud.class);
		}
		return cloud;
	}
}
