package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.domain.CustomFields;
import com.ableneo.liferay.portal.setup.domain.RolePermissionType;
import com.liferay.expando.kernel.model.*;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;

public final class SetupCustomFields {

    private static final Log LOG = LogFactoryUtil.getLog(SetupCustomFields.class);

    private SetupCustomFields() {}

    public static void setupExpandoFields(final List<CustomFields.Field> fields) {
        for (CustomFields.Field field : fields) {
            String className = field.getClassName();
            LOG.info("Add field " + field.getName() + "(" + className + ") to expando bridge");

            long runInCompanyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            ExpandoBridge bridge = ExpandoBridgeFactoryUtil.getExpandoBridge(runInCompanyId, className);
            addAttributeToExpandoBridge(bridge, field);
        }
    }

    /**
     * @return all expandos with types specified in the "excludeListed" List to
     *         avoid deleting all expandos in the portal!
     */
    private static List<ExpandoColumn> getAllExpandoColumns(final List<CustomFields.Field> customFields) {
        List<ExpandoColumn> all = new ArrayList<>();
        SortedSet<String> tables = new TreeSet<>();
        for (CustomFields.Field field : customFields) {
            ExpandoTable table;
            try {
                long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                table = ExpandoTableLocalServiceUtil.getDefaultTable(companyId, field.getClassName());
                if (table != null && !tables.contains(table.getName())) {
                    tables.add(table.getName());
                    List<ExpandoColumn> columns = ExpandoColumnLocalServiceUtil.getColumns(
                        companyId,
                        field.getClassName(),
                        table.getName()
                    );
                    all.addAll(columns);
                }
            } catch (PortalException | SystemException e) {
                LOG.error(String.format("Error in getAllExpandoColumns().%1$s", e.getMessage()));
            }
        }
        return all;
    }

    private static void addAttributeToExpandoBridge(final ExpandoBridge bridge, final CustomFields.Field field) {
        String name = field.getName();
        try {
            int fieldTypeKey = getFieldTypeKey(field.getType());
            if (bridge.hasAttribute(name)) {
                long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(
                    companyId,
                    bridge.getClassName(),
                    ExpandoTableConstants.DEFAULT_TABLE_NAME,
                    name
                );
                ExpandoColumnLocalServiceUtil.updateColumn(
                    column.getColumnId(),
                    name,
                    fieldTypeKey,
                    getAttributeFromString(fieldTypeKey, field.getDefaultData())
                );
            } else {
                bridge.addAttribute(name, fieldTypeKey, getAttributeFromString(fieldTypeKey, field.getDefaultData()));
            }
            UnicodeProperties properties = bridge.getAttributeProperties(name);
            properties.setProperty(
                ExpandoColumnConstants.INDEX_TYPE,
                Integer.toString(getIndexedType(field.getIndexed()))
            );
            properties.setProperty(
                ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE,
                getDisplayType(field.getDisplayType())
            );
            properties.setProperty(ExpandoColumnConstants.PROPERTY_LOCALIZE_FIELD_NAME, Boolean.FALSE.toString()); // todo localize-field-name on need
            properties.setProperty("localize-field", String.valueOf(field.isLocalizedValue())); // const missing

            bridge.setAttributeProperties(name, properties);
            setCustomFieldPermission(field.getRolePermission(), bridge, name);
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Could not set custom attribute: %1$s", name), e);
        }
    }

    private static void setCustomFieldPermission(
        final List<RolePermissionType> rolePermissions,
        final ExpandoBridge bridge,
        final String fieldName
    ) {
        LOG.info("Set read permissions on  field " + fieldName + " for " + rolePermissions.size() + " rolePermissions");
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(
            companyId,
            bridge.getClassName(),
            ExpandoTableConstants.DEFAULT_TABLE_NAME,
            fieldName
        );
        for (RolePermissionType rolePermission : rolePermissions) {
            String roleName = rolePermission.getRoleName();
            String permission = rolePermission.getPermission();
            switch (permission) {
                case "update":
                    SetupPermissions.addReadWrightRight(
                        roleName,
                        ExpandoColumn.class.getName(),
                        String.valueOf(column.getColumnId())
                    );
                    LOG.info(String.format("Added update permission on field %1$s for role %2$s", fieldName, roleName));
                    break;
                case "view":
                    SetupPermissions.addReadRight(
                        roleName,
                        ExpandoColumn.class.getName(),
                        String.valueOf(column.getColumnId())
                    );
                    LOG.info(String.format("Added read permission on field %1$s for role %2$s", fieldName, roleName));
                    break;
                default:
                    LOG.info(
                        "Unknown permission:" +
                        permission +
                        ". No permission added on " +
                        "field " +
                        fieldName +
                        " for role " +
                        roleName
                    );
                    break;
            }
        }
    }

    public static void deleteCustomField(final CustomFields.Field customField, final String deleteMethod) {
        deleteCustomFields(Arrays.asList(customField), deleteMethod);
    }

    public static void deleteCustomFields(final List<CustomFields.Field> customFields, final String deleteMethod) {
        if ("excludeListed".equals(deleteMethod)) {
            // delete all (from types in the list) but listed
            List<String> skipFields = attributeNamesList(customFields);
            List<ExpandoColumn> expandoColumns = getAllExpandoColumns(customFields);
            if (expandoColumns != null) {
                for (ExpandoColumn expandoColumn : expandoColumns) {
                    if (!skipFields.contains(expandoColumn.getName())) {
                        try {
                            ExpandoColumnLocalServiceUtil.deleteColumn(expandoColumn.getColumnId());
                        } catch (PortalException | SystemException e) {
                            LOG.error(String.format("Could not delete CustomField %1$s", expandoColumn.getName()), e);
                        }
                    }
                }
            }
        } else if (deleteMethod.equals("onlyListed")) {
            for (CustomFields.Field field : customFields) {
                try {
                    long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                    ExpandoTable table = ExpandoTableLocalServiceUtil.getDefaultTable(companyId, field.getClassName());
                    ExpandoColumnLocalServiceUtil.deleteColumn(
                        companyId,
                        field.getClassName(),
                        table.getName(),
                        field.getName()
                    );
                } catch (PortalException | SystemException e) {
                    LOG.error(String.format("Could not delete Custom Field %1$s", field.getName()), e);
                    continue;
                }
                LOG.info(String.format("custom field %1$s deleted ", field.getName()));
            }
        }
    }

    private static int getFieldTypeKey(final String name) {
        if ("stringArray".equals(name)) {
            return ExpandoColumnConstants.STRING_ARRAY;
        }
        if ("string".equals(name)) {
            return ExpandoColumnConstants.STRING;
        }
        if ("int".equals(name)) {
            return ExpandoColumnConstants.INTEGER;
        }
        if ("boolean".equals(name)) {
            return ExpandoColumnConstants.BOOLEAN;
        }
        if ("date".equals(name)) {
            return ExpandoColumnConstants.DATE;
        }
        if ("long".equals(name)) {
            return ExpandoColumnConstants.LONG;
        }
        if ("double".equals(name)) {
            return ExpandoColumnConstants.DOUBLE;
        }
        if ("float".equals(name)) {
            return ExpandoColumnConstants.FLOAT;
        }
        LOG.error(String.format("bad setup name: %1$s", name));
        return -1;
    }

    private static List<String> attributeNamesList(final List<CustomFields.Field> customFields) {
        List<String> names = new ArrayList<>();
        for (CustomFields.Field f : customFields) {
            if (f.getName() != null) {
                names.add(f.getName());
            }
        }
        return names;
    }

    private static int getIndexedType(final String indexed) {
        if ("none".equals(indexed)) {
            return ExpandoColumnConstants.INDEX_TYPE_NONE;
        } else if ("text".equals(indexed)) {
            return ExpandoColumnConstants.INDEX_TYPE_TEXT;
        } else if ("keyword".equals(indexed)) {
            return ExpandoColumnConstants.INDEX_TYPE_KEYWORD;
        } else {
            LOG.error(String.format("cannot get unknown index type: %1$s", indexed));
            return 0;
        }
    }

    private static String getDisplayType(final String displayType) {
        if ("checkbox".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_CHECKBOX;
        } else if ("radio".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_RADIO;
        } else if ("selection-list".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_SELECTION_LIST;
        } else if ("text-box".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX;
        } else if ("input-field".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_INPUT_FIELD;
        } else {
            LOG.error(String.format("cannot get unknown display type: %1$s", displayType));
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX;
        }
    }

    public static Serializable getAttributeFromString(final int type, final String attribute) {
        if (attribute == null) {
            return null;
        }

        if (type == ExpandoColumnConstants.BOOLEAN) {
            return GetterUtil.getBoolean(attribute);
        } else if (type == ExpandoColumnConstants.BOOLEAN_ARRAY) {
            return GetterUtil.getBooleanValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.DATE) {
            return GetterUtil.getDate(attribute, getDateFormat());
        } else if (type == ExpandoColumnConstants.DATE_ARRAY) {
            return GetterUtil.getDateValues(StringUtil.split(attribute), getDateFormat());
        } else if (type == ExpandoColumnConstants.DOUBLE) {
            return GetterUtil.getDouble(attribute);
        } else if (type == ExpandoColumnConstants.DOUBLE_ARRAY) {
            return GetterUtil.getDoubleValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.FLOAT) {
            return GetterUtil.getFloat(attribute);
        } else if (type == ExpandoColumnConstants.FLOAT_ARRAY) {
            return GetterUtil.getFloatValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.INTEGER) {
            return GetterUtil.getInteger(attribute);
        } else if (type == ExpandoColumnConstants.INTEGER_ARRAY) {
            return GetterUtil.getIntegerValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.LONG) {
            return GetterUtil.getLong(attribute);
        } else if (type == ExpandoColumnConstants.LONG_ARRAY) {
            return GetterUtil.getLongValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.SHORT) {
            return GetterUtil.getShort(attribute);
        } else if (type == ExpandoColumnConstants.SHORT_ARRAY) {
            return GetterUtil.getShortValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.STRING_ARRAY) {
            return StringUtil.split(attribute);
        } else {
            return attribute;
        }
    }

    private static DateFormat getDateFormat() {
        return DateUtil.getISO8601Format();
    }
}
