package org.alfresco.reporting.web.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.executer.ScriptActionExecuter;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExecuteScriptHandlerBean {
	private static final String PARAM_ID = "id";

	private static final String PARAM_SCRIPT_REF = "script";

	private static final String PARAM_LABEL_ID = "label-id";

	private static final String PARAM_ACTION_LOCATION = "actionLocation";

	private static Log logger = LogFactory.getLog(ExecuteScriptHandlerBean.class);

	private BrowseBean browseBean;

	private NodeService nodeService;

	private SearchService searchService;

	private NamespaceService namespaceService;

	private ActionService actionService;

	public void execute(ActionEvent event) {

		UIActionLink link = (UIActionLink) event.getComponent();
		Map<String, String> params = link.getParameterMap();

		String id = params.get(PARAM_ID);

		if (id == null || id.equals(""))
			throw new AlfrescoRuntimeException(
					"Required parameter 'id' is null or empty");

		String scriptName = params.get(PARAM_SCRIPT_REF);

		if (scriptName == null || scriptName.equals(""))
			throw new AlfrescoRuntimeException(
					"Required parameter 'script' is null or empty");

		String labelId = params.get(PARAM_LABEL_ID);

		if (labelId == null || labelId.equals(""))
			throw new AlfrescoRuntimeException(
					"Required parameter 'label-id' is null or empty");

		FacesContext context = FacesContext.getCurrentInstance();

		NodeRef actionedUponNodeRef = new NodeRef(Repository.getStoreRef(), id);

		NodeRef parent = nodeService.getPrimaryParent(actionedUponNodeRef)
				.getParentRef();

		if (nodeService.exists(actionedUponNodeRef)) {

			String xpath = Application.getRootPath(context)
					+ "/app:dictionary/app:scripts/cm:reporting/cm:" + scriptName;
			NodeRef rootNodeRef = nodeService.getRootNode(Repository
					.getStoreRef());
			List<NodeRef> nodes = searchService.selectNodes(rootNodeRef, xpath,
					null, namespaceService, false);

			if (nodes == null || nodes.size() == 0)
				throw new AlfrescoRuntimeException(
						"Unable to locate script with name: " + scriptName);

			NodeRef scriptRef = nodes.get(0);

			if (logger.isDebugEnabled())
				logger.debug("Script found: " + scriptRef);

			Map<String, Object> model = new HashMap<String, Object>(2);
			model.put("document", new ScriptNode(actionedUponNodeRef, Repository
					.getServiceRegistry(context)));
			model.put("space", new ScriptNode(parent, Repository
					.getServiceRegistry(context)));

			Action action = actionService
					.createAction(ScriptActionExecuter.NAME);
			action.setParameterValue(ScriptActionExecuter.PARAM_SCRIPTREF,
					scriptRef);
			actionService.executeAction(action, actionedUponNodeRef);

			BrowseBean browseBean = (BrowseBean) FacesHelper.getManagedBean(
					context, "BrowseBean");
			String actionLocation = params.get(PARAM_ACTION_LOCATION);

			if (actionLocation.equals("document-details") == true) {
				browseBean.getDocument().reset();
				UIComponent comp = context.getViewRoot().findComponent(
						"dialog:dialog-body:document-props");
				comp.getChildren().clear();
			} else if (actionLocation.equals("folder-details") == true) {
				if (browseBean.getActionSpace() != null)
					browseBean.getActionSpace().reset();
				UIComponent comp = context.getViewRoot().findComponent(
						"dialog:dialog-body:space-props");
				if (comp != null && comp.getChildren() != null)
					comp.getChildren().clear();
			} else if (actionLocation.equals("folder-browse") == true) {
				if (nodeService.exists(parent))
					browseBean.clickSpace(parent);
			}
		}

		String msg = Application.getMessage(context, labelId);

		FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
		context.addMessage(msg, fm);

	}



	public BrowseBean getBrowseBean() {
		return browseBean;
	}

	public void setBrowseBean(BrowseBean browseBean) {
		this.browseBean = browseBean;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public NamespaceService getNamespaceService() {
		return namespaceService;
	}

	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	public ActionService getActionService() {
		return actionService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

}
