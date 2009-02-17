package org.sakaiproject.sitestats.api.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.sitestats.api.parser.EventParserTip;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.ResourceLoader;


public class ToolInfo implements Serializable {
	private static final long			serialVersionUID	= 1L;
	private transient Log				LOG					= LogFactory.getLog(ToolInfo.class);
	private String						toolId;
	private List<String>				additionalToolIds;
	private String						toolName;
	private List<EventInfo>				eventInfos;
	private boolean						selected;
	private EventParserTip				eventParserTip;
	
	public ToolInfo(String toolId) {
		this.toolId = toolId;
		eventInfos = new ArrayList<EventInfo>();
	}
	public ToolInfo(String toolId, List<String> additionalToolIds) {
		this.toolId = toolId;
		this.additionalToolIds = additionalToolIds;
		eventInfos = new ArrayList<EventInfo>();
	}

	public List<EventInfo> getEvents() {
		return eventInfos;
	}

	public void setEvents(List<EventInfo> eventInfos) {
		this.eventInfos = eventInfos;
	}
	
	public void addEvent(EventInfo eventInfo){
		eventInfos.add(eventInfo);
	}
	
	public void removeEvent(EventInfo eventInfo) {
		eventInfos.remove(eventInfo);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getToolId() {
		return toolId;
	}

	public void setToolId(String toolId) {
		this.toolId = toolId;
	}

	public List<String> getAdditionalToolIds() {
		return additionalToolIds;
	}
	
	public void setAdditionalToolIds(List<String> ids) {
		this.additionalToolIds = ids;
	}

	public void setAdditionalToolIdsStr(String ids) {
		if(ids != null) {
			this.additionalToolIds = new ArrayList<String>();
			String[] _ids = ids.split(",");
			for(int i=0; i<_ids.length; i++)
				this.additionalToolIds.add(_ids[i].trim());
		}
	}

	public String getToolName() {
		ToolManager M_tm = (ToolManager) ComponentManager.get(ToolManager.class.getName());
		// main tool id
		try{
			toolName = M_tm.getTool(toolId).getTitle();
		}catch(Exception e){
			ResourceLoader msgs = new ResourceLoader("Events");
			try{
				LOG.info("No sakai tool found for toolId: " + toolId
						+ " (tool undeployed?). Using bundle (if supplied) in sitestats/sitestats-impl/impl/src/bundle/org/sakaiproject/sitestats/impl/bundle/ for tool name.");
				toolName = msgs.getString(toolId, toolId);
			}catch(Exception e1){
				LOG.info("No translation found for toolId: " + toolId
						+ " - using toolId as tool name. Please specify it in sitestats/sitestats-impl/impl/src/bundle/org/sakaiproject/sitestats/impl/bundle/");
				toolName = toolId;
			}
		}

		// secondary tool id
		if(additionalToolIds != null){
			Iterator<String> i = additionalToolIds.iterator();
			while (i.hasNext()){
				String tId = i.next();
				try{
					toolName += "/" + M_tm.getTool(tId).getTitle();
				}catch(Exception e){
					try{
						LOG.info("No sakai tool found for (additional) toolId: " + toolId
								+ " (tool undeployed?). Using bundle (if supplied) in sitestats/sitestats-impl/impl/src/bundle/org/sakaiproject/sitestats/impl/bundle/ for tool name.");
						ResourceLoader msgs = new ResourceLoader("Events");
						toolName += "/" + msgs.getString(tId, tId);
					}catch(Exception e1){
						LOG.info("No translation found for (additional) toolId: " + toolId
								+ " - using toolId as tool name. Please specify it in sitestats/sitestats-impl/impl/src/bundle/org/sakaiproject/sitestats/impl/bundle/");
						toolName += "/" + tId;
					}
				}
			}
		}
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}	
	
	@Override
	public boolean equals(Object arg0) {
		if(arg0 == null || !(arg0 instanceof ToolInfo))
			return false;
		else {
			ToolInfo other = (ToolInfo) arg0;
			return getToolId().equals(other.getToolId());
		}
	}
	
	@Override
	public int hashCode() {
		return getToolId().hashCode();
	}
	
	public EventParserTip getEventParserTip() {
		return eventParserTip;
	}

	public void setEventParserTip(EventParserTip eventParserTip) {
		this.eventParserTip = eventParserTip;
	}
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("ToolInfo: "+getToolId()+" ("+getToolName()+") ["+isSelected()+"]");
		if(additionalToolIds != null) {
			Iterator<String> i = additionalToolIds.iterator();
			while(i.hasNext())
				buff.append("/" + i.next());
		}
		buff.append("\n");
		Iterator<EventInfo> iE = getEvents().iterator();
		while(iE.hasNext()){
			EventInfo e = iE.next();
			buff.append(e.toString());
		}
		return buff.toString();
	}
	
}