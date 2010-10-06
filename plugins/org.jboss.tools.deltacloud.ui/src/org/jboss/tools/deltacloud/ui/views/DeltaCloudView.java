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
package org.jboss.tools.deltacloud.ui.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.jboss.tools.deltacloud.core.DeltaCloud;
import org.jboss.tools.deltacloud.core.DeltaCloudImage;
import org.jboss.tools.deltacloud.core.DeltaCloudInstance;
import org.jboss.tools.deltacloud.core.DeltaCloudManager;
import org.jboss.tools.deltacloud.core.ICloudManagerListener;
import org.jboss.tools.deltacloud.ui.SWTImagesFactory;
import org.jboss.tools.internal.deltacloud.ui.wizards.EditCloudConnection;
import org.jboss.tools.internal.deltacloud.ui.wizards.ImageFilter;
import org.jboss.tools.internal.deltacloud.ui.wizards.NewInstance;


public class DeltaCloudView extends ViewPart implements ICloudManagerListener, 
ITabbedPropertySheetPageContributor {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.jboss.tools.deltacloud.ui.views.DeltaCloudView";
	
	private static final String REMOVE_CLOUD = "RemoveCloud.label"; //$NON-NLS-1$
	private static final String EDIT_CLOUD = "EditCloud.label"; //$NON-NLS-1$
	private static final String REFRESH = "Refresh.label"; //$NON-NLS-1$
	private static final String CREATE_INSTANCE = "CreateInstance.label"; //$NON-NLS-1$
	private static final String CONFIRM_CLOUD_DELETE_TITLE = "ConfirmCloudDelete.title"; //$NON-NLS-1$
	private static final String CONFIRM_CLOUD_DELETE_MSG = "ConfirmCloudDelete.msg"; //$NON-NLS-1$
	private final static String START_LABEL = "Start.label"; //$NON-NLS-1$
	private final static String STOP_LABEL = "Stop.label"; //$NON-NLS-1$
	private final static String REBOOT_LABEL = "Reboot.label"; //$NON-NLS-1$
	private final static String DESTROY_LABEL = "Destroy.label"; //$NON-NLS-1$
	private final static String STARTING_INSTANCE_TITLE = "StartingInstance.title"; //$NON-NLS-1$
	private final static String STARTING_INSTANCE_MSG = "StartingInstance.msg"; //$NON-NLS-1$
	private final static String STOPPING_INSTANCE_TITLE = "StoppingInstance.title"; //$NON-NLS-1$
	private final static String STOPPING_INSTANCE_MSG = "StoppingInstance.msg"; //$NON-NLS-1$
	private final static String REBOOTING_INSTANCE_TITLE = "RebootingInstance.title"; //$NON-NLS-1$
	private final static String REBOOTING_INSTANCE_MSG = "RebootingInstance.msg"; //$NON-NLS-1$
	private final static String DESTROYING_INSTANCE_TITLE = "DestroyingInstance.title"; //$NON-NLS-1$
	private final static String DESTROYING_INSTANCE_MSG = "DestroyingInstance.msg"; //$NON-NLS-1$
	private final static String IMAGE_FILTER = "ImageFilter.label"; //$NON-NLS-1$
	
	public static final String COLLAPSE_ALL = "CollapseAll.label"; //$NON-NLS-1$

	private TreeViewer viewer;
	private Action removeCloud;
	private Action refreshAction;
	private Action startAction;
	private Action stopAction;
	private Action rebootAction;
	private Action destroyAction;
	private Action collapseall;
	private Action doubleClickAction;
	private Action createInstance;
	private Action editCloud;
	private Action imageFilterAction;
	
	private Map<String, Action> instanceActions;
	
	private CloudViewElement selectedElement;

	/**
	 * The constructor.
	 */
	public DeltaCloudView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new CloudViewContentProvider());
		viewer.setLabelProvider(new CloudViewLabelProvider());
		viewer.setInput(new CVRootElement(viewer));
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		getSite().setSelectionProvider(viewer); // for tabbed properties

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "org.jboss.tools.deltacloud.ui.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		hookSelection();
		contributeToActionBars();
		DeltaCloudManager.getDefault().addCloudManagerListener(this);
	}

	@Override
	public void dispose() {
		DeltaCloudManager.getDefault().removeCloudManagerListener(this);
		super.dispose();
	}
	
	private void hookSelection() {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelection();
			}
		});
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DeltaCloudView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		IMenuManager menuMgr = bars.getMenuManager();
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DeltaCloudView.this.fillLocalPullDown(manager);
			}
		});
		fillLocalPullDown(menuMgr);
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void handleSelection() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		selectedElement = (CloudViewElement)selection.getFirstElement();
		editCloud.setEnabled(selectedElement != null);
		removeCloud.setEnabled(selectedElement != null);
		refreshAction.setEnabled(selectedElement != null);
		imageFilterAction.setEnabled(selectedElement != null);
	}
	
	private void fillLocalPullDown(IMenuManager manager) {
		manager.removeAll();
		manager.add(editCloud);
		manager.add(removeCloud);
		manager.add(refreshAction);
		manager.add(imageFilterAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		if (selectedElement instanceof CVImageElement) {
			manager.add(createInstance);
		} else if (selectedElement instanceof CVInstanceElement) {
			CVInstanceElement element = (CVInstanceElement)selectedElement;
			DeltaCloudInstance instance = (DeltaCloudInstance)element.getElement();
			List<String> actions = instance.getActions();
			for (String action : actions) {
				manager.add(instanceActions.get(action));
			}
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
		manager.add(editCloud);
		manager.add(removeCloud);
		manager.add(imageFilterAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(collapseall);
	}

	private void makeActions() {
		removeCloud = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				CloudViewElement element = (CloudViewElement)selection.getFirstElement();
				while (element != null && !(element instanceof CVCloudElement)) {
					element = (CloudViewElement)element.getParent();
				}
				if (element != null) {
					CVCloudElement cve = (CVCloudElement)element;
					Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
					boolean confirmed = MessageDialog.openConfirm(shell, 
							CVMessages.getString(CONFIRM_CLOUD_DELETE_TITLE),
							CVMessages.getFormattedString(CONFIRM_CLOUD_DELETE_MSG, cve.getName()));
					if (confirmed) {
						DeltaCloudManager.getDefault().removeCloud((DeltaCloud)element.getElement());
						CloudViewContentProvider p = (CloudViewContentProvider)viewer.getContentProvider();
						Object[] elements = p.getElements(getViewSite());
						int index = -1;
						for (int i = 0; i < elements.length; ++i) {
							if (elements[i] == cve)
								index = i;
						}
						if (index >= 0)
							((TreeViewer)cve.getViewer()).remove(getViewSite(), index);
					}
				}
			}
		};
		removeCloud.setText(CVMessages.getString(REMOVE_CLOUD));
		removeCloud.setToolTipText(CVMessages.getString(REMOVE_CLOUD));
		removeCloud.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));

		createInstance = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Shell shell = viewer.getControl().getShell();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof CVImageElement) {
					CVImageElement imageElement = (CVImageElement)obj;
					DeltaCloudImage image = (DeltaCloudImage)imageElement.getElement();
					CVCategoryElement images = (CVCategoryElement)imageElement.getParent();
					CVCloudElement cloudElement = (CVCloudElement)images.getParent();
					DeltaCloud cloud = (DeltaCloud)cloudElement.getElement();
					IWizard wizard = new NewInstance(cloud, image);
					WizardDialog dialog = new WizardDialog(shell, wizard);
					dialog.create();
					dialog.open();
				}
			}
		};		
		createInstance.setText(CVMessages.getString(CREATE_INSTANCE));
		createInstance.setToolTipText(CVMessages.getString(CREATE_INSTANCE));
		createInstance.setImageDescriptor(SWTImagesFactory.DESC_INSTANCE);
		
		editCloud = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				CloudViewElement element = (CloudViewElement)selection.getFirstElement();
				while (element != null && !(element instanceof CVCloudElement)) {
					element = (CloudViewElement)element.getParent();
				}
				if (element != null) {
					CVCloudElement cloudElement = (CVCloudElement)element;
					DeltaCloud cloud = (DeltaCloud)cloudElement.getElement();
					IWizard wizard = new EditCloudConnection(cloud);
					Shell shell = viewer.getControl().getShell();
					WizardDialog dialog = new WizardDialog(shell, wizard);
					dialog.create();
					dialog.open();
				}
			}
		};		
		editCloud.setText(CVMessages.getString(EDIT_CLOUD));
		editCloud.setToolTipText(CVMessages.getString(EDIT_CLOUD));
		
		refreshAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof CloudViewElement) {
					CloudViewElement element = (CloudViewElement)obj;
					while (!(element instanceof CVCloudElement))
						element = (CloudViewElement)element.getParent();
					CVCloudElement cloud = (CVCloudElement)element;
					cloud.loadChildren();
				}
			}
		};
		refreshAction.setText(CVMessages.getString(REFRESH));
		refreshAction.setToolTipText(CVMessages.getString(REFRESH));
		refreshAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
	
		startAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof CVInstanceElement) {
					CVInstanceElement cvinstance = (CVInstanceElement)obj;
					DeltaCloudInstance instance = (DeltaCloudInstance)cvinstance.getElement();
					CloudViewElement element = (CloudViewElement)obj;
					while (!(element instanceof CVCloudElement))
						element = (CloudViewElement)element.getParent();
					CVCloudElement cvcloud = (CVCloudElement)element;
					DeltaCloud cloud = (DeltaCloud)cvcloud.getElement();
					PerformInstanceActionThread t = new PerformInstanceActionThread(cloud, instance, DeltaCloudInstance.START,
							CVMessages.getString(STARTING_INSTANCE_TITLE), 
							CVMessages.getFormattedString(STARTING_INSTANCE_MSG, new String[]{instance.getName()}),
							DeltaCloudInstance.RUNNING);
					t.setUser(true);
					t.schedule();
				}
			}
		};
		startAction.setText(CVMessages.getString(START_LABEL));
		startAction.setToolTipText(CVMessages.getString(START_LABEL));
		startAction.setImageDescriptor(SWTImagesFactory.DESC_START);
		startAction.setDisabledImageDescriptor(SWTImagesFactory.DESC_STARTD);
		
		stopAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof CVInstanceElement) {
					CVInstanceElement cvinstance = (CVInstanceElement)obj;
					DeltaCloudInstance instance = (DeltaCloudInstance)cvinstance.getElement();
					CloudViewElement element = (CloudViewElement)obj;
					while (!(element instanceof CVCloudElement))
						element = (CloudViewElement)element.getParent();
					CVCloudElement cvcloud = (CVCloudElement)element;
					DeltaCloud cloud = (DeltaCloud)cvcloud.getElement();
					PerformInstanceActionThread t = new PerformInstanceActionThread(cloud, instance, DeltaCloudInstance.STOP,
							CVMessages.getString(STOPPING_INSTANCE_TITLE), 
							CVMessages.getFormattedString(STOPPING_INSTANCE_MSG, new String[]{instance.getName()}),
							DeltaCloudInstance.STOPPED);
					t.setUser(true);
					t.schedule();
				}
			}
		};
		stopAction.setText(CVMessages.getString(STOP_LABEL));
		stopAction.setToolTipText(CVMessages.getString(STOP_LABEL));
		stopAction.setImageDescriptor(SWTImagesFactory.DESC_STOP);
		stopAction.setDisabledImageDescriptor(SWTImagesFactory.DESC_STOPD);
		
		rebootAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof CVInstanceElement) {
					CVInstanceElement cvinstance = (CVInstanceElement)obj;
					DeltaCloudInstance instance = (DeltaCloudInstance)cvinstance.getElement();
					CloudViewElement element = (CloudViewElement)obj;
					while (!(element instanceof CVCloudElement))
						element = (CloudViewElement)element.getParent();
					CVCloudElement cvcloud = (CVCloudElement)element;
					DeltaCloud cloud = (DeltaCloud)cvcloud.getElement();
					PerformInstanceActionThread t = new PerformInstanceActionThread(cloud, instance, DeltaCloudInstance.REBOOT,
							CVMessages.getString(REBOOTING_INSTANCE_TITLE), 
							CVMessages.getFormattedString(REBOOTING_INSTANCE_MSG, new String[]{instance.getName()}),
							DeltaCloudInstance.RUNNING);
					t.setUser(true);
					t.schedule();
				}
			}
		};
		rebootAction.setText(CVMessages.getString(REBOOT_LABEL));
		rebootAction.setToolTipText(CVMessages.getString(REBOOT_LABEL));
		rebootAction.setImageDescriptor(SWTImagesFactory.DESC_REBOOT);
		rebootAction.setDisabledImageDescriptor(SWTImagesFactory.DESC_REBOOTD);
		
		destroyAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof CVInstanceElement) {
					CVInstanceElement cvinstance = (CVInstanceElement)obj;
					DeltaCloudInstance instance = (DeltaCloudInstance)cvinstance.getElement();
					CloudViewElement element = (CloudViewElement)obj;
					while (!(element instanceof CVCloudElement))
						element = (CloudViewElement)element.getParent();
					CVCloudElement cvcloud = (CVCloudElement)element;
					DeltaCloud cloud = (DeltaCloud)cvcloud.getElement();
					PerformDestroyInstanceActionThread t = new PerformDestroyInstanceActionThread(cloud, instance,
							CVMessages.getString(DESTROYING_INSTANCE_TITLE), 
							CVMessages.getFormattedString(DESTROYING_INSTANCE_MSG, new String[]{instance.getName()}));
					t.setUser(true);
					t.schedule();
				}
			}
		};
		destroyAction.setText(CVMessages.getString(DESTROY_LABEL));
		destroyAction.setToolTipText(CVMessages.getString(DESTROY_LABEL));
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		ImageDescriptor delete = ImageDescriptor.createFromImage(sharedImages.getImage(ISharedImages.IMG_ETOOL_DELETE));
		ImageDescriptor delete_disabled = ImageDescriptor.createFromImage(sharedImages.getImage(ISharedImages.IMG_ETOOL_DELETE_DISABLED));
		destroyAction.setImageDescriptor(delete);
		destroyAction.setDisabledImageDescriptor(delete_disabled);

		instanceActions = new HashMap<String, Action>();
		instanceActions.put(DeltaCloudInstance.START, startAction);
		instanceActions.put(DeltaCloudInstance.STOP, stopAction);
		instanceActions.put(DeltaCloudInstance.REBOOT, rebootAction);
		instanceActions.put(DeltaCloudInstance.DESTROY, destroyAction);
		
		imageFilterAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				CloudViewElement element = (CloudViewElement)selection.getFirstElement();
				while (element != null && !(element instanceof CVCloudElement)) {
					element = (CloudViewElement)element.getParent();
				}
				if (element != null) {
					CVCloudElement cve = (CVCloudElement)element;
					final DeltaCloud cloud = (DeltaCloud)cve.getElement();
					Display.getDefault().asyncExec(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							Shell shell = viewer.getControl().getShell();
							IWizard wizard = new ImageFilter(cloud);
							WizardDialog dialog = new WizardDialog(shell, wizard);
							dialog.create();
							dialog.open();
						}

					});
				}
			}
		};
		imageFilterAction.setText(CVMessages.getString(IMAGE_FILTER));
		imageFilterAction.setToolTipText(CVMessages.getString(IMAGE_FILTER));
		
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				@SuppressWarnings("unused")
				Object obj = ((IStructuredSelection)selection).getFirstElement();
			}
		};
		collapseall = new Action() {
			public void run() {
				viewer.collapseAll();
			}
		};
		collapseall.setText(CVMessages.getString(COLLAPSE_ALL));
		collapseall.setToolTipText(CVMessages.getString(COLLAPSE_ALL));
		collapseall.setImageDescriptor(SWTImagesFactory.DESC_COLLAPSE_ALL);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void changeEvent(int type) {
		viewer.setInput(new CVRootElement(viewer));
	}

	@Override
	public String getContributorId() {
        return getSite().getId();
	}
	
    @SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
        if (adapter == IPropertySheetPage.class)
        	// If Tabbed view is desired, then change the
        	// following to new TabbedPropertySheetPage(this)
            return new CVPropertySheetPage();
        return super.getAdapter(adapter);
    }
}