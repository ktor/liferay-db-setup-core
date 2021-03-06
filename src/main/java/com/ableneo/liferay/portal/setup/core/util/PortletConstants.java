package com.ableneo.liferay.portal.setup.core.util;

import com.liferay.exportimport.kernel.staging.StagingConstants;

/**
 * Created by gustavnovotny on 12.09.17.
 */
public class PortletConstants {

    private PortletConstants() {}

    public static final String STAGING_PARAM_TEMPLATE =
        StagingConstants.STAGED_PREFIX + StagingConstants.STAGED_PORTLET + "#" + "--";
    public static final String STAGING_PORTLET_ID_ADT =
        "com_liferay_dynamic_data_mapping_web_portlet_PortletDisplayTemplatePortlet";
    public static final String STAGING_PORTLET_ID_BLOGS = "com_liferay_blogs_web_portlet_BlogsPortlet";
    public static final String STAGING_PORTLET_ID_BOOKMARKS = "com_liferay_bookmarks_web_portlet_BookmarksPortlet";
    public static final String STAGING_PORTLET_ID_CALENDAR = "com_liferay_calendar_web_portlet_CalendarPortlet";
    public static final String STAGING_PORTLET_ID_DDL = "com_liferay_dynamic_data_lists_web_portlet_DDLPortlet";
    public static final String STAGING_PORTLET_ID_DL = "com_liferay_document_library_web_portlet_DLPortlet";
    public static final String STAGING_PORTLET_ID_FORMS =
        "com_liferay_dynamic_data_lists_form_web_portlet_DDLFormAdminPortlet";
    public static final String STAGING_PORTLET_ID_MB = "com_liferay_message_boards_web_portlet_MBAdminPortlet";
    public static final String STAGING_PORTLET_ID_MDR = "com_liferay_mobile_device_rules_web_portlet_MDRPortlet";
    public static final String STAGING_PORTLET_ID_POLLS = "com_liferay_polls_web_portlet_PollsPortlet";
    public static final String STAGING_PORTLET_ID_WEB_CONTENT = "com_liferay_journal_web_portlet_JournalPortlet";
    public static final String STAGING_PORTLET_ID_WIKI = "com_liferay_wiki_web_portlet_WikiPortlet";

    public static final String STAGING_PORTLET_ID_ASSET_PUBLISHER =
        "com_liferay_asset_publisher_web_portlet_AssetPublisherPortlet";
    public static final String STAGING_PORTLET_ID_JOURNAL_CONTENT =
        "com_liferay_journal_content_web_portlet_JournalContentPortlet";
}
