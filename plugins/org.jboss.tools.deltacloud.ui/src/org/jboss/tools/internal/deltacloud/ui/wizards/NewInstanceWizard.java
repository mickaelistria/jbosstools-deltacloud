/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.internal.deltacloud.ui.wizards;

import org.eclipse.core.runtime.Assert;
import org.jboss.tools.common.jobs.ChainedJob;
import org.jboss.tools.deltacloud.core.DeltaCloud;
import org.jboss.tools.deltacloud.core.DeltaCloudException;
import org.jboss.tools.deltacloud.core.DeltaCloudImage;
import org.jboss.tools.deltacloud.core.DeltaCloudInstance;
import org.jboss.tools.deltacloud.core.job.InstanceStateJob;
import org.jboss.tools.deltacloud.ui.Activator;
import org.jboss.tools.deltacloud.ui.DeltacloudUIExtensionManager;
import org.jboss.tools.deltacloud.ui.ErrorUtils;
import org.jboss.tools.deltacloud.ui.IDeltaCloudPreferenceConstants;
import org.jboss.tools.deltacloud.ui.wizard.INewInstanceWizardPage;
import org.jboss.tools.internal.deltacloud.ui.utils.UIUtils;

/**
 * @author Jeff Johnston
 * @author André Dieitsheim
 */
public class NewInstanceWizard extends AbstractDeltaCloudWizard {

	private final static String CREATE_INSTANCE_FAILURE_TITLE = "CreateInstanceError.title"; //$NON-NLS-1$
	private final static String CREATE_INSTANCE_FAILURE_MSG = "CreateInstanceError.msg"; //$NON-NLS-1$
	private final static String CONFIRM_CREATE_TITLE = "ConfirmCreate.title"; //$NON-NLS-1$
	private final static String CONFIRM_CREATE_MSG = "ConfirmCreate.msg"; //$NON-NLS-1$
	private final static String DONT_SHOW_THIS_AGAIN_MSG = "DontShowThisAgain.msg"; //$NON-NLS-1$
	private final static String STARTING_INSTANCE_TITLE = "StartingInstance.title"; //$NON-NLS-1$

	protected NewInstancePage mainPage;
	protected INewInstanceWizardPage[] additionalPages;
	protected DeltaCloudInstance instance;
	/**
	 * Initial image, may be null
	 */
	private String imageId;

	public NewInstanceWizard(DeltaCloud cloud) {
		super(cloud);
	}

	public NewInstanceWizard(DeltaCloud cloud, String imageId) {
		this(cloud);
		this.imageId = imageId;
	}

	@Override
	public void addPages() {
		setWindowTitle(WizardMessages.getString("NewInstance.title"));
		mainPage = new NewInstancePage(getDeltaCloud(), imageId);
		addPage(mainPage);
		additionalPages = DeltacloudUIExtensionManager.getDefault().loadNewInstanceWizardPages();
		for (int i = 0; i < additionalPages.length; i++) {
			addPage(additionalPages[i]);
		}
	}

	@Override
	public boolean performFinish() {
		NewInstancePageModel model = mainPage.getModel();

		DeltaCloudImage image = model.getImage();
		Assert.isTrue(image != null);
		String imageId = image.getId();

		String profileId = model.getProfileId();
		String realmId = model.getRealmId();
		String memory = model.getMemory();
		String storage = model.getStorage();
		String keyId = model.getKeyId();
		String name = model.getName();

		// Save persistent settings for this particular cloud
		DeltaCloud cloud = getDeltaCloud();
		cloud.setLastImageId(imageId);
		cloud.setLastKeyname(keyId);

		boolean result = false;
		Exception e = null;
		try {
			if (UIUtils.openConfirmationDialog(
					WizardMessages.getString(CONFIRM_CREATE_TITLE), WizardMessages.getString(CONFIRM_CREATE_MSG),
					WizardMessages.getString(DONT_SHOW_THIS_AGAIN_MSG),
					IDeltaCloudPreferenceConstants.DONT_CONFIRM_CREATE_INSTANCE, Activator.PLUGIN_ID, getShell())) {

				instance = cloud.createInstance(name, imageId, realmId, profileId, keyId, memory, storage);
				if (instance != null) {
					result = true;
					if (instance.getState().equals(DeltaCloudInstance.State.PENDING)) {
						scheduleJobs(instance);
					}
				}
			}
		} catch (DeltaCloudException ex) {
			e = ex;
		}
		if (!result) {
			ErrorUtils.handleError(
					WizardMessages.getString(CREATE_INSTANCE_FAILURE_TITLE),
					WizardMessages.getFormattedString(CREATE_INSTANCE_FAILURE_MSG,
							new String[] { name, imageId, realmId, profileId }),
					e, getShell());
		}

		return result;
	}

	private void scheduleJobs(DeltaCloudInstance instance) {
		ChainedJob first =
				new InstanceStateJob(
						WizardMessages.getFormattedString(STARTING_INSTANCE_TITLE, instance.getAlias()),
						instance,
						DeltaCloudInstance.State.RUNNING);
		first.setUser(true);
		ChainedJob last = first;
		ChainedJob temp;
		for (int i = 0; i < additionalPages.length; i++) {
			temp = additionalPages[i].createPerformFinishJob(instance);
			if (temp != null) {
				last.setNextJob(temp);
				last = temp;
			}
		}
		first.schedule();
	}
}