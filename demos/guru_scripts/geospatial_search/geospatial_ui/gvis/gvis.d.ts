

declare module 'gvis' {
  export = gvis;
}

declare namespace gvis {
  
  /**
   * BaseApi Class for all future charts.
   * @preferred
   */
  export abstract class BaseApi {
      protected version: string;
      instanceIndex: number;
      typeName: string;
      container: any;
      data: any;
      render: any;
      event: any;
      layout: any;
      language: any;
      options: any;
      constructor();
      /**
       * Set customized color by name for all ECharts based chart.
       *
       * @example Change color for series data.
       * <pre>
       *  chart.setColorByName('Users', '#f00');
       *  chart.setColorByName('Series 1', '#f00');
       *  chart.setColorByName('Series 2', '#00ff00');
       *  chart.setColorByName('China', '#f0f');
       *  chart.setColorByName('Shanghai', '#aabbcc');
       *
       * </pre>
       * @param  {string} name  [description]
       * @param  {string} color [description]
       * @return {this}         [description]
       */
      setColorByName(name: string, color: string): this;
      /**
       * Return the customized color by name;
       *
       * @example Get current color for a series.
       * <pre>
    
       *  console.assert(chart.getColorByName('Users'), '#f00');
       *  console.assert(chart.getColorByName('Series 1'), '#f00');
       *  console.assert(chart.getColorByName('Series 2'), '#00ff00');
       *  console.assert(chart.getColorByName('China'), '#f0f');
       *  console.assert(chart.getColorByName('Shanghai'), '#aabbcc');
       * </pre>
       * @param  {string} name [description]
       * @return {string}      [description]
       */
      getColorByName(name: string): string | undefined;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Data Base Class.
   *
   * Created on: Oct 5th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  /**
   * Base Data Object Class for all Data object in the GVIS.
   * @preferred
   */
  export abstract class BaseData {
      /**
       * Store any additional data values within this field.
       */
      extra: any;
      /**
       * This field can be used to return any error message for any data object.
       */
      error: string;
      constructor();
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: base event Class.
   *
   * Created on: Oct 24th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  /**
   * Base Mouse Event Class. It represetns a single pointer.
   * @preferred
   */
  export class BaseMouseEvent {
      altKey: boolean;
      ctrlKey: boolean;
      shiftKey: boolean;
      /**
       * Returns which mouse button was pressed when the mouse event was triggered
       * 0 : The left mouse button
       * 1 : The middle mouse button
       * 2 : The right mouse button
       * @type {number}
       */
      button: number;
      /**
       * Returns which mouse buttons were pressed when the mouse event was triggered
       * 1 : The left mouse button
       * 2 : The right mouse button
       * 4 : The middle mouse button
       * 8 : The fourth mouse button (typically the "Browser Back" button)
       * 16 : The fifth mouse button (typically the "Browser Forward" button)
       * @type {number}
       */
      buttons: number;
      /**
       * Returns which mouse button was pressed when the mouse event was triggered
       * 1 : The left mouse button
       * 2 : The middle mouse button
       * 3 : The right mouse button
       * @type {number}
       */
      which: number;
      /** @type {number} Returns a number that indicates how many times the mouse was clicked */
      /** @type {number} Returns the horizontal coordinate of the mouse pointer, relative to the current window, when the mouse event was triggered */
      clientX: number;
      /** @type {number} Returns the vertical coordinate of the mouse pointer, relative to the current window, when the mouse event was triggered */
      clientY: number;
      /** @type {number} Returns the horizontal coordinate of the mouse pointer, relative to the screen, when an event was triggered */
      /** @type {number} Returns the vertical coordinate of the mouse pointer, relative to the screen, when an event was triggered */
      /** @type {number} Returns the horizontal coordinate of the mouse pointer, relative to the document, when the mouse event was triggered */
      pageX: number;
      /** @type {number} Returns the vertical coordinate of the mouse pointer, relative to the document, when the mouse event was triggered */
      pageY: number;
      /** @type {Object} Returns the element related to the element that triggered the mouse event */
      relatedTarget: Object;
      preventDefault: {
          (): void;
      };
      constructor(event: any);
  }
  /**
   * KeyBoard Base Event class. It represetns a single key press.
   */
  export class BaseKeyboardEvent {
      altKey: boolean;
      ctrlKey: boolean;
      shiftKey: boolean;
      metaKey: boolean;
      /** @type {string} Returns the Unicode character code of the key that triggered the onkeypress event */
      charCode: string;
      /** @type {string} Returns the key value of the key represented by the event */
      key: string;
      /** @type {number} Returns the Unicode character code of the key that triggered the onkeypress event, or the Unicode key code of the key that triggered the onkeydown or onkeyup event */
      keyCode: number;
      /** @type {number} Returns the Unicode character code of the key that triggered the onkeypress event, or the Unicode key code of the key that triggered the onkeydown or onkeyup event */
      which: number;
      /**
       * Returns the location of a key on the keyboard or device
       * 0 : represents a standard key (like "A")
       * 1 : represents a left key (like the left CTRL key)
       * 2 : represents a right key (like the right CTRL key)
       * 3 : represents a key on the numeric keypad (like the number "2" on the right keyboard side)
       * @type {number}
       */
      location: number;
      constructor(event: any);
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Base style class.
   *
   * Created on: Feb 17th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * The base chart setting except for graph chart.
   */
  export interface ChartSetting {
      render?: {
          container: Object;
          title?: {
              text?: string;
              subtext?: string;
          };
          legend?: {
              show: boolean;
          };
          grid?: {
              left?: number | string;
              top?: number | string;
              right?: number | string;
              bottom?: number | string;
          };
          textStyle?: {
              color?: string;
              fontStyle?: string;
              fontWeight?: string;
              fontFamily?: string;
              fontSize?: number;
          };
          xAxis?: {
              name?: string;
              nameLocation?: string;
              nameGap?: number;
              nameRotate?: number;
              nameTextStyle?: {
                  color?: string;
                  fontStyle?: string;
                  fontWeight?: string;
                  fontFamily?: string;
                  fontSize?: number;
              };
          };
          yAxis?: {
              name?: string;
              nameLocation?: string;
              nameGap?: number;
              nameRotate?: number;
              nameTextStyle?: {
                  color?: string;
                  fontStyle?: string;
                  fontWeight?: string;
                  fontFamily?: string;
                  fontSize?: number;
              };
          };
          [options: string]: any;
      };
      formatter?: {
          tooltip?: {
              (...args: any[]): string;
          };
      };
      language?: {
          selectedLanguage?: string;
          localization?: {
              [language: string]: {
                  vertex?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
                  edge?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
              };
          };
      };
  }
  /**
   * Base style class for all charts except the graph chart.
   */
  export class BaseStyle {
      defaultChartStyle: {
          [styles: string]: any;
      };
      color: Color;
      customizedColor: {
          [keys: string]: string;
      };
      constructor();
      /**
       * Set customized color by name for all ECharts based chart.
       * @param  {string} name  [description]
       * @param  {string} color [description]
       * @return {this}         [description]
       */
      setColorByName(name: string, color: string): this;
      /**
       * Return the color by name;
       * @param  {string} name [description]
       * @return {string}      [description]
       */
      getColorByName(name: string): string;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS library main interface.
   *
   * Created on: Oct 5th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Bar Chart Class
   *
   * Created on: Feb 15th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface BarChartSetting extends ChartSetting {
      formatter?: {
          /**
           * horizontal  | vertical;
           * @type {[type]}
           */
          orient?: string;
          /**
           *  xAxis label formatter
           * @param {string} value of the tick.
           * @param {number} index of the tick.
           * @return {string} label of the tick.
           */
          xAxis?: (value: string, index: number) => string;
          /**
           *  yAxis label formatter
           * @param {number} value of the tick.
           * @param {number} index of the tick.
           * @return {string} label of the tick.
           */
          yAxis?: (value: number, index: number) => string;
          tooltip?: (params: {
              componentType: 'series';
              seriesType: string;
              seriesIndex: number;
              seriesName: string;
              name: string;
              dataIndex: number;
              data: Object;
              value: any;
              color: string;
              percent: number;
          }, ticket?: string, callback?: {
              (ticket: string, html: string): void;
          }) => string;
      };
      label?: {
          show?: boolean;
          position?: string;
      };
      language?: {
          selectedLanguage?: string;
          localization?: {
              [language: string]: {
                  vertex?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
                  edge?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
              };
          };
      };
  }
  export class BarChart extends BaseApi {
      render: BarChartRender;
      data: BarChartData;
      event: BarChartEvent;
      options: BarChartSetting;
      /**
       * Creates an instance of BarChart.
       *
       * @example Create an instance of BarChart by using a customized configuration.
       * <pre>
       *  window.barchart = new gvis.BarChart({
       *    render: {
       *      container: document.getElementById("BarChart 1"),
       *      legend: {
       *        show: true
       *      },
       *      grid: {
       *        left: 250,
       *        right: 80
       *      },
       *      textStyle: {
       *        color: '#f00',
       *        fontStyle: 'italic',
       *        fontWeight: 'bolder',
       *        fontFamily: '"Courier New", Courier, monospace',
       *        fontSize: 20,
       *      }
       *    },
       *    formatter: {
       *      orient: 'horizontal',
       *      xAxis: function(v, i) {
       *        return `GET /vertex/value1 /value 2Test ${v}`;
       *      },
       *      yAxis: function(v, i) {
       *        return `${v} times/hour`;
       *      },
       *      tooltip: function(params, ticket) {
       *        console.log(params);
       *        return `${params}`;
       *      }
       *    },
       *    label: {
       *      show: true,
       *      position: 'right'
       *    }
       *  });
       *
       *  barchart
       *  .addData(testData)
       *  .update();
       * </pre>
       *
       * @param {BarChartSetting} options
       * @memberof BarChart
       */
      constructor(options: BarChartSetting);
      /**
       * Add a bar chart data
       *
       * @example Create a test data, and add it in barchart.
       *
       * <pre>
       * var testData = [];
       * for (var i=1; i&lt;testN; i++) {
       *
       *     testData.push({
       *       series: 'Count',
       *       x: i,
       *       y: +(testN + 2 * i * +Math.random()).toFixed(1)
       *     });
       *
       *     testData2.push({
       *       series: 'Count',
       *       x: i,
       *       y: +(2 * i * +Math.random()).toFixed(1)
       *     });
       *
       *     testData.push({
       *       series: 'Count'+i%2,
       *       x: i,
       *       y: +(testN - 2 * i * +Math.random()).toFixed(1)
       *     });
       * }
       *
       * barchart
       * .addData(testData)
       * .update();
       *
       * // add new data points for every 1 second.
       *
       *  setInterval(() =&gt; {
       *    var tmp = [];
       *    tmp.push({
       *        series: 'Count',
       *        x: testN,
       *        y: +(testN * Math.random()).toFixed(1)
       *      });
       *    barchart
       *    .addData(tmp)
       *    .update();
       *    testN += 1;
       *
       *    if (testN &gt;= 18) {
       *      clearInterval(window.tmp);
       *    }
       *  }, 1000);
       * </pre>
       *
       * @param {BarChartDataPoint[]} data
       * @returns {this}
       *
       * @memberOf BarChart
       */
      addData(data: BarChartDataPoint[]): this;
      /**
       * Drop all data from the chart, and load the new data in the chart.
       *
       * @example reload all data of barchart.
       * <pre>
       *  barchart.reloadData(testData).update();
       * </pre>
       * @param {BarChartDataPoint[]} data
       * @returns {this}
       *
       * @memberOf BarChart
       */
      reloadData(data: BarChartDataPoint[]): this;
      /**
       * Rerender the chart, it needs to be called for all the changes in the chart.
       * @example update barchart.
       * <pre>
       *  barchart.update();
       * </pre>
       * @returns {this}
       *
       * @memberOf BarChart
       */
      update(): this;
      /**
       * Update the setting of labels for the bar in the chart.
       * options:
       *  `show`:
       *    - `true`
       *    - `false`
       *  `position`:
       *    - `right`
       *    - `top`
       *    - `bottom`
       *    - `left`
       *
       * @example Update settings for labels of barchart
       * <pre>
       *  barchart.updateLabel({
       *    show: true,
       *    position: top
       *  })
       *  .update();
       * </pre>
       * @param {{
       *     show?: boolean;
       *     position?: string;
       *   }} option
       * @returns {this}
       *
       * @memberOf BarChart
       */
      updateLabel(option: {
          show?: boolean;
          position?: string;
      }): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Bar chart data module class.
   *
   * Created on: Feb 14th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * x is the category value. Such as Query Name
   * y is the value. Such call times.
   * series is the series value.
   * info can be use for other information.
   */
  export interface BarChartDataPoint {
      series: string;
      x: string;
      y: number;
      info?: {
          [key: number]: any;
      };
  }
  export class BarChartData {
      private chart;
      private inData;
      private maxBarNumber;
      private axisID;
      constructor(parent: BaseApi);
      /**
       * add a bar data in the bar chart. Not allow duplicated x value for a single series data.
       * @param  {BarChartDataPoint} data [description]
       * @return {this}                   [description]
       */
      addData(data: BarChartDataPoint): this;
      /**
       * Set the max bar number. If the bar number is larger than max bar number. The smallest bar will be discard.
       * @param  {number} n [description]
       * @return {this}     [description]
       */
      setMaxBarNumber(n: number): this;
      getSeries(): string[];
      /**
       * It returns the max value of y in all series. It will be used for the feature of the y axis range.
       * @return {number} [description]
       */
      getYMax(): number;
      /**
       * It returns the min value of y in all series. It will be used for generate yAxis range.
       * @return {number} [description]
       */
      getYMin(): number;
      getXMaxNumber(): number;
      /**
       * Get the largest series data x value array.
       * @return {any[]} [description]
       */
      getXAxisData(): any[];
      /**
       * change the major axis value series id. The bar order will be changed.
       * @param  {number} id [description]
       * @return {this}      [description]
       */
      setAxisID(id: number): this;
      /**
       * get data points array by series name.
       * @param  {string}            series [description]
       * @return {BarChartDataPoint[]}        [description]
       */
      getData(series: string): BarChartDataPoint[];
      /**
       * get the y axis data for a series. It will return an array which match the getXAxisData format. Undefined will be used for non existed value.
       * @param  {string}              series [description]
       * @return {BarChartDataPoint[]}        [description]
       */
      getYAxisData(series: string): any[];
      /**
       * Clean the data by series name.
       * @param  {string}            series [description]
       * @return {BarChartDataPoint[]}        [description]
       */
      cleanSeriesData(series: string): BarChartDataPoint[];
      /**
       * Sort the y value by ascending order.
       * @param  {BarChartDataPoint} a [description]
       * @param  {BarChartDataPoint} b [description]
       * @return {number}              [description]
       */
      private compare(a, b);
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Bar chart event module class.
   *
   * Created on: Feb 15th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface BarChartEventHandlers {
      [eventName: string]: {
          (...args: any[]): void;
      };
  }
  export interface EChartsMouseEvent {
      componentType: string;
      seriesType: string;
      seriesIndex: number;
      seriesName: string;
      name: string;
      dataIndex: number;
      data: Object;
      dataType: string;
      value: number | number[];
      color: string;
  }
  export class BarChartEvent {
      private chart;
      private defaultHandlers;
      private handlers;
      constructor(parent: BaseApi);
      getEventsName(): string[];
      /**
       * get event callback instance by event Name;
       * @param {BaseApi} eventName [description]
       */
      getEventHandler(eventName: string): {
          (...args: any[]): void;
      };
      setEventHandler(eventName: string, callback: {
          (...args: any[]): void;
      }): this;
  }
  
  /**
   * ECharts Options Typings. Needs to get ECharts Typings.
   */
  export interface EBarChartsOptions {
      [option: string]: any;
  }
  export class BarChartRender {
      private chart;
      private engine;
      private style;
      private timer;
      private timerTask;
      private options;
      constructor(parent: BaseApi);
      private init();
      /**
       * init Events binding.
       * @return {this} [description]
       */
      private initEvents();
      /**
       * Refined user input chart options for echarts.
       * @param  {EBarChartsOptions} options [description]
       * @return {any}                    [description]
       */
      private refineUserOptions(options);
      private initFormatter();
      update(): this;
      /**
       * Update the basic settings. It will affect current cart directly.
       * @return {this} [description]
       */
      updateSettings(options?: EBarChartsOptions): this;
      /**
       * Redraw the line chart. It will create a new engine instance.
       * @return {this} [description]
       */
      redraw(): this;
      /**
       * Clear the line chart object.
       */
      clear(): this;
      updateLabelSetting(option: {
          show?: boolean;
          position?: string;
      }): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: bar chart style module class.
   *
   * Created on: Feb 15th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class BarChartStyle extends BaseStyle {
      private chart;
      private defaultBarChartStyle;
      label: {
          [keys: string]: any;
      };
      constructor(parent: BaseApi);
      getStyleOptions(): any;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Graph Chart Class
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface GraphChartSetting {
      advanced?: {
          maxNodeLimit?: number;
          maxLinkLimit?: number;
          hideArrorMinZoom?: number;
          nodeToolTipSetting?: {
              (inNode: GraphChartDataNode, coreNode?: any, chart?: BaseApi): any;
          };
          linkToolTipSetting?: {
              (inLink: GraphChartDataLink, coreLink?: any, chart?: BaseApi): any;
          };
          nodeMenuContentsFunction?: {
              (inNode: GraphChartDataNode, coreNode?: any, config?: any, returnDefaultContent?: {
                  (): any;
              }): any;
          };
          linkMenuContentsFunction?: {
              (inLink: GraphChartDataLink, coreLink?: any, config?: any, returnDefaultContent?: {
                  (): any;
              }): any;
          };
      };
      renderType?: string;
      render?: any;
      style?: {
          node?: {
              condition: string | {
                  (node: GraphChartDataNode, chart: BaseApi): boolean;
              };
              style: GraphStyle.GraphChartNodeStyleInterface;
          }[];
          link?: {
              condition: string | {
                  (link: GraphChartDataLink, chart: BaseApi): boolean;
              };
              style: GraphStyle.GraphChartLinkStyleInterface;
          }[];
      };
      icon?: {
          [exType: string]: string;
      };
      graphSchema?: {
          VertexTypes: {
              Name: string;
          }[];
          EdgeTypes: {
              Name: string;
          }[];
      };
      nodeMenu?: {
          buttons?: {
              label: string;
              callback: {
                  (node: GraphChartDataNode, chart: BaseApi): void;
              };
          }[];
          enabled: boolean;
      };
      linkMenu?: {
          buttons?: {
              label: string;
              callback: {
                  (link: GraphChartDataLink, chart: BaseApi): void;
              };
          }[];
          enabled: boolean;
      };
      language?: {
          selectedLanguage?: string;
          localization?: {
              [language: string]: {
                  vertex?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
                  edge?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
              };
          };
      };
  }
  /**
   * Graph Chart Visualization Object
   * @preferred
   */
  export class GraphChart extends BaseApi {
      typeName: string;
      container: any;
      /**
       * graph data object
       * @type {GraphChartData}
       */
      data: GraphChartData;
      /**
       * User to parse external data to exteranl graph.
       * @type {GraphParser}
       */
      parser: GraphParser;
      /**
       * Rendering engine object.
       * @type {any}
       */
      render: GraphChartRender;
      /**
       * Layout object for graph visualization.
       * @type {GraphChartLayout}
       */
      layout: GraphChartLayout;
      /**
       * Event object for graph visualization.
       * @type {GraphChartEvent}
       */
      event: GraphChartEvent;
      /**
       * global settings for graph chart.
       * @type {GraphChartSetting}
       */
      options: GraphChartSetting;
      language: Localization;
      style: any;
      /**
       * Creates an instance of GraphChart by using GraphChartSetting.
       *
       * @example Create a graph chart instance.
       * <pre>
       * // import { GraphChart } from 'gvis';
       * // const GraphChart = require('gvis').GraphChart;
       *
       * var chart = new GraphChart({
       *  advanced: {
       *    maxNodeLimit: 5000,
       *    maxLinkLimit: 10000
       *  },
       *  render: {
       *    assetsUrlBase: '/dist/assets',
       *    advanced: {
       *      showFPS: true
       *    },
       *    container: document.getElementById('GraphChart'),
       *  },
       *  icon: {
       *    'UserCard': 'BankCard1',
       *    'IdCard': 'idCard'
       *  },
       *  style: {
       *    node: [{
       *      condition: 'type === "p_order" || id === "TID-00000000000000000100" || id.indexOf("User") !== -1',
       *      style: {
       *        radius: 100,
       *      }
       *    },{
       *      condition: (node, chart) => {
       *        return node.exType === "Txn" && node.exID.indexOf("1909") !== -1;
       *      },
       *      style: {
       *        radius: 200,
       *        icon: 'Open Disconnector',
       *        display: 'rectangle',
       *        aspectRatio: 0.35
       *      }
       *    }],
       *    link: [{
       *      condition: 'type === "prodOrder"',
       *      style: {
       *        radius: 20,
       *      }
       *    },
       *    {
       *      condition: 'type === "prodOrder"',
       *      style: {
       *        fillColor: '#000000'
       *      }
       *    },
       *    ]
       *  },
       *  language: {
       *    selectedLanguage: 'zh',
       *    localization: {
       *      'zh': {
       *        vertex: {
       *          'built-in-translate': {
       *            attrs: {
       *              'menu': {
       *                values: {
       *                  'Set as Start Vertex': '设置为出发点',
       *                  'Set as End Vertex': '设置为终止点',
       *                  'View Statistics Information': '查看统计信息'
       *                }
       *              }
       *            }
       *          },
       *          'Txn': {
       *            type: '交易',
       *            attrs: {
       *              'acct_date': {
       *                attr: '注册时间',
       *                values: {
       *                  '20150023': '2015年1月23号',
       *                  '20150313': '2015年4月13号'
       *                }
       *              },
       *              'trans_type': {
       *                values: {
       *                  'A': '非法交易',
       *                  'B': '黑交易',
       *                  'C': '白交易',
       *                  'D': '未知交易'
       *                }
       *              },
       *              'self': '交易流水号',
       *              'merchant': '商户号',
       *              'merchant_order': '商户订单号'
       *            }
       *          },
       *          'Sid': {
       *            attrs: {
       *              'id': 'ID',
       *              'type': 'Sid类型'
       *            }
       *          }
       *        },
       *        edge: {
       *          'built-in-translate': {
       *            attrs: {
       *              'menu': {
       *                values: {
       *                  'View Statistics Information': '查看统计信息'
       *                }
       *              }
       *            }
       *          },
       *          'Sid_User': {
       *            type: '使用中',
       *            attrs: {
       *              'txn_time': {
       *                attr: '交易时间',
       *                values: {
       *                  '13:42:06': '13点42分06秒'
       *                }
       *              }
       *            }
       *          }
       *        }
       *      }
       *    }
       *  },
       *  nodeMenu: {
       *    enabled: true,
       *    buttons: [{
       *      label: 'Set as Start Vertex',
       *      callback: function (node, chart) {
       *        console.log('start vertex menu click: ', node, ' ', chart);
       *      }
       *    },
       *    {
       *      label: 'Set as End Vertex',
       *      callback: function (node, chart) {
       *        console.log('end vertex menu click: ', node, ' ', chart);
       *      }
       *    },
       *    {
       *      label: 'View Statistics Information',
       *      callback: function (node, chart) {
       *        console.log('View Statistics Information Node menu click: ', node, ' ', chart);
       *      }
       *    }
       *    ]
       *  },
       *  linkMenu: {
       *    enabled: true,
       *    buttons: [{
       *      label: 'View Statistics Information',
       *      callback: function (link, chart) {
       *        console.log('View Statistics Information Link menu click: ', link, ' ', chart);
       *      }
       *    }]
       *  }
       * })
       *
       * </pre>
       * @param {GraphChartSetting} options
       */
      constructor(options: GraphChartSetting);
      /**
       * redraw the chart in graphics level.
       *
       * @returns {this}
       * @memberof GraphChart
       */
      paint(): this;
      /**
       * Rerender the graph chart.
       *
       * @returns {this}
       * @memberof GraphChart
       */
      update(): this;
      /**
       * Sync the coordinate of dynamic layout between render data and internal data.
       *
       * @returns {this}
       * @memberof GraphChart
       */
      updateCoordinate(): this;
      /**
       * Add sub graph.
       *
       * @example Add a sub graph.
       * <pre>
       * var graph = JSON Object;
       * chart.addData(graph, 'gquery');
       * chart.addData(graph, 'gquery', 'extend');
       * chart.addData(graph, 'ksubgraph', 'defaults');
       * chart.addData(graph, 'ksubgraph', 'extend', {
       *  node: 'extend',
       *  link: 'overwrite'
       * });
       * chart.addData(graph, 'gquery', undefined, {
       *  node: undefined,
       *  link: 'extend'
       * });
       * chart.addData(graph, 'gquery', 'extend', {
       *  node: 'extend',
       *  link: 'extend'
       * });
       * </pre>
       *
       * @param  {any} externalData [description]
       * @param  {any} externalDataType
       * It is used to specify the graph format of the external data. Base on this filed, the corresponding parser is used to parse external data into external graph. A list of built in parser:
       * - `static`  no parser. Use original data object.
       * - `gquery`  use for standard gquery output.
       * - `ksubgraph` use for ksubgraph output.
       * - `pta` use for pta output.
       * - `gpr` use for gpr output.
       * - `restpp` use for restpp standard api output.
       * - `gst` use for graph studio output.
       *
       * @param  {string}        nodeOption   =             `extend` [description]
       * Add node base on option. Check out the utils module for detailed information for option.
       * Example for adding a node A with an existing node B: two nodes A and B.
       * - `defaults` : Merge A and B, if there are same field, use values from A;
       * - `extend` : Merge A and B, if there are same field, use values from B;
       * - `overwrite` : Use values from B overwrite the same fields of A;
       *
       * @param  {AddLinkOption} linkOption   [description]
       * Add link base on option, and add link's source and target base on option.
       * Example for adding an link L1 = `A_`->`B_`: There is a link L2 = A->B is existed. `A_` and A point to a same node,
       * `B_` and B point to a same node. But attributes, styles, others of them may different.
       *
       * Link option.link (not handle source and target nodes):
       * - `defaults` : Merge L1 and L2, if there are same field, use values from L1;
       * - `extend` : Merge L1 and L2, if there are same field, use values from L2;
       * - `overwrite` : Use values from L2 overwrite the same fields of L2;
       *
       * Link option.node (handle source and target nodes)
       * This options is used for handling existed source or target node, such as `A_`(new) and A(old). The option is same as addNode APIs:
       * - `defaults` : Merge `A_` and A, if there are same field, use values from A;
       * - `extend` : Merge `A_` and A, if there are same field, use values from `A_`;
       * - `overwrite` : Use values from `A_` overwrite the same fields of A;
       *
       * @return {this}                       [description]
       */
      addData(externalData: any, externalDataType?: string, nodeOption?: string, linkOption?: AddLinkOption): this;
      /**
       * drop a sub graph base on the json object's format.
       *
       * @example Drop a sub graph.
       * <pre>
       * var graph = JSON Object;
       * chart.dropData(graph, 'gquery');
       * chart.dropData(graph, 'ksubgraph');
       * chart.dropData(graph, 'gpr');
       * chart.dropData(graph, 'gst');
       * ...
       * </pre>
       *
       * @param {*} externalData
       * @param {string} externalDataType
       * It is used to specify the graph format of the external data. Base on this filed, the corresponding parser is used to parse external data into external graph. A list of built in parser:
       * - `static`  no parser. Use original data object.
       * - `gquery`  use for standard gquery output.
       * - `ksubgraph` use for ksubgraph output.
       * - `pta` use for pta output.
       * - `gpr` use for gpr output.
       * - `restpp` use for restpp standard api output.
       * - `gst` use for graph studio output.
       *
       * @returns {this}
       */
      dropData(externalData: any, externalDataType?: string): this;
      /**
       * reload new graph in the graph visualization without clean the internal index.
       *
       * @example Reload a new graph.
       *
       * <pre>
       * var graph = JSON Object;
       * chart.reloadData(graph, 'gquery');
       * chart.reloadData(graph, 'gpr');
       * chart.reloadData(graph, 'restpp');
       * ...
       *
       * </pre>
       *
       * @param  {any} externalData [description]
       * @param  {any} externalDataType
       * It is used to specify the graph format of the external data. Base on this filed, the corresponding parser is used to parse external data into external graph. A list of built in parser:
       * - `static`  no parser. Use original data object.
       * - `gquery`  use for standard gquery output.
       * - `ksubgraph` use for ksubgraph output.
       * - `pta` use for pta output.
       * - `gpr` use for gpr output.
       * - `restpp` use for restpp standard api output.
       * - `gst` use for graph studio output.
       *
       * @return {this}                       [description]
       */
      reloadData(externalData: any, externalDataType?: string): this;
      /**
       * delete the old graph visualization, and recreate a new graph visualization by using the new graph.
       *
       * @example Reset a new graph.
       *
       * <pre>
       * var graph = JSON Object;
       * chart.resetData(graph, 'gquery');
       * chart.resetData(graph, 'gpr');
       * chart.resetData(graph, 'restpp');
       * ...
       *
       * </pre>
       *
       * @param {*} externalData
       * @param {string} externalDataType
       * It is used to specify the graph format of the external data. Base on this filed, the corresponding parser is used to parse external data into ext ernal graph. A list of built in parser:
       * - `static`  no parser. Use original data object.
       * - `gquery`  use for standard gquery output.
       * - `ksubgraph` use for ksubgraph output.
       * - `pta` use for pta output.
       * - `gpr` use for gpr output.
       * - `restpp` use for restpp standard api output.
       * - `gst` use for graph studio output.
       *
       * @returns {this}
       */
      resetData(externalData: any, externalDataType?: string): this;
      /**
       * Reset layout and redarw everything in the viewport.
       *
       * @example Redraw a graph.
       * <pre>
       *  var graph = JSON Object;
       *  chart
       *  .addData(graph)
       *  .runLayout('force')
       *  .update();
       *
       *  chart.redraw();
       * </pre>
       * @return {this} [description]
       */
      redraw(): this;
      /**
       * drop a node by external type and id.
       *
       * @example Drop nodes.
       * <pre>
       *    // Drop current selected nodes
       *    chart.getSelection().nodes.forEach(n =&gt; {
       *      chart.dropNode(n.exType, n.exID, true);
       *    })
       *
       *    // Drop a taget node.
       *    chart.dropNode('Txn', '001', false);
       *
       *    // Drop the first node in graph.
       *    var node = chart.getNodes()[0];
       *    chart.dropNode(node.exType, node.exID);
       * </pre>
       * @param  {[type]}      ExternalNode [description]
       * @param  {boolean}            option  dropping node will drop related links. While we drop links, we use option
       * to determine whether or not we drop the isolated nodes:
       * - `true`: drop isolated nodes
       * - `false`: do not drop isolated nodes.
       * @return {DroppedItems}         The nodes dropped. The first one is the target dropping node.
       */
      dropNode(exType: string, exID: string, option?: boolean): DroppedItems;
      /**
       * drop a link by external type, source, target.
       *
       * @example Drop Links
       * <pre>
       *  // Drop current selected links
       *  chart.getSelection().links.forEach(l => {
       *    chart.dropLink(l.exType, l.source.exType, l.source.exID, l.target.exType, l.target.exID);
       *  })
       *
       *  // Drop the first link in the graph
       *  var link = chart.getLinks()[0];
       *  chart.dropLink(link.exType, link.source.exType, link.source.exID, link.target.exType, link.target.exID);
       *
       *  // Drop the last link in the graph, and remove the target or source nodes if it is isolated nodes.
       *  var links = chart.getLinks();
       *  var link = links.pop();
       *  chart.dropLink(link.exType, link.source.exType, link.source.exID, link.target.exType, link.target.exID, false);
       * </pre>
       *
       * @param  {string}       exType       link external type
       * @param  {string}       sourceExType source node external type
       * @param  {string}       sourceExID   source node external id
       * @param  {string}       targetExType target node external type
       * @param  {string}       targetExID   target node external id
       * @param  {[type]}       option       It is sed to determine if you want to drop isolated nodes:
       * - `true`: don't remove any nodes from graph while dropping links.
       * - `false`: do remove source and target nodes of this link, if the node is isolated. And use the same option for dropping other related nodes and links.
       * @return {DroppedItems}              [description]
       */
      dropLink(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string, option?: boolean): DroppedItems;
      /**
       * Closes a node. Hide target node's neighbor nodes which only connetect with target.
       *
       * @example Close 1 step neighborhood of a target node.
       * <pre>
       *   var node = chart.getSelection().nodes[0];
       *   chart
       *   .closeNode(node.exType, node.exID)
       *   .update();
       * </pre>
       *
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      closeNode(exType: string, exID: string): this;
      /**
       * Collapse a node. Hide target node, and all its neighbor nodes.
       *
       * @example Collapse the target node.
       * <pre>
       *  var node = chart.getSelection().nodes[0];
       *  chart
       *  .collapseNode(node.exType, node.exID)
       *  .update();
       * </pre>
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      collapseNode(exType: string, exID: string): this;
      /**
       * Expands a visible node. Show all one step neighbor nodes of target node.
       *
       * @example Expand 1 step neighborhood of a target node.
       * <pre>
       *  var node = chart.getSelection().nodes[0];
       *
       *  chart
       *  .expandNode(node.exType, node.exID)
       *  .update();
       * </pre>
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      expandNode(exType: string, exID: string): this;
      /**
       * hide a node.
       *
       * @example Hide current selected nodes.
       * <pre>
       *  var nodes = chart.getSelection().nodes;
       *
       *  nodes.forEach(n => {
       *    chart.hideNode(n.exType, n.exID)
       *  })
       *
       *  chart.update();
       *
       * </pre>
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      hideNode(exType: string, exID: string): this;
      /**
       * show an hidden node.
       *
       * @example Show a hidden node.
       * <pre>
       *  var node = chart.getNodes()[0];
       *  chart.hideNode(n.exType, n.exID);
       *  chart.showNode(n.exType, n.exID)
       *
       *  chart.update();
       *
       * </pre>
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      showNode(exType: string, exID: string): this;
      /**
       * Fixates a node in place.
       *
       * @example Lock a node, and fixates it to [0, 0];
       * <pre>
       * var node = chart.getNodes()[0];
       *
       * chart.lockNode(node, 0, 0).paint();
       * </pre>
       * @param {GraphChartDataNode} node
       * @param {number} [x]
       * @param {number} [y]
       * @returns {this}
       * @memberof GraphChart
       */
      lockNode(node: GraphChartDataNode, x?: number, y?: number): this;
      /**
       * Unfixates a node and allows it to be repositioned by the layout algorithms.
       *
       * @example ublock a node;
       * <pre>
       * var node = chart.getNodes()[0];
       *
       * chart.unlockNode(node);
       * </pre>
       * @param {GraphChartDataNode} node
       * @returns {this}
       * @memberof GraphChart
       */
      unlockNode(node: GraphChartDataNode): this;
      /**
       * Lowlight a node base on external type, and external ID.
       *
       * @example Lowlight all nodes.
       * <pre>
       *  var nodes = chart.getNodes();
       *
       *  nodes.forEach(n => {
       *    chart.lowlightNode(n.exType, n.exID)
       *  })
       *
       *  chart.update();
       *
       * </pre>
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      lowlightNode(exType: string, exID: string): this;
      /**
       * UnLowlight a node base on external type, and external ID.
       *
       * @example UnLowlight all nodes.
       * <pre>
       *  var nodes = chart.getNodes();
       *
       *  nodes.forEach(n => {
       *    chart.unLowlightNode(n.exType, n.exID)
       *  })
       *
       *  chart.update();
       *
       * </pre>
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      unLowlightNode(exType: string, exID: string): this;
      /**
       * Lowlight a link base on edge type, source, and target.
       *
       * @example Lowlight all links.
       * <pre>
       *  var links = chart.getLinks();
       *
       *  links.forEach(l => {
       *    chart.lowlightLink(l.exType, l.source.exType, l.source.exID, l.target.exType, l.target.exID);
       *  })
       *
       *  chart.update();
       *
       * </pre>
       * @param  {string} exType       [description]
       * @param  {string} sourceExType [description]
       * @param  {string} sourceExID   [description]
       * @param  {string} targetExType [description]
       * @param  {string} targetExID   [description]
       * @return {this}                [description]
       */
      lowlightLink(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string): this;
      /**
       * Un Lowlight a link base on edge type, source, and target.
       *
       * @example UnLowlight all links.
       * <pre>
       *  var links = chart.getLinks();
       *
       *  links.forEach(l => {
       *    chart.unLowlightLink(l.exType, l.source.exType, l.source.exID, l.target.exType, l.target.exID);
       *  })
       *
       *  chart.update();
       *
       * </pre>
       * @param  {string} exType       [description]
       * @param  {string} sourceExType [description]
       * @param  {string} sourceExID   [description]
       * @param  {string} targetExType [description]
       * @param  {string} targetExID   [description]
       * @return {this}                [description]
       */
      unLowlightLink(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string): this;
      /**
       * Clear current visualization viewport.
       *
       * @example Clear the current visualization.
       * <pre>
       *  chart.clear();
       * </pre>
       *
       * @return {this} [description]
       */
      clear(): this;
      /**
       * Export current graph viewport to a file.
       *
       * @example Export visualization result as a certain format.
       * <pre>
       *  chart.export('screenshot');
       *  chart.export('screenshot1', 'pdf');
       *  chart.export('screenshot2', 'png');
       *  chart.export('screenshot3', 'jpg');
       *  chart.export('data', 'csv');
       *  ...
       *
       * </pre>
       *
       * @param  {string} name output file name which is not include extension.
       * @param  {string} type [description] value supports:
       * - `pdf`
       * - `png`
       * - `jpg`
       * - `pdf`
       * - `csv`
       * @return {this}        [description]
       */
      export(name: string, type?: string): this;
      /**
       * get nodes APIs.
       *
       * @example Get nodes data object in graph visualization.
       * <pre>
       *  // Get all nodes.
       *  var nodes = chart.getNodes();
       *  console.log(`There are ${nodes.length} nodes in visualization.`);
       *
       *  // Get all `type` nodes.
       *  var nodes = chart.getNodes('Person');
       *  console.log(`There are ${nodes.length} "Person" nodes in visualization.`);
       *
       *  // Get a person node of which name is 'Joe'.
       *  var node = chart.getNodes('Person', 'Joe');
       *
       * </pre>
       * @param  {string}             exType external node type.
       * @param  {string}             exID   external node id.
       *
       * 1. If no parameters input, return all nodes as array. Returns [] for an empty graph.
       * 2. If only exType input, returns all nodes with this type. Returns [] for not type exist.
       * 3. If both exType, exID are inputed, return the target node. Return undefined for not exist node.
       * @return {GraphChartDataNode | GraphChartDataNode[]}        [description]
       */
      getNodes(exType?: string, exID?: string): GraphChartDataNode | GraphChartDataNode[];
      /**
       *
       * Get all 1-step neighbors of a target node.
       *
       * @example Get selected node's 1 step neighborhood.
       * <pre>
       * let node = chart.getSelection().nodes[0];
       * chart.getNeighbors(node);
       *
       * </pre>
       * @param {GraphChartDataNode} node
       * @returns {{
       *     all: GraphChartDataNode[],
       *     in: GraphChartDataNode[],
       *     out: GraphChartDataNode[]
       *   }}
       * @memberof GraphChart
       */
      getNeighbors(node: GraphChartDataNode): {
          all: GraphChartDataNode[];
          in: GraphChartDataNode[];
          out: GraphChartDataNode[];
      };
      /**
       * get links APIs.
       *
       * @example Get links data object in graph visualization.
       * <pre>
       *  // Get all links.
       *  var links = chart.getLinks();
       *  console.log(`There are ${links.length} links in visualization.`);
       *
       *  // Get all `type` links.
       *  var links = chart.getLinks('Connected');
       *  console.log(`There are ${links.length} "Connected" links in visualization.`);
       *
       *  // Get a "Connected" link between Person Joe and Person Shawn
       *  var node = chart.getLinks('Connected', 'Person', 'Joe', 'Person', 'Shawn');
       *
       * </pre>
       *
       * @param  {string}             exType       external link type
       * @param  {string}             sourceExType source node external type
       * @param  {string}             sourceExID   source node exteranl id
       * @param  {string}             targetExType target node exteranl type
       * @param  {string}             targetExID   target node external id
       *
       * 1. If no parameters is inputed, return all links as array. Returns [] for an empty graph.
       * 2. If only exType is inputed, returns all links with this type. Returns [] for not type exist.
       * 3. If exType, sourceType, sourceID are inputed, returns all links from the source node.
       * 4. If input all parameters, return the target link. Return undefined for not exist link.
       * @return {GraphChartDataLink | GraphChartDataLink[]}              [description]
       */
      getLinks(exType?: string, sourceExType?: string, sourceExID?: string, targetExType?: string, targetExID?: string): GraphChartDataLink | GraphChartDataLink[];
      /**
       * return all nodes and links in array
       *
       * @example Get graph.
       * <pre>
       *  var graph = chart.getData();
       *
       *  console.log(`There are ${graph.nodes.length} nodes.`);
       *  console.log(`There are ${graph.links.length} links.`);
       * </pre>
       */
      getData(): {
          nodes: GraphChartDataNode | GraphChartDataNode[];
          links: GraphChartDataLink | GraphChartDataLink[];
      };
      /**
       * Get current selected nodes and links.
       *
       * @example Get selected subgraph.
       * <pre>
       *  var graph = chart.getSelection();
       *
       *  console.log(`${graph.nodes.length} nodes are selected.`);
       *  console.log(`${graph.links.length} links are selected.`);
       * </pre>
       *
       * @returns {{ nodes: GraphChartDataNode[], links: GraphChartDataLink[] }}
       */
      getSelection(): {
          nodes: GraphChartDataNode[];
          links: GraphChartDataLink[];
      };
      /**
       * Set selected nodes and links.
       *
       * @example Set selected nodes and links.
       *
       * <pre>
       *  // select all nodes and links.
       *  var nodes = chart.getNodes();
       *  var links = chart.getLinks();
       *
       *  chart.setSelection([...nodes, ...links]);
       *
       *  // select the root node.
       *  chart.setSelection([chart.getRoot()]);
       *  chart.setSelection(chart.getRoots());
       *
       *  // select the all 'Connected' links.
       *  chart.setSelection(chart.getLinks('Connected'));
       *
       * </pre>
       *
       * @param {((GraphChartDataNode | GraphChartDataLink)[])} selected
       * @returns {this}
       * @memberof GraphChart
       */
      setSelection(selected: (GraphChartDataNode | GraphChartDataLink)[]): this;
      /**
       * unselect all elements.
       *
       * @example Unselecte all elements.
       * <pre>
       *  chart.unselectAllElements();
       *  var graph = chart.getSelection();
       *
       *  console.assert(graph.nodes.length === 0);
       *  console.assert(graph.links.length === 0);
       * </pre>
       * @returns {this}
       */
      unselectAllElements(): this;
      /**
       * Export legend related information and label selection information.
       *
       * @example export the legend object.
       * <pre>
       *  var legendObj = chart.exportLegends();
       *
       *  console.log(`There are ${legendObj.nodes.length} different type of nodes.`);
       *  console.log(`There are ${legendObj.links.length} different type of links.`);
       * </pre>
       *
       * @returns {{
       *     nodes: {
       *       type: string;
       *       icon: any;
       *       color: string;
       *       labels: {
       *         attributeName: string;
       *         isSelected: boolean;
       *       }[];
       *     }[];
       *     links: {
       *       type: string;
       *       color: string;
       *       labels: {
       *         attributeName: string;
       *         isSelected: boolean;
       *       }[];
       *     }[];
       *   }}
       */
      exportLegends(): {
          nodes: {
              type: string;
              icon: any;
              color: string;
              labels: {
                  attributeName: string;
                  isSelected: boolean;
              }[];
          }[];
          links: {
              type: string;
              color: string;
              labels: {
                  attributeName: string;
                  isSelected: boolean;
              }[];
          }[];
      };
      /**
       * Customize an event call back behavior in visualization.
       * @example Binding event handler.
       *
       * <pre>
       *  chart.on('onKeyPress', (nodes, links, event, chartObject) => {
       *    // Do something here. event call back.
       *  })
       *
       *  chart.on('onSelectionChange', (nodes, links, chartObject) => {
       *    // Do something here. event call back.
       *  })
       *
       *  chart.on('onClick', (item) => {
       *    // Do something here. event call back.
       *    // Example:
       *    if (item.isNode) {
       *      console.log(`Node ${item.exType} ${item.exID} is clicked.`);
       *    } else if (item.isLink) {
       *      cpmsole.log(`Link ${item.id} is clicked.`);
       *    }
       *  })
       *
       *  chart.on('onDoubleClick', (item) => {
       *    // Do something here for double click event.
       *  })
       *
       *  chart.on('onRightClick', (item) => {
       *    // Do something here for right click event.
       *  })
       *
       *  chart.on('onExceedLimit', (nodes, links, chart) => {
       *     // Do something here to handle the graph size is larger than the limitation.
       *  })
       * </pre>
       *
       * @param  {string} event
       * Current supported events and their call back function interfaces are shown as follow:
       *
       * - `onKeyPress` : { (nodes: GraphChartDataNode[], links: GraphChartDataLink[], event: BaseKeyboardEvent, chart: GraphChart): void };
       * - `onChartUpdate` : { (chart: GraphChart): void };
       * - `onSelectionChange` : { (nodes: GraphChartDataNode[], links: GraphChartDataLink[], chart: GraphChart): void };
       * - `onClick` : { (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: GraphChart): void };
       * - `onDoubleClick` : { (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: GraphChart): void };
       * - `onHoverChange` : { (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: GraphChart): void };
       * - `onPositionChange` : { (event: BaseMouseEvent, chart: GraphChart): void };
       * - `onRightClick` : { (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: GraphChart): void };
       * - `onExceedLimit` : { (nodes: GraphChartDataNode[], links: GraphChartDataLink[], chart: GraphChart): void };
       * @param  {void }}  callback      event call back function.
       * @return {this}            chart object
       */
      on(event: string, callback: {
          (item?: GraphChartDataNode | GraphChartDataLink, nodes?: GraphChartDataNode[], links?: GraphChartDataLink[], keyboardEvent?: BaseKeyboardEvent, mouseEvent?: BaseMouseEvent, chart?: GraphChart): void;
      }): this;
      /**
       * Removes an event listener by event name
       *
       * @example unBinding event handler.
       *
       * <pre>
       *  chart.off('onKeyPress')l;
       *  chart.off('onSelectionChange');
       *  chart.off('onClick');
       *  chart.off('onDoubleClick');
       *  chart.off('onHoverChange');
       *  chart.off('onRightClick');
       *
       *  ...
       * </pre>
       * @param  {string} event Current supported events:
       * - `onKeyPress`
       * - `onChartUpdate`
       * - `onSelectionChange`
       * - `onClick`
       * - `onDoubleClick`
       * - `onHoverChange`
       * - `onPositionChange`
       * - `onRightClick`
       * @return {this}         [description]
       */
      off(event: string): this;
      /**
       * Customize node tool tip GraphStyle. callback function returns html string to display in passed nodes info popup
       * @param  {function} callback [description]
       * @return {this}            [description]
       */
      /**
       * Customize link tool tip GraphStyle. callback function returns html string to display in passed links info popup.
       * @param  {function} callback [description]
       * @return {this}            [description]
       */
      /**
       * Show labels of a target node.
       *
       * @example Show labels of nodes.
       * <pre>
       *  // show 'type' in labels for all nodes.
       *  var nodes = chart.getNodes();
       *  nodes.forEach(n => {
       *    chart.showNodeLabels(n.exType, exID, ['type']);
       *  })
       *
       *  chart.update();
       *
       *
       *  // show 'id' and 'type' in labels for all current selected nodes.
       *  var nodes = chart.getSelection().nodes;
       *  nodes.forEach(n => {
       *    chart.showNodeLabels(n.exType, n.exID, ['type', 'id']);
       *  })
       *
       *  ...
       * </pre>
       * @param  {string}   exType node external type
       * @param  {string}   exID   node external id
       * @param  {string[]} labels a list of labels which want to be shown.
       * @return {this}            [description]
       */
      showNodeLabels(exType: string, exID: string, labels: string[]): this;
      /**
       * Show labels of nodes with one kind of type.
       *
       * @example Show labels of nodes by type.
       * <pre>
       *  // show 'type' in labels for all 'Person' nodes.
       *  chart
       *  .showNodeLabelsByType('Person', ['type']);
       *  .update()
       *
       *  // show 'id' and 'type' in labels for all nodes.
       *  chart.exportLegends().nodes.map(n => n.type).forEach(t => {
       *    chart.showNodeLabelsByType(t, ['id', 'type'])
       *  });
       *  chart.update();
       *  ...
       * </pre>
       *
       * @param  {string}   exType node external type
       * @param  {string[]} labels the labels want to be shown
       * @return {this}            [description]
       */
      showNodeLabelsByType(exType: string, labels: string[]): this;
      /**
       * hide a target node's labels
       *
       * @example Hide labels of nodes
       * <pre>
       *  // Hide 'id' and 'type' in labels of the node Person Joe.
       *  chart.hideNodeLabels('Person', 'Joe', ['id', 'type']).update();
       *
       *  // Hide 'id' and 'type' for all nodes.
       *  chart.getNodes().forEach(n => {
       *    chart.hideNodeLabels(n.exType, n.exID, ['id', 'type']);
       *  })
       *
       *  chart.update();
       * </pre>
       * @param  {string}   exType node external type
       * @param  {string}   exID   node external id
       * @param  {string[]} labels a list of labels which want to be hidden.
       * @return {this}            [description]
       */
      hideNodeLabels(exType: string, exID: string, labels: string[]): this;
      /**
       * hide labels of node with one kind of type.
       *
       * @example Hide labels of nodes by type
       * <pre>
       *  // Hide 'id' and 'type' in labels of the node Person.
       *  chart.hideNodeLabelsByType('Person', ['id', 'type']).update();
       *
       *  // Hide 'id' and 'type' for all nodes.
       *  chart.exportLegends().nodes.map(n => n.type).forEach(t => {
       *    chart.hideNodeLabelsByType(t, ['id', 'type']);
       *  })
       *
       *  chart.update();
       * </pre>
       *
       * @param  {string}   exType node exteranl type
       * @param  {string[]} labels a list of labels that want to be hidden.
       * @return {this}            [description]
       */
      hideNodeLabelsByType(exType: string, labels: string[]): this;
      /**
       * Hide all nodes' labels.
       *
       * @example Hide labels of nodes by type
       * <pre>
       *  chart.hideAllNodeLabels().update();
       * </pre>
       * @return {this} [description]
       */
      hideAllNodeLabels(): this;
      /**
       * Show a target links's label
       *
       * @example Show link labels.
       * <pre>
       *  // Show a link's 'type' in label
       *  chart
       *  .showLinkLabels('Connected', 'Person', 'Joe', 'Person', 'Joe', ['type'])
       *  .update();
       *
       *  // Show all links' 'type' in label.
       *  chart
       *  .getLinks().forEach(l => {
       *    chart.showLinkLabels(l.exType, l.source.exType, l.source.exID, l.target.exType, l.target.exID, ['type']);
       *  })
       *
       *  chart.update();
       * </pre>
       * @param  {string}   exType       link external type
       * @param  {string}   sourceExType source node external type
       * @param  {string}   sourceExID   source node exteranl id
       * @param  {string}   targetExType target node external type
       * @param  {string}   targetExID   target node external id
       * @param  {string[]} labels       a list of labels that want to be shwon. such as ['type', 'time']
       * @return {this}                  [description]
       */
      showLinkLabels(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string, labels: string[]): this;
      /**
       * show links with a same type
       *
       * @example Show links' labels.
       * <pre>
       *  // Show label 'type' of 'Connected' links.
       *  chart
       *  .showLinkLabelsByType('Connected', ['type'])
       *  .update();
       *
       * </pre>
       *
       * @param  {string}   exType link external type
       * @param  {string[]} labels [description]
       * @return {this}            [description]
       */
      showLinkLabelsByType(exType: string, labels: string[]): this;
      /**
       * hide a target link.
       *
       * @example Hide link labels.
       * <pre>
       *  // Hide a link's 'type' in label
       *  chart
       *  .hideLinkLabels('Connected', 'Person', 'Joe', 'Person', 'Joe', ['type'])
       *  .update();
       *
       *  // Hide all links' 'type' in label.
       *  chart
       *  .getLinks().forEach(l => {
       *    chart.hideLinkLabels(l.exType, l.source.exType, l.source.exID, l.target.exType, l.target.exID, ['type']);
       *  })
       *
       *  chart.update();
       * </pre>
       * @param  {string}   exType       [description]
       * @param  {string}   sourceExType [description]
       * @param  {string}   sourceExID   [description]
       * @param  {string}   targetExType [description]
       * @param  {string}   targetExID   [description]
       * @param  {string[]} labels       [description]
       * @return {this}                  [description]
       */
      hideLinkLabels(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string, labels: string[]): this;
      /**
       * hide links' label with a same type
       *
       * @example Hide links' labels.
       * <pre>
       *  // Hide label 'type' of 'Connected' links.
       *  chart
       *  .hideLinkLabelsByType('Connected', ['type'])
       *  .update();
       *
       * </pre>
       * @param  {string}   exType [description]
       * @param  {string[]} labels [description]
       * @return {this}            [description]
       */
      hideLinkLabelsByType(exType: string, labels: string[]): this;
      /**
       * hide all links labels.
       * @example Hide all links label
       * <pre>
       *  chart.hideAllLinkLabels().update();
       * </pre>
       * @return {this} [description]
       */
      hideAllLinkLabels(): this;
      /**
       * Zoom in the view. ( x2 );
       *
       * @example zoom in the view.
       * <pre>
       *  chart.zoomIn().update();
       * </pre>
       *
       * @return {this} [description]
       */
      zoomIn(): this;
      /**
       * Zoom out the view port. ( /2 );
       *
       * @example zoom out the view.
       * <pre>
       *  chart.zoomOut().update();
       * </pre>
       *
       * @return {this} [description]
       */
      zoomOut(): this;
      /**
       * Auto fit current graph in the view.
       *
       * @example autoFit the view.
       * <pre>
       *  chart.autoFit().update();
       * </pre>
       *
       * @return {this} [description]
       */
      autoFit(): this;
      /**
       * Auto center a group of nodes.
       *
       * @example Center the selected nodes.
       * <pre>
       *  var nodes = chart.getSelection().nodes;
       *  chart.scrollIntoView(nodes)
       * </pre>
       *
       * @return {this} [description]
       */
      scrollIntoView(nodes?: GraphChartDataNode[]): this;
      /**
       * Auto fit selected nodes and their 1-step neightborhood nodes.
       *
       * @example Center the view.
       * <pre>
       *  chart.centerView()
       * </pre>
       *
       * @return {this} [description]
       */
      centerView(): this;
      /**
       * Call when the container size has been changed to update the chart.
       *
       * @example Resize the viewport
       * <pre>
       *  document.on('resize', () => {
       *    chart.updateSize();
       *  })
       * </pre>
       *
       * @return {this} [description]
       */
      updateSize(): this;
      /**
       * enable/disable fullscreen of visualization.
       *
       * @example Enable/
       * <pre>
       *  // Enable Fullscreen.
       *  chart.fullscreen(true);
       *
       *  // Disable Fullscreen.
       *  chart.fullscreen(false);
       * </pre>
       *
       * @param  {boolean} isFullscreen true: fullscreen, false: normal model.
       * @return {this}                 [description]
       */
      fullscreen(isFullscreen: boolean): this;
      /**
       * change the default layout. Once set layout does not run the layout. It will affect the behavoir of runLayout() API.
       *
       * @example set graph layout
       * <pre>
       *  chart.setLayout('default').runLayout();
       *  chart.setLayout('force').runLayout();
       *  chart.setLayout('hierarchy').runLayout();
       *  chart.setLayout('tree').runLayout();
       *  chart.setLayout('circle').runLayout();
       *  chart.setLayout('sphere').runLayout();
       *  chart.setLayout('random').runLayout();
       *  chart.setLayout('static').runLayout();
       * </pre>
       *
       * @param  {string} layoutName [description]
       * Current default supported layout:
       * - `default`
       * - `force`
       * - `hierarchy`
       * - `tree`
       * - `circle`
       * - `sphere`
       * - `random`
       * - `static`
       * @return {this}              [description]
       */
      setLayout(layoutName: string): this;
      /**
       * Add a customized layout algorithm. The layoutName must be not existed. The callback function provide graph object and chart object as input. Change node.x and node.y in graph.nodes array.
       *
       * @example add layout `sphere`
       * <pre>
       * // add a customized layout called mySphere
       * chart.addLayout('mySphere', function(graph, theChart) {
       *   let N = graph.nodes.length;
       *   let radius = 2 * N * 30 / Math.PI;
       *
       *   graph.nodes.forEach((node, i) =&gt; {
       *     let position = Utils.rotate(0, 0, 0, radius, i / N * 360);
       *     node.x = position[0];
       *     node.y = position[1];
       *   });
       * })
       *
       * chart.setLayout('mySphere').runLayout();
       * chart.runLayout('mySphere');
       * </pre>
       * @param {string}                  layoutName [description]
       * @param { (graph: { nodes: GraphChartDataNode[], links: GraphChartDataLink[] }, chart: BaseApi): void }
       * callback customized function for layout algorithm.
       * @return {this}              [description]
       */
      addLayout(layoutName: string, callback: {
          (graph: {
              nodes: GraphChartDataNode[];
              links: GraphChartDataLink[];
          }, chart: BaseApi): void;
      }): this;
      /**
       * overwrite an existed layout.
       *
       * @example Overwrite a layout.
       *
       * <pre>
       * // Overwrite the mySphere layout.
       * chart.overwriteLayout('mySphere', function(graph, theChart) {
       *   // Here for new layout algorithm.
       *   let N = graph.nodes.length;
       *   let radius = 2 * N * 30 / Math.PI;
       *
       *   graph.nodes.forEach((node, i) =&gt; {
       *     let position = Utils.rotate(0, 0, 0, radius, i / N * 360);
       *     node.x = position[0];
       *     node.y = position[1];
       *   });
       * })
       *
       * chart.runLayout();
       * </pre>
       *
       * @param {string} layoutName
       * @param {{
       *     (graph: {
       *       nodes: GraphChartDataNode[],
       *       links: GraphChartDataLink[]
       *     },
       *       chart: BaseApi): void
       *   }} callback
       * @returns {this}
       *
       */
      overwriteLayout(layoutName: string, callback: {
          (graph: {
              nodes: GraphChartDataNode[];
              links: GraphChartDataLink[];
          }, chart: BaseApi): void;
      }): this;
      /**
       * run a layout by using a layout name. And set the layout to be selected layout. This function will update and autoFit by default.
       * If there is not input, the current selected layout will be used, or the default (`force`) will be used.
       * Current default supported layout:
       * - `default`
       * - `force`
       * - `hierarchy`
       * - `tree`
       * - `circle`
       * - `sphere`
       * - `random`
       * - `static`
       *
       * @example run a layout for visualization
       * <pre>
       *  chart.runLayout('force');
       *  chart.runLayout('tree');
       *  chart.runLayout('sphere');
       *  chart.runLayout('random');
       *
       *  ...
       * </pre>
       * @param  {string} layoutName
       * @return {this}              [description] can not be used with update();
       */
      runLayout(layoutName?: string, autoFit?: boolean): this;
      /**
       * add an new external data parser. The parser can not be added, if the name is already added.
       * After add parser, the parser can be used in addData API.
       *
       * @example add a parser function for graph visualization.
       * <pre>
       *  var graph = JSON String;
       *  chart.addParser('JSON', (externalData) =&gt; {
       *    var graph = JSON.parse(externalData);
       *
       *    return graph;
       *  })
       *
       *  chart.addData(graph, 'JSON').update();
       * </pre>
       * @param  {string}           parserName [description]
       * @param  {ExternalGraph }}       callback      [description]
       * @return {this}                        [description]
       */
      addParser(parserName: string, callback: {
          (externalData: any): ExternalGraph;
      }): this;
      /**
       * Get a parser function by a parser name.
       *
       * @example Get a parser function by name.
       *
       * <pre>
       *  // highlight the new coming nodes;
       *
       *  var graph = JSON String;
       *  var parser = chart.getParser('JSON');
       *  var parsedGraph = parser(graph);
       *
       *  chart.addData(parsedGraph, 'static');
       *
       *  chart.getNodes().forEach(n => {
       *    chart.lowlightNode(n.exType, n.exID);
       *  });
       *
       *  parsedGraph.nodes.forEach(n => {
       *    chart.unLowlightNode(n.exType, n.exID);
       *  })
       *
       *  chart.update();
       * </pre>
       * @param {string} parserName
       * @returns {{ (externalData: any): ExternalGraph }}
       */
      getParser(parserName: string): {
          (externalData: any): ExternalGraph;
      };
      /**
       * Update an existed parser. Warning message will be submited, if the target parser is not existed.
       *
       * @example Update static parser
       *
       * <pre>
       *  var graph = JSON String;
       *
       *  chart.updateParser('static', (externalData) => {
       *    var parsedGraph = JSON.parse(externalData);
       *    return chart.getParser('pta')(parsedGraph);
       *  })
       *
       *  chart.addData(graph, 'static').update();
       * </pre>
       * @param  {string}           parserName [description]
       * @param  {ExternalGraph }}       callback      [description]
       * @return {this}                        [description]
       */
      updateParser(parserName: string, callback: {
          (externalData: any): ExternalGraph;
      }): this;
      /**
       * Update visualization setting. Have not finished yet.
       * @param  {any}  options [description]
       * @return {this}        [description]
       */
      updateSettings(options: {
          area?: any;
          node?: any;
          link?: any;
      }): this;
      /**
       * Set the target node to be The first item in the root node lists. In most of the case, the layout needs a single root node. You can use getRoot() to get current root node.
       *
       * @example Set the root node.
       *
       * <pre>
       *  // Run tree layout by setting the first selected node as root node.
       *  var node = chart.getSelection().nodes[0];
       *  chart.setRoot(node.exType, node.exID);
       *
       *  chart.runLayout('tree');
       * </pre>
       * @param  {string} exType target node external type
       * @param  {string} exID   target node external id
       * @return {this}          [description]
       */
      setRoot(exType: string, exID: string): this;
      /**
       * Get the single root node.
       *
       * @example Get the first root node.
       *
       * <pre>
       *  var root = chart.getRoot();
       *
       *  console.assert(root.isNode)
       * </pre>
       * @return {GraphChartDataNode} Return the first node in the root node list.
       */
      getRoot(): GraphChartDataNode;
      /**
       * add a target node in the root node list.
       *
       * @example add a node in root node list.
       *
       * <pre>
       *  var node = chart.getSelection().nodes[0];
       *  chart.addRoot(node.exType, node.exID);
       *  chart.runLayout('tree');
       * </pre>
       *
       * @param  {string} exType node external id
       * @param  {string} exID   node external type
       * @return {this}          [description]
       */
      addRoot(exType: string, exID: string): this;
      /**
       * get the root node list.
       *
       * @example Get all nodes in root node list.
       *
       * <pre>
       *  var nodes = chart.getRoots();
       * </pre>
       * @return {GraphChartDataNode[]} [description]
       */
      getRoots(): GraphChartDataNode[];
      /**
       * clean current root node list.
       *
       * @example Clear all nodes in root node list.
       *
       * <pre>
       *  chart.cleanRoots();
       *  console.assert(chart.layout.roots.length === 0)
       * </pre>
       * @return {this} [description]
       */
      cleanRoots(): this;
      /**
       * Translate a list of nodes base on current selectedLanguage, and localization settings.
       *
       * @example Get all translated nodes.
       * <pre>
       *  chart.translateNodes(chart.getNodes());
       * </pre>
       * @param  {[type]}               nodes The list of nodes, such as the result of getNodes(), getSelectedNodes, etc..
       * @return {GraphChartDataNode[]}       [description]
       */
      translateNodes(nodes: GraphChartDataNode[]): GraphChartDataNode[];
      /**
       * Translate a list of links base on current selectedLanguage, and localization settings.
       *
       * @example Get all translated links.
       * <pre>
       *  chart.translateLinks(chart.getLinks());
       * </pre>
       * @param  {[type]}               links The list of links, such as the result of getLinks(), getSelectedLinks(), etc..
       * @return {GraphChartDataLink[]}       [description]
       */
      translateLinks(links: GraphChartDataLink[]): GraphChartDataLink[];
      /**
       * get the vertex translation base on the input values.
       * Translate type, if only type is inputed. Translate attribute, if type and attribute are inputed. Translate value, if all arguments are inputed.
       * It will translate the value for specific input based on the vertex type and attribute.
       *
       * @example Translate information for a node.
       * <pre>
       *  var chart = new GraphChart({
       *    render: {
       *      assetsUrlBase: '/dist/assets',
       *      container: document.getElementById('GraphChart'),
       *    },
       *    language: {
       *      selectedLanguage: 'zh',
       *      localization: {
       *        'zh': {
       *          vertex: {
       *            'Person': {
       *              type: '用户',
       *              attrs: {
       *                'lastName': '姓',
       *                'gender': {
       *                  attr: '性别',
       *                  value: {
       *                    '0': '男',
       *                    '1': '女'
       *                  }
       *                }
       *              }
       *            }
       *          }
       *        }
       *      }
       *    }
       *  });
       *
       *  var node = {
       *    exType: 'Person',
       *    exID: 'Shawn',
       *    attrs: {
       *      lastName: 'Huang',
       *      gender: '0'
       *    }
       *  };
       *
       *  console.assert(chart.translateForNode(node.exType), '用户');
       *  console.assert(chart.translateForNode(node.exType, 'lastName'), '姓');
       *  console.assert(chart.translateForNode(node.exType, 'gender'), '性别');
       *  console.assert(chart.translateForNode(node.exType, 'gender', node.attrs.gender), '男');
       *  console.assert(chart.translateForNode('Person', 'gender', '1'), '女');
       *
       *  ...
       * </pre>
       * @param  {string} type      Vertex type
       * @param  {string} attribute Vertex attribute
       * @param  {string} value     Vertex value
       * @return {string}           the translated value
       */
      translateForNode(vertexType: string, attribute?: string, value?: string): string;
      /**
       * get the edge translation base on the input values.
       * Translate type, if only type is inputed. Translate attribute, if type and attribute are inputed. Translate value, if all arguments are inputed.
       * It will translate the value for specific input based on the edge type and attribute.
       *
       * @example translate the information of links base on a predefined configuration.
       * <pre>
       * var chart = new GraphChart({
       *    render: {
       *      assetsUrlBase: '/dist/assets',
       *      container: document.getElementById('GraphChart'),
       *    },
       *    language: {
       *      selectedLanguage: 'zh',
       *      localization: {
       *        'zh': {
       *          edge: {
       *            'Connected': {
       *              type: '朋友',
       *              attrs: {
       *                'lastName': '姓',
       *                'gender': {
       *                  attr: '性别',
       *                  value: {
       *                    '0': '男',
       *                    '1': '女'
       *                  }
       *                }
       *              }
       *            }
       *          }
       *        }
       *      }
       *    }
       *  });
       *  var link = {
       *    exType: 'Connected',
       *    source: {
       *      exType: 'Person',
       *      exID: 'Shawn'
       *    },
       *    target: {
       *      exType: 'Person',
       *      exID: 'Joe'
       *    },
       *    attrs: {
       *      lastName: 'Huang',
       *      gender: '0'
       *    }
       *  };
       *
       *  console.assert(chart.translateForLink(link.exType), '朋友');
       *  console.assert(chart.translateForLink(link.exType, 'lastName'), '姓');
       *  console.assert(chart.translateForLink(link.exType, 'gender'), '性别');
       *  console.assert(chart.translateForLink(link.exType, 'gender', link.attrs.gender), '男');
       *  console.assert(chart.translateForLink('Person', 'gender', '1'), '女');
       *
       * </pre>
       * @param  {string} type      Edge type
       * @param  {string} attribute Edge attribute
       * @param  {string} value     Edge value
       * @return {string}           the translated value
       */
      translateForLink(edgeType: string, attribute?: string, value?: string): string;
      /**
       * hide menu action. Hide current menu of graph visualization either node menu or link menu.
       *
       * @example hide all menu.
       * <pre>
       *  chart.hideMenu();
       * </pre>
       * @return {this} [description]
       */
      hideMenu(): this;
      /**
       * Set icon for a type.
       *
       * @example Set icon for a certain vertex type.
       * <pre>
       *  chart.setIcon('Person', 'user');
       *  chart.setIcon('Person', 'Address1');
       *  chart.setIcon('Person', 'icon_001');
       *  chart.setIcon('Person', 'icon_225');
       *
       *  chart.update();
       * </pre>
       *
       * @param {string} type
       * @param {string} icon
       */
      setIcon(type: string, icon: string): this;
      /**
       * Set color for a type.
       *
       * @example Set color for a certain vertex or edge type.
       * <pre>
       *  chart.setColor('Person', '#fff');
       *  chart.setColor('Person', '#abc');
       *  chart.setColor('Person', '#f00');
       *  chart.setColor('Connected', '#00ff00');
       *
       *  chart.update();
       * @param {string} type
       * @param {string} color
       */
      setColor(type: string, color: string): this;
      /**
       * Add pre defined style for nodes.
       * @example add preNode style
       * <pre>
       *
       * var preNodeStyles = [
       *   {
       *     condition: 'type === "p_order" || id === "TID-00000000000000000100" || id.indexOf("User") !== -1',
       *     style: {
       *       radius: 100,
       *     }
       *   },
       *   {
       *     condition: (node, chart) => { return node.exType === "p_order"}',
       *     style: {
       *       fillColor: '#000000',
       *       display: 'rectangle'
       *     }
       *   },
       *   {
       *     condition: 'type === "stocking"',
       *     style: {
       *       radius: 100,
       *       lineColor: '#222222',
       *       lineWidth: 15
       *     }
       *   },
       *   {
       *     condition: 'attrs["cert_type"] !== undefined && attrs["cert_type"].indexOf("07") !== -1',
       *     style: {
       *       radius: 100,
       *       lineColor: '#222222',
       *       lineWidth: 15
       *     }
       *   }
       * ];
       *
       * preNodeStyles.forEach(style => {
       *   chart.addNodePreStyle(style);
       * })
       *
       * </pre>
       *
       * @param {({
       *     condition: string | { (node: GraphChartDataNode, chart: BaseApi): boolean };
       *     style: {
       *       [key: string]: any;
       *     };
       *   })} preStyle
       * @returns {this}
       * @memberof GraphChart
       */
      addNodePreStyle(preStyle: {
          condition: string | {
              (node: GraphChartDataNode, chart: BaseApi): boolean;
          };
          style: {
              [key: string]: any;
          };
      }): this;
      /**
       * Add pre defined style for links.
       *
       * @example Same as `addNodePreStyle`.
       *
       * @param {{
       *     condition: string | { (link: GraphChartDataLink, chart: BaseApi): boolean };
       *     style: {
       *       [key: string]: any;
       *     };
       *   }} preStyle
       * @returns {this}
       */
      addLinkPreStyle(preStyle: {
          condition: string | {
              (link: GraphChartDataLink, chart: BaseApi): boolean;
          };
          style: {
              [key: string]: any;
          };
      }): this;
      /**
       * get nodes by expression conditon.
       *
       * @example Get nodes by expression.
       * <pre>
       * chart.getNodesByExpression('node.attrs["fee"] &gt; "9" && type === "Product"');
       * chart.getNodesByExpression('true');
       * chart.getNodesByExpression('type === "Person"');
       * chart.getNodesByExpression('node.attrs["lastName"] === "Huang"');
       * chart.getNodesByExpression('type === "p_order" || id === "TID-00000000000000000100" || id.indexOf("User") !== -1');
       *
       * </pre>
       *
       * @param {string} condition
       * @returns
       */
      getNodesByExpression(condition: string): any;
      /**
       * get links by expression condition.
       *
       * @example Get links by expression.
       * <pre>
       * chart.getLinksByExpression('link.attrs["fee"] > "9" && type === "Connected"');
       * chart.getLinksByExpression('true');
       * chart.getLinksByExpression('type === "Connected"');
       * chart.getLinksByExpression('link.attrs["value"] === "certainValue"');
       * chart.getLinksByExpression('type === "Connected" || link.source.id === "TID-00000000000000000100" || link.target.id.indexOf("User") !== -1');
       *
       * </pre>
       * @param {string} condition
       * @returns
      () */
      getLinksByExpression(condition: string): any;
      /**
       * Get rendering related info for a node.
       *
       * offsetRadius: the pixel numbers of the radius of the node relative to the container. Affected by css transform.
       * offsetX: the pixel numbers to the left of the container. Affected by css transform.
       * offsetY: the pixel numbers to the top of the container. Affected by css transform.
       * offsetBounds: the pixel coordiates of top-left point and bottom-right point of the bounding in the container.
       * zoom: the zoom level in graphcarht.
       * zindex: zindex in the graph visualization. The hovered node is always shown as MAX zindex.
       *
       * The client offsetLeft and offsetTop can be got as follow:
       * @example Example
       *
       * <pre>
       *
       * var node = chart.getSelection().nodes[0];
       * var shape = chart.getNodeShape(node);
       *
       * var element = chart.container, viewportLeft = shape.offsetX, viewportTop = shape.offsetY;
       * while (element) {
       *     viewportLeft += a.offsetLeft;
       *     viewportTop += a.offsetTop;
       *     element = element.offsetParent;
       * }
       *
       * return {viewportLeft, viewportTop};
       *
       * </pre>
       * @param {GraphChartDataNode} node
       * @returns {{
       *   offsetRadius: number;
       *   offsetX: number;
       *   offsetY: number;
       *   offsetBounds: {
       *     x0: number,
       *     x1: number,
       *     y0: number,
       *     y1: number
       *   };
       *   zoom: number;
       *   zindex: number;
       * }}
       * @memberof GraphChart
       */
      getNodeShape(node: GraphChartDataNode): {
          offsetRadius: number;
          offsetX: number;
          offsetY: number;
          offsetBounds: {
              x0: number;
              x1: number;
              y0: number;
              y1: number;
          };
          zoom: number;
          zindex: number;
      };
      /**
       * Get rendering related info for a link.
       *
       * offsetRadius: the pixel numbers of the radius of the node relative to the container. Affected by css transform. This may not be accurate if the pixel is smaller than 1.
       * offsetPointsX: the x-coordinate of the points for rendering the link relative to the container in pxiels.
       * offsetPointsY: the y-coordinate of the points for rendering the link relative to the container in pxiels.
       * offsetBounds: the pixel coordiates of top-left point and bottom-right point of the bounding in the container.
       * zoom: the zoom level in graphcarht.
       *
       * @param {GraphChartDataLink} link
       * @returns {{
       *     offsetRadius: number;
       *     offsetPointsX: number[];
       *     offsetPointsY: number[];
       *     offsetBounds: {
       *       x0: number,
       *       x1: number,
       *       y0: number,
       *       y1: number
       *     };
       *     zoom: number;
       *   }}
       * @memberof GraphChart
       */
      getLinkShape(link: GraphChartDataLink): {
          offsetRadius: number;
          offsetPointsX: number[];
          offsetPointsY: number[];
          offsetBounds: {
              x0: number;
              x1: number;
              y0: number;
              y1: number;
          };
          zoom: number;
      };
      /**
       * set nodes draggable. Needs call update to apply to the chart.
       * draggable is ture: Draggable.
       * draggable is false: Non-Draggable.
       *
       * @example Disable dragging for selected nodes.
       * <pre>
       *  var nodes = chart.getSelection().nodes;
       *  chart
       *    .setNodesDraggable(nodes, false)
       *    .update();
       *
       * </pre>
       * @param {GraphChartDataNode[]} nodes
       * @param {boolean} [draggable=true]
       * @returns {this}
       * @memberof GraphChart
       */
      setNodesDraggable(nodes: GraphChartDataNode[], draggable?: boolean): this;
      /**
       * set invisible of nodes.
       *
       * @example show a group of nodes. hide a group of nodes.
       * <pre>
       *   // hide selected nodes;
       *   var nodes = chart.getSelection().nodes;
       *   chart.setNodesInvisible(nodes);
       *
       *   // show all invisible nodes
       *    var nodes = chart.getNodes().filter(n => {
       *      return n.styles.isInvisible;
       *    });
       *    chart.setNodesInvisible(nodes, false);
       *
       * </pre>
       *
       * @param {GraphChartDataNode[]} nodes
       * @param {boolean} [invisible=true]
       * @returns {this}
       * @memberof GraphChart
       */
      setNodesInvisible(nodes: GraphChartDataNode[], invisible?: boolean): this;
      /**
       * set invisible of link.
       *
       * @example show a group of nodes. hide a group of links.
       * <pre>
       *   // hide selected links;
       *   var links = chart.getSelection().links;
       *   chart.setLinksInvisible(links);
       *
       *   // show all invisible links
       *    var links = chart.getLinks().filter(l => {
       *      return l.styles.isInvisible;
       *    });
       *    chart.setLinksInvisible(links, false);
       *
       * </pre>
       *
       * @param {GraphChartDataLink[]} links
       * @param {boolean} [invisible=true]
       * @returns {this}
       * @memberof GraphChart
       */
      setLinksInvisible(links: GraphChartDataLink[], invisible?: boolean): this;
      /**
       * Clear graph and add a graph contains random N nodes and 2*N links for performance test.
       * @param {[type]} NumOfNodes = 500 [description]
       */
      runStressTest(NumOfNodes?: number): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: graph chart data class.
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface ExternalGraph {
      nodes: ExternalNode[];
      links: ExternalLink[];
  }
  /**
   * Graph data structure for graph chart.
   * @preferred
   */
  export class GraphChartData extends ItemsData {
      private chart;
      /**
       * Internal graph data object.
       * @type {GraphDataInternalData}
       */
      private inData;
      constructor(parent?: BaseApi);
      /**
       * Initial data object and internal index.
       * @return {boolean}
       */
      private initData();
      /**
       * Reset interal data object, then load new graph.
       * @param  {[type]} externalGraph external node array and link array.
       * @param  {any =             {}}        options [description]
       * @return {this}                 [description]
       */
      resetGraph(externalGraph: ExternalGraph, options?: any): this;
      /**
       * reload new graph. Keep using the previous location (x, y) of nodes.
       * @param  {[type]} externalGraph external node array and link array.
       * @param  {any =             {}}        options [description]
       * @return {this}                 [description]
       */
      reloadGraph(externalGraph: ExternalGraph, options?: any): this;
      /**
       * Add a new graph in current internal graph object.
       * @param  {ExternalGraph} externalGraph [description]
       * @param  {any        =             {}}        options [description]
       * @return {this}                        [description]
       */
      addGraph(externalGraph: ExternalGraph, options?: {
          node: string;
          link: AddLinkOption;
      }): this;
      /**
       * Drop a sub graph.
       *
       * @param {ExternalGraph} externalGraph
       * @returns {this}
       *
       * @memberof GraphChartData
       */
      dropGraph(externalGraph: ExternalGraph): this;
      /**
       * Add node base on option. Check out the utils module for detailed information for option.
       * Example for adding an existing node : two nodes A and B. [option](A, B)
       * defaults : Merge A and B, if there are same field, use values from A;
       * extend : Merge A and B, if there are same field, use values from B;
       * overwrite : Use values from B overwrite the same fields of A;
       *
       * @param  {ExternalNode}       node   [description]
       * @param  {[type]}             option =             'extend' defaults | extend | overwrite
       * @return {GraphChartDataNode}        [description]
       */
      addNode(node: ExternalNode, option?: string): GraphChartDataNode;
      /**
       * Example for adding an existing link : two nodes A and B. [option](A, B)
       * defaults : Merge A and B, if there are same field, use values from A;
       * extend : Merge A and B, if there are same field, use values from B;
       * overwrite : Use values from B overwrite the same fields of A;
       *
       * @param  {ExternalLink}       link   external link object
       * @param  {AddLinkOption}             option link/node:  defaults | extend | overwrite
       * @return {GraphChartDataLink}        [description]
       */
      addLink(link: ExternalLink, option?: AddLinkOption): GraphChartDataLink;
      getInternalData(): GraphDataInternalData;
      getData(): {
          nodes: GraphChartDataNode | GraphChartDataNode[];
          links: GraphChartDataLink | GraphChartDataLink[];
      };
      getNodes(exType?: string, exID?: string): GraphChartDataNode | GraphChartDataNode[];
      getLinks(exType?: string, sourceExType?: string, sourceExID?: string, targetExType?: string, targetExID?: string): GraphChartDataLink | GraphChartDataLink[];
      /**
       * Drop node API.
       * @param  {string}               exType [description]
       * @param  {string}               exID   [description]
       * @param  {[type]}               option =             false [description]
       * @return {DroppedItems}        [description]
       */
      dropNode(exType: string, exID: string, option?: boolean): DroppedItems;
      /**
       * Drop a link by external source, target, type.
       * @param  {string}               exType       [description]
       * @param  {string}               sourceExType [description]
       * @param  {string}               sourceExID   [description]
       * @param  {string}               targetExType [description]
       * @param  {string}               targetExID   [description]
       * @param  {boolean}              option       while dropping links, option is used to determine whether or not we drop isolated nodes.
       * - true: we don't remove any nodes from graph while dropping links.
       * - false: we do remove source and target nodes of this link, if the node is isloated. And use the same option for dropping other related nodes and links.
       * @return {DroppedItems}              [description]
       */
      dropLink(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string, option?: boolean): DroppedItems;
      /**
       * Drop all nodes and links of the internal graph data object. But it will keep the idSet. It is different with initData().
       * @return {DroppedItems} [description]
       */
      dropAll(): DroppedItems;
      getNeighbors(exType: string, exID: string): {
          all: GraphChartDataNode[];
          in: GraphChartDataNode[];
          out: GraphChartDataNode[];
      };
      setSelection(selected: (GraphChartDataNode | GraphChartDataLink)[]): this;
      getSelectedNodes(): GraphChartDataNode[];
      /**
       * return selected Links and both source and target selected links.
       * @return {GraphChartDataLink[]} [description]
       */
      getSelectedLinks(): GraphChartDataLink[];
      getNodeLegends(): string[];
      getLinkLegends(): string[];
      getNodeAttributes(): {
          type: string;
          icon: any;
          color: string;
          labels: {
              attributeName: string;
              isSelected: boolean;
          }[];
      }[];
      getLinkAttributes(): {
          type: string;
          color: string;
          labels: {
              attributeName: string;
              isSelected: boolean;
          }[];
      }[];
      /**
       * evaluate node by expression. The condition supports the javascript syntax.
       *
       * @param {GraphChartDataNode} inNode
       * @param {(string | { (node: GraphChartDataNode, chart: BaseApi): boolean })} condition
       * @returns {boolean}
       * @memberof GraphChartData
       */
      evaluateNodeExpression(inNode: GraphChartDataNode, condition: string | {
          (node: GraphChartDataNode, chart: BaseApi): boolean;
      }): boolean;
      /**
       * apply a style config for a node data object.
       * @param  {GraphChartDataNode} inNode [description]
       * @param  {any}                style  the external style config.
       * @return {this}                      [description]
       */
      applyNodeStyle(inNode: GraphChartDataNode, style: any): this;
      /**
       * Evaluate link by external expression. The condition supports javascript syntax.
       *
       *
       * @param {GraphChartDataLink} inLink
       * @param {(string | { (link: GraphChartDataLink, chart: BaseApi): boolean })} condition
       * @returns {boolean}
       * @memberof GraphChartData
       */
      evaluateLinkExpression(inLink: GraphChartDataLink, condition: string | {
          (link: GraphChartDataLink, chart: BaseApi): boolean;
      }): boolean;
      /**
       * apply a style config for a link data object.
       * @param  {GraphChartDataLink} inLink [description]
       * @param  {any}                style  the external style config
       * @return {this}                      [description]
       */
      applyLinkStyle(inLink: GraphChartDataLink, style: any): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: graph chart id mapping class.
   *
   * Created on: Oct 10th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  /**
   * ID set class for mapping external ID+Type to internal ID for graph node/link.
   */
  export class GraphDataIDSet {
      private static nodeIDConcChar;
      private static linkIDConcChar;
      /**
       * This field is used for generating internal id index for external node id.
       * @type {number}
       */
      private nodesIDCount;
      /**
       * This field is use for generating internal type index for external node type.
       * @type {number}
       */
      private nodesTypeCount;
      /**
       * This field is used for maping link type from external type to internal type index.
       * @type {number}
       */
      private linksTypeCount;
      /**
       * Internal and external id mapping object. Id translation is using this object.
       */
      private idMap;
      constructor();
      /**
       * register an external node in id map for internal node id.
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {string}        [description]
       */
      registerExternalNode(exType: string, exID: string): string;
      /**
       * Check if a external node is mapped in idMap.
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {string}        if exist, return internalID, else return undefined;
       */
      checkExternalNode(exType: string, exID: string): string;
      registerExternalLink(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string): string;
      checkExternalLink(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string): string;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: graph chart internal graph data class.
   *
   * Created on: Oct 10th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface DroppedItems {
      target?: GraphChartDataNode | GraphChartDataLink;
      nodes: GraphChartDataNode[];
      links: GraphChartDataLink[];
  }
  /**
   * Internal graph data object.
   */
  export class GraphDataInternalData {
      /**
       * Mapper for external id+type to internal id for node and link.
       * @type {GraphDataIDSet}
       */
      private idSet;
      /**
       * The main data array for storing node and link objects.
       */
      array: {
          nodes: GraphChartDataNode[];
          links: GraphChartDataLink[];
      };
      /**
       * Internal indices for nodes and links in graph.
       */
      index: {
          nodes: {
              [nodeInternalID: string]: GraphChartDataNode;
          };
          links: {
              [linkInternalID: string]: GraphChartDataLink;
          };
      };
      /**
       * These indices refer from node to their neighboring nodes and related links.
       */
      neighbors: {
          in: {
              [toID: string]: {
                  [fromID: string]: {
                      [linkID: string]: GraphChartDataLink;
                  };
              };
          };
          out: {
              [fromID: string]: {
                  [toID: string]: {
                      [linkID: string]: GraphChartDataLink;
                  };
              };
          };
          all: {
              [nodeID: string]: {
                  [nodeID: string]: {
                      [linkID: string]: GraphChartDataLink;
                  };
              };
          };
          inCount: {
              [toID: string]: number;
          };
          outCount: {
              [fromID: string]: number;
          };
          allCount: {
              [nodeID: string]: number;
          };
      };
      constructor();
      private init();
      /**
       * inject a new link in internal graph data.
       * - b. If the new link is not exist.
       *   - b1. Add link to index.
       *   - b2. Add link to array.
       *   - b3. Update neighbors.
       * @param  {ExternalLink}       link [description]
       * @return {GraphChartDataLink}      [description]
       */
      private injectNewLink(link);
      getIDSet(): GraphDataIDSet;
      /**
       * @param  {string} inID
       * @return {boolean}
       */
      isNodeExist(inID: string): boolean;
      /**
       * get nodes by a array of internal id.
       * @param  {string[]}             ids [description]
       * @return {GraphChartDataNode[]}     [description]
       */
      getNodes(ids: string[]): GraphChartDataNode[];
      getNodeByInternalID(inID: string): GraphChartDataNode;
      getNodeByExternalTypeID(type: string, id: string): GraphChartDataNode;
      /**
       * Add an external node in internal data object.
       * Example for adding an existing node: two nodes A and B. [option](A, B)
       * defaults : Merge A and B, if there are same field, use values from A;
       * extend : Merge A and B, if there are same field, use values from B;
       * overwrite : Use values from B overwrite the same fields of A;
       *
       * 1. IdSet mapping.
       * 2. Handle the case that node is in aggregated node.
       * 3. Add node in data.
       * - 1. add node in index;
       * - 2. add node in array;
       * - 3. add node in neighbors;
       * @param  {ExternalNode}       node   External Node
       * @param  {string}             option Option for adding node. defaults | extend | overwrite
       * @return {GraphChartDataNode}        Added the internal GraphChartDataNode object.
       */
      addNode(node: ExternalNode, option: string): GraphChartDataNode;
      /**
       * Drop a node by internal id from the internal data object.
       * @param  {string}             id    internal id.
       * @param  {boolean}            option  ture: drop isolated nodes, false: do not drop isolated nodes.
       * dropping node will drop related links. While we drop links, we use option
       * to determine whether or not we drop the isolated nodes.
       * @return {DroppedItems}         The nodes dropped. The first one is the target dropping node.
       */
      dropNode(id: string, option: boolean): DroppedItems;
      /**
       * @param  {string} inID internalID of the target link
       * @return {boolean}
       */
      isLinkExist(inID: string): boolean;
      getLinks(ids: string[]): GraphChartDataLink[];
      getLinkByInternalID(inID: string): GraphChartDataLink;
      getLinkByExternalTypeID(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string): GraphChartDataLink;
      /**
       * Add an external link in internal data object.
       *  1. Check existing of the new link in aggregated node.
       *  - a. If the link belongs to aggregated node, then update aggregated node contained link list, and create virtual link.
       *    - a1 : If link.source and link.target are both in aggregated nodes.
       *    - a2 : If link.source or link.target is in aggregated node.
       *    - a3 : If neighber of source or target is in aggregated node.
       *
       *  2. Add non-existed source/target node base on option.node.
       *  3. Add link to index. Needs to check reverse link for direceted/undirected links.
       *  4. if link is a new link, add it to array.
       *  - a. Update neighbors for target and source nodes.
       *
       * @param  {ExternalLink}       link   [description]
       * @param  {AddLinkOption}             option { link: defaults | extend | overwrite, node: defaults | extend | overwrite}
       * @return {GraphChartDataLink}        [description]
       */
      addLink(link: ExternalLink, option: AddLinkOption): GraphChartDataLink;
      /**
       * Drop a link by internal id.
       * @param  {string}               id internal id.
       * @param  {boolean}              option       while dropping links, option is used to determine whether or not we drop isolated nodes.
       * true: we don't remove any nodes from graph while dropping links.
       * false: we do remove source and target nodes of this link, if the node is isloated. And use the same option for dropping other related nodes and links.
       * @return {DroppedItems}              [description]
       */
      dropLink(id: string, option: boolean): DroppedItems;
      getNeighbors(id: string): {
          all: GraphChartDataNode[];
          in: GraphChartDataNode[];
          out: GraphChartDataNode[];
      };
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Define link object for graph chart.
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface ExternalLink {
      source: {
          id: string;
          type: string;
      };
      target: {
          id: string;
          type: string;
      };
      type: string;
      directed?: boolean;
      attrs?: {
          [key: string]: string | number | boolean | Object;
      };
      styles?: {
          [key: string]: string | number | Object;
      };
      others?: any;
      labels?: any;
  }
  /**
   * AddLinkOption interface.
   * It is used to determine how handle the nodes and link for a added link.
   * `defaults` | `extend` | `overwrite`
   * If a node or link exists:
   * `defaults`: keep using old values for any existed field, use new values for non existed field
   * `extend`: Merge new values with old values, prefer using new values for duplicated fields.
   * `overwrite`: descard all old values. Use new values instead.
   */
  export interface AddLinkOption {
      link?: string;
      node?: string;
  }
  /**
   * Graph chart link object.
   * @preferred
   */
  export class GraphChartDataLink extends ItemsDataLink {
      /**
       * source node object;
       * @type {GraphChartDataNode}
       */
      source: GraphChartDataNode;
      /**
       * target node object;
       * @type {GraphChartDataNode}
       */
      target: GraphChartDataNode;
      /**
       * external link type
       * @type {string}
       */
      exType: string;
      /**
       * Indicate directed or undirected type link;
       * @type {boolean}
       */
      directed: boolean;
      /**
       * Style object for graph link object.
       */
      styles: GraphStyle.GraphChartLinkStyles;
      /**
       * label object to determine which label needs to be shown in visualization.
       */
      labels: {
          [attrName: string]: boolean;
      };
      private pointsX;
      readonly x: number[];
      private pointsY;
      readonly y: number[];
      /**
       * This field is used for storing middle reuslt from several algorithms which need to be shown in visualization or affect layout.
       * @type {any}
       */
      others: any;
      isLink: boolean;
      /**
       * An internal link object.
       * @param {string}             internalID internalID by registerExternalLink;
       * @param {ExternalLink}       exLink     external link data object
       * @param {GraphChartDataNode} source     internal source node object
       * @param {GraphChartDataNode} target     internal target node object
       */
      constructor(internalID: string, exLink: ExternalLink, source: GraphChartDataNode, target: GraphChartDataNode);
      /**
       * Merge external node. Prefer external node values.
       * @param  {ExternalNode} exNode [description]
       * @return {this}                [description]
       */
      extendLink(exLink: ExternalLink): this;
      /**
       * Merge external Link. Prefer old Link values.
       * @param  {ExternalLink} exLink [description]
       * @return {this}                [description]
       */
      defaultsLink(exLink: ExternalLink): this;
      /**
       * Assign external Link values to old Links.
       * @param  {ExternalLink} exLink [description]
       * @return {this}                [description]
       */
      overwriteLink(exLink: ExternalLink): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Define node data object class for graph chart.
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface ExternalNode {
      id: string;
      type: string;
      attrs?: {
          [key: string]: string | number | boolean | Object;
      };
      styles?: {
          [key: string]: string | number | Object;
      };
      others?: any;
      labels?: any;
      x?: number;
      y?: number;
  }
  /**
   * Graph chrt node object.
   * @preferred
   */
  export class GraphChartDataNode extends ItemsDataNode {
      /**
       * node external id. It is mapped to internal id index. id index + type index = interal id
       * @type {string}
       */
      exID: string;
      /**
       * node external type. It is mapped to internal type index.
       * @type {string}
       */
      exType: string;
      /**
       * Style object for graph node object.
       */
      attrs: {
          [attrName: string]: number | string | boolean | Object;
      };
      /**
       * Style object for graph node object.
       */
      styles: GraphStyle.GraphChartNodeStyles;
      /**
       * label object to determine which label needs to be shown in visualization.
       */
      labels: {
          [attrName: string]: boolean;
      };
      /**
       * External x value.
       * @type {number}
       */
      exX: number;
      /**
       * Exteranl y value.
       * @type {number}
       */
      exY: number;
      /**
       * Layout x position
       * @type {number}
       */
      x: number;
      /**
       * Layout y position
       * @type {number}
       */
      y: number;
      /**
       * This field is used for storing middle reuslt from several algorithms which need to be shown in visualization or affect layout.
       * @type {any}
       */
      others: any;
      isNode: boolean;
      constructor(internalID: string, exNode: ExternalNode);
      /**
       * Merge external node. Prefer external node values.
       * @param  {ExternalNode} exNode [description]
       * @return {this}                [description]
       */
      extendNode(exNode: ExternalNode): this;
      /**
       * Merge external node. Prefer old node values.
       * @param  {ExternalNode} exNode [description]
       * @return {this}                [description]
       */
      defaultsNode(exNode: ExternalNode): this;
      /**
       * Assign external node values to old nodes.
       * @param  {ExternalNode} exNode [description]
       * @return {this}                [description]
       */
      overwriteNode(exNode: ExternalNode): this;
  }
  
  export class GraphChartEvent {
      private chart;
      private defaultHandlers;
      handlers: {
          onKeyPress: {
              (nodes: GraphChartDataNode[], links: GraphChartDataLink[], event: BaseKeyboardEvent, chart: BaseApi): void;
          };
          onChartUpdate: {
              (chart: BaseApi): void;
          };
          onDataUpdated: {
              (chart: BaseApi): void;
          };
          onSelectionChange: {
              (nodes: GraphChartDataNode[], links: GraphChartDataLink[], chart: BaseApi): void;
          };
          onClick: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onDoubleClick: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onHoverChange: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onPositionChange: {
              (event: BaseMouseEvent, chart: BaseApi): void;
          };
          onRightClick: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onPointerDown: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onPointerUp: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onPointerDrag: {
              (item: GraphChartDataNode | GraphChartDataLink, event: BaseMouseEvent, chart: BaseApi): void;
          };
          onExceedLimit: {
              (nodes: GraphChartDataNode[], links: GraphChartDataLink[], chart: BaseApi): void;
          };
          [event: string]: any;
      };
      constructor(parent?: BaseApi);
      private setDefaultHandlers();
      /**
       * Set an event handler base on name.
       * @param  {string} eventName [description]
       * @param  {void}} callback  [description]
       * @return {this}             [description]
       */
      setEventHandler(eventName: string, callback: {
          (...args: any[]): void;
      }): this;
      /**
       * Set an event to use the default handler.
       * @param  {string} eventName [description]
       * @return {this}             [description]
       */
      defaultEventHandler(eventName: string): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS library layout module class.
   *
   * Created on: Oct 25th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class GraphChartLayout {
      private chart;
      private currentNodeRadius;
      private treeLayoutHorizontalScale;
      private treeLayoutVerticalScale;
      currentLayout: string;
      private isLockedUpdateAsyncPositionOfNodesLock;
      private defaultLayouts;
      private layouts;
      /**
       * Root node array.
       * The first node in array will be used as root node in single root layout.
       * @type {GraphChartDataNode[]}
       */
      private roots;
      constructor(parent: BaseApi);
      /**
       * Scale tree layout.
       *
       * @param {number} horizontalScale
       * @param {number} verticalScale
       * @memberof GraphChartLayout
       */
      scaleTreeLayout(horizontalScale: number, verticalScale: number): void;
      private setDefaultLayout();
      private createBFSTreeArray(options);
      private iterateTree(parents, result, nodes, graph);
      /**
       * Update the inNode x and x position from core node.
       */
      updateSyncPositionOfNodes(): this;
      updateSyncPositionOfNodeByInternalID(id: string): this;
      updateSyncPositionOfLinkByInternalID(id: string): this;
      /**
       * Since layouts are async process. Need to call the function to update the position for layout.
       * @param {number} delay million second for get updated position of each nodes.
       * @return {this} [description]
       */
      updateAsyncPositionOfNodes(delay?: number): this;
      clear(): this;
      /**
       * It will be mainly used for reset the dynamic layout, since in some case, it takes too long to be converged.
       *
       * @returns {this}
       *
       * @memberOf GraphChartLayout
       */
      resetLayout(): this;
      runLayout(layoutName?: string): this;
      setLayout(layoutName: string): this;
      addLayout(layoutName: string, callback: {
          (graph: {
              nodes: GraphChartDataNode[];
              links: GraphChartDataLink[];
          }, chart: BaseApi): void;
      }): this;
      overwriteLayout(layoutName: string, callback: {
          (graph: {
              nodes: GraphChartDataNode[];
              links: GraphChartDataLink[];
          }, chart: BaseApi): void;
      }): this;
      setRoot(exType: string, exID: string): this;
      /**
       * get single root node.
       * @return {GraphChartDataNode} [description]
       */
      getRoot(): GraphChartDataNode;
      /**
       * Get all root nodes.
       * @return {GraphChartDataNode[]} [description]
       */
      getRoots(): GraphChartDataNode[];
      /**
       * add a root node in root node array.
       * @param  {string} exType [description]
       * @param  {string} exID   [description]
       * @return {this}          [description]
       */
      addRoot(exType: string, exID: string): this;
      /**
       * Return current layout name.
       */
      getLayouts(): string[];
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS canvas rendering engine module class.
   *
   * Created on: Oct 14th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class GraphChartRenderCanvas extends GraphChartRenderEngineBase {
      private chart;
      core: any;
      color: Color;
      icons: ICON;
      private zoom;
      private otherOptions;
      private canvasOptions;
      constructor(options: any, chart: BaseApi);
      private initOptions(options);
      /**
       * Initalize event handler for rendering engine.
       */
      private initEventHandler();
      private initOtherOptions(options);
      getCore(): any;
      replaceData(internalData: any): this;
      addFocusNode(id: string): this;
      clearFocus(): this;
      collapseNode(id: string): this;
      expandNode(id: string): this;
      private initNodeLabelSetting(node);
      private initNodeRenderSetting(node);
      private initNodeItemsSetting(node);
      private initLinkLabelSetting(link);
      private initLinkRenderSetting(link);
      private initLinkItemsSetting(link);
      private nodeToolTipSetting(inNode, node, chart);
      private linkToolTipSetting(inLink, link, chart);
      private nodeMenuContentsFunction(data, coreNode?, targetOption?, defaultContentsFunction?);
      private linkMenuContentsFunction(data, coreLink?, targetOption?, defaultContentsFunction?);
      updateAreaSettings(settings: any): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS library render module class.
   *
   * Created on: Oct 14th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class GraphChartRender {
      private chart;
      engine: GraphChartRenderEngineBase;
      private zoomLevel;
      private exportType;
      private maxNodeLimit;
      private maxLinkLimit;
      private defaultMaxNodeLimit;
      private defaultMaxLinkLimit;
      private preStyle;
      /** built in icons mapping. This will be extend by outside options. */
      private icons;
      private defaultOptions;
      style: BaseStyle;
      constructor(parent?: BaseApi);
      /**
       * Does immediate repaint.
       *
       * @returns {this}
       * @memberof GraphChartRender
       */
      paint(): this;
      /**
       * render graph.
       * @return {this} [description]
       */
      update(): this;
      updateCoordinate(): this;
      /**
       * reset layout and redarw everything in the viewport.
       * @return {this} [description]
       */
      redraw(): this;
      closeNode(exType: string, exID: string): void;
      collapseNode(exType: string, exID: string): void;
      expandNode(exType: string, exID: string): void;
      hideNode(exType: string, exID: string): void;
      showNode(exType: string, exID: string): void;
      setNodesInvisible(nodes: GraphChartDataNode[], invisible?: boolean): this;
      setLinksInvisible(links: GraphChartDataLink[], invisible?: boolean): this;
      zoom(level: number): number;
      zoomIn(): this;
      zoomOut(): this;
      fullscreen(isFullscreen: boolean): this;
      autoFit(): this;
      /**
       * center current visualization base on selected node. We scroll in to the view of all the selected nodes, and their 1-step neighborhood. This logic can be changed.
       * @return {this} [description]
       */
      centerView(): this;
      export(name: string, type: string): this;
      private downloadFile(filename, text);
      /**
       * Unselect all elements in visualization.
       * @return {this} [description]
       */
      unselectAllElements(): this;
      showNodeLabels(exType: string, exID: string, labels: string[]): this;
      showNodeLabelsByType(exType: string, labels: string[]): this;
      hideNodeLabels(exType: string, exID: string, labels: string[]): this;
      hideNodeLabelsByType(exType: string, labels: string[]): this;
      hideAllNodeLabels(): this;
      showLinkLabels(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string, labels: string[]): this;
      showLinkLabelsByType(exType: string, labels: string[]): this;
      hideLinkLabels(exType: string, sourceExType: string, sourceExID: string, targetExType: string, targetExID: string, labels: string[]): this;
      hideLinkLabelsByType(exType: string, labels: string[]): this;
      hideAllLinkLabels(): this;
      updateAreaSettings(options: any): this;
      hideMenu(): this;
      updateSize(): this;
      setIcon(type: string, icon: string): void;
      addPreStyle(style: {
          target: string;
          obj: {
              condition: string | {
                  (node: GraphChartDataNode, chart: BaseApi): boolean;
              } | {
                  (link: GraphChartDataLink, chart: BaseApi): boolean;
              };
              style: {
                  [key: string]: any;
              };
          };
      }): void;
      /**
       * Fixates a node in place.
       *
       * @param {string} id
       * @param {number} x
       * @param {number} y
       * @returns {this}
       * @memberof GraphChartRender
       */
      lockNode(id: string, x: number, y: number): this;
      /**
       * Unfixates a node and allows it to be repositioned by the layout algorithms.
       *
       * @param {string} id
       * @returns {this}
       * @memberof GraphChartRender
       */
      unlockNode(id: string): this;
      /**
       * Return the shape related information of a target node.
       *
       * @param {string} id
       * @returns {{
       *   offsetRadius: number;
       *   offsetX: number;
       *   offsetY: number;
       *   offsetBounds: {
       *     x0: number,
       *     x1: number,
       *     y0: number,
       *     y1: number
       *   };
       *   zoom: number;
       *   zindex: number;
       * }}
       * @memberof GraphChartRender
       */
      getNodeShape(id: string): {
          offsetRadius: number;
          offsetX: number;
          offsetY: number;
          offsetBounds: {
              x0: number;
              x1: number;
              y0: number;
              y1: number;
          };
          zoom: number;
          zindex: number;
      };
      /**
       * Return the shape related information of a target link.
       *
       * @param {string} id
       * @returns {{
       *     offsetRadius: number;
       *     offsetPointsX: number;
       *     offsetPointsY: number;
       *     offsetBounds: {
       *       x0: number,
       *       x1: number,
       *       y0: number,
       *       y1: number
       *     };
       *     zoom: number;
       *   }}
       * @memberof GraphChartRender
       */
      getLinkShape(id: string): {
          offsetRadius: number;
          offsetPointsX: number[];
          offsetPointsY: number[];
          offsetBounds: {
              x0: number;
              x1: number;
              y0: number;
              y1: number;
          };
          zoom: number;
      };
      scrollIntoView(ids: string[]): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS library render engine base class.
   *
   * Created on: Oct 12th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  export abstract class GraphChartRenderEngineBase {
      renderType: string;
      core: any;
      color: any;
      constructor(type: string);
      replaceData(data: any): this;
      getCore(): any;
      updateAreaSettings(settings: any): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2018, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS geo render module class.
   *
   * Created on: Mar 28th, 2018
   *      Author: Renchu Song
   ******************************************************************************/
  
  export interface GeoInternalElement {
      attrs: {
          [attrName: string]: any;
      };
      icon?: string;
      id: string;
      exType: string;
      imageSlicing: [number, number, number, number];
      opacity: number;
      scaleWithZoom: boolean;
      showAtZoom: number;
      hideAtZoom: number;
  }
  export interface GeoInternalNode extends GeoInternalElement {
      exId: string;
      display: string;
      fillColor: string;
      bounds: {
          x1: number;
          x0: number;
          y1: number;
          y0: number;
      };
      aspectRatio: number;
      radius: number;
      links: GeoInternalLink[];
      x: number;
      y: number;
  }
  export interface GeoInternalLink extends GeoInternalElement {
      lineColor: string;
      lineWidth: number;
      from: GeoInternalNode;
      to: GeoInternalNode;
      directed: boolean;
      iconRadius: number;
      arrowSize: number;
  }
  export interface HeatPoint {
      x: number;
      y: number;
      weight?: number;
  }
  export class GeoCore {
      geoMap: any;
      heatmapLayer: any;
      contourLineLayer: any;
      graphLayer: any;
      icons: ICON;
      private heatmapData;
      private heatPointDefaultRadius;
      private heatPointDefaultWeight;
      private contourXRange;
      private contourYRange;
      private contourValues;
      private thresholds;
      private fillRange;
      private colors;
      private range;
      private internalNodesMap;
      private internalLinksMap;
      private internalNodesArray;
      private internalLinksArray;
      private distanceBetweenEdgeArrows;
      private showNodeTooltip;
      private showLinkTooltip;
      private showNodeLabel;
      private showLinkLabel;
      private nodeCallbackFunctions;
      private linkCallbackFunctions;
      private nodeToolTipCallback;
      private linkToolTipCallback;
      private nodeLabelCallback;
      private linkLabelCallback;
      private dragCallback;
      zoom(zoomLevel?: number): void;
      resetLayout(): void;
      updateSettings(options?: any): void;
      /**
       * Paint the heatmap, contour line layer and graph correspondingly.
       *
       * @memberof GeoCore
       */
      paintNow(): void;
      /**
       * Update the dafault configs of heatmap layer.
       *
       * @param {number} defaultRadius
       * @param {number} defaultWeight
       * @memberof GeoCore
       */
      updateHeatmapConfigs(defaultRadius: number, defaultWeight: number): void;
      /**
       * Update the heatmap data.
       *
       * @param {HeatPoint[]} [heatMap]
       * @memberof GeoCore
       */
      updateHeatmapData(heatMap?: HeatPoint[]): void;
      /**
       * Paint the heatmap layer.
       *
       * @memberof GeoCore
       */
      paintHeatMap(): void;
      /**
       * Update the depicting configs of contour line layer.
       *
       * @param {boolean} fillRange
       * @memberof GeoCore
       */
      updateContourLineConfigs(fillRange: boolean): void;
      /**
       * Update the contour lines data.
       *
       * @param {number[][]} [contourData]
       * @param {number[]} [thresholds]
       * @param {ContourColor[]} [colors]
       * @param {{ x0: number, y0: number, x1: number, y1: number }} [range]
       * @returns
       * @memberof GeoCore
       */
      updateContourLineData(contourData?: number[][], thresholds?: number[], colors?: string[], range?: {
          x0: number;
          y0: number;
          x1: number;
          y1: number;
      }): void;
      /**
       * Paint the contour line layer.
       *
       * @memberof GeoCore
       */
      paintContourLines(): void;
      /**
       * Update graph rendering configs.
       *
       * @param {{
       *     distanceBetweenEdgeArrows?: number,
       *     showNodeTooltip?: boolean,
       *     showLinkTooltip?: boolean,
       *     nodeToolTipCallback?: (node: GeoInternalNode) => string,
       *     linkToolTipCallback?: (link: GeoInternalLink) => string,
       *     showNodeLabel?: boolean,
       *     showLinkLabel?: boolean,
       *     nodeLabelCallback?: (node: GeoInternalNode) => string,
       *     linkLabelCallback?: (link: GeoInternalLink) => string
       *   }} config
       * @memberof GeoCore
       */
      updateGraphConfigs(config: {
          distanceBetweenEdgeArrows?: number;
          showNodeTooltip?: boolean;
          showLinkTooltip?: boolean;
          nodeToolTipCallback?: (node: GeoInternalNode) => string;
          linkToolTipCallback?: (link: GeoInternalLink) => string;
          showNodeLabel?: boolean;
          showLinkLabel?: boolean;
          nodeLabelCallback?: (node: GeoInternalNode) => string;
          linkLabelCallback?: (link: GeoInternalLink) => string;
          dragCallback?: (lat: number, long: number) => void;
      }): void;
      addNodeListener(eventType: string, callback: (node: GeoInternalNode) => any): void;
      addLinkListener(eventType: string, callback: (link: GeoInternalLink) => any): void;
      nodes(): GeoInternalNode[];
      getNode(id: string): GeoInternalNode;
      links(): GeoInternalLink[];
      getLink(id: string): GeoInternalLink;
      replaceData(internalData: {
          nodes: GraphChartDataNode[];
          links: GraphChartDataLink[];
      }): void;
      circle: {
          latitude: number;
          longitude: number;
          radius: number;
          iconString: string;
          iconSize: number;
      };
      /**
       * Paint the graph layer.
       *
       * @memberof GeoCore
       */
      paintGraph(circle?: {
          latitude: number;
          longitude: number;
          radius: number;
          iconString: string;
          iconSize: number;
      }): void;
      /**
       * Rotate vector for radian.
       *
       * @private
       * @param {[number, number]} vec
       * @param {number} rad
       * @returns {[number, number]}
       * @memberof GeoCore
       */
      private rotate(vec, rad);
  }
  export class GraphChartRenderGeo extends GraphChartRenderEngineBase {
      private chart;
      core: GeoCore;
      color: Color;
      icons: ICON;
      private zoom;
      private canvasOptions;
      constructor(options: any, chart: BaseApi);
      private initOptions(options);
      /**
       * Initalize event handler for rendering engine.
       */
      private initEventHandler();
      getCore(): any;
      collapseNode(id: string): this;
      expandNode(id: string): this;
      private initNodeLabelSetting(node);
      private initNodeRenderSetting(node);
      private initNodeItemsSetting(node);
      private initLinkLabelSetting(link);
      private initLinkItemsSetting(link);
      private nodeToolTipSetting(inNode, node, chart);
      private linkToolTipSetting(inLink, link, chart);
      private nodeMenuContentsFunction(data, coreNode?, targetOption?, defaultContentsFunction?);
      private linkMenuContentsFunction(data, coreLink?, targetOption?, defaultContentsFunction?);
      updateAreaSettings(settings: any): this;
      replaceData(internalData: any): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS svg render module class.
   *
   * Created on: Oct 12th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class GraphChartRenderSVG extends GraphChartRenderEngineBase {
      constructor(options: any);
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS webgl render module class.
   *
   * Created on: Oct 12th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class GraphChartRenderWebgl extends GraphChartRenderEngineBase {
      constructor(options: any);
  }
  
  /**
  * GVIS library Graph Chart Style Module.
  */
  export module GraphStyle {
      /**
       * Node style interface.
       */
      interface GraphChartNodeStyleInterface {
          /** The ID of one or more auras to which the node belongs. Nodes with the same aura ID will be visually grouped together. */
          aura?: string;
          /** Cursor to show when node is hovered. */
          cursor?: string;
          /** Valid values: circle (default), text, roundtext, droplet, rectangle, customShape */
          display?: string;
          fillColor?: string;
          icon?: string;
          lineColor?: string;
          lineDash?: Array<number>;
          lineWidth?: number;
          /** Node opacity. */
          opacity?: number;
          /** @type {number} Node radius */
          radius?: number;
          shadowBlur?: number;
          shadowColor?: string;
          shadowOffsetX?: number;
          shadowOffsetY?: number;
          draggable?: boolean;
          [more: string]: any;
      }
      /**
       * Link style interface
       */
      interface GraphChartLinkStyleInterface {
          cursor?: string;
          /** null or 'U', 'D', 'L', 'R' */
          direction?: string;
          fillColor?: string;
          length?: number;
          lineDash?: Array<number>;
          /** Specifies the width of the line rendered for this link. */
          radius?: number;
          shadowBlur?: number;
          shadowColor?: string;
          shadowOffsetX?: number;
          shadowOffsetY?: number;
          /** @type {number} specifies the force directed layout. */
          strength?: number;
          [more: string]: any;
      }
      /**
       * Defailt event flag for items(both link and node) in graph chart.
       */
      interface GraphChartItemStyleFlagInterface {
          isInvisible?: boolean;
          isExpanded?: boolean;
          isHovered?: boolean;
          isLowlighted?: boolean;
          isLocked?: boolean;
          isSelected?: boolean;
          [more: string]: any;
      }
      /**
       * visualization area setting.
       * @type {[type]}
       */
      let defualtAreaStyle: any;
      /**
       * default node base style in graph chart
       * @type {GraphChartNodeStyleInterface}
       */
      let defaultNodeBaseStyle: GraphChartNodeStyleInterface;
      /**
       * Detail link base style in graph chart
       * @type {GraphChartLinkStyleInterface}
       */
      let defaultLinkBaseStyle: GraphChartLinkStyleInterface;
      /**
       * The node style object in graph chart.
       */
      class GraphChartNodeStyles implements GraphChartItemStyleFlagInterface {
          base: GraphChartNodeStyleInterface;
          isHovered: boolean;
          hovered: GraphChartNodeStyleInterface;
          isSelected: boolean;
          selected: GraphChartNodeStyleInterface;
          isLowlighted: boolean;
          lowlighted: GraphChartNodeStyleInterface;
          isLocked: boolean;
          locked: GraphChartNodeStyleInterface;
          isExpanded: boolean;
          expanded: GraphChartNodeStyleInterface;
          isInvisible: boolean;
          invisible: GraphChartNodeStyleInterface;
          constructor(newStyles: any);
      }
      /**
       * The link style object for graph chart.
       */
      class GraphChartLinkStyles implements GraphChartItemStyleFlagInterface {
          base: GraphChartLinkStyleInterface;
          isHovered: boolean;
          hovered: GraphChartNodeStyleInterface;
          isSelected: boolean;
          selected: GraphChartNodeStyleInterface;
          isLowlighted: boolean;
          lowlighted: GraphChartNodeStyleInterface;
          isInvisible: boolean;
          invisible: GraphChartNodeStyleInterface;
          constructor(newStyles: any);
      }
      let defaultNodeStyle: GraphChartNodeStyles;
      let defaultLinkStyle: GraphChartLinkStyles;
      let virtualRootNode: {
          id: string;
          exID: string;
          exType: string;
      };
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: All chart item data object class.
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * Data object which can be binded with a visual item.
   * @preferred
   */
  export class ItemsData extends BaseData {
      constructor();
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Define base class of all links object.
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * Base link object.
   * @preferred
   */
  export class ItemsDataLink extends ItemsData {
      /**
       * The unique indentifier of the link
       * @type {string}
       */
      id: string;
      /**
       * id of the source node which is the node where the link originates.
       * @type {string}
       */
      from: string;
      /**
       * id of the target node.
       * @type {string}
       */
      to: string;
      /**
       * Attribute object of the link.
       * @type {Object}
       */
      attrs: {
          [key: string]: number | string | boolean | Object;
      };
      /**
       * visualization style of the link object.
       * @type {Object}
       */
      styles: Object;
      /**
       * This filed can be used to provide multiple classes for styling.
       * Such as className = "classA classB classN";
       * @type {string}
       */
      className: string;
      constructor(internalID: string, sourceID: string, targetID: string);
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Define base class of all nodes object.
   *
   * Created on: Oct 6th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * Base node object.
   * @preferred
   */
  export class ItemsDataNode extends ItemsData {
      /**
       * node id which is the unique identifier of the node;
       * @type {string}
       */
      id: string;
      /**
       * This filed can be used to provide multiple classes for styling.
       * Such as className = "classA classB classN";
       * @type {string}
       */
      className: string;
      /**
       * attribute object for a node data object.
       * @type {Object}
       */
      attrs: Object;
      /**
       * visualization style of the node object.
       * @type {Object}
       */
      styles: Object;
      constructor(internalID: string);
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Line Chart Class
   *
   * Created on: Feb 2nd, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * LineChart Setting typings.
   */
  export interface LineChartSetting extends ChartSetting {
      renderType?: string;
      data?: {
          /**
           * It will be used to control the x axis range. If the "range" is set, the min and max will be disabled. The max will be set implicitly as 'dataMax', the min will be set to the 'dataMax' - range;
           * If min or max is set, it will use the new value as min or max. The default min and max value is 'dataMin' and 'dataMax' which indicates the minimum value and maximum value this axis.
           * This is designed for data zoom bar. If the default is set, it will be used for adding non exist point as the value of y.
           * The two non-existed points will be added, as "range min" and the smallest x value.
           *
           */
          xAxis?: {
              range?: number;
              min?: number;
              max?: number;
              default?: number;
          };
          yAxis?: {
              min?: number;
              max?: number;
          };
          /** @type {number} If sample points are more than maxNumber, the oldest data points will be discard. */
          maxNumber?: number;
      };
      formatter?: {
          /**
           *  xAxis label formatter
           * @param {number} value of the tick.
           * @param {number} index of the tick.
           * @return {string} label of the tick.
           */
          xAxis?: (value: number, index: number) => string;
          /**
           *  yAxis label formatter
           * @param {number} value of the tick.
           * @param {number} index of the tick.
           * @return {string} label of the tick.
           */
          yAxis?: (value: number, index: number) => string;
          tooltip?: (params: {
              componentType: 'series';
              seriesType: string;
              seriesIndex: number;
              seriesName: string;
              name: string;
              dataIndex: number;
              data: Object;
              value: any;
              color: string;
              percent: number;
          }, ticket?: string, callback?: {
              (ticket: string, html: string): void;
          }) => string;
      };
  }
  /**
   * Line Chart Class
   * @preferred
   */
  export class LineChart extends BaseApi {
      render: LineChartRender;
      data: LineChartData;
      event: LineChartEvent;
      /**
       * Render type : 'time' for time series data. 'value' for normal data.
       * @type {LineChartSetting}
       */
      options: LineChartSetting;
      /**
       * Create an instance of LineChart.
       *
       * @example Create an instance of LineChart.
       * <pre>
       *
       *  window.linechart = new gvis.LineChart({
       *    renderType: 'time',
       *    render: {
       *      container: document.getElementById("LineChart1"),
       *      legend: {
       *        show: true
       *      },
       *      textStyle: {
       *        fontStyle: 'italic',
       *        fontWeight: 'bolder',
       *        fontFamily: '"Courier New", Courier, monospace',
       *        fontSize: 12,
       *      },
       *      dataZoom: {
       *          show: true,
       *          top: 0,
       *          bottom: 0,
       *          handleSize: '20%'
       *      },
       *      title: {
       *        text: 'Memory Usage',
       *        subtext: 'Test subtext'
       *      }
       *    },
       *    data: {
       *      xAxis: {
       *        range: 1000 * 60 * 20
       *      },
       *      yAxis: {
       *        min: 0
       *      }
       *    },
       *    formatter: {
       *      tooltip: function(data) {
       *        return 'Customized tooltip base on data';
       *      }
       *    }
       *  });
       *
       * linechart
       *  .addData(testData)
       *  .update();
       * </pre>
       * @param {LineChartSetting} options
       * @memberof LineChart
       */
      constructor(options: LineChartSetting);
      /**
       * Add data points in linechart.
       *
       * @example Add a test dataset in linechart
       *
       * <pre>
       *
       *   window.testData = [];
       *   var testNow = new Date();
       *   var testN = 100;
       *
       *   for (var i=0; i&lt;testN; i++) {
       *       testNow = +new Date(testNow - 1000);
       *
       *       testData.push({
       *         series: 'name',
       *         x: testNow,
       *         y: testN - 2 * i * +Math.random().toFixed(1)
       *       });
       *
       *       testData.push({
       *         series: 'name'+i%2,
       *         x: testNow,
       *         y: testN - 2 * i * +Math.random().toFixed(1)
       *       });
       *   }
       *
       *   linechart
       *   .addData(testData)
       *   .update();
       *
       *   // Dynamiclly add new data points.
       *   window.lineInt = setInterval(() =&gt; {
       *     var testData = [];
       *     testN += 5 * Math.random();
       *
       *     testN = +testN.toFixed(1);
       *
       *     testData.push({
       *       series: 'name',
       *       x: + new Date(),
       *       y: testN
       *     });
       *
       *     testData.push({
       *       series: 'name0',
       *       x: + new Date(),
       *       y: +(testN * (1 + 0.2 * Math.random())).toFixed(1)
       *     });
       *
       *     testData.push({
       *       series: 'name1',
       *       x: + new Date(),
       *       y: +(testN * (1 + 0.2 * Math.random())).toFixed(1)
       *     });
       *
       *     linechart
       *     .addData(testData)
       *     .update();
       *
       *     if (testN > 250) {
       *       clearInterval(window.lineInt);
       *     }
       *   }, 1000);
       * </pre>
       *
       * @param {SeriesDataPoint[]} data
       * @returns {this}
       * @memberof LineChart
       */
      addData(data: SeriesDataPoint[]): this;
      update(): this;
      reloadData(data: SeriesDataPoint[]): this;
      /**
       * Customize an event callback function.
       * Currently supported events:
       *  - `dataZoom`
       *  - `legendselectchanged`
       * The detial of callback function interfaces can be found in linechart.event module.
       *
       * @example binding legendselectchanged event.
       * <pre>
       *   linechart.on('legendselectchanged', function(event, chart) {
       *     console.log(chart === linechart);
       *     console.log(event);
       *   });
       * </pre>
       * @param  {string}  eventName [description]
       * @param  {void }}      callback      [description]
       * @return {this}              [description]
       */
      on(eventName: string, callback: {
          (event: any, chart?: LineChart): void;
      }): this;
      /**
       * Update the linechart settings.
       *
       * @example Update settings for linechart.
       *
       * <pre>
       *  var newOptions = {
       *    [anyKey: string]: any;
       *  };
       *
       *  linechart.updateSettings(newOptions);
       *  linechart.update();
       *
       * </pre>
       *
       * @param {*} [options]
       * @returns {this}
       * @memberof LineChart
       */
      updateSettings(options?: any): this;
      /**
       * Zoom in the line chart by X Axis by start index and end index. The index 0-100 indicates 0% - 100%
       *
       * @example zoom X axis.
       * <pre>
       *  linechart.zoomX(0, 100);
       *  linechart.zoomX(90, 100);
       *  linechart.zoomX(50, 55);
       *  linechart.zoomX(10, 12);
       *  linechart.zoomX(11, 34);
       * </pre>
       * @param  {number} start [description]
       * @param  {number} end   [description]
       * @return {this}         [description]
       */
      zoomX(start: number, end: number): this;
      /**
       * Contrl the legend of line chart. If "select" is undefined, Toggle the target legend. If "select" is true, select target legend. If "select" is false, unselect target legend.
       *
       * @example Togglelegned for linechart2 base on linechart event.
       * <pre>
       *  linechart.on('legendselectchanged', function (event, chart) {
       *    console.log(chart === linechart);
       *    console.log(event);
       *
       *    for (var name in event.selected) {
       *      linechart2.toggleLegend(name, event.selected[name]);
       *    }
       *  });
       * </pre>
       * @param  {string}  series name   [description]
       * @param  {boolean} select [description]
       * @return {this}           [description]
       */
      toggleLegend(name: string, select?: boolean): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Line chart data module class.
   *
   * Created on: Feb 2nd, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface SeriesDataPoint {
      series: string;
      x: number;
      y: number;
      info?: {
          [key: string]: any;
      };
  }
  export class LineChartData {
      private chart;
      private inData;
      private maxNumber;
      constructor(parent: BaseApi);
      /**
       * add data.
       * @param  {SeriesDataPoint} data [description]
       * @return {this}                 [description]
       */
      addData(data: SeriesDataPoint): this;
      /**
       * get data points array by series name.
       * @param  {string}            series [description]
       * @return {SeriesDataPoint[]}        [description]
       */
      getData(series: string): SeriesDataPoint[];
      /**
       * get the series name in string array.
       * @return {string[]} [description]
       */
      getSeries(): string[];
      /**
       * Clean the data by series name.
       * @param  {string}            series [description]
       * @return {SeriesDataPoint[]}        [description]
       */
      cleanSeriesData(series: string): SeriesDataPoint[];
      /**
       * cb It will return the value that use to compare items. 0 is equal. -1 is a > b. 1 is a < b.
       * @param  {SeriesDataPoint} a [description]
       * @param  {SeriesDataPoint} b [description]
       * @return {number}            [description]
       */
      private compare(a, b);
      /**
       * It returns the max value of x in all series. It will be used for the feature of the x axis range.
       * @return {number} [description]
       */
      getXMax(): number;
      /**
       * It returns the min value of x in all series. It will be used for generate xAxis range.
       * @return {number} [description]
       */
      getXMin(): number;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Line chart event module class.
   *
   * Created on: Feb 10th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface LineChartEventHandlers {
      dataZoom: {
          (event: {
              type: 'datazoom';
              start: number;
              end: number;
              startValue?: number;
              endValue?: number;
          }, chart: BaseApi): string | void;
      };
      legendselectchanged: {
          (event: {
              type: 'legendselectchanged';
              name: string;
              selected: Object;
          }, chart: BaseApi): string | void;
      };
      [events: string]: {
          (event: any, chart: BaseApi): string | void;
      };
  }
  export class LineChartEvent {
      private chart;
      private defaultHandlers;
      handlers: LineChartEventHandlers;
      constructor(parent: BaseApi);
      getEventsName(): string[];
      /**
       * get event callback instance by event Name;
       * @param {BaseApi} eventName [description]
       */
      getEventHandler(eventName: string): {
          (event: any, chart?: BaseApi): void;
      };
      setEventHandler(eventName: string, callback: {
          (event: any, chart?: BaseApi): void;
      }): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Line chart render module class.
   *
   * Created on: Feb 2nd, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * Line chart render class.
   */
  export class LineChartRender {
      private chart;
      private engine;
      private style;
      private defaultOptions;
      constructor(parent: BaseApi);
      update(): this;
      updateSettings(options?: any): this;
      redraw(): this;
      updateXAxisRange(start: number, end: number): this;
      legendSelect(name: string, select?: boolean): this;
      legendToggle(name: string): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Line chart render engine module class.
   *
   * Created on: Feb 6th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * ECharts Options Typings. Needs to get ECharts Typings.
   */
  export interface ELineChartsOptions {
      [option: string]: any;
  }
  export class LineChartRenderEngine {
      private chart;
      private options;
      private timer;
      private timerTask;
      core: any;
      constructor(parent: BaseApi, options: ELineChartsOptions);
      /**
       * refine user input chart options for echarts.
       * @param  {ELineChartsOptions} options [description]
       * @return {any}                    [description]
       */
      private refineUserOptions(options);
      /**
       * Init the core object of echarts. And binding the resize event with window.
       * @return {this} [description]
       */
      private init();
      /**
       * init the formatter base on options. It needs to be called before updateSetting.
       * @return {this} [description]
       */
      private initFormatter();
      /**
       * init Events binding.
       * @return {this} [description]
       */
      private initEvents();
      /**
       * Redraw the line chart. It will create a new core instance.
       * @return {this} [description]
       */
      redraw(): this;
      /**
       * Clear the line chart object.
       */
      clear(): this;
      /**
       * Update the basic settings. It will affect current cart directly.
       * @return {this} [description]
       */
      updateSettings(options?: ELineChartsOptions): this;
      /**
       * update x Axis Range and update the line chart.
       * @param  {number} start [description]
       * @param  {number} end   [description]
       * @return {this}         [description]
       */
      updateXAxisRange(start: number, end: number): this;
      /**
       * Unbinds event-handling function.
       * @param  {string} eventName        [description]
       * @param  {any}    callbackInstance [description]
       * @return {this}                    [description]
       */
      offEvent(eventName: string, callbackInstance: any): this;
      /**
       * Binds event-handling function.
       * @param  {string} eventname        [description]
       * @param  {any}    callbackInstance [description]
       * @return {this}                    [description]
       */
      onEvent(eventname: string, callbackInstance: any): this;
      /**
       * Update the line chart rendering which includes all the changes through echarts.setOptions.
       * @return {this} [description]
       */
      update(): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Line chart style module class.
   *
   * Created on: Feb 2nd, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class LineChartStyle extends BaseStyle {
      private chart;
      private defaultLineChartStyle;
      constructor(parent: BaseApi);
      getStyleOptions(): any;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Pie Chart Class
   *
   * Created on: Feb 17th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface PieChartSetting extends ChartSetting {
      name?: string;
      formatter?: {
          /**
           * itemName for data item name, dataValue for data value, percentage for percentage. color for the item color.
           * @type {[type]}
           */
          tooltip?: {
              (itemName?: string, dataValue?: number, percentage?: number, color?: string): string;
          };
          label?: {
              (itemName?: string, dataValue?: number, percentage?: number, color?: string): string;
          };
      };
  }
  export interface PieChartDataPoint {
      name: string;
      value: number;
  }
  export class PieChart extends BaseApi {
      render: PieChartRender;
      event: PieChartEvent;
      data: PieChartDataPoint[];
      options: PieChartSetting;
      /**
       * Creates an instance of PieChart.
       *
       * @example Create an instance of PieChart by using a customized configuration.
       * <pre>
       *  window.piechart = new gvis.PieChart({
       *    name: 'Something',
       *    render: {
       *      container: document.getElementById("piechart1"),
       *      legend: {
       *        show: true
       *      },
       *      textStyle: {
       *        fontStyle: 'italic',
       *        fontWeight: 'bolder',
       *        fontFamily: '"Courier New", Courier, monospace',
       *        fontSize: 12,
       *      }
       *    },
       *    formatter:{
       *      tooltip: function(itemName, dataValue, percentage, color) {
       *        return `&lt;span style="color: ${color}"&gt;[${itemName}] : ${dataValue} (${percentage.toFixed(0)}%)&lt;/span&gt;`;
       *      }
       *    }
       *  });
       *
       *  var piechart = window.piechart;
       * </pre>
       *
       * @param {PieChartSetting} options
       * @memberof PieChart
       */
      constructor(options: PieChartSetting);
      /**
       * Add data pints in the piechart.
       *
       * @example add data in piechart.
       * <pre>
       *  window.testData = [];
       *  testData2 = [];
       *  var testN = 5;
       *
       *  for (var i=1; i&lt;testN; i++) {
       *
       *      testData.push({
       *        name: 'Part ' + i,
       *        value: +(testN + 2 * i * +Math.random()).toFixed(1)
       *      });
         *
       *      testData2.push({
       *        name: 'Usage ' + i,
       *        value: +(testN * Math.random() * Math.random()).toFixed(1)
       *      });
         *
       *      // testData.push({
       *      //   name: 'Part ' + i,
       *      //   value: +(testN + 2 * i * +Math.random()).toFixed(1)
       *      // });
       *  }
       *
       *  piechart
       *  .addData(testData)
       *  .update();
       *
       * // Dynamicly add data in piechart.
       *  setInterval(() =&gt; {
       *    var tmp = [];
       *
       *    tmp.push({
       *        series: 'Count',
       *        name: 'Part ' + testN,
       *        value: +(testN + 2 * i * +Math.random()).toFixed(1)
       *      });
       *
       *    piechart
       *    .addData(tmp)
       *    .update();
       *
       *    testN += 1;
       *
       *    if (testN &gt;= 10) {
       *      clearInterval(window.pietmp);
       *    }
       *  }, 1000);
       * </pre>
       *
       * @param {PieChartDataPoint[]} data
       * @returns {this}
       * @memberof PieChart
       */
      addData(data: PieChartDataPoint[]): this;
      /**
       * reload the data in piechart. The previous data will be deleted from piechart.
       *
       * @example reload data in piechart.
       *
       * <pre>
       *  piechart.reload(testData).update();
       * </pre>
       *
       * @param {PieChartDataPoint[]} data
       * @returns {this}
       * @memberof PieChart
       */
      reloadData(data: PieChartDataPoint[]): this;
      /**
       * render the updates of piechart.
       *
       * @example update the piechart
       * <pre>
       *  piechart.update();
       * </pre>
       * @returns {this}
       * @memberof PieChart
       */
      update(): this;
      /**
       * binding event callback by name.
       *
       * @example Binding pieselectchanged event
       * <pre>
       *  piechart.on('pieselectchanged', (data, chart) =&gt; {
       *    console.log('Changed information : ', data);
       *    console.assert(chart === piechart);
       *  })
       *
       * </pre>
       * @param {string}   eventName [description]
       * Currently supported events:
       *  - `pieselectchanged`
       *
       * @param {string}} callback  [description]
       */
      on(eventName: string, callback: {
          (event: {
              type: string;
              seriesId: string;
              name: string;
              selected: {
                  [itemName: string]: boolean;
              };
          }, chart: BaseApi): string;
      }): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: Pie chart event module class.
   *
   * Created on: Feb 17th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export interface PieChartEventHandlers {
      pieselectchanged: {
          (event: {
              type: string;
              seriesId: string;
              name: string;
              selected: {
                  [itemName: string]: boolean;
              };
          }, chart: BaseApi): string;
      };
      [eventName: string]: {
          (...args: any[]): void;
      };
  }
  export class PieChartEvent {
      private chart;
      private defaultHandlers;
      private handlers;
      constructor(parent: BaseApi);
      getEventsName(): string[];
      /**
       * get event callback instance by event Name;
       * @param {BaseApi} eventName [description]
       */
      getEventHandler(eventName: string): {
          (...args: any[]): void;
      };
      setEventHandler(eventName: string, callback: {
          (...args: any[]): void;
      }): this;
  }
  
  /**
   * ECharts Options Typings. Needs to get ECharts Typings.
   */
  export interface EPieChartsOptions {
      [option: string]: any;
  }
  export class PieChartRender {
      private chart;
      private engine;
      private style;
      private timer;
      private timerTask;
      private options;
      constructor(parent: BaseApi);
      private init();
      /**
       * init Events binding.
       * @return {this} [description]
       */
      private initEvents();
      private initFormatter();
      /**
       * Update the basic settings. It will affect current cart directly.
       * @return {this} [description]
       */
      updateSettings(options?: EPieChartsOptions): this;
      private refineUserOptions(options);
      update(): this;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: pie chart style module class.
   *
   * Created on: Feb 17th, 2017
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  export class PieChartStyle extends BaseStyle {
      private chart;
      private defaultPieChartStyle;
      dataConfig: {
          [keys: string]: any;
      };
      constructor(parent: BaseApi);
      getStyleOptions(): any;
  }
  
  /**
   * Color class for GVIS.
   */
  export class Color {
      private static c10;
      private static c10a;
      private static c10b;
      private static c10c;
      private static c10d;
      private static c10e;
      private static c20;
      private static c30;
      private static c40;
      private static c50;
      private static c60;
      /**
       * current color schema color string.
       * @type {string}
       */
      private colorSchema;
      /**
       * color array which is parsed from colorSchema.
       * @type {string[]}
       */
      private colorArray;
      /**
       * key is the type, it will be mapped to a index value or a color string.
       * Index will be used to get the color string in colorArray, by colorArray[index % colorArray.length];
       * Color string will be used as color.
       */
      private colorMap;
      private parseColorSchema(schema);
      constructor(theme?: string);
      /**
       * Initialize color for a list of types.
       * @param  {string[]} types the list of types, example: ['Company', 'Car', 'Member']
       * @return {this}           [description]
       */
      initialization(types: string[]): this;
      /**
       * get color for a type. Dunamically update the colorMap while getting color.
       * @type  {string} id for the color.
       * @return {string} color string.
       */
      getColor(type: string): string;
      /**
       * Customize a color for a type, it will override the index color.
       * @type  {string}
       * @color  {string}
       * @return {this}
       */
      setColor(type: string, color: string): this;
      /**
       * Change the color shcema theme. It will change color schema and color array after set a acceptable theme.
       * @theme  {string}  Names of schema: 'c10', 'c10a', 'c10b', 'c10c', 'c10d', 'c10e', 'c20', 'c30', 'c40', 'c50', 'c60'
       * @return {this}
       */
      setColorTheme(theme: string): this;
      /**
       * Change the color schema for the color Array.
       * @schema  {string}
       * @return {this}
       */
      setColorSchema(schema: string): this;
      /**
       * Reset the color map object. Does not change the color schema or color array.
       * @return {this}
       */
      reset(): this;
      /**
       * use to generate random RGB color string. The random color could be duplicated.
       * @return {string} random RGB color string.
       */
      randomColor(): string;
      /**
       * use to return the privagte memeber color array. It will retrun an array contains currently used colors.
       * @return {string[]} [description]
       */
      getColorArray(): string[];
  }
  
  /**
   * Exception class for handler all the exception in GVIS library.
   * function GExceptionExample() {
   *   try {
   *     throw new GException(1000, 'customized error message');
   *   } catch (e) {
   *     console.log(r.toString);
   *   }
   * }
   */
  export class GException {
      /**
       * Exception status
       * @type {number}
       */
      status: number;
      /**
       * Exception customized message. By default is empty.
       * @type {string}
       */
      body: string;
      /**
       * Current error type which is defiend in ErrorStatus object. A status shows undefined if one status is not defined
       * @type {string}
       */
      errorType: string;
      /**
       * Error message which contains all the error information. Use this field for console.error output.
       * @type {string}
       */
      error: string;
      /**
       * @param {number} exception status code
       * @param {string} customized message.
       */
      constructor(status: number, body?: string);
      toString(): string;
  }
  
  export const builtinIcons: {
      [iconName: string]: any;
  };
  export const otherIcons: {
      [iconName: string]: {
          fileName: string;
          size?: [number, number];
          padding?: [number, number, number, number];
      };
  };
  /**
   * ICON class for gvis visualization icon framework.
   */
  export class ICON {
      private icons;
      baseURL: string;
      constructor(baseURL?: string);
      /**
       * add icon for visualization..
       * @param  {[type]}  iconName  The name of the icon which will be used in node GraphStyle.base.icon
       * @param  {[type]}  fileName  icon image name.
       * @param  {[number, number, number, number]} padding [top, right, buttom, left] percentage of the icon which will be shown. default value is [0.2, 0.2, 0.2, 0.2]
       * @param  {[number, number]} size  [width, height] of the icon image in pixel. Default value is [512, 512].
       * @param  {[boolean]}  overwrite Determiner whether or not overwirte existed icon in the object. Defualt is false
       * @return {this}              [description]
       */
      addIcon(iconName: string, fileName: string, padding?: [number, number, number, number], size?: [number, number], overwrite?: boolean): this;
      /**
       * Get Icon by iconName. If icon name does not exist, return {}.
       * @param {[type]} iconName [description]
       */
      getIcon(iconName: string): {
          image: string;
          /** @type {[type]} [top, right, buttom, left] */
          padding: [number, number, number, number];
          /** @type {[type]} [left, top, width, height]*/
          imageSlicing: [number, number, number, number];
      };
  }
  
  /**
   * Localization module for gvis library.
   */
  export class Localization {
      private localization;
      private selectedLanguage;
      constructor(language: {
          selectedLanguage?: string;
          localization?: {
              [language: string]: {
                  vertex?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
                  edge?: {
                      [type: string]: string | {
                          type?: string;
                          attrs: {
                              [attribute: string]: string | {
                                  attr?: string;
                                  values: {
                                      [value: string]: string;
                                  };
                              };
                          };
                      };
                  };
              };
          };
      });
      /**
       * Set current selected language. if the selectedLanguage is not found, selectedLanguage set to be undefined.
       * @param  {[type]} selectedLanguage  existed language name.
       * @return {this}                    [description]
       */
      setSelectedLanguage(selectedLanguage: string): this;
      /**
       * get the vertex translation base on the input values.
       * Translate type, if only type is inputed. Translate attribute, if type and attribute are inputed. Translate value, if all arguments are inputed.
       * It will translate the value for specific input based on the vertex type and attribute.
       * @param  {string} type      Vertex type
       * @param  {string} attribute Vertex attribute
       * @param  {string} value     Vertex value
       * @return {string}           the translated value
       */
      getVertexString(type: string, attribute?: string, value?: string): string;
      /**
       * get the edge translation base on the input values.
       * Translate type, if only type is inputed. Translate attribute, if type and attribute are inputed. Translate value, if all arguments are inputed.
       * It will translate the value for specific input based on the edge type and attribute.
       * @param  {string} type      Edge type
       * @param  {string} attribute Edge attribute
       * @param  {string} value     Edge value
       * @return {string}           the translated value
       */
      getEdgeString(type: string, attribute?: string, value?: string): string;
      translateOneNode(node: GraphChartDataNode): any;
      translateOneLink(link: GraphChartDataLink): any;
  }
  
  /******************************************************************************
   * Copyright (c) 2017, TigerGraph Inc.
   * All rights reserved.
   * Project: GVIS
   * Brief: GVIS library External Graph Parser Module.
   *
   * Created on: Oct 27th, 2016
   *      Author: Xiaoke Huang
   ******************************************************************************/
  
  /**
   * parser format standard class.
   * @type {[type]}
   */
  export class GraphParserFormat {
      gquery: {
          isNode: {
              (obj: any): boolean;
          };
          isLink: {
              (obj: any): boolean;
          };
          parseNode: {
              (obj: any): ExternalNode;
          };
          parseLink: {
              (obj: any): ExternalLink | ExternalLink[];
          };
      };
      pta: {
          isNode: {
              (obj: any): boolean;
          };
          isLink: {
              (obj: any): boolean;
          };
          parseNode: {
              (obj: any): ExternalNode;
          };
          parseLink: {
              (obj: any): ExternalLink | ExternalLink[];
          };
      };
      ksubgraph: {
          isNode: {
              (obj: any): boolean;
          };
          isLink: {
              (obj: any): boolean;
          };
          parseNode: {
              (obj: any): ExternalNode;
          };
          parseLink: {
              (obj: any): ExternalLink;
          };
      };
      gpr: {
          isNode: {
              (obj: any): boolean;
          };
          isLink: {
              (obj: any): boolean;
          };
          parseNode: {
              (obj: any): ExternalNode;
          };
          parseLink: {
              (obj: any): ExternalLink;
          };
      };
      restpp: {
          isNode: {
              (obj: any): boolean;
          };
          isLink: {
              (obj: any): boolean;
          };
          parseNode: {
              (obj: any): ExternalNode;
          };
          parseLink: {
              (obj: any): ExternalLink;
          };
      };
      gst: {
          isNode: {
              (obj: any): boolean;
          };
          isLink: {
              (obj: any): boolean;
          };
          parseNode: {
              (obj: any): ExternalNode;
          };
          parseLink: {
              (obj: any): ExternalLink;
          };
      };
      constructor();
      private initGQueryFormat();
      private initPTAFormat();
      private initKSubGraphFormat();
      private initGPRFormat();
      private initRestppFormat();
      private initGSTFormat();
      /**
       * convert gpe topology attribute standard output to json object.
       * @example
       * <pre>
       * 'type:2|od_et4:2|od_et5:1|name(0):site12|formula_order(0):2|useAmount(1):6000.000000|' =>
       *  { name: 'site12', 'formula_order': '2', 'useAmount': '6000.000000'}
       * Split by '|', then for each item parse /(.*)\(\d*\):(.*)$/. In this case, 'type', 'od_et4', 'od_et5' are not parsed.
       * </pre>
       * @param {string} attrString [description]
       */
      private gpeTopologyAttributesOutputToJSON(attrString);
  }
  /**
   * Graph parser class.
   */
  export class GraphParser {
      chart: BaseApi;
      parsers: {
          [parserName: string]: {
              (externalData: any): ExternalGraph;
          };
      };
      private parserFormat;
      constructor(parent: BaseApi);
      /**
       * recursively parsing obj to get external graph elements, such as nodes and links. Then add external graph elements in to targetGraph base on the format setting.
       * @param {any}             obj         raw input. It is an random json object, which may contains graph elements.
       * @param {ExternalGraph}   targetGraph The output graph. It is the graph in visualization format.
       * @param {ExternalLink };                       }} format  This is the format object which contians 4 functions:
       * isNode: { (obj: any): boolean }; Check if an obj is a node.
       * isLink: { (obj: any): boolean }; Check if an obj is a link.
       * parseNode: { (obj: any): ExternalNode }; Parser function for a node.
       * parseLink: { (obj: any): ExternalLink }; Parser function for a link.
       */
      private recursivelyGetExternalElement(obj, targetGraph, format);
      /**
       * Add a parser function by name. Then we can use it by getParser('parserName');
       * @param  {string}           parserName [description]
       * @param  {ExternalGraph }}       callback      [description]
       * @return {this}                        [description]
       */
      addParser(parserName: string, callback: {
          (externalData: any): ExternalGraph;
      }): this;
      /**
       * Update a parser function by name. Then we use the updated parser function by getParser('parserName');
       * @param  {string}           parserName [description]
       * @param  {ExternalGraph }}       callback      [description]
       * @return {this}                        [description]
       */
      updateParser(parserName: string, callback: {
          (externalData: any): ExternalGraph;
      }): this;
      /**
       * Get the target parser function by name.
       * @param  {any}           parserName [description]
       * @return {ExternalGraph}            [description]
       */
      getParser(parserName: string): {
          (externalData: any): ExternalGraph;
      };
  }
  /**
   * Array parser class. Need to be implemented for LineChart, once we have time.
   */
  export class ArrayParser {
  }
  
  /**
   * GVIS library Utility Module.
   */
  export module Utils {
      /**
       * Download a uri in to a file.
       * @param {[type]} uri  [description]
       * @param {[type]} name [description]
       */
      function downloadURI(uri: string, name: string): void;
      /**
       * Convert GRB hexColor '#ffffff'code and alpha 1  to 'rgba(255, 255, 255, 1)'.
       * @param  {string} hexColor hex color code.
       * @param  {[type]} alpha The opacity of the color.
       * @return {string}          The converted color object string for canvas rendering engine.
       */
      function rgbToRgba(hexColor: string, alpha?: string): string;
      /**
       * Calcualte coordinates for a point after rotate certain degree.
       * @cx {number} rotate origin x
       * @cy {number} rotate origin y
       * @x {number} x of point
       * @y {number} y of point
       * @degree {number} degree of the rotation, range: [-180, 180]
       */
      function rotate(cx: number, cy: number, x: number, y: number, degree: number): [number, number];
      /**
       * Performs a deep comparison between two values to determine if they are
       * equivalent.
       *
       * **Note:** This method supports comparing arrays, array buffers, booleans,
       * date objects, error objects, maps, numbers, `Object` objects, regexes,
       * sets, strings, symbols, and typed arrays. `Object` objects are compared
       * by their own, not inherited, enumerable properties. Functions and DOM
       * nodes are **not** supported.
       *
       * @static
       * @memberOf _
       * @category Lang
       * @param {*} value The value to compare.
       * @param {*} other The other value to compare.
       * @returns {boolean} Returns `true` if the values are equivalent, else `false`.
       * @example
       *
       * var object = { 'user': 'fred' };
       * var other = { 'user': 'fred' };
       *
       * _.isEqual(object, other);
       * // => true
       *
       * object === other;
       * // => false
       */
      function isEqual(value: Object, other: Object): boolean;
      /**
       * This method is like `_.clone` except that it recursively clones `value`.
       *
       * @static
       * @memberOf _
       * @category Lang
       * @param {*} value The value to recursively clone.
       * @returns {*} Returns the deep cloned value.
       * @example
       *
       * var objects = [{ 'a': 1 }, { 'b': 2 }];
       *
       * var deep = _.cloneDeep(objects);
       * console.log(deep[0] === objects[0]);
       * // => false
       */
      function cloneDeep<T>(obj: T): T;
      /**
       * Creates a shallow clone of `value`.
       *
       * **Note:** This method is loosely based on the
       * [structured clone algorithm](https://mdn.io/Structured_clone_algorithm)
       * and supports cloning arrays, array buffers, booleans, date objects, maps,
       * numbers, `Object` objects, regexes, sets, strings, symbols, and typed
       * arrays. The own enumerable properties of `arguments` objects are cloned
       * as plain objects. An empty object is returned for uncloneable values such
       * as error objects, functions, DOM nodes, and WeakMaps.
       *
       * @static
       * @memberOf _
       * @category Lang
       * @param {*} value The value to clone.
       * @returns {*} Returns the cloned value.
       * @example
       *
       * var objects = [{ 'a': 1 }, { 'b': 2 }];
       *
       * var shallow = _.clone(objects);
       * console.log(shallow[0] === objects[0]);
       * // => true
       */
      function clone<T>(obj: T): T;
      /**
       * Creates a new array concatenating `array` with any additional arrays
       * and/or values.
       *
       * @static
       * @memberOf _
       * @category Array
       * @param {Array} array The array to concatenate.
       * @param {...*} [values] The values to concatenate.
       * @returns {Array} Returns the new concatenated array.
       * @example
       *
       * var array = [1];
       * var other = _.concat(array, 2, [3], [[4]]);
       *
       * console.log(other);
       * // => [1, 2, 3, [4]]
       *
       * console.log(array);
       * // => [1]
       */
      function concat<T>(...values: (T | T[])[]): T[];
      /**
       * Assigns own enumerable properties of source object(s) to the destination object for all destination
       * properties that resolve to undefined. Once a property is set, additional values of the same property are
       * ignored.
       *
       * Note: This method mutates object.
       *
       * @param object The destination object.
       * @param sources The source objects.
       * @return The destination object.
       */
      function defaults<T>(target: T, ...sources: any[]): any;
      /**
       * This method is like _.defaults except that it recursively assigns default properties.
       * @param object The destination object.
       * @param sources The source objects.
       * @return Returns object.
       */
      function defaultsDeep<T>(target: T, ...sources: any[]): any;
      /**
       * Recursively merges own and inherited enumerable properties of source
       * objects into the destination object, skipping source properties that resolve
       * to `undefined`. Array and plain object properties are merged recursively.
       * Other objects and value types are overridden by assignment. Source objects
       * are applied from left to right. Subsequent sources overwrite property
       * assignments of previous sources.
       *
       * **Note:** This method mutates `object`.
       *
       * @static
       * @memberOf _
       * @category Object
       * @param {Object} object The destination object.
       * @param {...Object} [sources] The source objects.
       * @returns {Object} Returns `object`.
       * @example
       *
       * var users = {
       *   'data': [{ 'user': 'barney' }, { 'user': 'fred' }]
       * };
       *
       * var ages = {
       *   'data': [{ 'age': 36 }, { 'age': 40 }]
       * };
       *
       * _.merge(users, ages);
       * // => { 'data': [{ 'user': 'barney', 'age': 36 }, { 'user': 'fred', 'age': 40 }] }
       */
      function extend<T>(destiation: T, ...sources: any[]): any;
      /**
       * assign handles unassigned but the others will skip it.
       * This method is like `_.assign` except that it iterates over own and
       * inherited source properties.
       *
       * **Note:** This method mutates `object`.
       *
       * @static
       * @memberOf _
       * @alias extend
       * @category Object
       * @param {Object} object The destination object.
       * @param {...Object} [sources] The source objects.
       * @returns {Object} Returns `object`.
       * @example
       *
       * function Foo() {
       *   this.b = 2;
       * }
       *
       * function Bar() {
       *   this.d = 4;
       * }
       *
       * Foo.prototype.c = 3;
       * Bar.prototype.e = 5;
       *
       * _.assignIn({ 'a': 1 }, new Foo, new Bar);
       * // => { 'a': 1, 'b': 2, 'c': 3, 'd': 4, 'e': 5 }
       */
      function assignIn<T>(destiation: T, ...source: any[]): any;
      /**
       * Removes elements from array corresponding to the given indexes and returns an array of the removed elements.
       * Indexes may be specified as an array of indexes or as individual arguments.
       *
       * Note: Unlike _.at, this method mutates array.
       *
       * @param array The array to modify.
       * @param indexes The indexes of elements to remove, specified as individual indexes or arrays of indexes.
       * @return Returns the new array of removed elements.
       */
      function pullAt<T>(array: T[], indexes: number | number[]): T[];
      /**
       * Gets a random element from `collection`.
       *
       * @static
       * @memberOf _
       * @category Collection
       * @param {Array|Object} collection The collection to sample.
       * @returns {*} Returns the random element.
       * @example
       *
       * _.sample([1, 2, 3, 4]);
       * // => 2
       */
      function sample<T>(collection: T[], index?: number): any;
      function loadCSSIfNotAlreadyLoaded(pathToCSS: string): void;
      /**
       * Insert a new data in a sorted array in O(Nlog(N)) time base on sort callback funtion.
       * @type {T}
       */
      function binaryInsert<T>(data: T, array: T[], callback: (a: T, b: T) => number, startVal?: number, endVal?: number): void;
  }
  
}