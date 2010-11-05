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
package org.jboss.tools.deltacloud.core.client;

public class DeltaCloudNotFoundClientException extends DeltaCloudClientException {

	private static final long serialVersionUID = 1L;
	
	public DeltaCloudNotFoundClientException(String message, Throwable clause)
	{
		super(message, clause);
	}
	
	public DeltaCloudNotFoundClientException(Throwable clause)
	{
		super(clause);
	}
	
	public DeltaCloudNotFoundClientException(String message)
	{
		super(message);
	}
	
}