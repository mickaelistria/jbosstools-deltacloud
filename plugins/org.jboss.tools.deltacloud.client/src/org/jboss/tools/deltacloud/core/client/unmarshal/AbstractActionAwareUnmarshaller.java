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

package org.jboss.tools.deltacloud.core.client.unmarshal;

import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.deltacloud.core.client.Action;
import org.jboss.tools.deltacloud.core.client.DeltaCloudClientException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author André Dietisheim
 *
 * @param <DELTACLOUDOBJECT>
 */
public abstract class AbstractActionAwareUnmarshaller<DELTACLOUDOBJECT> extends AbstractDOMUnmarshaller<DELTACLOUDOBJECT>{

	private String actionElementName;
	public AbstractActionAwareUnmarshaller(String tagName, Class<DELTACLOUDOBJECT> type, String actionElementName) {
		super(tagName, type);
		this.actionElementName = actionElementName;
	}

	protected List<Action<DELTACLOUDOBJECT>> getActions(Element element, DELTACLOUDOBJECT owner) throws DeltaCloudClientException {
		if (element == null) {
			return null;
		}
		List<Action<DELTACLOUDOBJECT>> actions = new ArrayList<Action<DELTACLOUDOBJECT>>();
		NodeList nodeList = element.getElementsByTagName(actionElementName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node linkNode = nodeList.item(i);
			Action<DELTACLOUDOBJECT> action = createAction(linkNode);
			if (action != null) {
				action.setOwner(owner);
				actions.add(action);
			}
		}
		return actions;
	}

	protected Action<DELTACLOUDOBJECT> createAction(Node node) throws DeltaCloudClientException {
		if (!(node instanceof Element)) {
			return null;
		}
		return unmarshallAction((Element) node);
	}
	
	protected abstract Action<DELTACLOUDOBJECT> unmarshallAction(Element element) throws DeltaCloudClientException;
}