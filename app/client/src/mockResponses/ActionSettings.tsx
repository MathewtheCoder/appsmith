export const queryActionSettingsConfig = [
  {
    sectionName: "",
    id: 1,
    children: [
      {
        label: "Run query on page load",
        configProperty: "executeOnLoad",
        controlType: "SWITCH",
        info: "Will refresh data each time the page is loaded",
      },
      {
        label: "Request confirmation before running query",
        configProperty: "confirmBeforeExecute",
        controlType: "SWITCH",
        info: "Ask confirmation from the user each time before refreshing data",
      },
      // {
      //   label: "Cache response",
      //   configProperty: "shouldCacheResponse",
      //   controlType: "SWITCH",
      // },
      // {
      //   label: "Cache timeout (in milliseconds)",
      //   configProperty: "cacheTimeout",
      //   controlType: "INPUT_TEXT",
      //   dataType: "NUMBER",
      // },
      {
        label: "Query timeout (in milliseconds)",
        info: "Maximum time after which the query will return",
        configProperty: "actionConfiguration.timeoutInMillisecond",
        controlType: "INPUT_TEXT",
        dataType: "NUMBER",
      },
    ],
  },
];

export const apiActionSettingsConfig = [
  {
    sectionName: "",
    id: 1,
    children: [
      {
        label: "Run API on Page load",
        configProperty: "executeOnLoad",
        controlType: "CHECKBOX",
        info: "Will refresh data each time the page is loaded",
      },
      {
        label: "Request confirmation before running API",
        configProperty: "confirmBeforeExecute",
        controlType: "CHECKBOX",
        info: "Ask confirmation from the user each time before refreshing data",
      },
      {
        label: "Encode query params",
        configProperty: "actionConfiguration.encodeParamsToggle",
        controlType: "CHECKBOX",
        info: "Will encode query params. Also form body for x-www-form-encoded",
      },
      // {
      //   label: "Cache response",
      //   configProperty: "shouldCacheResponse",
      //   controlType: "SWITCH",
      // },
      // {
      //   label: "Cache timeout (in milliseconds)",
      //   configProperty: "cacheTimeout",
      //   controlType: "INPUT_TEXT",
      //   dataType: "NUMBER",
      // },
      {
        label: "API timeout (in milliseconds)",
        info: "Maximum time after which the API will return",
        configProperty: "actionConfiguration.timeoutInMillisecond",
        controlType: "NUMBER_INPUT",
        dataType: "number",
      },
    ],
  },
];
