package org.jboss.tools.deltacloud.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jboss.tools.deltacloud.core.DeltaCloud;
import org.jboss.tools.deltacloud.core.DeltaCloudImage;
import org.jboss.tools.deltacloud.core.DeltaCloudManager;
import org.jboss.tools.deltacloud.core.ICloudManagerListener;
import org.jboss.tools.deltacloud.core.IImageListListener;
import org.jboss.tools.internal.deltacloud.ui.wizards.NewInstance;

public class ImageView extends ViewPart implements ICloudManagerListener, IImageListListener {

	private final static String CLOUD_SELECTOR_LABEL = "CloudSelector.label"; //$NON-NLS-1$
	private final static String LAUNCH_INSTANCE = "CreateInstance.label"; //$NON-NLS-1$
	
	private TableViewer viewer;
	private Composite container;
	private Combo cloudSelector;
	@SuppressWarnings("unused")
	private DeltaCloudImage selectedElement;
	
	private DeltaCloud[] clouds;
	private DeltaCloud currCloud;
	
	private ImageViewLabelAndContentProvider contentProvider;
	
	private Action doubleClickAction;
	private Action launchAction;
	
	private ImageView parentView;

	public ImageView() {
		parentView = this;
	}

	private ModifyListener cloudModifyListener = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent e) {
			int index = cloudSelector.getSelectionIndex();
			currCloud = clouds[index];
			Display.getCurrent().asyncExec(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					currCloud.removeImageListListener(parentView);
					viewer.setInput(currCloud);
					currCloud.addImageListListener(parentView);
					viewer.refresh();
					
				}
				
			});
		}
		
	};
	
	private class ColumnListener extends SelectionAdapter {
		
		private int column;
		private TableViewer viewer;
		
		public ColumnListener(int column, TableViewer viewer) {
			this.column = column;
			this.viewer = viewer;
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			ImageComparator comparator = (ImageComparator)viewer.getComparator();
			Table t = viewer.getTable();
			if (comparator.getColumn() == column) {
				comparator.reverseDirection();
			}
			comparator.setColumn(column);
			TableColumn tc = (TableColumn)e.getSource();
			t.setSortColumn(tc);
			t.setSortDirection(SWT.NONE);
			viewer.refresh();
		}
	
	};

	@Override
	public void dispose() {
		for (DeltaCloud cloud : clouds) {
			cloud.removeImageListListener(this);
		}
		DeltaCloudManager.getDefault().removeCloudManagerListener(this);
		super.dispose();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		FormLayout layout = new FormLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		
		Label cloudSelectorLabel = new Label(container, SWT.NULL);
		cloudSelectorLabel.setText(CVMessages.getString(CLOUD_SELECTOR_LABEL));
		
		cloudSelector = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		initializeCloudSelector();
		cloudSelector.addModifyListener(cloudModifyListener);
		// Following is a kludge so that on Linux the Combo is read-only but
		// has a white background.
		cloudSelector.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = false;
			}
		});
		
		Composite tableArea = new Composite(container, SWT.NULL);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableArea.setLayout(tableLayout);
		
		viewer = new TableViewer(tableArea, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		contentProvider = new ImageViewLabelAndContentProvider();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(contentProvider);
		ImageComparator comparator = new ImageComparator(0);
		viewer.setComparator(comparator);
		
		for (int i = 0; i < ImageViewLabelAndContentProvider.Column.getSize(); ++i) {
			ImageViewLabelAndContentProvider.Column c = 
				ImageViewLabelAndContentProvider.Column.getColumn(i);
			TableColumn tc = new TableColumn(table, SWT.NONE);
			if (i == 0)
				table.setSortColumn(tc);
			tc.setText(CVMessages.getString(c.name()));
			tableLayout.setColumnData(tc, new ColumnWeightData(c.getWeight(), true));
			tc.addSelectionListener(new ColumnListener(i, viewer));
		}
		table.setSortDirection(SWT.NONE);
		
		if (clouds.length > 0) {
			currCloud = clouds[0];
			currCloud.removeImageListListener(parentView);
			viewer.setInput(clouds[0]);
			currCloud.addImageListListener(parentView);
		}

		FormData f = new FormData();
		f.top = new FormAttachment(0, 8);
		f.left = new FormAttachment(0, 30);
		cloudSelectorLabel.setLayoutData(f);
		
		f = new FormData();
		f.top = new FormAttachment(0, 5);
		f.left = new FormAttachment(cloudSelectorLabel, 5);
		cloudSelector.setLayoutData(f);

		f = new FormData();
		f.top = new FormAttachment(cloudSelector, 8);
		f.left = new FormAttachment(0, 0);
		f.right = new FormAttachment(100, 0);
		f.bottom = new FormAttachment(100, 0);
		tableArea.setLayoutData(f);
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "org.jboss.tools.deltacloud.ui.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		hookSelection();
		contributeToActionBars();
		
		DeltaCloudManager.getDefault().addCloudManagerListener(this);
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
				ImageView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void handleSelection() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		selectedElement = (DeltaCloudImage)selection.getFirstElement();
	}
	
	private void fillLocalPullDown(IMenuManager manager) {
		//TODO
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(launchAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		//TODO
	}

	private void makeActions() {
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};

		launchAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				final DeltaCloudImage image = (DeltaCloudImage)((IStructuredSelection)selection).getFirstElement();
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Shell shell = viewer.getControl().getShell();
						IWizard wizard = new NewInstance(currCloud, image);
						WizardDialog dialog = new WizardDialog(shell, wizard);
						dialog.create();
						dialog.open();
						
					}
					
				});
			}
		};
		launchAction.setText(CVMessages.getString(LAUNCH_INSTANCE));
		launchAction.setToolTipText(CVMessages.getString(LAUNCH_INSTANCE));
	}
	
	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			CVMessages.getString("CloudViewName"), //$NON-NLS-1$
			message);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	private void initializeCloudSelector() {
		clouds = DeltaCloudManager.getDefault().getClouds();
		String[] cloudNames = new String[clouds.length];
		for (int i = 0; i < clouds.length; ++i) {
			cloudNames[i] = clouds[i].getName();
		}
		cloudSelector.setItems(cloudNames);
		if (clouds.length > 0) {
			cloudSelector.setText(cloudNames[0]);
			currCloud = clouds[0];
		}
	}
	
	public void changeEvent(int type) {
		String currName = null;
		clouds = DeltaCloudManager.getDefault().getClouds();
		if (currCloud != null) {
			currName = currCloud.getName();
		}
		String[] cloudNames = new String[clouds.length];
		int index = 0;
		for (int i = 0; i < clouds.length; ++i) {
			cloudNames[i] = clouds[i].getName();
			if (cloudNames[i].equals(currName))
				index = i;
		}
		cloudSelector.removeModifyListener(cloudModifyListener);
		cloudSelector.setItems(cloudNames);
		if (cloudNames.length > 0) {
			cloudSelector.setText(cloudNames[index]);
			currCloud = clouds[index];
			viewer.setInput(currCloud);
		} else {
			currCloud = null;
			cloudSelector.setText("");
			viewer.setInput(new DeltaCloudImage[0]);
		}
		cloudSelector.addModifyListener(cloudModifyListener);
	}

	public void listChanged(DeltaCloudImage[] list) {
		currCloud.removeImageListListener(parentView);
		viewer.setInput(list);
		currCloud.addImageListListener(parentView);
		viewer.refresh();
	}

}