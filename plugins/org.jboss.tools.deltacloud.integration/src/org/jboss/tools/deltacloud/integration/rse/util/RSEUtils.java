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
package org.jboss.tools.deltacloud.integration.rse.util;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.IRSECoreRegistry;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.model.SystemStartHere;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.jboss.tools.deltacloud.core.DeltaCloudInstance;
import org.jboss.tools.deltacloud.integration.DeltaCloudIntegrationPlugin;
import org.jboss.tools.deltacloud.integration.Messages;
import org.jboss.tools.internal.deltacloud.ui.utils.UIUtils;

/**
 * @author André Dietisheim
 */
public class RSEUtils {

	private static final String VIEW_REMOTESYSEXPLORER_ID = "org.eclipse.rse.ui.view.systemView";

	public static IRSESystemType getSSHOnlySystemType() {
		IRSESystemType sshType = null;
		RSECorePlugin rsep = RSECorePlugin.getDefault();
		IRSECoreRegistry coreRegistry = rsep.getCoreRegistry();
		IRSESystemType[] sysTypes = coreRegistry.getSystemTypes();
		for (IRSESystemType sysType : sysTypes) {
			if (sysType.getId().equals(IRSESystemType.SYSTEMTYPE_SSH_ONLY_ID))
				sshType = sysType;
		}
		Assert.isTrue(sshType != null,
				"Remote System Explorer could not initialize SSH subsystem: ssh type not found");
		return sshType;
	}

	public static ISystemRegistry getSystemRegistry() {
		return SystemStartHere.getSystemRegistry();
	}

	public static String createConnectionName(DeltaCloudInstance instance) {
		Assert.isLegal(instance != null, "Cannot create connection name: instance is not defined");
		return instance.getName() + " [" + instance.getId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String createHostName(DeltaCloudInstance instance) {
		Assert.isLegal(instance != null, "Cannot get hostname: instance is not defined");

		String hostName = instance.getHostName();
		System.out.println(instance.getHostName());
		Assert.isTrue(hostName != null && hostName.length() > 0,
				MessageFormat.format("Cannot get host name: not defined for instance {0}", instance.getName()));
		return hostName;
	}

	public static IHost createHost(String connectionName, String hostname, IRSESystemType systemType,
			ISystemRegistry systemRegistry) throws Exception {
		// TODO: Internationalize strings
		Assert.isLegal(connectionName != null && connectionName.length() > 0,
				"Cannot create Host: connectionName is not defined");
		Assert.isLegal(hostname != null && hostname.length() > 0, "Cannot create Host: hostname is not defined");
		Assert.isLegal(systemType != null, "Cannot create Host: system type is not defined");
		Assert.isLegal(systemRegistry != null, "Cannot create Host: system registry is not defined");

		IHost host = systemRegistry.createHost(systemType, connectionName, hostname, null);
		host.setDefaultUserId("root"); //$NON-NLS-1$
		return host;
	}

	public static IConnectorService getConnectorService(IHost host) {
		IConnectorService[] services = host.getConnectorServices();
		if (services == null || services.length <= 0) {
			return null;
		}
		return services[0];
	}

	public static Job connect(String connectionName, final IConnectorService service)
			throws Exception {
		// TODO: internationalize strings
		Assert.isLegal(connectionName != null,
				"Remote System Explorer could not connect: connection name is not defined");
		Assert.isLegal(service != null, "Remote System Explorer could not connect: connector service not found.");

		Job job = new Job(NLS.bind(Messages.RSE_CONNECTING_MESSAGE, connectionName)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.worked(1);
					service.connect(monitor);
					monitor.done();
					return Status.OK_STATUS;
				} catch (Exception e) {
					// odd behavior: service reports connection failure even if things seem to work (view opens up with connection in it)
					// ignore errors since things work
					//
					// return StatusFactory.getInstance(IStatus.ERROR,
					// Activator.PLUGIN_ID, e.getMessage(), e);
					return Status.OK_STATUS;
				}
			}
		};
		job.setUser(true);
		job.schedule();
		return job;
	}

	public static void showRemoteSystemExplorer(Job job) {
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				super.done(event);
				showRemoteSystemExplorer();
			}
		});
	}

	public static void showRemoteSystemExplorer() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				try {
					UIUtils.showView(VIEW_REMOTESYSEXPLORER_ID);
				} catch (PartInitException e) {
					// I have no idea wtf is wrong here
					// but my dev environment will not let me use common classes
//					IStatus status = StatusFactory.getInstance(IStatus.ERROR, 
//							DeltaCloudIntegrationPlugin.PLUGIN_ID, e.getMessage(), e);
					Status status = new Status(IStatus.ERROR, DeltaCloudIntegrationPlugin.PLUGIN_ID, e.getMessage(), e);
					ErrorDialog.openError(UIUtils.getActiveShell(),
								Messages.ERROR,
								Messages.COULD_NOT_LAUNCH_RSE_EXPLORER,
							status);
				}
			}
		});
	}
}