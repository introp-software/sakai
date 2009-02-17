package org.sakaiproject.sitestats.tool.wicket.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.sitestats.api.PrefsData;
import org.sakaiproject.sitestats.api.ResourceStat;
import org.sakaiproject.sitestats.api.Stat;
import org.sakaiproject.sitestats.api.StatsManager;
import org.sakaiproject.sitestats.api.report.Report;
import org.sakaiproject.sitestats.api.report.ReportDef;
import org.sakaiproject.sitestats.api.report.ReportManager;
import org.sakaiproject.sitestats.api.report.ReportParams;
import org.sakaiproject.sitestats.tool.facade.SakaiFacade;
import org.sakaiproject.user.api.UserNotDefinedException;

public class ResourcesWidget extends Panel {
	private static final long		serialVersionUID	= 1L;
	private static Log				LOG					= LogFactory.getLog(ResourcesWidget.class);

	/** Inject Sakai facade */
	@SpringBean
	private transient SakaiFacade	facade;

	/** The site id. */
	private String					siteId				= null;
	private PrefsData				prefsdata			= null;
	
	private int						totalFiles			= -1;
	

	/**
	 * Default constructor.
	 * @param id The wicket:id
	 * @param siteId The related site id
	 */
	public ResourcesWidget(String id, final String siteId) {
		super(id);
		this.siteId = siteId;
		setRenderBodyOnly(true);
		setOutputMarkupId(true);
		
		// Single values (MiniStat)
		List<WidgetMiniStat> widgetMiniStats = new ArrayList<WidgetMiniStat>();
		widgetMiniStats.add(getMiniStatFiles());
		widgetMiniStats.add(getMiniStatOpenedFiles());
		widgetMiniStats.add(getMiniStatMostOpenedFile());
		widgetMiniStats.add(getMiniStatUserThatOpenedMoreFiles());		
		
		// Tabs
		List<AbstractTab> tabs = new ArrayList<AbstractTab>();
		tabs.add(new AbstractTab(new ResourceModel("overview_tab_bydate")) {
			private static final long	serialVersionUID	= 1L;
			@Override
			public Panel getPanel(String panelId) {
				return getWidgetTabByDate(panelId);
			}			
		});
		tabs.add(new AbstractTab(new ResourceModel("overview_tab_byuser")) {
			private static final long	serialVersionUID	= 1L;
			@Override
			public Panel getPanel(String panelId) {
				return getWidgetTabByUser(panelId);
			}			
		});
		tabs.add(new AbstractTab(new ResourceModel("overview_tab_byresource")) {
			private static final long	serialVersionUID	= 1L;
			@Override
			public Panel getPanel(String panelId) {
				return getWidgetTabByResource(panelId);
			}			
		});

		// Final Widget object		
		String icon = "/sakai-sitestats-tool/images/silk/icons/folder_page.png";
		String title = (String) new ResourceModel("overview_title_resources").getObject();
		Widget widget = new Widget("widget", icon, title, widgetMiniStats, tabs);
		add(widget);
	}

	// -------------------------------------------------------------------------------	
	/** MiniStat:: Files & Folders count */
	private WidgetMiniStat getMiniStatFiles() {
		return new WidgetMiniStat() {
			private static final long	serialVersionUID	= 1L;
			@Override
			public String getValue() {
				return Integer.toString(getTotalFiles());
			}
			@Override
			public String getSecondValue() {
				return null;
			}
			@Override
			public String getTooltip() {
				return null;
			}
			@Override
			public boolean isWiderText() {
				return false;
			}
			@Override
			public String getLabel() {
				return (String) new ResourceModel("overview_title_resources_sum").getObject();
			}
			@Override
			public ReportDef getReportDefinition() {
				ReportDef r = new ReportDef();
				r.setId(0);
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				rp.setWhatLimitedAction(true);
				rp.setWhatResourceAction(ReportManager.WHAT_RESOURCES_ACTION_NEW);
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				// when
				rp.setWhen(ReportManager.WHEN_ALL);
				// who
				rp.setWho(ReportManager.WHO_ALL);
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				totalsBy.add(StatsManager.T_RESOURCE);
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				rp.setHowSortBy(StatsManager.T_RESOURCE);
				rp.setHowSortAscending(true);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_TABLE);
				r.setReportParams(rp);
				return r;
			}
		};
	}
	
	/** MiniStat:: Opened files */
	private WidgetMiniStat getMiniStatOpenedFiles() {
		return new WidgetMiniStat() {
			private static final long	serialVersionUID			= 1L;
			private long				totalDistinctFileReads		= -1;
			
			@Override
			public String getValue() {
				processData();
				return Long.toString(totalDistinctFileReads);
			}
			
			@Override
			public String getSecondValue() {
				double percentage = getTotalFiles()==0 ? 0 : round(100 * totalDistinctFileReads / getTotalFiles(), 0);
				return String.valueOf((int) percentage) + '%';
			}
			
			@Override
			public String getTooltip() {
				return null;
			}
			
			@Override
			public boolean isWiderText() {
				return false;
			}
			
			@Override
			public String getLabel() {
				return (String) new ResourceModel("overview_title_openedfiles_sum").getObject();
			}
			
			private ReportDef getCommonReportDefition() {
				ReportDef r = new ReportDef();
				r.setId(0);
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				rp.setWhatLimitedAction(true);
				rp.setWhatResourceAction(ReportManager.WHAT_RESOURCES_ACTION_READ);
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				// when
				rp.setWhen(ReportManager.WHEN_ALL);
				// who
				rp.setWho(ReportManager.WHO_ALL);
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				totalsBy.add(StatsManager.T_RESOURCE);
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				rp.setHowSortBy(StatsManager.T_TOTAL);
				rp.setHowSortAscending(false);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_TABLE);
				r.setReportParams(rp);
				return r;
			}
			
			private void processData() {
				if(totalDistinctFileReads == -1) {
					Report r = getFacade().getReportManager().getReport(getCommonReportDefition(), true);
					try{
						totalDistinctFileReads = 0;
						for(Stat s : r.getReportData()) {
							try{
								String resId = ((ResourceStat)s).getResourceRef();
								String prefix = "/content";
								if(resId.startsWith(prefix)) {
									resId = resId.substring(prefix.length());
								}
								getFacade().getContentHostingService().getResource(resId);
								totalDistinctFileReads++;
							}catch(Exception e) {
								// skip: doesn't exist or is a collection
							}
						}
					}catch(Exception e) {
						totalDistinctFileReads = 0;
					}
				}
			}
			
			@Override
			public ReportDef getReportDefinition() {
				return getCommonReportDefition();
			}
		};
	}
	
	/** MiniStat:: Most opened file */
	private WidgetMiniStat getMiniStatMostOpenedFile() {
		return new WidgetMiniStat() {
			private static final long	serialVersionUID			= 1L;
			private String				mostOpenedFile				= null;
			
			@Override
			public String getValue() {
				processData();
				if(mostOpenedFile != null) {
					return getFacade().getStatsManager().getResourceName(mostOpenedFile, false);
				}else{
					return "-";
				}
			}
			
			@Override
			public String getSecondValue() {
				return null;
			}
			
			@Override
			public String getTooltip() {
				if(mostOpenedFile != null) {
					return getFacade().getStatsManager().getResourceName(mostOpenedFile, true);
				}else{
					return null;
				}
			}
			
			@Override
			public boolean isWiderText() {
				return true;
			}
			
			@Override
			public String getLabel() {
				return (String) new ResourceModel("overview_title_mostopenedfile_sum").getObject();
			}
			
			private ReportDef getCommonReportDefition() {
				ReportDef r = new ReportDef();
				r.setId(0);
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				rp.setWhatLimitedAction(true);
				rp.setWhatResourceAction(ReportManager.WHAT_RESOURCES_ACTION_READ);
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				// when
				rp.setWhen(ReportManager.WHEN_ALL);
				// who
				rp.setWho(ReportManager.WHO_ALL);
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				totalsBy.add(StatsManager.T_RESOURCE);
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				rp.setHowSortBy(StatsManager.T_TOTAL);
				rp.setHowSortAscending(false);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_TABLE);
				r.setReportParams(rp);
				return r;
			}
			
			private void processData() {
				if(mostOpenedFile == null) {
					Report r = getFacade().getReportManager().getReport(getCommonReportDefition(), true);
					try{
						boolean first = true;
						for(Stat s : r.getReportData()) {
							ResourceStat es = (ResourceStat) s;
							if(first) {
								mostOpenedFile = es.getResourceRef();
								first = false;
								break;
							}
						}
					}catch(Exception e) {
						mostOpenedFile = null;
					}
				}
			}
			
			@Override
			public ReportDef getReportDefinition() {
				return getCommonReportDefition();
			}
		};
	}
	
	/** MiniStat:: User that opened more file */
	private WidgetMiniStat getMiniStatUserThatOpenedMoreFiles() {
		return new WidgetMiniStat() {
			private static final long	serialVersionUID			= 1L;
			private String				user						= null;
			
			@Override
			public String getValue() {
				processData();
				if(user != null) {
					String id = null;
					if(("-").equals(user) || ("?").equals(user)){
						id = "-";
					}else{
						try{
							id = getFacade().getUserDirectoryService().getUser(user).getDisplayId();
						}catch(UserNotDefinedException e1){
							id = user;
						}
					}
					return id;
				}else{
					return "-";
				}
			}
			
			@Override
			public String getSecondValue() {
				return null;
			}
			
			@Override
			public String getTooltip() {
				if(user != null) {
					String name = null;
					if(("-").equals(user)) {
						name = (String) new ResourceModel("user_anonymous").getObject();
					}else if(("?").equals(user)) {
						name = (String) new ResourceModel("user_anonymous_access").getObject();
					}else{
						try{
							name = getFacade().getUserDirectoryService().getUser(user).getDisplayName();
						}catch(UserNotDefinedException e1){
							name = (String) new ResourceModel("user_unknown").getObject();
						}
					}
					return name;
				}else{
					return null;
				}
			}
			
			@Override
			public boolean isWiderText() {
				return true;
			}
			
			@Override
			public String getLabel() {
				return (String) new ResourceModel("overview_title_useropenedmorefile_sum").getObject();
			}
			
			private ReportDef getCommonReportDefition() {
				ReportDef r = new ReportDef();
				r.setId(0);
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				rp.setWhatLimitedAction(true);
				rp.setWhatResourceAction(ReportManager.WHAT_RESOURCES_ACTION_READ);
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				// when
				rp.setWhen(ReportManager.WHEN_ALL);
				// who
				rp.setWho(ReportManager.WHO_ALL);
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				totalsBy.add(StatsManager.T_USER);
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				rp.setHowSortBy(StatsManager.T_TOTAL);
				rp.setHowSortAscending(false);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_TABLE);
				r.setReportParams(rp);
				return r;
			}
			
			private void processData() {
				if(user == null) {
					Report r = getFacade().getReportManager().getReport(getCommonReportDefition(), true);
					try{
						boolean first = true;
						for(Stat s : r.getReportData()) {
							ResourceStat es = (ResourceStat) s;
							if(first) {
								user = es.getUserId();
								first = false;
								break;
							}
						}
					}catch(Exception e) {
						user = null;
					}
				}
			}
			
			@Override
			public ReportDef getReportDefinition() {
				return getCommonReportDefition();
			}
		};
	}
	
	// -------------------------------------------------------------------------------
	
	/** WidgetTab: By date */
	protected WidgetTabTemplate getWidgetTabByDate(String panelId) {
		WidgetTabTemplate wTab = new WidgetTabTemplate(panelId, ResourcesWidget.this.siteId) {
			private static final long	serialVersionUID	= 1L;

			@Override
			public List<Integer> getFilters() {
				return Arrays.asList(FILTER_DATE, FILTER_ROLE, FILTER_RESOURCE_ACTION);
			}

			@Override
			public boolean useChartReportDefinitionForTable() {
				return true;
			}
			
			@Override
			public ReportDef getChartReportDefinition() {
				return getTableReportDefinition();
			}
			
			@Override
			public ReportDef getTableReportDefinition() {
				String dateFilter = getDateFilter();
				String roleFilter = getRoleFilter();
				String resActionFilter = getResactionFilter();
				
				ReportDef r = new ReportDef();
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				// limit to Resources tool:
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				if(resActionFilter != null) {
					rp.setWhatLimitedAction(true);
					rp.setWhatResourceAction(resActionFilter);
				}
				// when
				rp.setWhen(dateFilter);
				// who
				if(!ReportManager.WHO_ALL.equals(roleFilter)) {
					rp.setWho(ReportManager.WHO_ROLE);
					rp.setWhoRoleId(roleFilter);
				}
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					totalsBy.add(StatsManager.T_DATEMONTH);
				}else{
					totalsBy.add(StatsManager.T_DATE);
				}
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				/*if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					rp.setHowSortBy(StatsManager.T_DATEMONTH);
				}else{
					rp.setHowSortBy(StatsManager.T_DATE);
				}*/
				rp.setHowSortBy(StatsManager.T_TOTAL);
				rp.setHowSortAscending(false);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_BOTH);
				rp.setHowChartType(StatsManager.CHARTTYPE_TIMESERIESBAR);
				rp.setHowChartSource(StatsManager.T_DATE);
				rp.setHowChartSeriesSource(StatsManager.T_NONE);
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_MONTH);
				}else if(dateFilter.equals(ReportManager.WHEN_LAST30DAYS)) {
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_DAY);
				}else{
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_WEEKDAY);
				}
				r.setReportParams(rp);
				
				return r;
			}					
		};
		return wTab;
	}
	
	/** WidgetTab: By user */
	protected WidgetTabTemplate getWidgetTabByUser(String panelId) {
		WidgetTabTemplate wTab = new WidgetTabTemplate(panelId, ResourcesWidget.this.siteId) {
			private static final long	serialVersionUID	= 1L;

			@Override
			public List<Integer> getFilters() {
				return Arrays.asList(FILTER_DATE, FILTER_ROLE, FILTER_RESOURCE_ACTION);
			}

			@Override
			public boolean useChartReportDefinitionForTable() {
				return false;
			}
			
			@Override
			public ReportDef getTableReportDefinition() {
				ReportDef r = getChartReportDefinition();
				ReportParams rp = r.getReportParams();
				rp.setHowTotalsBy(Arrays.asList(StatsManager.T_USER));
				rp.setHowSort(true);
				rp.setHowSortBy(StatsManager.T_TOTAL);
				rp.setHowSortAscending(false);	
				rp.setHowLimitedMaxResults(true);
				rp.setHowMaxResults(MAX_TABLE_ROWS);	
				r.setReportParams(rp);
				return r;
			}
			
			@Override
			public ReportDef getChartReportDefinition() {
				String dateFilter = getDateFilter();
				String roleFilter = getRoleFilter();
				String resActionFilter = getResactionFilter();
				
				ReportDef r = new ReportDef();
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				// limit to Resources tool:
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				if(resActionFilter != null) {
					rp.setWhatLimitedAction(true);
					rp.setWhatResourceAction(resActionFilter);
				}
				// when
				rp.setWhen(dateFilter);
				// who
				if(!ReportManager.WHO_ALL.equals(roleFilter)) {
					rp.setWho(ReportManager.WHO_ROLE);
					rp.setWhoRoleId(roleFilter);
				}
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					totalsBy.add(StatsManager.T_DATEMONTH);
				}else{
					totalsBy.add(StatsManager.T_DATE);
				}
				totalsBy.add(StatsManager.T_USER);
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					rp.setHowSortBy(StatsManager.T_DATEMONTH);
				}else{
					rp.setHowSortBy(StatsManager.T_DATE);
				}							
				rp.setHowSortAscending(false);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_BOTH);
				/*rp.setHowChartType(StatsManager.CHARTTYPE_TIMESERIES);
				rp.setHowChartSource(StatsManager.T_DATE);
				rp.setHowChartSeriesSource(StatsManager.T_USER);
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_MONTH);
				}else if(dateFilter.equals(ReportManager.WHEN_LAST30DAYS)) {
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_DAY);
				}else{
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_WEEKDAY);
				}*/
				rp.setHowChartType(StatsManager.CHARTTYPE_PIE);
				rp.setHowChartSource(StatsManager.T_USER);
				r.setReportParams(rp);
				
				return r;
			}					
		};
		return wTab;
	}
	
	/** WidgetTab: By resource */
	protected WidgetTabTemplate getWidgetTabByResource(String panelId) {
		WidgetTabTemplate wTab = new WidgetTabTemplate(panelId, ResourcesWidget.this.siteId) {
			private static final long	serialVersionUID	= 1L;

			@Override
			public List<Integer> getFilters() {
				return Arrays.asList(FILTER_DATE, FILTER_ROLE, FILTER_RESOURCE_ACTION);
			}

			@Override
			public boolean useChartReportDefinitionForTable() {
				return false;
			}
			
			@Override
			public ReportDef getTableReportDefinition() {
				ReportDef r = getChartReportDefinition();
				ReportParams rp = r.getReportParams();
				rp.setHowTotalsBy(Arrays.asList(StatsManager.T_RESOURCE));
				rp.setHowSort(true);
				rp.setHowSortBy(StatsManager.T_TOTAL);
				rp.setHowSortAscending(false);
				rp.setHowLimitedMaxResults(true);
				rp.setHowMaxResults(MAX_TABLE_ROWS);
				r.setReportParams(rp);
				return r;
			}
			
			@Override
			public ReportDef getChartReportDefinition() {
				String dateFilter = getDateFilter();
				String roleFilter = getRoleFilter();
				String resActionFilter = getResactionFilter();
				
				ReportDef r = new ReportDef();
				r.setSiteId(siteId);
				ReportParams rp = new ReportParams(siteId);
				// what
				rp.setWhat(ReportManager.WHAT_RESOURCES);
				// limit to Resources tool:
				rp.setWhatLimitedResourceIds(true);
				rp.setWhatResourceIds(Arrays.asList(StatsManager.RESOURCES_DIR + siteId + "/"));
				if(resActionFilter != null) {
					rp.setWhatLimitedAction(true);
					rp.setWhatResourceAction(resActionFilter);
				}
				// when
				rp.setWhen(dateFilter);
				// who
				if(!ReportManager.WHO_ALL.equals(roleFilter)) {
					rp.setWho(ReportManager.WHO_ROLE);
					rp.setWhoRoleId(roleFilter);
				}
				// grouping
				List<String> totalsBy = new ArrayList<String>();
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					totalsBy.add(StatsManager.T_DATEMONTH);
				}else{
					totalsBy.add(StatsManager.T_DATE);
				}
				totalsBy.add(StatsManager.T_RESOURCE);
				rp.setHowTotalsBy(totalsBy);
				// sorting
				rp.setHowSort(true);
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					rp.setHowSortBy(StatsManager.T_DATEMONTH);
				}else{
					rp.setHowSortBy(StatsManager.T_DATE);
				}							
				rp.setHowSortAscending(false);
				// chart
				rp.setHowPresentationMode(ReportManager.HOW_PRESENTATION_BOTH);
				rp.setHowChartType(StatsManager.CHARTTYPE_PIE);
				rp.setHowChartSource(StatsManager.T_RESOURCE);
				/*rp.setHowChartType(StatsManager.CHARTTYPE_TIMESERIES);
				rp.setHowChartSource(StatsManager.T_DATE);
				rp.setHowChartSeriesSource(StatsManager.T_RESOURCE);
				if(dateFilter.equals(ReportManager.WHEN_LAST365DAYS) || dateFilter.equals(ReportManager.WHEN_ALL)) {
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_MONTH);
				}else if(dateFilter.equals(ReportManager.WHEN_LAST30DAYS)) {
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_DAY);
				}else{
					rp.setHowChartSeriesPeriod(StatsManager.CHARTTIMESERIES_WEEKDAY);
				}*/
				r.setReportParams(rp);
				
				return r;
			}					
		};
		return wTab;
	}
	
	// -------------------------------------------------------------------------------

	
	
	// -------------------------------------------------------------------------------

	private SakaiFacade getFacade() {
		if(facade == null) {
			InjectorHolder.getInjector().inject(this);
		}
		return facade;
	}
	
	private PrefsData getPrefsdata() {
		if(prefsdata == null) {
			prefsdata = getFacade().getStatsManager().getPreferences(siteId, true);
		}
		return prefsdata;
	}
	
	/** Return total (existent) files (excluding collections). */
	private int getTotalFiles() {
		if(totalFiles == -1) {
			try{
				String siteCollectionId = getFacade().getContentHostingService().getSiteCollection(siteId);
				totalFiles = getFacade().getContentHostingService().getAllResources(siteCollectionId).size();
			}catch(Exception e){
				totalFiles = 0;
			}		
		}
		return totalFiles;
	}
	
	private static double round(double val, int places) {
		long factor = (long) Math.pow(10, places);
		// Shift the decimal the correct number of places to the right.
		val = val * factor;
		// Round to the nearest integer.
		long tmp = Math.round(val);
		// Shift the decimal the correct number of places back to the left.
		return (double) tmp / factor;
	}
}
